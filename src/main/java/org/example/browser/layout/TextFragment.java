package org.example.browser.layout;

public class TextFragment {
     public final String text;
    public final BoxSpec spec;   // color / font for this fragment
    public final float width;    // measured pixel width
    public float x;              // left edge of the word
    public float ascent;         // baseline offset when drawing

    public TextFragment(String text, BoxSpec spec, float width) {
        this.text = text; this.spec = spec; this.width = width;
    }
}
