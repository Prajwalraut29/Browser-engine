package org.example.browser.dom;

import java.util.List;
import java.util.Map;

// foundation of the whole document tree 
// every node in html/dom is element of tag or text

public interface DomNode {
    
    // name of the node : div, span
    String getNodeName();

    // key-value attribute like id="main", class="container"
    Map<String, String> getAttributes();

    // child nodes - this forms the tree structure 
    List<DomNode> getChildren();
      
    // only meaningfull for text nodes: return null for elements 
    String getTextContent();

    // check is element or text node 
    boolean isElement();
}
