package org.example.browser.layout;

public class EdgesSizes {
    public float top, right, bottom, left;

    public EdgesSizes() {}
    public EdgesSizes(float top, float right, float bottom, float left){
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }

    @Override public String toString(){
        return "[" + top + "," + right + "," + bottom + "," + left + "]";
    } 
}
