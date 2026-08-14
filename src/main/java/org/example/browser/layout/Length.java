package org.example.browser.layout;

public class Length {
    public enum Type{ Px, Percent, Auto}

    public final Type type;
    public final float value;

    private Length(Type type, float value) { this.type = type; this.value = value; }

    public static final Length AUTO = new Length(Type.Auto, 0);

     public static Length parse(String s) {
        if (s == null) return AUTO;
        s = s.trim();
        if (s.isEmpty() || s.equals("auto")) return AUTO;
        if (s.endsWith("%")) {
            return new Length(Type.Percent, Float.parseFloat(s.substring(0, s.length() - 1)));
        }
        if (s.endsWith("px")) s = s.substring(0, s.length() - 2);
        return new Length(Type.Px, Float.parseFloat(s));
    }
     
    public float resolve(float base){
         switch (type) {
            case Px:      return value;
            case Percent: return value / 100f * base;
            default:      return 0;
        }
    }

    public float resolveNonAuto(float base ) {
        return type == Type.Auto ? 0 : resolve(base);
    }

    @Override public String toString(){
         switch (type) {
            case Px:      return value + "px";
            case Percent: return value + "%";
            default:      return "auto";
        }
    }
}
