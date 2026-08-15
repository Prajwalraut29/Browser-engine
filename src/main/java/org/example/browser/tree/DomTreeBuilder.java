package org.example.browser.tree;

import org.example.browser.dom.DomNode;

/**
 * Converts the parsed DOM into the generic Tree<DomNode> data structure.
 * T = DomNode: each TreeNode wraps an ElementNode or TextNode.
 */

public class DomTreeBuilder {
    public static Tree<DomNode> build(DomNode domRoot) {
        TreeNode<DomNode> treeRoot = new TreeNode<>(domRoot, null);
        for (DomNode child : domRoot.getChildren()) {
            buildRec(child, treeRoot);
        }
        return new Tree<>(treeRoot);
    }

    private static void buildRec(DomNode dom, TreeNode<DomNode> parent) {
        TreeNode<DomNode> node = parent.addChild(dom);
        for (DomNode child : dom.getChildren()) {
            buildRec(child, node);
        }
    }
}
