package org.example.browser.tree;

import java.util.*;

public class Tree<T> {

    private final TreeNode<T> root;

    public Tree(TreeNode<T> root) {
        this.root = root;
    }

    public TreeNode<T> getRoot() {
        return root;
    }

    /** DFS pre-order (discovery order) visit. */
    public List<TreeNode<T>> preOrder() {
        List<TreeNode<T>> out = new ArrayList<>();
        preOrderRec(root, out);
        return out;
    }

    private void preOrderRec(TreeNode<T> node, List<TreeNode<T>> out) {
        out.add(node);
        for (TreeNode<T> child : node.getChildren())
            preOrderRec(child, out);
    }

    /** DFS post-order (finish order) visit. */
    public List<TreeNode<T>> postOrder() {
        List<TreeNode<T>> out = new ArrayList<>();
        postOrderRec(root, out);
        return out;
    }

    private void postOrderRec(TreeNode<T> node, List<TreeNode<T>> out) {
        for (TreeNode<T> child : node.getChildren())
            postOrderRec(child, out);
        out.add(node);
    }

    // BFS level-order one list per level : walked with a queue

    public List<List<TreeNode<T>>> levels() {
        List<List<TreeNode<T>>> levels = new ArrayList<>();

        if (root == null)
            return levels;
        Deque<TreeNode<T>> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            int size = queue.size();
            List<TreeNode<T>> level = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                TreeNode<T> node = queue.poll();
                level.add(node);
                queue.addAll(node.getChildren());
            }
            levels.add(level);
        }
        return levels;
    }

    // BFS visit sequence
    public List<TreeNode<T>> breadthFirst() {
        List<TreeNode<T>> out = new ArrayList<>();
        for (List<TreeNode<T>> level : levels())
            out.addAll(level);
        return out;
    }

    /** Height = number of edges on the longest root-to-leaf path. */
    public int height() {
        return heightRec(root);
    }

    private int heightRec(TreeNode<T> node) {
        int max = 0;
        for (TreeNode<T> child : node.getChildren())
            max = Math.max(max, 1 + heightRec(child));
        return max;
    }

    public int nodeCount() {
        return preOrder().size();
    }

    public int leafCount() {
        int count = 0;
        for (TreeNode<T> node : preOrder())
            if (node.isLeaf())
                count++;
        return count;
    }

}