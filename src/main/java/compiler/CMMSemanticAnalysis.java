package compiler;

import GUI.CompilerGUI;
import structure.ConstVar;
import structure.SymbolTable;
import structure.SymbolTableElement;
import structure.TreeNode;

import javax.swing.*;
import java.math.BigDecimal;

public class CMMSemanticAnalysis extends Thread {
    private SymbolTable table = new SymbolTable();      //语义分析时的符号表

    private TreeNode root;                              //语法分析得到的抽象语法树的根节点

    private String errorInfo = "";                      //语义分析错误信息

    private int errorNum = 0;                           //语义分析错误个数

    private int level = 0;                              //语义分析标识符作用域，进入大括号+1，退出-1

    private String userInput;                           //用户输入

    public CMMSemanticAnalysis(TreeNode root) {
        this.root = root;
    }

    private void error(String error, int line) {
        errorNum++;
        String s = ConstVar.ERROR + "第 " + line + " 行：" + error + "\n";
        errorInfo += s;
    }

    /**
     * 功能介绍：识别正确的整数：排除多个零的情况
     * @param input 要识别的字符串
     * @return 布尔值
     */
    private static boolean matchInteger(String input) {
        return input.matches("^-?\\d+$") && !input.matches("^-?0{1,}\\d+$");
    }

    /**
     * 功能介绍：识别正确的浮点数：排除00.000的情况
     * @param input 要识别的字符串
     * @return 布尔值
     */
    private static boolean matchReal(String input) {
        return input.matches("^(-?\\d+)(\\.\\d+)+$") && !input.matches("^(-?0{2,}+)(\\.\\d+)+$");
    }

    /**
     * 功能介绍：设置用户输入
     * @param userInput 输入的内容
     */
    public synchronized void setUserInput(String userInput) {
        //当需要进行设置用户输入的时候，唤醒该进程
        this.userInput = userInput;
        notify();
    }

