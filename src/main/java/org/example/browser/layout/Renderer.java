package org.example.browser.layout;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public final class Renderer {

    private Renderer() {}

    public static void render(LayoutBox root, float width, float height, String path) {
        render(root, width, height, path, false);
    }

    /** @param showBoxModel paint the margin/border/padding/content overlay (DevTools style). */
    public static void render(LayoutBox root, float width, float height, String path, boolean showBoxModel) {
        BufferedImage img = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, (int) width, (int) height);
        paint(g, root);
        if (showBoxModel) {
            paintOverlay(g, root);
            drawLegend(g);
        }
        g.dispose();
        try {
            ImageIO.write(img, "png", new File(path));
            System.out.println("Saved " + path);
        } catch (Exception e) {
            System.err.println("Render failed: " + e.getMessage());
        }
    }

    private static void paint(Graphics2D g, LayoutBox box) {
        BoxDimensions d = box.dimensions;

        Color bg = parseColor(box.spec.backgroundColor);
        if (bg != null) {
            Rectangle r = new Rectangle((int) d.paddingBox().x, (int) d.paddingBox().y,
                                        (int) d.paddingBox().width, (int) d.paddingBox().height);
            g.setColor(bg);
            g.fill(r);
        }

        g.setColor(Color.BLACK);
        if (d.border.left > 0)
            g.fillRect((int)(d.content.x - d.padding.left - d.border.left), (int)(d.content.y - d.padding.top),
                       (int)d.border.left, (int)(d.padding.top + d.content.height + d.padding.bottom));
        if (d.border.right > 0)
            g.fillRect((int)(d.content.x + d.content.width + d.padding.right), (int)(d.content.y - d.padding.top),
                       (int)d.border.right, (int)(d.padding.top + d.content.height + d.padding.bottom));
        if (d.border.top > 0)
            g.fillRect((int)(d.content.x - d.padding.left), (int)(d.content.y - d.padding.top - d.border.top),
                       (int)(d.padding.left + d.content.width + d.padding.right), (int)d.border.top);
        if (d.border.bottom > 0)
            g.fillRect((int)(d.content.x - d.padding.left), (int)(d.content.y + d.content.height + d.padding.bottom),
                       (int)(d.padding.left + d.content.width + d.padding.right), (int)d.border.bottom);

        for (LineBox line : box.lines) {
            for (TextFragment f : line.fragments) {
                g.setFont(TextMetrics.fontFor(f.spec));
                Color c = parseColor(f.spec.color);
                g.setColor(c == null ? Color.BLACK : c);
                g.drawString(f.text, (int) f.x, (int) (line.y + f.ascent));
            }
        }

        for (LayoutBox child : box.children) paint(g, child);
    }

    // ---- box-model overlay (margin / border / padding / content) ------------

    private static void paintOverlay(Graphics2D g, LayoutBox box) {
        BoxDimensions d = box.dimensions;
        Rect content = d.content;
        Rect pad = d.paddingBox();
        Rect border = d.borderBox();
        Rect margin = d.marginBox();

        if (content.width > 0 && content.height > 0) {
            g.setColor(new Color(0, 102, 255, 70));       // content = blue
            g.fillRect((int) content.x, (int) content.y, (int) content.width, (int) content.height);
        }
        g.setColor(new Color(0, 190, 0, 80));              // padding = green
        fillRing(g, content, pad);
        g.setColor(new Color(150, 90, 30, 110));           // border = brown
        fillRing(g, pad, border);
        g.setColor(new Color(255, 140, 0, 90));            // margin = orange
        fillRing(g, border, margin);

        for (LayoutBox child : box.children) paintOverlay(g, child);
    }

    /** Draws the 4 side bands between inner and outer (the "ring" area). */
    private static void fillRing(Graphics2D g, Rect inner, Rect outer) {
        g.fillRect((int) outer.x, (int) outer.y, (int) outer.width, (int) (inner.y - outer.y));                         // top
        g.fillRect((int) outer.x, (int) inner.bottom(), (int) outer.width, (int) (outer.bottom() - inner.bottom()));   // bottom
        g.fillRect((int) outer.x, (int) inner.y, (int) (inner.x - outer.x), (int) inner.height);                       // left
        g.fillRect((int) inner.right(), (int) inner.y, (int) (outer.right() - inner.right()), (int) inner.height);     // right
    }

    private static void drawLegend(Graphics2D g) {
        int x = 12, y = 26, size = 16, gap = 8;
        g.setColor(new Color(255, 255, 255, 230));
        g.fillRoundRect(x - 6, y - size - 8, 330, size + 16, 8, 8);
        g.setFont(new Font("sans-serif", Font.PLAIN, 14));
        legendItem(g, x, y, size, new Color(255, 140, 0), "margin");
        legendItem(g, x + 90 + gap, y, size, new Color(150, 90, 30), "border");
        legendItem(g, x + 190 + 2 * gap, y, size, new Color(0, 190, 0), "padding");
        legendItem(g, x + 300 + 3 * gap, y, size, new Color(0, 102, 255), "content");
    }

    private static void legendItem(Graphics2D g, int x, int y, int size, Color c, String label) {
        g.setColor(c);
        g.fillRect(x, y - size, size, size);
        g.setColor(Color.BLACK);
        g.drawString(label, x + size + 6, y);
    }

    private static Color parseColor(String s) {
        if (s == null || s.equals("transparent")) return null;
        try { if (s.startsWith("#")) return Color.decode(s); } catch (NumberFormatException ignored) {}
        switch (s.toLowerCase()) {
            case "black":     return Color.BLACK;
            case "white":     return Color.WHITE;
            case "red":       return Color.RED;
            case "navy":      return new Color(0x000080);
            case "lightblue": return new Color(0xADD8E6);
            default:
                try {
                    return (Color) Color.class.getField(s.toLowerCase()).get(null);
                } catch (Exception e) {
                    return Color.GRAY;
                }
        }
    }
}