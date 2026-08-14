package org.example.browser.layout;

public final class LayoutEngine {

    private LayoutEngine() {}

    public static void layout(LayoutBox root, float viewportWidth, float viewportHeight) {
        BoxDimensions viewport = new BoxDimensions();
        viewport.content.width = viewportWidth;
        viewport.content.height = viewportHeight;
        BlockLayout.layout(root, viewport);
    }

    public static void dumpTree(LayoutBox box, int indent) {
        String pad = "  ".repeat(indent);
        System.out.println(pad + "[" + box.boxType + "]"
                + " content=" + box.dimensions.content
                + " margin=" + box.dimensions.margin
                + " border=" + box.dimensions.border
                + " padding=" + box.dimensions.padding);
        for (LayoutBox c : box.children) dumpTree(c, indent + 1);
    }
}