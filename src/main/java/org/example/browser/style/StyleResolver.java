package org.example.browser.style;

import org.example.browser.css.*;
import org.example.browser.dom.*;

import java.util.*;

public class StyleResolver {
    private final List<CssRule> rules;
    private static final Set<String> INHERITABLE = Set.of("color", "font-size", "font-family");

    public StyleResolver(List<CssRule> rules) {
        this.rules = rules;
    }

    /**
     * Walks the entire DOM tree and computes the final CSS style for every element.
     * The computed style is attached directly to each ElementNode.
     *
     * @param root        the root DOM node to start from
     * @param parentStyle the computed style of the parent element (null for the root)
     */
    public void resolveTree(DomNode root, Map<String, String> parentStyle) {
        if (root.isElement()) {
            ElementNode el = (ElementNode) root;
            // 1. Start with the browser's default stylesheet
            Map<String, String> style = new HashMap<>(getDefaultStyle());

            // 2. Inherit properties from the parent
            if (parentStyle != null) {
                for (String prop : INHERITABLE) {
                    if (parentStyle.containsKey(prop)) {
                        style.put(prop, parentStyle.get(prop));
                    }
                }
            }

            // 3. Apply all matching CSS rules (sorted by specificity)
            List<CssRule> matchedRules = matchRules(el);
            matchedRules.sort(Comparator.comparingInt(r -> r.selectors.get(0).specificity()));
            for (CssRule rule : matchedRules) {
                style.putAll(rule.declarations);
            }

            // 4. Inline styles (style attribute) override everything else
            String inline = el.getAttributes().get("style");
            if (inline != null) {
                for (String decl : inline.split(";")) {
                    int colon = decl.indexOf(':');
                    if (colon != -1) {
                        String prop = decl.substring(0, colon).trim().toLowerCase();
                        String val = decl.substring(colon + 1).trim();
                        style.put(prop, val);
                    }
                }
            }

            el.getComputedStyle(style);
        }

        // Recursively resolve children, passing the current element's style for inheritance
        for (DomNode child : root.getChildren()) {
            resolveTree(child, root.isElement() ? ((ElementNode) root).getComputedStyle() : parentStyle);
        }
    }

    /**
     * Returns all CSS rules whose at least one selector matches the given element.
     */
    private List<CssRule> matchRules(ElementNode el) {
        List<CssRule> matched = new ArrayList<>();
        for (CssRule rule : rules) {
            for (CssSelector sel : rule.selectors) {
                if (matches(el, sel)) {
                    matched.add(rule);
                    break; // a rule matches if any of its comma‑separated selectors matches
                }
            }
        }
        return matched;
    }

    /**
     * Checks whether a single selector matches the given element.
     */
    private boolean matches(ElementNode el, CssSelector sel) {
        // tag selector
        if (sel.tag != null && !sel.tag.equals(el.getNodeName())) return false;
        // id selector
        if (sel.id != null && !sel.id.equals(el.getAttributes().get("id"))) return false;
        // class selector(s)
        if (sel.classes != null) {
            String classAttr = el.getAttributes().get("class");
            if (classAttr == null) return false;
            Set<String> elClasses = new HashSet<>(Arrays.asList(classAttr.split("\\s+")));
            for (String cls : sel.classes) {
                if (!elClasses.contains(cls)) return false;
            }
        }
        return true;
    }

    /**
     * Provides the default style for every element (like a user‑agent stylesheet).
     * Real browsers have many more defaults, but this covers the basics.
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
}