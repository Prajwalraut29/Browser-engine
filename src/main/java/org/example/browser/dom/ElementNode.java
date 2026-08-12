package org.example.browser.dom;

import java.util.*;

public class ElementNode implements DomNode {
    private final String tagName; // eg "div"
    private final Map<String,String> attributes; // {"id : main"}
    private final List<DomNode> children = new ArrayList<>();
    private Map<String,String> computedStyle;
    private DomNode parent;

    public ElementNode(String tagName, Map<String,String> attributes){
        this.tagName = tagName.toLowerCase();
        this.attributes = Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    @Override public String getNodeName() {return tagName;}
    @Override public Map<String, String> getAttributes() {return attributes;}
    @Override public List<DomNode> getChildren() { return children;}
    @Override public DomNode getParent() { return parent;}
    @Override public String getTextContent() { return null; }
    @Override public boolean isElement() { return true; }

    public void addChild(DomNode child) { children.add(child); }
    public void setParent(DomNode parent) { this.parent = parent; }

    public Map<String, String> getComputedStyle() { return computedStyle;}
    public void getComputedStyle(Map<String, String> style) {
        this.computedStyle = style;
    }
}