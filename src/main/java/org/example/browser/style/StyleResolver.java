package org.example.browser.style;

import org.example.browser.css.*;
import org.example.browser.dom.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StyleResolver {
    private final List<CssRule> rules;
    private static final Set<String> INHERITABLE = Set.of("color", "font-size", "font-family");

    // Parsed but never matching: this engine has no interaction state.
    private static final Set<String> UNSUPPORTED_PSEUDO =
            Set.of("hover", "active", "focus", "visited", "link");

    private static final Pattern NTH_PATTERN = Pattern.compile("([+-]?\\d*)n([+-]\\d+)?");

    public StyleResolver(List<CssRule> rules) {
        this.rules = rules;
    }

    /**
     * Walks the entire DOM tree and computes the final CSS style for every element.
     * The computed style is attached directly to each ElementNode.
     *
     * @param root   the root DOM node to start from
     * @param parent the parent element (null for the root)
     */
    public void resolveTree(DomNode root, ElementNode parent) {
        if (root.isElement()) {
            ElementNode el = (ElementNode) root;

            // 1. Browser defaults
            Map<String, String> style = new HashMap<>(getDefaultStyle());

            // 2. Inherit properties from the parent
            if (parent != null && parent.getComputedStyle() != null) {
                for (String prop : INHERITABLE) {
                    if (parent.getComputedStyle().containsKey(prop)) {
                        style.put(prop, parent.getComputedStyle().get(prop));
                    }
                }
            }

            // 3. The cascade: stylesheets + inline, normal + !important
            applyCascade(el, style);

            el.getComputedStyle(style);
        }

        // Recurse into children, passing the current element for inheritance/sibling context
        for (DomNode child : root.getChildren()) {
            resolveTree(child, root.isElement() ? (ElementNode) root : parent);
        }
    }

    /**
     * Computes the winning declaration for every property and merges it into style.
     * Priority (per the CSS cascade):
     *   normal rules (by specificity, then source order)
     *   < normal inline style
     *   < !important rules (by specificity, then source order)
     *   < !important inline style
     */
    private void applyCascade(ElementNode el, Map<String, String> style) {
        List<MatchedRule> matched = matchRules(el);

        // stable sort: ascending specificity, source order breaks ties
        matched.sort(Comparator.comparingInt(m -> m.specificity));

        Map<String, CssDeclaration> applied = new LinkedHashMap<>();

        // pass 1: normal stylesheet declarations
        for (MatchedRule m : matched) {
            for (Map.Entry<String, CssDeclaration> e : m.rule.declarations.entrySet()) {
                if (!e.getValue().important) applied.put(e.getKey(), e.getValue());
            }
        }

        Map<String, CssDeclaration> inline = parseInlineStyle(el);

        // pass 2: normal inline style overrides the stylesheet
        for (Map.Entry<String, CssDeclaration> e : inline.entrySet()) {
            if (!e.getValue().important) applied.put(e.getKey(), e.getValue());
        }

        // pass 3: !important stylesheet declarations beat everything normal
        for (MatchedRule m : matched) {
            for (Map.Entry<String, CssDeclaration> e : m.rule.declarations.entrySet()) {
                if (e.getValue().important) applied.put(e.getKey(), e.getValue());
            }
        }

        // pass 4: inline !important wins over everything
        for (Map.Entry<String, CssDeclaration> e : inline.entrySet()) {
            if (e.getValue().important) applied.put(e.getKey(), e.getValue());
        }

        for (Map.Entry<String, CssDeclaration> e : applied.entrySet()) {
            style.put(e.getKey(), e.getValue().value);
        }
    }

    // Returns all matching rules (with their highest matching specificity) in source order.
    private List<MatchedRule> matchRules(ElementNode el) {
        List<MatchedRule> matched = new ArrayList<>();
        for (CssRule rule : rules) {
            int best = -1;
            for (CssSelector sel : rule.selectors) {
                if (selectorMatches(sel, el)) {
                    int spec = specKey(sel);
                    if (spec > best) best = spec;
                }
            }
            if (best != -1) matched.add(new MatchedRule(rule, best));
        }
        return matched;
    }

    // The right-most part of the selector is the subject; the rest is context.
    private boolean selectorMatches(CssSelector sel, ElementNode el) {
        List<CssSelector.Part> parts = sel.parts;
        int last = parts.size() - 1;
        if (!compoundMatches(parts.get(last).compound, el, el.getParent())) return false;
        return matchChain(parts, last - 1, el);
    }

    /**
     * Recursively checks the parts to the left of the already-matched part at
     * (index + 1). The combinator on part (index + 1) tells us how the element
     * matched by part (index + 1) relates to the element part (index) must match.
     */
    private boolean matchChain(List<CssSelector.Part> parts, int index, ElementNode current) {
        if (index < 0) return true;
        CssSelector.Part part = parts.get(index);
        String comb = parts.get(index + 1).combinator;

        switch (comb) {
            case ">": // child combinator: the direct parent must match
                ElementNode parent = parentElement(current);
                if (parent == null) return false;
                return compoundMatches(part.compound, parent, parent.getParent())
                        && matchChain(parts, index - 1, parent);

            case " ": // descendant combinator: any ancestor must match
                for (DomNode anc = current.getParent(); anc != null; anc = anc.getParent()) {
                    if (anc.isElement()) {
                        ElementNode ae = (ElementNode) anc;
                        if (compoundMatches(part.compound, ae, ae.getParent())
                                && matchChain(parts, index - 1, ae)) {
                            return true;
                        }
                    }
                }
                return false;

            case "+": // adjacent sibling: the immediately preceding element sibling
                ElementNode prev = previousElementSibling(current);
                if (prev == null) return false;
                return compoundMatches(part.compound, prev, prev.getParent())
                        && matchChain(parts, index - 1, prev);

            case "~": // general sibling: any preceding element sibling
                for (ElementNode sib : previousElementSiblings(current)) {
                    if (compoundMatches(part.compound, sib, sib.getParent())
                            && matchChain(parts, index - 1, sib)) {
                        return true;
                    }
                }
                return false;

            default:
                return false;
        }
    }

    private boolean compoundMatches(SimpleSelector s, ElementNode el, DomNode parent) {
        if (s.tag != null && !s.tag.equals(el.getNodeName())) return false;
        if (s.id != null && !s.id.equals(el.getAttributes().get("id"))) return false;
        if (!s.classes.isEmpty()) {
            String classAttr = el.getAttributes().get("class");
            if (classAttr == null) return false;
            Set<String> elClasses = new HashSet<>(Arrays.asList(classAttr.split("\\s+")));
            for (String cls : s.classes) {
                if (!elClasses.contains(cls)) return false;
            }
        }
        for (String pseudo : s.pseudoClasses) {
            if (!pseudoMatches(pseudo, el, parent)) return false;
        }
        return true;
    }

    private boolean pseudoMatches(String pseudo, ElementNode el, DomNode parent) {
        if (UNSUPPORTED_PSEUDO.contains(pseudo)) return false;

        switch (pseudo) {
            case "first-child":
                return previousElementSibling(el) == null;
            case "last-child":
                return nextElementSibling(el) == null;
            case "only-child":
                return previousElementSibling(el) == null && nextElementSibling(el) == null;
            case "first-of-type":
                return previousElementSiblingOfType(el) == null;
            case "last-of-type":
                return nextElementSiblingOfType(el) == null;
            default:
                break;
        }
        if (pseudo.startsWith("nth-child(")) {
            String expr = pseudo.substring("nth-child(".length(), pseudo.length() - 1);
            return matchNth(expr, elementIndex(el));
        }
        if (pseudo.startsWith("nth-of-type(")) {
            String expr = pseudo.substring("nth-of-type(".length(), pseudo.length() - 1);
            return matchNth(expr, elementIndexOfType(el));
        }
        if (pseudo.startsWith("not(")) {
            SimpleSelector arg = SimpleCssParser.parseCompound(pseudo.substring(4, pseudo.length() - 1));
            return !compoundMatches(arg, el, parent);
        }
        return false; // unknown pseudo-classes never match
    }

    // Matches the An+B syntax: 3, odd, even, 2n+1, -n+3, ...
    private boolean matchNth(String expr, int idx) {
        expr = expr.trim().replace(" ", "");
        if (expr.equalsIgnoreCase("odd")) expr = "2n+1";
        else if (expr.equalsIgnoreCase("even")) expr = "2n";

        if (expr.matches("-?\\d+")) {
            return idx == Integer.parseInt(expr);
        }

        Matcher m = NTH_PATTERN.matcher(expr);
        if (!m.matches()) return false;

        String aStr = m.group(1);
        int a = 1;
        if (aStr != null && !aStr.isEmpty() && !aStr.equals("+")) {
            a = aStr.equals("-") ? -1 : Integer.parseInt(aStr);
        }
        int b = 0;
        if (m.group(2) != null) b = Integer.parseInt(m.group(2));

        if (a == 0) return idx == b;
        if (a > 0) return idx >= b && (idx - b) % a == 0;
        return idx <= b && (b - idx) % (-a) == 0;
    }

    // ---- sibling / position helpers ---------------------------------------

    private ElementNode parentElement(ElementNode el) {
        DomNode p = el.getParent();
        return (p != null && p.isElement()) ? (ElementNode) p : null;
    }

    private ElementNode previousElementSibling(ElementNode el) {
        DomNode parent = el.getParent();
        if (parent == null) return null;
        ElementNode prev = null;
        for (DomNode child : parent.getChildren()) {
            if (child == el) break;
            if (child.isElement()) prev = (ElementNode) child;
        }
        return prev;
    }

    private ElementNode nextElementSibling(ElementNode el) {
        DomNode parent = el.getParent();
        if (parent == null) return null;
        boolean seen = false;
        for (DomNode child : parent.getChildren()) {
            if (child == el) { seen = true; continue; }
            if (seen && child.isElement()) return (ElementNode) child;
        }
        return null;
    }

    private List<ElementNode> previousElementSiblings(ElementNode el) {
        List<ElementNode> sibs = new ArrayList<>();
        DomNode parent = el.getParent();
        if (parent == null) return sibs;
        for (DomNode child : parent.getChildren()) {
            if (child == el) break;
            if (child.isElement()) sibs.add((ElementNode) child);
        }
        return sibs;
    }

    // 1-based index among element siblings
    private int elementIndex(ElementNode el) {
        int idx = 1;
        for (ElementNode s : previousElementSiblings(el)) idx++;
        return idx;
    }

    private ElementNode previousElementSiblingOfType(ElementNode el) {
        DomNode parent = el.getParent();
        if (parent == null) return null;
        ElementNode prev = null;
        for (DomNode child : parent.getChildren()) {
            if (child == el) break;
            if (child.isElement() && ((ElementNode) child).getNodeName().equals(el.getNodeName())) {
                prev = (ElementNode) child;
            }
        }
        return prev;
    }

    private ElementNode nextElementSiblingOfType(ElementNode el) {
        DomNode parent = el.getParent();
        if (parent == null) return null;
        boolean seen = false;
        for (DomNode child : parent.getChildren()) {
            if (child == el) { seen = true; continue; }
            if (seen && child.isElement() && ((ElementNode) child).getNodeName().equals(el.getNodeName())) {
                return (ElementNode) child;
            }
        }
        return null;
    }

    private int elementIndexOfType(ElementNode el) {
        int idx = 1;
        ElementNode prev = previousElementSiblingOfType(el);
        while (prev != null) {
            idx++;
            prev = previousElementSiblingOfType(prev);
        }
        return idx;
    }

    // Encodes the (ids, classes, types) triple into one sortable number.
    private int specKey(CssSelector sel) {
        int[] spec = sel.specificity();
        return spec[0] * 10000 + spec[1] * 100 + spec[2];
    }

    private Map<String, CssDeclaration> parseInlineStyle(ElementNode el) {
        Map<String, CssDeclaration> map = new LinkedHashMap<>();
        String inline = el.getAttributes().get("style");
        if (inline == null) return map;
        for (String decl : inline.split(";")) {
            int colon = decl.indexOf(':');
            if (colon == -1) continue;
            String prop = decl.substring(0, colon).trim().toLowerCase();
            String value = decl.substring(colon + 1).trim();
            boolean important = value.matches("(?i)^.*!important\\s*$");
            if (important) {
                value = value.replaceFirst("(?i)\\s*!important\\s*$", "").trim();
            }
            map.put(prop, new CssDeclaration(value, important));
        }
        return map;
    }

    /**
     * Provides the default style for every element (like a user-agent stylesheet).
     */
    private Map<String, String> getDefaultStyle() {
        Map<String, String> d = new HashMap<>();
        d.put("display", "inline");
        d.put("margin-top", "0"); d.put("margin-right", "0"); d.put("margin-bottom", "0"); d.put("margin-left", "0");
        d.put("padding-top", "0"); d.put("padding-right", "0"); d.put("padding-bottom", "0"); d.put("padding-left", "0");
        d.put("border-top-width", "0"); d.put("border-right-width", "0"); d.put("border-bottom-width", "0"); d.put("border-left-width", "0");
        d.put("color", "black");
        d.put("background-color", "transparent");
        d.put("width", "auto"); d.put("height", "auto");
        d.put("font-size", "16px"); d.put("font-family", "serif");
        return d;
    }

    // A matched rule together with the specificity of the selector that matched it.
    private static class MatchedRule {
        final CssRule rule;
        final int specificity;

        MatchedRule(CssRule rule, int specificity) {
            this.rule = rule;
            this.specificity = specificity;
        }
    }
}
