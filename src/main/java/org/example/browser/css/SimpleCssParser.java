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
            Map<String, String> declarations = parseDeclarations(declStr);
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
                if (end == 1)
                    return s.length();
                i = end + 2;
            } else {
                break;
            }
        }
        return i;
    }

    // Finds the closing } accounting for nested braces (unlikely in simple CSS but
    // safe)
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

    // Parses a comma-separated list of simple selectors like "div.foo#bar"
    private List<CssSelector> parseSelectors(String selectorList) {
        List<CssSelector> result = new ArrayList<>();
        for (String sel : selectorList.split(",")) {
            sel = sel.trim();
            if (sel.isEmpty())
                continue;
            CssSelector selector = new CssSelector();

            // Extract tag name (starts with letter/digit or hyphen)
            int i = 0;
            StringBuilder tag = new StringBuilder();
            while (i < sel.length() && (Character.isLetterOrDigit(sel.charAt(i)) || sel.charAt(i) == '-')) {
                tag.append(sel.charAt(i));
                i++;
            }
            if (!tag.isEmpty())
                selector.tag = tag.toString().toLowerCase();

            // Extract ID (#...) and classes (. ...)
            while (i < sel.length()) {
                if (sel.charAt(i) == '#') {
                    i++;
                    StringBuilder id = new StringBuilder();
                    while (i < sel.length() && (Character.isLetterOrDigit(sel.charAt(i)) || sel.charAt(i) == '-')) {
                        id.append(sel.charAt(i));
                        i++;
                    }
                    selector.id = id.toString();
                } else if (sel.charAt(i) == '.') {
                    i++;
                    StringBuilder cls = new StringBuilder();
                    while (i < sel.length() && (Character.isLetterOrDigit(sel.charAt(i)) || sel.charAt(i) == '-')) {
                        cls.append(sel.charAt(i));
                        i++;
                    }
                    if (selector.classes == null)
                        selector.classes = new HashSet<>();
                    selector.classes.add(cls.toString());
                } else
                    i++; // skip unknown characters (like pseudo-classes, but we ignore them)
            }
            result.add(selector);
        }
        return result;
    }

    // Parses the block of property:value; pairs
    private Map<String, String> parseDeclarations(String declStr) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String part : declStr.split(";")) {
            int colon = part.indexOf(':');
            if (colon == -1)
                continue;
            String prop = part.substring(0, colon).trim().toLowerCase();
            String value = part.substring(colon + 1).trim();
            map.put(prop, value);
        }
        return map;
    }
}