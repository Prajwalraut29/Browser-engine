package org.example.browser.layout;

import org.example.browser.dom.DomNode;
import org.example.browser.dom.ElementNode;

import java.util.*;


public class LayoutTreeBuilder {
    private static final Set<String> HIDDEN_TAGS = Set.of("head", "style", "script", "meta", "link");

    public LayoutBox build(DomNode root) {
        LayoutBox rootBox = new LayoutBox(BoxType.BlockNode, root, new BoxSpec(computedStyle(root)));
        buildChildren(root, rootBox, true);
        return rootBox;
    }

     private void buildChildren(DomNode parent, LayoutBox parentBox, boolean wrapInlines) {
        List<LayoutBox> pending = new ArrayList<>();
      for (DomNode child : parent.getChildren()) {
            if (!child.isElement()) {
                LayoutBox t = new LayoutBox(BoxType.InlineNode, child, zeroedSpec(parentBox.spec));
                if (wrapInlines) pending.add(t); else parentBox.children.add(t);
                continue;
            }
            ElementNode el = (ElementNode) child;
            if (HIDDEN_TAGS.contains(el.getNodeName())) continue;

            Map<String, String> style = el.getComputedStyle();
            if (style == null) continue;
            String display = style.getOrDefault("display", "inline");
            if (display.equals("none")) continue;

            if (display.equals("block")) {
                if (wrapInlines) flushInlines(pending, parentBox);
                LayoutBox cb = new LayoutBox(BoxType.BlockNode, el, new BoxSpec(style));
                parentBox.children.add(cb);
                buildChildren(child, cb, true);
            } else {
                LayoutBox ib = new LayoutBox(BoxType.InlineNode, el, new BoxSpec(style));
                if (wrapInlines) pending.add(ib); else parentBox.children.add(ib);
                buildChildren(child, ib, false);
            }
        }
        if (wrapInlines) flushInlines(pending, parentBox);
    }

     private void flushInlines(List<LayoutBox> pending, LayoutBox parentBox) {
        if (pending.isEmpty()) return;
        LayoutBox anon = new LayoutBox(BoxType.AnonymousBlock, null, zeroedSpec(parentBox.spec));
        anon.children.addAll(pending);
        parentBox.children.add(anon);
        pending.clear();
    }

  private BoxSpec zeroedSpec(BoxSpec src) {
        Map<String, String> m = new HashMap<>();
        m.put("display", "block");
        m.put("width", "auto");
        m.put("height", "auto");
        for (String k : List.of("margin-top","margin-right","margin-bottom","margin-left",
                                "padding-top","padding-right","padding-bottom","padding-left",
                                "border-top-width","border-right-width","border-bottom-width","border-left-width")) {
            m.put(k, "0");
        }
        m.put("background-color", "transparent");
        m.put("color", src.color);
        m.put("font-family", src.fontFamily);
        m.put("font-size", src.fontSize.toString());
        return new BoxSpec(m);
    }

    private Map<String, String> computedStyle(DomNode node) {
        if (node.isElement()) {
            Map<String, String> s = ((ElementNode) node).getComputedStyle();
            if (s != null) return s;
        }
        return Collections.emptyMap();
    }


}
