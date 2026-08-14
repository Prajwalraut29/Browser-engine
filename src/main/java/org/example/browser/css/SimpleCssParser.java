package org.example.browser.css;

import java.util.*;

public class SimpleCssParser {
    // Takes a raw CSS string and returns a list of parsed rules.
    public List<CssRule> parse(String css) {
        List<CssRule> rules = new ArrayList<>();
        int i = 0;
        while (i < css.length()) {
            i = skipWhitespaceAndComments(css, i); // ignore spaces and /* comments */
            if (i >= css.length())
                break;

            // Find the opening brace of the declaration block
            int braceIdx = css.indexOf('{', i);
            if (braceIdx == -1)
                break;

            String selectorStr = css.substring(i, braceIdx).trim();
            i = braceIdx + 1;

            // Find the matching closing brace
            int closeIdx = findMatchingBrace(css, i);
            if (closeIdx == -1)
                break;

            String declStr = css.substring(i, closeIdx);
            i = closeIdx + 1;

            // Parse the selectors and the declaration block
            List<CssSelector> selectors = parseSelectors(selectorStr);
            Map<String, CssDeclaration> declarations = parseDeclarations(declStr);
            rules.add(new CssRule(selectors, declarations));
        }
        return rules;
    }

    // Skips whitespace and CSS block comments ( /* ... */ )
    private int skipWhitespaceAndComments(String s, int i) {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '*') {
                int end = s.indexOf("*/", i + 2);
                if (end == -1)
                    return s.length();
                i = end + 2;
            } else {
                break;
            }
        }
        return i;
    }

    // Finds the closing } accounting for nested braces
    private int findMatchingBrace(String s, int start) {
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{')
                depth++;
            else if (c == '}') {
                if (depth == 0)
                    return i;
                depth--;
            }
        }
        return -1;
    }

    /**
     * Parses a comma-separated list of selectors into complex selectors.
     * Supports tag, #id, .class, * and pseudo-classes, joined by the
     * combinators ' ' (descendant), '>', '+' and '~'.
     */
    private List<CssSelector> parseSelectors(String selectorList) {
        List<CssSelector> result = new ArrayList<>();
        for (String sel : splitTopLevel(selectorList, ',')) {
            sel = sel.trim();
            if (sel.isEmpty())
                continue;

            List<CssSelector.Part> parts = new ArrayList<>();
            int i = 0;
            String pending = null;       // combinator waiting to be attached to the next compound
            SimpleSelector cur = new SimpleSelector();
            boolean hasCur = false;      // whether the current compound has any content yet

            while (i < sel.length()) {
                char c = sel.charAt(i);
                if (Character.isWhitespace(c)) {
                    // whitespace after a complete compound means "descendant"
                    if (hasCur) {
                        parts.add(new CssSelector.Part(pending == null ? "" : pending, cur));
                        cur = new SimpleSelector();
                        hasCur = false;
                        pending = " ";
                    }
                    i++;
                } else if (c == '>' || c == '+' || c == '~') {
                    if (hasCur) {
                        parts.add(new CssSelector.Part(pending == null ? "" : pending, cur));
                        cur = new SimpleSelector();
                        hasCur = false;
                    }
                    pending = String.valueOf(c);
                    i++;
                } else if (c == '#') {
                    hasCur = true;
                    i++;
                    StringBuilder id = new StringBuilder();
                    while (i < sel.length() && (Character.isLetterOrDigit(sel.charAt(i)) || sel.charAt(i) == '-')) {
                        id.append(sel.charAt(i));
                        i++;
                    }
                    cur.id = id.toString();
                } else if (c == '.') {
                    hasCur = true;
                    i++;
                    StringBuilder cls = new StringBuilder();
                    while (i < sel.length() && (Character.isLetterOrDigit(sel.charAt(i)) || sel.charAt(i) == '-')) {
                        cls.append(sel.charAt(i));
                        i++;
                    }
                    cur.classes.add(cls.toString());
                } else if (c == ':') {
                    hasCur = true;
                    i++;
                    StringBuilder name = new StringBuilder();
                    while (i < sel.length() && (Character.isLetterOrDigit(sel.charAt(i)) || sel.charAt(i) == '-')) {
                        name.append(sel.charAt(i));
                        i++;
                    }
                    String pseudo = name.toString();
                    if (i < sel.length() && sel.charAt(i) == '(') {
                        int close = findClosingParen(sel, i);
                        pseudo = pseudo + "(" + sel.substring(i + 1, close) + ")";
                        i = close + 1;
                    }
                    cur.pseudoClasses.add(pseudo);
                } else if (c == '*') {
                    hasCur = true; // universal selector: matches any element
                    i++;
                } else if (Character.isLetterOrDigit(c) || c == '-') {
                    hasCur = true;
                    StringBuilder tag = new StringBuilder();
                    while (i < sel.length() && (Character.isLetterOrDigit(sel.charAt(i)) || sel.charAt(i) == '-')) {
                        tag.append(sel.charAt(i));
                        i++;
                    }
                    cur.tag = tag.toString().toLowerCase();
                } else {
                    i++; // skip unknown characters
                }
            }
            if (hasCur) {
                parts.add(new CssSelector.Part(pending == null ? "" : pending, cur));
            }

            if (!parts.isEmpty()) {
                result.add(new CssSelector(parts));
            }
        }
        return result;
    }

    /**
     * Parses a single compound selector string (used for the :not() argument),
     * e.g. ".disabled" or "input[type]" is not supported - only tag/#id/.class.
     */
    public static SimpleSelector parseCompound(String s) {
        s = s.trim();
        SimpleSelector sel = new SimpleSelector();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '#') {
                i++;
                StringBuilder id = new StringBuilder();
                while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '-')) {
                    id.append(s.charAt(i));
                    i++;
                }
                sel.id = id.toString();
            } else if (c == '.') {
                i++;
                StringBuilder cls = new StringBuilder();
                while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '-')) {
                    cls.append(s.charAt(i));
                    i++;
                }
                sel.classes.add(cls.toString());
            } else if (c == ':') {
                i++;
                StringBuilder name = new StringBuilder();
                while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '-')) {
                    name.append(s.charAt(i));
                    i++;
                }
                sel.pseudoClasses.add(name.toString());
            } else if (Character.isLetterOrDigit(c) || c == '-') {
                StringBuilder tag = new StringBuilder();
                while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '-')) {
                    tag.append(s.charAt(i));
                    i++;
                }
                sel.tag = tag.toString().toLowerCase();
            } else {
                i++;
            }
        }
        return sel;
    }

    // Parses the block of property:value; pairs, detecting !important flags.
    private Map<String, CssDeclaration> parseDeclarations(String declStr) {
        Map<String, CssDeclaration> map = new LinkedHashMap<>();
        for (String part : declStr.split(";")) {
            int colon = part.indexOf(':');
            if (colon == -1)
                continue;
            String prop = part.substring(0, colon).trim().toLowerCase();
            String value = part.substring(colon + 1).trim();
            boolean important = value.matches("(?i)^.*!important\\s*$");
            if (important) {
                value = value.replaceFirst("(?i)\\s*!important\\s*$", "").trim();
            }
            map.put(prop, new CssDeclaration(value, important));
        }
        return expandShorthands(map);
    }

    /**
     * Expands CSS shorthands into the longhands the layout engine reads:
     * margin / padding / border-width (1-4 value box form), border (width style color),
     * and background (treated as background-color).
     */
    private Map<String, CssDeclaration> expandShorthands(Map<String, CssDeclaration> m) {
        Map<String, CssDeclaration> out = new LinkedHashMap<>(m);

        expandBoxShorthand(out, "margin", "margin-", "");
        expandBoxShorthand(out, "padding", "padding-", "");
        expandBoxShorthand(out, "border-width", "border-", "-width");

        CssDeclaration b = out.get("border");
        if (b != null) {
            String width = "0";
            for (String tok : b.value.trim().split("\\s+")) {
                if (tok.matches("(?i)\\d*\\.?\\d+(px|em|rem|%)?") && !"none".equalsIgnoreCase(tok)) {
                    width = tok;
                    break;
                }
            }
            for (String side : List.of("border-top-width", "border-right-width",
                    "border-bottom-width", "border-left-width")) {
                out.put(side, new CssDeclaration(width, b.important));
            }
            out.remove("border");
        }

        // per-side border: border-left / border-top / ... -> border-left-width
        for (String side : List.of("top", "right", "bottom", "left")) {
            CssDeclaration bs = out.get("border-" + side);
            if (bs != null) {
                String width = "0";
                for (String tok : bs.value.trim().split("\\s+")) {
                    if (tok.matches("(?i)\\d*\\.?\\d+(px|em|rem|%)?") && !"none".equalsIgnoreCase(tok)) {
                        width = tok;
                        break;
                    }
                }
                out.put("border-" + side + "-width", new CssDeclaration(width, bs.important));
                out.remove("border-" + side);
            }
        }

        CssDeclaration bg = out.get("background");
        if (bg != null) {
            out.put("background-color", new CssDeclaration(bg.value.trim(), bg.important));
            out.remove("background");
        }
        return out;
    }

    /** margin/padding/border-width box shorthand: 1-4 values, top right bottom left ordering. */
    private void expandBoxShorthand(Map<String, CssDeclaration> out, String prop, String prefix, String suffix) {
        CssDeclaration d = out.get(prop);
        if (d == null) return;
        String[] v = d.value.trim().split("\\s+");
        if (v.length >= 1 && v.length <= 4) {
            String top = v[0];
            String right = v.length >= 2 ? v[1] : top;
            String bottom = v.length >= 3 ? v[2] : top;
            String left = v.length >= 4 ? v[3] : right;
            out.put(prefix + "top" + suffix, new CssDeclaration(top, d.important));
            out.put(prefix + "right" + suffix, new CssDeclaration(right, d.important));
            out.put(prefix + "bottom" + suffix, new CssDeclaration(bottom, d.important));
            out.put(prefix + "left" + suffix, new CssDeclaration(left, d.important));
        }
        out.remove(prop);
    }

    // Finds the index of the parenthesis that closes the one at openIdx.
    private int findClosingParen(String s, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(')
                depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0)
                    return i;
            }
        }
        return s.length() - 1;
    }

    // Splits a string on a separator, ignoring separators inside parentheses.
    private List<String> splitTopLevel(String s, char sep) {
        List<String> out = new ArrayList<>();
        int depth = 0, start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(')
                depth++;
            else if (c == ')')
                depth--;
            else if (c == sep && depth == 0) {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        out.add(s.substring(start));
        return out;
    }
}
