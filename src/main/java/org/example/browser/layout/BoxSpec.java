package org.example.browser.layout;
import java.util.Map;
public class BoxSpec {
     public final Length width, height;
    public final Length marginTop, marginRight, marginBottom, marginLeft;
    public final Length paddingTop, paddingRight, paddingBottom, paddingLeft;
    public final Length borderTop, borderRight, borderBottom, borderLeft;
    public final String backgroundColor;
    public final String color;
    public final String fontFamily;
    public final Length fontSize;
    
     public BoxSpec(Map<String, String> s) {
        width  = Length.parse(s.get("width"));
        height = Length.parse(s.get("height"));
        marginTop    = Length.parse(s.get("margin-top"));
        marginRight  = Length.parse(s.get("margin-right"));
        marginBottom = Length.parse(s.get("margin-bottom"));
        marginLeft   = Length.parse(s.get("margin-left"));
        paddingTop    = Length.parse(s.get("padding-top"));
        paddingRight  = Length.parse(s.get("padding-right"));
        paddingBottom = Length.parse(s.get("padding-bottom"));
        paddingLeft   = Length.parse(s.get("padding-left"));
        borderTop    = Length.parse(s.get("border-top-width"));
        borderRight  = Length.parse(s.get("border-right-width"));
        borderBottom = Length.parse(s.get("border-bottom-width"));
        borderLeft   = Length.parse(s.get("border-left-width"));
        backgroundColor = s.getOrDefault("background-color", "transparent");
        color          = s.getOrDefault("color", "black");
        fontFamily     = s.getOrDefault("font-family", "serif");
        fontSize       = Length.parse(s.getOrDefault("font-size", "16px"));
    }
}
