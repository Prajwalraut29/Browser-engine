package org.example.browser.layout;

public class BlockLayout {
     public static void layout(LayoutBox box, BoxDimensions containing) {
        layoutWidth(box, containing);
        if (box.isAnonymous()) {
            InlineLayout.layout(box, containing);          // inline content inside
        } else {
            layoutChildren(box, containing);
            layoutHeight(box, containing);
        }
    }

    private static void layoutWidth(LayoutBox box, BoxDimensions containing) {
        BoxSpec s = box.spec;
        float cbWidth = containing.content.width;

        // Margins/padding/borders resolve against the containing block WIDTH
        // (CSS rule - even the top/bottom ones).
        float marginLeft   = s.marginLeft.resolveNonAuto(cbWidth);
        float marginRight  = s.marginRight.resolveNonAuto(cbWidth);
        float borderLeft   = s.borderLeft.resolveNonAuto(cbWidth);
        float borderRight  = s.borderRight.resolveNonAuto(cbWidth);
        float paddingLeft  = s.paddingLeft.resolveNonAuto(cbWidth);
        float paddingRight = s.paddingRight.resolveNonAuto(cbWidth);

        float width;
        if (s.width.type == Length.Type.Auto) {
            width = cbWidth - marginLeft - marginRight - borderLeft - borderRight
                    - paddingLeft - paddingRight;             // fill remaining space
        } else {
            width = s.width.resolve(cbWidth);                 // fixed / percentage
        }

        // Leftover space flows into auto margins -> margin:auto centers the box.
        float total = width + marginLeft + marginRight + borderLeft + borderRight
                     + paddingLeft + paddingRight;
        float underflow = cbWidth - total;
        if (s.marginLeft.type == Length.Type.Auto) {
            if (s.marginRight.type == Length.Type.Auto) {
                marginLeft = underflow / 2f;
                marginRight = underflow / 2f;
            } else {
                marginLeft = underflow;
            }
        } else if (s.marginRight.type == Length.Type.Auto) {
            marginRight = underflow;
        }

        BoxDimensions d = box.dimensions;
        d.content.x = containing.content.x + marginLeft + borderLeft + paddingLeft;
        d.content.width = Math.max(0, width);
        d.margin.left = marginLeft;  d.margin.right = marginRight;
        d.border.left = borderLeft;  d.border.right = borderRight;
        d.padding.left = paddingLeft; d.padding.right = paddingRight;
        d.margin.top    = s.marginTop.resolveNonAuto(cbWidth);
        d.margin.bottom = s.marginBottom.resolveNonAuto(cbWidth);
        d.border.top    = s.borderTop.resolveNonAuto(cbWidth);
        d.border.bottom = s.borderBottom.resolveNonAuto(cbWidth);
        d.padding.top    = s.paddingTop.resolveNonAuto(cbWidth);
        d.padding.bottom = s.paddingBottom.resolveNonAuto(cbWidth);
    }

    /** Top-down: position each child, collapsing vertical margins. */
    private static void layoutChildren(LayoutBox box, BoxDimensions containing) {
        float cursorY = box.dimensions.content.y;
        float cbWidth = box.dimensions.content.width;
        float prevBottom = 0;

        for (int i = 0; i < box.children.size(); i++) {
            LayoutBox child = box.children.get(i);
            // Top/bottom margins resolve against the containing block width
            // and are known before the child itself is laid out.
            float childTop = child.spec.marginTop.resolveNonAuto(cbWidth);

            float gap;
            if (i == 0) {
                // First child: its top margin collapses with this box's top margin,
                // only if nothing (border/padding/fixed height) separates them.
                boolean blocked = box.dimensions.border.top > 0
                        || box.dimensions.padding.top > 0
                        || box.spec.height.type != Length.Type.Auto;
                float parentTop = box.dimensions.margin.top;
                gap = blocked ? childTop
                              : MarginCollapser.collapse(parentTop, childTop) - parentTop;
            } else {
                // Adjacent siblings: collapse previous bottom with this top.
                gap = MarginCollapser.collapse(prevBottom, childTop);
            }
            gap = Math.max(0, gap);
            cursorY += gap;   // cursorY is now this child's border-box top

            // The content box starts below the child's own border + padding.
            child.dimensions.content.y = cursorY
                    + child.spec.borderTop.resolveNonAuto(cbWidth)
                    + child.spec.paddingTop.resolveNonAuto(cbWidth);
            layout(child, box.dimensions);

            cursorY += child.dimensions.content.height
                     + child.dimensions.border.top + child.dimensions.border.bottom
                     + child.dimensions.padding.top + child.dimensions.padding.bottom;
            prevBottom = child.spec.marginBottom.resolveNonAuto(cbWidth);
        }
    }

    /** Bottom-up: height:auto = content box down to the deepest child border box. */
    private static void layoutHeight(LayoutBox box, BoxDimensions containing) {
        BoxDimensions d = box.dimensions;
        float height;
        if (box.spec.height.type == Length.Type.Auto) {
            height = 0;
            for (LayoutBox c : box.children) {
                float bottom = c.dimensions.content.y + c.dimensions.content.height
                        + c.dimensions.border.top + c.dimensions.border.bottom
                        + c.dimensions.padding.top + c.dimensions.padding.bottom;
                height = Math.max(height, bottom - d.content.y);
            }
        } else {
            height = box.spec.height.resolve(containing.content.height);
        }
        d.content.height = Math.max(0, height);
    }
}
