package org.example.browser.layout;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.util.HashMap;
import java.util.Map;

public class TextMetrics {
   private static final FontRenderContext FRC = new FontRenderContext(null, false, false);
    private static final Map<String, Font> FONT_CACHE = new HashMap<>();

      public static Font fontFor(BoxSpec spec) {
        int size = Math.round(spec.fontSize.resolve(0));
        if (size <= 0) size = 16;
        final int s = size;
        return FONT_CACHE.computeIfAbsent(spec.fontFamily + "@" + s,
                k -> new Font(spec.fontFamily, Font.PLAIN, s));
    }
    
    public static float measure(String text, BoxSpec spec){
        return (float) fontFor(spec).getStringBounds(text, FRC).getWidth();
    }

     /** Distance from line top to the baseline. */
    public static float ascent(BoxSpec spec) {
        return (float) fontFor(spec).getLineMetrics("Ag", FRC).getAscent();
    }

    /** Distance from baseline to the bottom of the line. */
    public static float descent(BoxSpec spec) {
        return (float) fontFor(spec).getLineMetrics("Ag", FRC).getDescent();
    }

} 
