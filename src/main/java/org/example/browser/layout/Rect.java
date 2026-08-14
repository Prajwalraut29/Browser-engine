package org.example.browser.layout;

public class Rect {
    public float x, y, width, height;

    public Rect() {
    }

    public Rect(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public float right() {
        return x + width;
    }

    public float bottom() {
        return y + height;
    }

    public void expandBy(EdgesSizes edge) {
        x -= edge.left;
        y -= edge.top;
        width += edge.left + edge.right;
        height += edge.top + edge.bottom;
    }

    @Override public String toString(){
        return "(" + x + "," + y + " " + width + "x" + height + ")";
    }
}
