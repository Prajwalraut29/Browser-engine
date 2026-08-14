package org.example.browser.layout;

import java.util.ArrayList;
import java.util.List;

public class LineBox {
    public final List<TextFragment> fragments = new ArrayList<>();
    public float y; // top of the line box
    public float height; // measured after fragments are placed

    public LineBox() {
    }

}
