package org.example.browser.css;

import java.util.*;

/**
 * A compound (simple) selector: one tag, at most one id, any number of
 * classes and pseudo-classes. Examples: "div", ".box", "#main", "p:first-child",
 * "li:nth-child(2n)", "input:not(.disabled)".
 */
public class SimpleSelector {
    public String tag;                    // element tag, null for the universal selector (*)
    public String id;                     // #id
    public Set<String> classes;           // .class (multiple allowed)
    public List<String> pseudoClasses;    // "first-child", "nth-child(2n+1)", "not(.foo)", ...

    public SimpleSelector() {
        this.classes = new HashSet<>();
        this.pseudoClasses = new ArrayList<>();
    }

    /**
     * Adds this compound selector's contribution to a specificity triple
     * spec = [ids, classes, types] (the CSS a/b/c weighting).
     */
    public void addSpecificity(int[] spec) {
        if (id != null) spec[0]++;
        spec[1] += classes.size();
        if (tag != null) spec[2]++;
        for (String p : pseudoClasses) {
            if (p.startsWith("not(")) {
                // :not() inherits the specificity of its argument
                SimpleSelector arg = SimpleCssParser.parseCompound(p.substring(4, p.length() - 1));
                arg.addSpecificity(spec);
            } else {
                spec[1]++; // every pseudo-class counts like a class
            }
        }
    }
}
