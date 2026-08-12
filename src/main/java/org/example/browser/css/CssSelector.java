package org.example.browser.css;

import java.util.*;

/**
 * A complex selector: a chain of compound selectors joined by combinators.
 * E.g. "div p", "div > p.box", "h1 + p", "li.active ~ li".
 * The right-most part is the subject; the others restrict context.
 */
public class CssSelector {
    /**
     * One link in the chain. The combinator is empty ("") for the first part,
     * " " for descendant, ">" for child, "+" for adjacent sibling and "~" for
     * general sibling. It describes the relationship between this part and the
     * part to its right.
     */
    public static class Part {
        public final String combinator;      // "", " ", ">", "+", "~"
        public final SimpleSelector compound;

        public Part(String combinator, SimpleSelector compound) {
            this.combinator = combinator;
            this.compound = compound;
        }
    }

    public final List<Part> parts;

    public CssSelector(List<Part> parts) {
        this.parts = parts;
    }

    public boolean isComplex() {
        return parts.size() > 1;
    }

    /**
     * Specificity of the whole complex selector as an [ids, classes, types]
     * triple: the sum over all compound parts.
     */
    public int[] specificity() {
        int[] spec = new int[3];
        for (Part p : parts) {
            p.compound.addSpecificity(spec);
        }
        return spec;
    }
}
