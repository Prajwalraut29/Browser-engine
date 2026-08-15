package org.example.browser.tree;

import java.util.ArrayList;
import java.util.List;


public class TreeNode<T> {

    private final T value;
    private final TreeNode<T> parent;
    private final List<TreeNode<T>> children = new ArrayList<>();

    public TreeNode(T value, TreeNode<T> parent) {
        this.value = value;
        this.parent = parent;
    }

    public T getValue() {
        return value;
    }

    public TreeNode<T> getParent() {
        return parent;
    }

    public List<TreeNode<T>> getChildren() {
        return children;
    }

    public TreeNode<T> addChild(T childValue) {
        TreeNode<T> child = new TreeNode<>(childValue, this);
        children.add(child);
        return child;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

}