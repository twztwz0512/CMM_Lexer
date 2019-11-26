package GUI;

import compiler.CMMLexer;
import compiler.CMMParser;
import compiler.CMMSemanticAnalysis;
import structure.Token;
import structure.TreeNode;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Enumeration;

public class CompilerGUI {
    private JFrame jFrame;
    private JPanel panel;
    private JTextArea textArea;
    private JScrollPane textScroll;
    private static JTextArea resultArea;
    private JScrollPane resultScroll;
    private static JTextArea errorArea;
    private JScrollPane errorScroll;
    private JButton lexerButton;
    private JButton fileChooseButton;
    private JButton parserButton;
    private JButton executeButton;
    private String filePath;

    public CompilerGUI() {
        makeGUI();
    }

    private void makeGUI() {
        jFrame = new JFrame("CMM_Compiler");
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JFrame.setDefaultLookAndFeelDecorated(true);
        jFrame.setBounds(100, 100, 800, 800);
        jFrame.setResizable(false);

        panel = new JPanel();
        panel.setLayout(null);
        jFrame.add(panel);

        textArea = new JTextArea();
        textArea.setFont(new Font("宋体", Font.BOLD, 15));
        textScroll = new JScrollPane(textArea);
        //textScroll.setRowHeaderView(new LineNumberHeaderView());
        textScroll.setBounds(10, 10, 600, 300);
        textScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        textScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(textScroll);

        resultArea = new JTextArea();
        resultArea.setFont(new Font("宋体", Font.BOLD, 15));
        resultScroll = new JScrollPane(resultArea);
        resultScroll.setBounds(10, 350, 600, 200);
        resultScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        resultScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(resultScroll);

        errorArea = new JTextArea();
        errorArea.setFont(new Font("宋体", Font.BOLD, 15));
        errorScroll = new JScrollPane(errorArea);
        errorScroll.setBounds(10, 590, 600, 150);
        errorScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        errorScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(errorScroll);

        fileChooseButton = new JButton("选择文件");
        fileChooseButton.setBounds(650, 10, 100, 30);
        panel.add(fileChooseButton);

        lexerButton = new JButton("词法分析");
        lexerButton.setBounds(650, 50, 100, 30);
        panel.add(lexerButton);

        parserButton = new JButton("语法分析");
        parserButton.setBounds(650, 90, 100, 30);
        panel.add(parserButton);

        executeButton = new JButton("执行程序");
        executeButton.setBounds(650, 130, 100, 30);
        panel.add(executeButton);
        //panel.setBackground(new Color(255, 239, 201));
        jFrame.setVisible(true);
        buttonEvent();
    }

