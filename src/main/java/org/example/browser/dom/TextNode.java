package org.example.browser.dom;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TextNode implements DomNode {
    private final String text;

    public TextNode(String text) {
        this.text = text;
    }

    @Override // DOM standard 
    public String getNodeName() {
        return "#text";
    }  

    @Override
    public Map<String, String> getAttributes() {
        return Collections.emptyMap();
    }

    @Override  // text nodes have no children
    public List<DomNode> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public String getTextContent() {
        return text;
    }

    @Override
    public boolean isElement() {
        return false;
    }

}