    /**
     * 功能介绍：读取用户输入（先挂起，userInput不为空的时候唤醒）
     * @return 返回用户输入内容的字符串形式
     */
    private synchronized String readInput() {
        String result;
        try {
            while (userInput == null) {
                wait();
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        result = userInput;
        userInput = null;
        return result;
    }

    /**
     * 功能介绍：进程运行时执行的方法
     */
    public void run() {
        table.removeAll();
        statement(root);
        if (errorNum != 0) {
            CompilerGUI.getErrorArea().append("该程序中共有" + errorNum + "个语义错误！\n");
            CompilerGUI.getErrorArea().append(errorInfo);
        }
    }

    /**
     * 功能介绍：语义分析主方法
     * @param root 根结点
     */
    private void statement(TreeNode root) {
        for (int i = 0; i < root.getChildCount(); i++) {
            TreeNode currentNode = root.getChildAt(i);
            String content = currentNode.getContent();
            switch (content) {
                case ConstVar.INT:
                case ConstVar.REAL:
                case ConstVar.BOOL:
                case ConstVar.STRING:
                    forDeclare(currentNode);
                    break;
                case ConstVar.ASSIGN:
                    forAssign(currentNode);
                    break;
                case ConstVar.FOR:
                    // 进入for循环语句，改变作用域
                    level++;
                    forFor(currentNode);
                    // 退出for循环语句，改变作用域并更新符号表
                    level--;
                    table.update(level);       //当level减小时更新符号表,去除无用的元素
                    break;
                case ConstVar.IF:
                    // 进入if语句，改变作用域
                    level++;
                    forIf(currentNode);
                    // 退出if语句，改变作用域并更新符号表
                    level--;
                    table.update(level);
                    break;
                case ConstVar.WHILE:
                    // 进入while语句，改变作用域
                    level++;
                    forWhile(currentNode);
                    // 退出while语句，改变作用域并更新符号表
                    level--;
                    table.update(level);
                    break;
                case ConstVar.READ:
                    forRead(currentNode.getChildAt(0));
                    break;
                case ConstVar.WRITE:
                    forWrite(currentNode.getChildAt(0));
                    break;
            }
        }
    }

    /**
     * 功能介绍：分析declare语句
     * @param root 根结点
     */
    private void forDeclare(TreeNode root) {
        // 结点显示的内容,即声明变量的类型int real bool string
        String content = root.getContent();
        int index = 0;
        while (index < root.getChildCount()) {
            TreeNode temp = root.getChildAt(index);
            // 变量名
            String name = temp.getContent();
            // 判断变量是否已经被声明
            if (table.getCurrentLevel(name, level) == null) {
                // 声明普通变量(非数组)
                if (temp.getChildCount() == 0) {
                    SymbolTableElement element = new SymbolTableElement(temp.getContent(), content, temp.getLineNum(), level);
                    index++;
                    // 判断变量是否在声明时被初始化
                    if (index < root.getChildCount()
                            && root.getChildAt(index).getContent().equals(ConstVar.ASSIGN)) {
                        // 获得变量的初始值结点
                        TreeNode valueNode = root.getChildAt(index).getChildAt(0);
                        String value = valueNode.getContent();
                        switch (content) {
                            case ConstVar.INT:  // 声明int型变量
                                if (matchInteger(value)) {
                                    element.setIntValue(value);
                                    element.setRealValue(String.valueOf(Double.parseDouble(value)));
                                } else if (matchReal(value)) {
                                    String error = "不能将浮点数赋值给整型变量";
                                    error(error, valueNode.getLineNum());
                                } else if (value.equals("true")|| value.equals("false")) {
                                    String error = "不能将" + value + "赋值给整型变量";
                                    error(error, valueNode.getLineNum());
                                } else if (valueNode.getNodeKind().equals("字符串")) {
                                    String error = "不能将字符串赋值给整型变量";
                                    error(error, valueNode.getLineNum());
                                } else if (valueNode.getNodeKind().equals("标识符")) {
                                    if (checkID(valueNode, level)) {
                                        switch (table.getAllLevel(valueNode.getContent(), level).getKind()) {
                                            case ConstVar.INT:
                                                element.setIntValue(table.getAllLevel(
                                                        valueNode.getContent(), level).getIntValue());
                                                element.setRealValue(table.getAllLevel(
                                                        valueNode.getContent(), level).getRealValue());
                                                break;
                                            case ConstVar.REAL: {
                                                String error = "不能将浮点型变量赋值给整型变量";
                                                error(error, valueNode.getLineNum());
                                                break;
                                            }
                                            case ConstVar.BOOL: {
                                                String error = "不能将布尔型变量赋值给整型变量";
                                                error(error, valueNode.getLineNum());
                                                break;
                                            }
                                            case ConstVar.STRING: {
                                                String error = "不能将字符串变量赋值给整型变量";
                                                error(error, valueNode.getLineNum());
                                                break;
                                            }
                                        }
                                    } else {
                                        return;
                                    }
                                } else if (value.equals(ConstVar.PLUS)
                                        || value.equals(ConstVar.MINUS)
                                        || value.equals(ConstVar.TIMES)
                                        || value.equals(ConstVar.DIVIDE)) {
                                    String result = forExpression(valueNode);
                                    if (result != null) {
                                        if (matchInteger(result)) {
                                            element.setIntValue(result);
                                            element.setRealValue(String.valueOf(Double.parseDouble(result)));
                                        } else if (matchReal(result)) {
                                            String error = "不能将浮点数赋值给整型变量";
                                            error(error, valueNode.getLineNum());
                                            return;
                                        } else {
                                            return;
                                        }
                                    } else {
                                        return;
                                    }
                                }
                                break;
                            case ConstVar.REAL:  // 声明real型变量
                                if (matchInteger(value)) {
                                    element.setRealValue(String.valueOf(Double.parseDouble(value)));   //将整数变成小数类型
                                } else if (matchReal(value)) {
                                    element.setRealValue(value);
                                } else if (value.equals("true")|| value.equals("false")) {
                                    String error = "不能将" + value + "赋值给浮点型变量";
                                    error(error, valueNode.getLineNum());
                                } else if (valueNode.getNodeKind().equals("字符串")) {
                                    String error = "不能将字符串给浮点型变量";
                                    error(error, valueNode.getLineNum());
                                } else if (valueNode.getNodeKind().equals("标识符")) {
                                    if (checkID(valueNode, level)) {
                                        switch (table.getAllLevel(valueNode.getContent(), level).getKind()) {
                                            case ConstVar.INT:
                                            case ConstVar.REAL:
                                                element.setRealValue(table.getAllLevel(
                                                        valueNode.getContent(), level).getRealValue());
                                                break;
                                            case ConstVar.BOOL: {
                                                String error = "不能将布尔型变量赋值给浮点型变量";
                                                error(error, valueNode.getLineNum());
                                                break;
                                            }
                                            case ConstVar.STRING: {
                                                String error = "不能将字符串变量赋值给浮点型变量";
                                                error(error, valueNode.getLineNum());
                                                break;
                                            }
                                        }
                                    } else {
                                        return;
                                    }
                                } else if (value.equals(ConstVar.PLUS)
                                        || value.equals(ConstVar.MINUS)
                                        || value.equals(ConstVar.TIMES)
                                        || value.equals(ConstVar.DIVIDE)) {
                                    String result = forExpression(valueNode);
                                    if (result != null) {
                                        if (matchInteger(result)) {
                                            element.setRealValue(String.valueOf(Double.parseDouble(result)));
                                        } else if (matchReal(result)) {
                                            element.setRealValue(result);
                                        }
                                    } else {
                                        return;
                                    }
                                }
                                break;
                            case ConstVar.STRING:  // 声明string型变量
                                if (matchInteger(value)) {
                                    String error = "不能将整数赋值给字符串型变量";
                                    error(error, valueNode.getLineNum());
                                } else if (matchReal(value)) {
                                    String error = "不能将浮点数赋值给字符串型变量";
                                    error(error, valueNode.getLineNum());
                                } else if (value.equals("true")|| value.equals("false")) {
                                    String error = "不能将" + value + "赋值给字符串型变量";
                                    error(error, valueNode.getLineNum());
                                } else if (valueNode.getNodeKind().equals("字符串")) {
                                    element.setStringValue(value);
                                } else if (valueNode.getNodeKind().equals("标识符")) {
                                    if (checkID(valueNode, level)) {
                                        switch (table.getAllLevel(valueNode.getContent(), level).getKind()) {
                                            case ConstVar.INT: {
                                                String error = "不能将整数赋值给字符串型变量";
                                                error(error, valueNode.getLineNum());
                                                break;
                                            }
                                            case ConstVar.REAL: {
                                                String error = "不能将浮点数赋值给字符串型变量";
                                                error(error, valueNode.getLineNum());
                                                break;
                                            }
                                            case ConstVar.BOOL: {
                                                String error = "不能将布尔型变量赋值给字符串型变量";
                                                error(error, valueNode.getLineNum());
                                                break;
                                            }
                                            case ConstVar.STRING:
                                                element.setStringValue(value);
                                                break;
                                        }
                                    } else {
                                        return;
                                    }
                                } else if (value.equals(ConstVar.PLUS)
                                        || value.equals(ConstVar.MINUS)
                                        || value.equals(ConstVar.TIMES)
                                        || value.equals(ConstVar.DIVIDE)) {
                                    //不允许对字符串的+-*/操作
                                    String error = "不能将算术表达式赋值给字符串型变量";
                                    error(error, valueNode.getLineNum());
                                }
                                break;
                            default:  // 声明bool型变量
                                if (matchInteger(value)) {
                                    // 如果是0或负数则记为false,其他记为true
                                    int i = Integer.parseInt(value);
                                    if (i <= 0)
                                        element.setStringValue("false");
                                    else
                                        element.setStringValue("true");
                                } else if (matchReal(value)) {
                                    String error = "不能将浮点数赋值给布尔型变量";
                                    error(error, valueNode.getLineNum());
                                } else if (value.equals("true") || value.equals("false")) {
                                    element.setStringValue(value);
                                } else if (valueNode.getNodeKind().equals("字符串")) {
                                    String error = "不能将字符串给布尔型变量";
                                    error(error, valueNode.getLineNum());
                                } else if (valueNode.getNodeKind().equals("标识符")) {
                                    if (checkID(valueNode, level)) {
                                        switch (table.getAllLevel(
                                                valueNode.getContent(), level).getKind()) {
                                            case ConstVar.INT:
                                                int i = Integer.parseInt(table.getAllLevel(
                                                                valueNode.getContent(),level).getIntValue());
                                                if (i <= 0)
                                                    element.setStringValue("false");
                                                else
                                                    element.setStringValue("true");
                                                break;
                                            case ConstVar.REAL: {
                                                String error = "不能将浮点型变量赋值给布尔型变量";
                                                error(error, valueNode.getLineNum());
                                                break;
                                            }
                                            case ConstVar.BOOL:
                                                element.setStringValue(table.getAllLevel(
                                                        valueNode.getContent(),level).getStringValue());
                                                break;
                                            case ConstVar.STRING: {
                                                String error = "不能将字符串变量赋值给布尔型变量";
                                                error(error, valueNode.getLineNum());
                                                break;
                                            }
                                        }
                                    } else {
                                        return;
                                    }
                                } else if (value.equals(ConstVar.EQUAL)
                                        || value.equals(ConstVar.NEQUAL)
                                        || value.equals(ConstVar.LT)
                                        || value.equals(ConstVar.GT)) {
                                    boolean result = forCondition(valueNode);
                                    if (result) {
                                        element.setStringValue("true");
                                    } else {
                                        element.setStringValue("false");
                                    }
                                }
                                break;
                        }
                        index++;
                    }
                    table.add(element);
                } else { // 声明数组
                    SymbolTableElement element = new SymbolTableElement(temp.getContent(), content, temp.getLineNum(), level);
                    String sizeValue = temp.getChildAt(0).getContent();
                    if (matchInteger(sizeValue)) {
                        int i = Integer.parseInt(sizeValue);
                        if (i < 1) {
                            String error = "数组大小必须大于零";
                            error(error, root.getLineNum());
                            return;
                        }
                    } else if (temp.getChildAt(0).getNodeKind().equals("标识符")) {
                        if (checkID(root, level)) {
                            SymbolTableElement tempElement = table.getAllLevel(root.getContent(), level);
                            if (tempElement.getKind().equals(ConstVar.INT)) {
                                int i = Integer.parseInt(tempElement.getIntValue());
                                if (i < 1) {
                                    String error = "数组大小必须大于零";
                                    error(error, root.getLineNum());
                                    return;
                                } else {
                                    sizeValue = tempElement.getIntValue();
                                }
                            } else {
                                String error = "类型不匹配,数组大小必须为整数类型";
                                error(error, root.getLineNum());
                                return;
                            }
                        } else {
                            return;
                        }
                    } else if (sizeValue.equals(ConstVar.PLUS)
                            || sizeValue.equals(ConstVar.MINUS)
                            || sizeValue.equals(ConstVar.TIMES)
                            || sizeValue.equals(ConstVar.DIVIDE)) {
                        sizeValue = forExpression(temp.getChildAt(0));
                        if (sizeValue != null) {
                            if (matchInteger(sizeValue)) {
                                int i = Integer.parseInt(sizeValue);
                                if (i < 1) {
                                    String error = "数组大小必须大于零";
                                    error(error, root.getLineNum());
                                    return;
                                }
                            } else {
                                String error = "类型不匹配,数组大小必须为整数类型";
                                error(error, root.getLineNum());
                                return;
                            }
                        } else {
                            return;
                        }
                    }
                    if(matchReal(sizeValue)){
                        String error = "类型不匹配,数组大小必须为整数类型";
                        error(error, root.getLineNum());
                        return;
                    }
                    element.setArrayElementsNum(Integer.parseInt(sizeValue));
                    table.add(element);
                    index++;
                    for (int j = 0; j < Integer.parseInt(sizeValue); j++) {
                        String s = temp.getContent() + "@" + j;
                        SymbolTableElement ste = new SymbolTableElement(s, content, temp.getLineNum(), level);
                        table.add(ste);
                    }
                }
            } else { // 报错
                String error = "变量" + name + "已被声明,请重命名该变量";
                error(error, temp.getLineNum());
                return;
            }
        }
    }

    /**
     * 功能介绍：分析assign语句
     * @param root 语法树中assign语句结点
     */
    private void forAssign(TreeNode root) {
        // 赋值语句左半部分
        TreeNode node1 = root.getChildAt(0);
        // 赋值语句左半部分标识符
        String node1Value = node1.getContent();
        if (table.getAllLevel(node1Value, level) != null) {
            if (node1.getChildCount() != 0) {
                String s = forArray(node1.getChildAt(0), table.getAllLevel(node1Value, level).getArrayElementsNum());
                if (s != null)
                    node1Value += "@" + s;
                else
                    return;
            }
        } else {
            String error = "变量" + node1Value + "在使用前未声明";
            error(error, node1.getLineNum());
            return;
        }
        // 赋值语句左半部分标识符类型
        String node1Kind = table.getAllLevel(node1Value, level).getKind();
        // 赋值语句右半部分
        TreeNode node2 = root.getChildAt(1);
        String node2Kind = node2.getNodeKind();
        String node2Value = node2.getContent();
        // 赋值语句右半部分的值
        String value = "";
        if (node2Kind.equals("整数")) { // 整数
            value = node2Value;
            node2Kind = "int";
        } else if (node2Kind.equals("实数")) { // 实数
            value = node2Value;
            node2Kind = "real";
        } else if (node2Kind.equals("字符串")) { // 字符串
            value = node2Value;
            node2Kind = "string";
        } else if (node2Kind.equals("布尔值")) { // true和false
            value = node2Value;
            node2Kind = "bool";
        } else if (node2Kind.equals("标识符")) { // 标识符
            if (checkID(node2, level)) {
                if (node2.getChildCount() != 0) {
                    String s = forArray(node2.getChildAt(0), table.getAllLevel(node2Value, level).getArrayElementsNum());
                    if (s != null)
                        node2Value += "@" + s;
                    else
                        return;
                }
                SymbolTableElement temp = table.getAllLevel(node2Value, level);
                switch (temp.getKind()) {
                    case ConstVar.INT:
                        value = temp.getIntValue();
                        break;
                    case ConstVar.REAL:
                        value = temp.getRealValue();
                        break;
                    case ConstVar.BOOL:
                    case ConstVar.STRING:
                        value = temp.getStringValue();
                        break;
                }
                node2Kind = table.getAllLevel(node2Value, level).getKind();
            } else {
                return;
            }
        } else if (node2Value.equals(ConstVar.PLUS)
                || node2Value.equals(ConstVar.MINUS)
                || node2Value.equals(ConstVar.TIMES)
                || node2Value.equals(ConstVar.DIVIDE)) { // 表达式
            String result = forExpression(node2);
            if (result != null) {
                if (matchInteger(result))
                    node2Kind = "int";
                else if (matchReal(result))
                    node2Kind = "real";
                value = result;
            } else {
                return;
            }
        } else if (node2Value.equals(ConstVar.EQUAL)
                || node2Value.equals(ConstVar.NEQUAL)
                || node2Value.equals(ConstVar.LT)
                || node2Value.equals(ConstVar.GT)) { // 逻辑表达式
            boolean result = forCondition(node2);
            node2Kind = "bool";
            value = String.valueOf(result);
        }
        switch (node1Kind) {
            case ConstVar.INT:
                switch (node2Kind) {
                    case ConstVar.INT:
                        table.getAllLevel(node1Value, level).setIntValue(value);
                        table.getAllLevel(node1Value, level).setRealValue(String.valueOf(Double.parseDouble(value)));
                        break;
                    case ConstVar.REAL: {
                        String error = "不能将浮点数赋值给整型变量";
                        error(error, node1.getLineNum());
                        return;
                    }
                    case ConstVar.BOOL: {
                        String error = "不能将布尔值赋值给整型变量";
                        error(error, node1.getLineNum());
                        return;
                    }
                    case ConstVar.STRING: {
                        String error = "不能将字符串给整型变量";
                        error(error, node1.getLineNum());
                        return;
                    }
                }
                break;
            case ConstVar.REAL:
                switch (node2Kind) {
                    case ConstVar.INT:
                        table.getAllLevel(node1Value, level).setRealValue(String.valueOf(Double.parseDouble(value)));
                        break;
                    case ConstVar.REAL:
                        table.getAllLevel(node1Value, level).setRealValue(value);
                        break;
                    case ConstVar.BOOL: {
                        String error = "不能将布尔值赋值给浮点型变量";
                        error(error, node1.getLineNum());
                        return;
                    }
                    case ConstVar.STRING: {
                        String error = "不能将字符串给浮点型变量";
                        error(error, node1.getLineNum());
                        return;
                    }
                }
                break;
            case ConstVar.BOOL:
                switch (node2Kind) {
                    case ConstVar.INT:
                        int i = Integer.parseInt(node2Value);
                        if (i <= 0)
                            table.getAllLevel(node1Value, level).setStringValue("false");
                        else
                            table.getAllLevel(node1Value, level).setStringValue("true");
                        break;
                    case ConstVar.REAL: {
                        String error = "不能将浮点数赋值给布尔型变量";
                        error(error, node1.getLineNum());
                        return;
                    }
                    case ConstVar.BOOL:
                        table.getAllLevel(node1Value, level).setStringValue(value);
                        break;
                    case ConstVar.STRING: {
                        String error = "不能将字符串赋值给布尔型变量";
                        error(error, node1.getLineNum());
                        return;
                    }
                }
                break;
            case ConstVar.STRING:
                switch (node2Kind) {
                    case ConstVar.INT: {
                        String error = "不能将整数赋值给字符串变量";
                        error(error, node1.getLineNum());
                        return;
                    }
                    case ConstVar.REAL: {
                        String error = "不能将浮点数赋值给字符串变量";
                        error(error, node1.getLineNum());
                        return;
                    }
                    case ConstVar.BOOL: {
                        String error = "不能将布尔变量赋值给字符串变量";
                        error(error, node1.getLineNum());
                        return;
                    }
                    case ConstVar.STRING:
                        table.getAllLevel(node1Value, level).setStringValue(value);
                        break;
                }
                break;
        }
    }

    /**
     * 功能介绍：分析for语句
     * @param root 语法树中for语句结点(存在逻辑错误问题未能实现)
     */
    private void forFor(TreeNode root) {
        // 根结点Initialization
        TreeNode initializationNode = root.getChildAt(0);
        // 根结点Condition
        TreeNode conditionNode = root.getChildAt(1);
        // 根结点Change
        TreeNode changeNode = root.getChildAt(2);
        // 根结点Statements
        TreeNode statementNode = root.getChildAt(3);
        // for循环语句初始化
        forAssign(initializationNode.getChildAt(0));
        // 条件为真
        while (forCondition(conditionNode.getChildAt(0))) {
            statement(statementNode);
            level--;
            table.update(level);
            level++;
            // for循环执行一次后改变循环条件中的变量
            forAssign(changeNode.getChildAt(0));
        }
    }

    /**
     * 功能介绍：分析if语句
     * @param root 语法树中if语句结点
     */
    private void forIf(TreeNode root) {
        int count = root.getChildCount();
        // 根结点Condition
        TreeNode conditionNode = root.getChildAt(0);
        // 根结点Statements（中间代码块）
        TreeNode statementNode = root.getChildAt(1);
        // 条件为真
        if (forCondition(conditionNode.getChildAt(0))) {
            statement(statementNode);
        } else if (count == 3) { // 条件为假且有else语句
            TreeNode elseNode = root.getChildAt(2);
            level++;
            statement(elseNode);
            level--;
            table.update(level);
        }  // 条件为假同时没有else语句

    }

    /**
     * 功能介绍：分析while语句
     * @param root 语法树中while语句结点
     */
    private void forWhile(TreeNode root) {
        // 根结点Condition
        TreeNode conditionNode = root.getChildAt(0);
        // 根结点Statements
        TreeNode statementNode = root.getChildAt(1);
        while (forCondition(conditionNode.getChildAt(0))) {
            statement(statementNode);
            level--;
            table.update(level);
            level++;
        }
    }

    /**
     * 功能介绍：分析read语句
     * @param root 语法树中read语句结点
     */
    private void forRead(TreeNode root) {
        // 要读取的变量的名字
        String idName = root.getContent();
        // 查找变量
        SymbolTableElement element = table.getAllLevel(idName, level);
        // 判断变量是否已经声明
        if (element != null) {
            if (root.getChildCount() != 0) {//数组的情况
                String s = forArray(root.getChildAt(0), element.getArrayElementsNum());
                if (s != null) {
                    idName += "@" + s;
                } else {
                    return;
                }
            }
            String value = readInput();
            switch (element.getKind()) {
                case ConstVar.INT:
                    if (matchInteger(value)) {
                        table.getAllLevel(idName, level).setIntValue(value);
                        table.getAllLevel(idName, level).setRealValue(String.valueOf(Double.parseDouble(value)));
                    } else { // 报错
                        String error = "不能将\"" + value + "\"赋值给变量" + idName;
                        JOptionPane.showMessageDialog(new JPanel(), error, "输入错误",JOptionPane.ERROR_MESSAGE);
                    }
                    break;
                case ConstVar.REAL:
                    if (matchReal(value)) {
                        table.getAllLevel(idName, level).setRealValue(value);
                    } else if (matchInteger(value)) {
                        table.getAllLevel(idName, level).setRealValue(String.valueOf(Double.parseDouble(value)));
                    } else { // 报错
                        String error = "不能将\"" + value + "\"赋值给变量" + idName;
                        JOptionPane.showMessageDialog(new JPanel(), error, "输入错误",JOptionPane.ERROR_MESSAGE);
                    }
                    break;
                case ConstVar.BOOL:
                    switch (value) {
                        case "true":
                            table.getAllLevel(idName, level).setStringValue("true");
                            break;
                        case "false":
                            table.getAllLevel(idName, level).setStringValue("false");
                            break;
                        default:  // 报错
                            String error = "不能将\"" + value + "\"赋值给变量" + idName;
                            JOptionPane.showMessageDialog(new JPanel(), error, "输入错误",JOptionPane.ERROR_MESSAGE);
                            break;
                    }
                    break;
                case ConstVar.STRING:
                    table.getAllLevel(idName, level).setStringValue(value);
                    break;
            }
        } else { // 报错
            String error = "变量" + idName + "在使用前未声明";
            error(error, root.getLineNum());
        }
    }

    /**
     * 功能介绍：分析write语句
     * @param root 语法树中write语句结点
     */
    private void forWrite(TreeNode root) {
        // 结点显示的内容
        String content = root.getContent();
        // 结点的类型
        String kind = root.getNodeKind();
        if (kind.equals("整数") || kind.equals("实数")) { // 常量
            CompilerGUI.getResultArea().setText(CompilerGUI.getResultArea().getText() + content + "\n");
        } else if (kind.equals("字符串")) { // 字符串
            CompilerGUI.getResultArea().setText(CompilerGUI.getResultArea().getText() + content + "\n");
        } else if (kind.equals("标识符")) { // 标识符
            if (checkID(root, level)) {
                if (root.getChildCount() != 0) {
                    String s = forArray(root.getChildAt(0), table.getAllLevel(
                            content, level).getArrayElementsNum());
                    if (s != null)
                        content += "@" + s;
                    else
                        return;
                }
                SymbolTableElement temp = table.getAllLevel(content, level);
                switch (temp.getKind()) {
                    case ConstVar.INT:
                        CompilerGUI.getResultArea().setText(CompilerGUI.getResultArea().getText() + temp.getIntValue() + "\n");
                        break;
                    case ConstVar.REAL:
                        CompilerGUI.getResultArea().setText(CompilerGUI.getResultArea().getText() + temp.getRealValue() + "\n");
                        break;
                    default:
                        CompilerGUI.getResultArea().setText(CompilerGUI.getResultArea().getText() + temp.getStringValue() + "\n");
                        break;
                }
            }
        } else if (content.equals(ConstVar.PLUS)
                || content.equals(ConstVar.MINUS)
                || content.equals(ConstVar.TIMES)
                || content.equals(ConstVar.DIVIDE)) { // 表达式,先计算出结果再将结果输出
            String value = forExpression(root);
            if (value != null) {
                CompilerGUI.getResultArea().setText(CompilerGUI.getResultArea().getText() + value + "\n");
            }
        }
    }

    /**
     * 功能介绍：分析if和while语句的条件
     * @param root 根结点
     * @return 返回计算结果
     */
    private boolean forCondition(TreeNode root) {
        // > < <> == true false 布尔变量
        String content = root.getContent();
        if (content.equals(ConstVar.TRUE)) {
            return true;
        } else if (content.equals(ConstVar.FALSE)) {
            return false;
        } else if (root.getNodeKind().equals("标识符")) {
            if (checkID(root, level)) {
                if (root.getChildCount() != 0) {//标识符为数组的情况
                    String s = forArray(root.getChildAt(0), table.getAllLevel(
                            content, level).getArrayElementsNum());
                    if (s != null)
                        content += "@" + s;
                    else
                        return false;
                }
                SymbolTableElement temp = table.getAllLevel(content, level);
                if (temp.getKind().equals(ConstVar.BOOL)) {
                    return temp.getStringValue().equals(ConstVar.TRUE);
                } else { // 报错
                    String error = "不能将变量" + content + "作为判断条件";
                    error(error, root.getLineNum());
                }
            } else {
                return false;
            }
        } else if (content.equals(ConstVar.EQUAL)
                || content.equals(ConstVar.NEQUAL)
                || content.equals(ConstVar.LT) || content.equals(ConstVar.GT)) {
            // 存放两个待比较对象的值
            String[] results = new String[2];
            for (int i = 0; i < root.getChildCount(); i++) {
                String kind = root.getChildAt(i).getNodeKind();
                String tempContent = root.getChildAt(i).getContent();
                if (kind.equals("整数") || kind.equals("实数")) { // 常量
                    results[i] = tempContent;
                } else if (kind.equals("标识符")) { // 标识符
                    if (checkID(root.getChildAt(i), level)) {
                        if (root.getChildAt(i).getChildCount() != 0) {
                            String s = forArray(root.getChildAt(i).getChildAt(0),
                                    table.getAllLevel(tempContent, level).getArrayElementsNum());
                            if (s != null)
                                tempContent += "@" + s;
                            else
                                return false;
                        }
                        SymbolTableElement temp = table.getAllLevel(tempContent, level);
                        if (temp.getKind().equals(ConstVar.INT)) {
                            results[i] = temp.getIntValue();
                        } else {
                            results[i] = temp.getRealValue();
                        }
                    } else {
                        return false;
                    }
                } else if (tempContent.equals(ConstVar.PLUS)
                        || tempContent.equals(ConstVar.MINUS)
                        || tempContent.equals(ConstVar.TIMES)
                        || tempContent.equals(ConstVar.DIVIDE)) { // 表达式
                    String result = forExpression(root.getChildAt(i));
                    if (result != null)
                        results[i] = result;
                    else
                        return false;
                }
            }
            if (!results[0].equals("") && !results[1].equals("")) {
                double element1 = Double.parseDouble(results[0]);
                double element2 = Double.parseDouble(results[1]);
                switch (content) {
                    case ConstVar.GT:  // >
                        if (element1 > element2)
                            return true;
                        break;
                    case ConstVar.LT:  // <
                        if (element1 < element2)
                            return true;
                        break;
                    case ConstVar.EQUAL:  // ==
                        if (element1 == element2)
                            return true;
                        break;
                    default:  // <>
                        if (element1 != element2)
                            return true;
                        break;
                }
            }
        }
        // 语义分析出错或者分析条件结果为假返回false
        return false;
    }

    /**
     * 功能介绍：分析表达式并计算
     * @param root 根结点
     * @return 返回计算结果
     */
    private String forExpression(TreeNode root) {
        boolean isInt = true;
        // + -
        String content = root.getContent();
        // 存放两个运算对象的值
        String[] results = new String[2];
        for (int i = 0; i < root.getChildCount(); i++) {
            TreeNode tempNode = root.getChildAt(i);
            String kind = tempNode.getNodeKind();
            String tempContent = tempNode.getContent();
            if (kind.equals("整数")) { // 整数
                results[i] = tempContent;
            } else if (kind.equals("实数")) { // 实数
                results[i] = tempContent;
                isInt = false;
            } else if (kind.equals("标识符")) { // 标识符
                if (checkID(tempNode, level)) {
                    if (tempNode.getChildCount() != 0) {
                        String s = forArray(tempNode.getChildAt(0),
                                table.getAllLevel(tempContent, level).getArrayElementsNum());
                        if (s != null)
                            tempContent += "@" + s;
                        else
                            return null;
                    }
                    SymbolTableElement temp = table.getAllLevel(tempNode.getContent(), level);
                    if (temp.getKind().equals(ConstVar.INT)) {
                        results[i] = temp.getIntValue();
                    } else if (temp.getKind().equals(ConstVar.REAL)) {
                        results[i] = temp.getRealValue();
                        isInt = false;
                    }
                } else {
                    return null;
                }
            } else if (tempContent.equals(ConstVar.PLUS)
                    || tempContent.equals(ConstVar.MINUS)
                    || tempContent.equals(ConstVar.TIMES)
                    || tempContent.equals(ConstVar.DIVIDE)) { // 表达式
                String result = forExpression(root.getChildAt(i));
                if (result != null) {
                    results[i] = result;
                    if (matchReal(result))
                        isInt = false;
                } else
                    return null;
            }
        }
        if (isInt) {
            int e1 = Integer.parseInt(results[0]);
            int e2 = Integer.parseInt(results[1]);
            switch (content) {
                case ConstVar.PLUS:
                    return String.valueOf(e1 + e2);
                case ConstVar.MINUS:
                    return String.valueOf(e1 - e2);
                case ConstVar.TIMES:
                    return String.valueOf(e1 * e2);
                default:
                    if (e2 == 0) {
                        String error = "除数不能为0";
                        error(error, root.getLineNum());
                        return null;
                    } else {
                        return String.valueOf(e1 / e2);
                    }
            }
        } else {
            double e1 = Double.parseDouble(results[0]);
            double e2 = Double.parseDouble(results[1]);
            BigDecimal bd1 = new BigDecimal(e1);
            BigDecimal bd2 = new BigDecimal(e2);
            switch (content) {
                case ConstVar.PLUS:
                    return String.valueOf(bd1.add(bd2).floatValue());
                case ConstVar.MINUS:
                    return String.valueOf(bd1.subtract(bd2).floatValue());
                case ConstVar.TIMES:
                    return String.valueOf(bd1.multiply(bd2).floatValue());
                default:
                    if(bd2.equals(BigDecimal.valueOf(0))){
                        String error = "除数不能为0";
                        error(error, root.getLineNum());
                        return null;
                    }
                    return String.valueOf(bd1.divide(bd2, 3,BigDecimal.ROUND_HALF_UP).floatValue());
            }
        }
    }

    /**
     * 功能介绍： array
     * @param root      根结点
     * @param arraySize 数组大小
     * @return 出错返回null
     */
    private String forArray(TreeNode root, int arraySize) {
        if (root.getNodeKind().equals("整数")) {
            int i = Integer.parseInt(root.getContent());
            if (i > -1 && i < arraySize) {
                return root.getContent();
            } else if (i < 0) {
                String error = "数组下标不能为负数";
                error(error, root.getLineNum());
                return null;
            } else {
                String error = "数组下标越界";
                error(error, root.getLineNum());
                return null;
            }
        } else if (root.getNodeKind().equals("标识符")) {
            // 检查标识符
            if (checkID(root, level)) {
                SymbolTableElement temp = table.getAllLevel(root.getContent(),level);
                if (temp.getKind().equals(ConstVar.INT)) {
                    int i = Integer.parseInt(temp.getIntValue());
                    if (i > -1 && i < arraySize) {
                        return temp.getIntValue();
                    } else if (i < 0) {
                        String error = "数组下标不能为负数";
                        error(error, root.getLineNum());
                        return null;
                    } else {
                        String error = "数组下标越界";
                        error(error, root.getLineNum());
                        return null;
                    }
                } else {
                    String error = "类型不匹配,数组索引号必须为整数类型";
                    error(error, root.getLineNum());
                    return null;
                }
            } else {
                return null;
            }
        } else if (root.getContent().equals(ConstVar.PLUS)
                || root.getContent().equals(ConstVar.MINUS)
                || root.getContent().equals(ConstVar.TIMES)
                || root.getContent().equals(ConstVar.DIVIDE)) { // 表达式
            String result = forExpression(root);
            if (result != null) {
                if (matchInteger(result)) {
                    int i = Integer.parseInt(result);
                    if (i > -1 && i < arraySize) {
                        return result;
                    } else if (i < 0) {
                        String error = "数组下标不能为负数";
                        error(error, root.getLineNum());
                        return null;
                    } else {
                        String error = "数组下标越界";
                        error(error, root.getLineNum());
                        return null;
                    }
                } else {
                    String error = "类型不匹配,数组索引号必须为整数类型";
                    error(error, root.getLineNum());
                    return null;
                }
            } else
                return null;
        }else {
            String error = "类型不匹配,数组索引号必须为整数类型";
            error(error, root.getLineNum());
            return null;
        }
    }

    /**
     * 功能介绍：检查字符串是否声明和初始化
     * @param root  字符串结点
     * @param level 字符串作用域
     * @return 如果声明且初始化则返回true, 否则返回false
     */
    private boolean checkID(TreeNode root, int level) {
        // 标识符名字
        String idName = root.getContent();
        // 标识符未声明
        if (table.getAllLevel(idName, level) == null) {
            String error = "变量" + idName + "在使用前未声明";
            error(error, root.getLineNum());
            return false;
        } else {
            if (root.getChildCount() != 0) {
                String tempString = forArray(root.getChildAt(0), table
                        .getAllLevel(idName, level).getArrayElementsNum());
                if (tempString != null)
                    idName += "@" + tempString;
                else
                    return false;
            }
            SymbolTableElement temp = table.getAllLevel(idName, level);
            // 变量未初始化
            if (temp.getIntValue().equals("") && temp.getRealValue().equals("")
                    && temp.getStringValue().equals("")) {
                String error = "变量" + idName + "在使用前未初始化";
                error(error, root.getLineNum());
                return false;
            } else {
                return true;
            }
        }
    }

    public String getErrorInfo() {
        return errorInfo;
    }

    public void setErrorInfo(String errorInfo) {
        this.errorInfo = errorInfo;
    }

    public int getErrorNum() {
        return errorNum;
    }

    public void setErrorNum(int errorNum) {
        this.errorNum = errorNum;
    }

}