    private void buttonEvent() {
        lexerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                errorArea.setText("");
                resultArea.setText("");
                CMMLexer cmmLexer = new CMMLexer();
                cmmLexer.setSourceText(textArea.getText());
                cmmLexer.execute(textArea.getText());
                ArrayList<Token> tokens = cmmLexer.getTokens();

                String error = cmmLexer.getErrorInfo();
                for (Token token : tokens) {
                    resultArea.append(token.toString() + "\n");
                }
                errorArea.append("词法分析共" + cmmLexer.getErrorNum() + "个错误：" + "\n");
                if (error != null && !error.equals("")) {
                    errorArea.append(error);
                }
            }
        });

        fileChooseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setLookAndFeel();

                JFileChooser jFileChooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter(null, "txt");
                jFileChooser.setFileFilter(filter);
                jFileChooser.showOpenDialog(null);
                filePath = jFileChooser.getSelectedFile().getAbsolutePath();
                JOptionPane.showMessageDialog(null, "文件导入成功");
                String source = "";
                try {
                    BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
                    String curLine;
                    while (null != (curLine = br.readLine())) {
                        source = source + curLine + "\n";
                    }
                    br.close();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                textArea.setText(source);
                resultArea.setText("");
                errorArea.setText("");
            }
        });

        parserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setLookAndFeel();
                errorArea.setText("");
                CMMLexer cmmLexer = new CMMLexer();
                cmmLexer.setSourceText(textArea.getText());
                cmmLexer.execute(textArea.getText());
                ArrayList<Token> tokens = cmmLexer.getTokens();
                CMMParser cmmParser = new CMMParser(tokens);
                TreeNode root = cmmParser.execute();

                JTree jTree = getJTree(root);
                JFrame treeFrame = new JFrame("语法分析树");
                treeFrame.setBounds(200, 200, 600, 600);
                JPanel panel = new JPanel();
                panel.setLayout(null);
                treeFrame.add(panel);
                JScrollPane jScrollPane = new JScrollPane(jTree);
                jScrollPane.setBounds(10, 10, 550, 500);
                jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                panel.add(jScrollPane);

                treeFrame.setVisible(true);

                String lexerError = cmmLexer.getErrorInfo();
                String parserError = cmmParser.getErrorInfo();
                errorArea.append("词法分析共" + cmmLexer.getErrorNum() + "个错误：" + "\n");
                if (lexerError != null && !lexerError.equals("")) {
                    errorArea.append(lexerError);
                }
                errorArea.append("\n" + "语法分析共" + cmmParser.getErrorNum() + "个错误：" + "\n");
                if (parserError != null && !parserError.equals("")) {
                    errorArea.append(parserError);
                }

            }
        });

        executeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                errorArea.setText("");
                resultArea.setText("");
                CMMLexer cmmLexer = new CMMLexer();
                cmmLexer.setSourceText(textArea.getText());
                cmmLexer.execute(textArea.getText());
                ArrayList<Token> tokens = cmmLexer.getTokens();
                CMMParser cmmParser = new CMMParser(tokens);
                TreeNode root = cmmParser.execute();

                if (cmmLexer.getErrorNum() != 0 || cmmParser.getErrorNum() != 0 || root == null) {
                    String lexerError = cmmLexer.getErrorInfo();
                    String parserError = cmmParser.getErrorInfo();
                    errorArea.append("词法分析共" + cmmLexer.getErrorNum() + "个错误：" + "\n");
                    if (lexerError != null && !lexerError.equals("")) {
                        errorArea.append(lexerError);
                    }
                    errorArea.append("\n" + "语法分析共" + cmmParser.getErrorNum() + "个错误：" + "\n");
                    if (parserError != null && !parserError.equals("")) {
                        errorArea.append(parserError);
                    }
                } else {
                    CMMSemanticAnalysis cmmSemanticAnalysis = new CMMSemanticAnalysis(root);
                    //添加read操作
                    resultArea.addKeyListener(new KeyAdapter() {
                        @Override
                        public void keyPressed(KeyEvent e) {
                            super.keyPressed(e);
                            if(e.getKeyCode()== KeyEvent.VK_ENTER){
                                String content = resultArea.getText();
                                String[] values = content.split("\n");
                                String input = values[values.length-1];
                                cmmSemanticAnalysis.setUserInput(input);
                            }
                        }
                    });

                    cmmSemanticAnalysis.start();

                    String errorInfo = cmmSemanticAnalysis.getErrorInfo();
                    errorArea.setText(errorInfo);
                }
            }
        });

    }

    private void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | UnsupportedLookAndFeelException | IllegalAccessException e1) {
            e1.printStackTrace();
        }
    }

    private JTree getJTree(TreeNode treeNode) {
        DefaultTreeModel model = new DefaultTreeModel(treeNode);
        JTree tree = new JTree(model);
        TreeNode root = (TreeNode) tree.getModel().getRoot();
        expandTree(tree, new TreePath(root));
        return tree;
    }

    private void expandTree(JTree tree, TreePath parent) {
         //有重复定义的TreeNode，所以此处加上包的名字
        javax.swing.tree.TreeNode node = (javax.swing.tree.TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration<?> e = node.children(); e.hasMoreElements(); ) {
                javax.swing.tree.TreeNode n = (javax.swing.tree.TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandTree(tree, path);
            }
        }
        tree.expandPath(parent);
    }

    public static JTextArea getResultArea() {
        return resultArea;
    }

    public static void setResultAreaTest(String text) {
        resultArea.setText(text);
    }

    public static JTextArea getErrorArea() {
        return errorArea;
    }

    public static void setErrorAreaText(String text) {
        errorArea.setText(text);
    }
}
