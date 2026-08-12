package org.example.browser.css;

import java.util.*;

public class CssRule {
    public final List<CssSelector> selectors;             // a rule can have multiple selectors separated by commas
    public final Map<String, CssDeclaration> declarations; // e.g., "color" -> red (maybe !important)

    public CssRule(List<CssSelector> selectors, Map<String, CssDeclaration> declarations) {
        this.selectors = selectors;
        this.declarations = declarations;
    }
}
