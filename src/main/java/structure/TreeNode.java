package structure;

import javax.swing.tree.DefaultMutableTreeNode;

public class TreeNode extends DefaultMutableTreeNode {
    private String nodeKind;      //当前结点类型
    private String content;       //当前结点内容
    private int lineNum;          //当前结点行号

    public TreeNode() {
        super();
        nodeKind = "";
        content = "";
    }

    public TreeNode(String content) {
        super(content);
        this.content = content;
        nodeKind = "";
    }

    public TreeNode(String kind, String content) {
        super(content);
        this.content = content;
        nodeKind = kind;
    }

    public TreeNode(String kind, String content, int lineNum) {
        super(content);
        this.content = content;
        this.lineNum = lineNum;
        nodeKind = kind;
    }

    public String getNodeKind() {
        return nodeKind;
    }

    public void setNodeKind(String nodeKind) {
        this.nodeKind = nodeKind;
    }

    public int getLineNum() {
        return lineNum;
    }

    public void setLineNum(int lineNum) {
        this.lineNum = lineNum;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        setUserObject(content);
    }

    /**
     * 功能介绍：为该结点添加孩子结点
     *
     * @param childNode 要添加的孩子结点
     */
    public void add(TreeNode childNode) {
        super.add(childNode);
    }

    public TreeNode getChildAt(int index) {
        return (TreeNode) super.getChildAt(index);
    }

}