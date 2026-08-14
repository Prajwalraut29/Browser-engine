package org.example.browser.layout;

import java.util.ArrayList;
import java.util.List;

import org.example.browser.dom.DomNode;

public class LayoutBox {
    public final BoxType boxType;
    public final DomNode element;
    public final BoxSpec spec;
    public final BoxDimensions dimensions = new BoxDimensions();
    public final List<LayoutBox> children = new ArrayList<>();
    public final List<LineBox> lines = new ArrayList<>();

    public LayoutBox(BoxType boxType, DomNode element, BoxSpec spec) {
        this.boxType = boxType;
        this.element = element;
        this.spec = spec;
    }

    public boolean isBlock() {
        return boxType == BoxType.BlockNode || boxType == BoxType.AnonymousBlock;
    }

    public boolean isInline() {
        return boxType == BoxType.InlineNode;
    }

    public boolean isAnonymous() {
        return boxType == BoxType.AnonymousBlock;
    }
}
