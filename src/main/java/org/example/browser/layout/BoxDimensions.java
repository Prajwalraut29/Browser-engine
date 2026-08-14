package org.example.browser.layout;

public class BoxDimensions {
    public final Rect content = new Rect();
    public final EdgesSizes padding = new EdgesSizes();
    public final EdgesSizes border = new EdgesSizes();
    public final EdgesSizes margin = new EdgesSizes();

    public Rect paddingBox() {
        Rect r = new Rect(content.x,content.y,content.width, content.height);
        r.expandBy(padding);
        return r;
    }

    public Rect borderBox(){
        Rect r = paddingBox();
        r.expandBy(border);
        return r;
    }

    public Rect marginBox(){
        Rect r = borderBox();
        r.expandBy(margin);
        return r;
    }

}
