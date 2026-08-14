package org.example.browser.layout;

import org.example.browser.dom.TextNode;

import java.util.ArrayList;
import java.util.List;

public class InlineLayout {

    public static void layout(LayoutBox box, BoxDimensions containing) {
        List<TextFragment> fragments = new ArrayList<>();
        collectFragments(box, box.spec, fragments);

        List<TextFragment> words = new ArrayList<>();
        for (TextFragment f : fragments) {
            for (String word : f.text.trim().split("\\s+")) {
                if (!word.isEmpty()) {
                    words.add(new TextFragment(word, f.spec, TextMetrics.measure(word, f.spec)));
                }
            }
        }
        box.lines.clear();
        if (words.isEmpty()) {
            box.dimensions.content.height = 0;
            return;
        }

        float contentX = box.dimensions.content.x;
        float contentY = box.dimensions.content.y;
        float lineMaxX = contentX + Math.max(1, box.dimensions.content.width);
        List<LineBox> lines = new ArrayList<>();
        LineBox line = new LineBox();
        line.y = contentY;
        float cursorX = contentX;
        float cursorY = contentY;

        for (TextFragment word : words) {
            float space = line.fragments.isEmpty() ? 0 : TextMetrics.measure(" ", word.spec);
            float width = word.width + space;

            // Overflow the line? Wrap.
            if (cursorX + width > lineMaxX && !line.fragments.isEmpty()) {
                line.height = lineHeightOf(line);
                lines.add(line);
                cursorY += line.height;
                line = new LineBox();
                line.y = cursorY;
                cursorX = contentX;
            }

            word.x = cursorX;
            word.ascent = TextMetrics.ascent(word.spec);
            cursorX += width;
            line.fragments.add(word);
        }
        line.height = lineHeightOf(line);
        lines.add(line);

        box.lines.addAll(lines);
        box.dimensions.content.height = (cursorY + line.height) - contentY;
        
    }
      /** Flattens nested inline boxes (span → its text), keeping the nearest element's styles. */
    private static void collectFragments(LayoutBox box, BoxSpec inherited, List<TextFragment> out) {
        if (box.element instanceof TextNode) {
            out.add(new TextFragment(((TextNode) box.element).getTextContent(), inherited, 0));
            return;
        }
        for (LayoutBox c : box.children) collectFragments(c, box.spec, out);
    }

    private static float lineHeightOf(LineBox line) {
        float h = 0;
        for (TextFragment f : line.fragments) {
            h = Math.max(h, TextMetrics.ascent(f.spec) + TextMetrics.descent(f.spec));
        }
        return h;
    }
}
