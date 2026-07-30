package org.example.browser.css;

import java.util.*;

public class CssRule {
    public final List<CssSelector> selectors;       // a rule can have multiple selectors separated by commas
    public final Map<String, String> declarations;  // e.g., "color":"red", "margin":"10px"

    public CssRule(List<CssSelector> selectors, Map<String, String> declarations) {
        this.selectors = selectors;
        this.declarations = declarations;
    }
}