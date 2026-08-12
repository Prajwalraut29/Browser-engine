package org.example.browser.css;

/**
 * A single property declaration such as "color: red !important".
 * The important flag takes part in the cascade.
 */
public class CssDeclaration {
    public final String value;
    public final boolean important;

    public CssDeclaration(String value, boolean important) {
        this.value = value;
        this.important = important;
    }

    @Override
    public String toString() {
        return value + (important ? " !important" : "");
    }
}
