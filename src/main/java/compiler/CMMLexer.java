package compiler;

import structure.ConstVar;
import structure.Token;
import structure.TreeNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;


public class CMMLexer {
    private boolean isNotation = false;                             // 注释的标志

    private int errorNum = 0;                                       // 错误个数

    private String errorInfo = "";                                  // 错误信息

    private ArrayList<Token> tokens = new ArrayList<Token>();           // 分析后得到的tokens集合，用于其后的语法及语义分析，忽略空格，注释后的结果

    private ArrayList<Token> displayTokens = new ArrayList<Token>();    // 分析后得到的所有tokens集合，包含注释、空格等

    private String sourceText;                                      //源代码

    private int offset = 0;                                         //用于判断*/

    /**
     * 功能介绍：识别字母
     */
    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    /**
     * 功能介绍：识别数字
     */
    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * 功能介绍：识别正确的整数：排除多个零的情况
     * @param input 要识别的字符串
     * @return 布尔值
     */
    private static boolean matchInteger(String input) {
        return input.matches("^-?\\d+$")
                && !input.matches("^-?0{1,}\\d+$");
    }

    /**
     * 功能介绍：识别正确的浮点数：排除00.000的情况
     * @param input 要识别的字符串
     * @return 布尔值
     */
    private static boolean matchReal(String input) {
        return input.matches("^([-+])?\\d+(\\.\\d+)?$")
                && !input.matches("^(-?0{2,}+)(\\.\\d+)+$");
    }

    /**
     * 功能介绍：识别正确的标识符：由字母、数字、下划线组成，必须以字母开头，不能以下划线结尾
     * @param input 要识别的字符串
     * @return 布尔值
     */
    private static boolean matchID(String input) {
        return input.matches("^\\w+$") && !input.endsWith("_")
                && input.substring(0, 1).matches("[A-Za-z]");
    }

    /**
     * 功能介绍：识别关键字Keyword
     * @param str 要分析的字符串
     * @return 布尔值
     */
    private static boolean isKey(String str) {
        return str.equals(ConstVar.IF) || str.equals(ConstVar.ELSE)
                || str.equals(ConstVar.WHILE) || str.equals(ConstVar.READ)
                || str.equals(ConstVar.WRITE) || str.equals(ConstVar.INT)
                || str.equals(ConstVar.REAL) || str.equals(ConstVar.BOOL)
                || str.equals(ConstVar.STRING) || str.equals(ConstVar.TRUE)
                || str.equals(ConstVar.FALSE) || str.equals(ConstVar.FOR);
    }

    /**
     * 功能介绍: 找到分隔符在str中对应的index索引位置
     */
    private static int find(int begin, String str) {
        if (begin >= str.length()) {
            return str.length();
        }
        for (int i = begin; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\n' || c == ',' || c == ' ' || c == '\t' || c == '{'
                    || c == '}' || c == '(' || c == ')' || c == ';' || c == '='
                    || c == '+' || c == '-' || c == '*' || c == '/' || c == '['
                    || c == ']' || c == '<' || c == '>') {
                return i - 1;
            }
        }
        return str.length();
    }

    /**
     * 功能介绍：分析一行CMM程序，并返回分析一行得到的TreeNode
     * @param cmmText 当前行字符串
     * @param lineNum 当前行号
     * @return 分析生成的TreeNode
     */
    private TreeNode executeLine(String cmmText, int lineNum) {
        // 创建当前行根结点
        String content = "第" + lineNum + "行： " + cmmText;
        TreeNode node = new TreeNode(content);
        // 词法分析每行结束的标志
        cmmText += "\n";
        int length = cmmText.length();
        // switch状态值
        int state = 0;
        // 记录token开始位置
        int begin = 0;
        // 记录token结束位置
        int end;
        // 逐个读取当前行字符，进行分析，如果不能判定，向前多看k位
        for (int i = 0; i < length; i++) {
            offset++;
            char ch = cmmText.charAt(i);
            if (!isNotation) {
                if (ch == '(' || ch == ')' || ch == ';' || ch == '{'
                        || ch == '}' || ch == '[' || ch == ']' || ch == ','
                        || ch == '+' || ch == '-' || ch == '*' || ch == '/'
                        || ch == '=' || ch == '<' || ch == '>' || ch == '"'
                        || isLetter(ch) || isDigit(ch)
                        || String.valueOf(ch).equals(" ") || String.valueOf(ch).equals("\n")
                        || String.valueOf(ch).equals("\r") || String.valueOf(ch).equals("\t")) {
                    switch (state) {
                        case 0:
                            // 分隔符直接打印
                            if (ch == '(' || ch == ')' || ch == ';' || ch == '{'
                                    || ch == '}' || ch == '[' || ch == ']' || ch == ',') {
                                state = 0;
                                node.add(new TreeNode("分隔符 ： " + ch));
                                tokens.add(new Token(lineNum, i + 1, "分隔符", String.valueOf(ch)));
                                displayTokens.add(new Token(lineNum, i + 1, "分隔符", String.valueOf(ch)));
                            } else if (ch == '+')   // 加号+
                                state = 1;
                            else if (ch == '-')   // 减号-
                                state = 2;
                            else if (ch == '*')   // 乘号*
                                state = 3;
                            else if (ch == '/')   // 除号/
                                state = 4;
                            else if (ch == '=')    // 赋值符号==或者等号=
                                state = 5;
                            else if (ch == '<')    // 小于符号<或者不等于<>
                                state = 6;
                            else if (ch == '>')    // 大于>
                                state = 9;
                            else if (isLetter(ch)) {    // 关键字或者标识符
                                state = 7;
                                begin = i;
                            } else if (isDigit(ch)) {   // 整数或者浮点数
                                begin = i;
                                state = 8;
                            } else if (String.valueOf(ch).equals(ConstVar.DQ)) {    // 出现双引号" ，则判断是不是字符串类型的数据
                                begin = i + 1;
                                state = 10;
                                node.add(new TreeNode("分隔符 ： " + ch));
                                tokens.add(new Token(lineNum, begin, "分隔符", ConstVar.DQ));
                                displayTokens.add(new Token(lineNum, begin, "分隔符", ConstVar.DQ));
                            } else if (String.valueOf(ch).equals(" ")) {                          // 空白符
                                state = 0;
                                displayTokens.add(new Token(lineNum, i + 1, "空白符", " "));
                            } else if (String.valueOf(ch).equals("\n")) {                         // 换行符
                                state = 0;
                                displayTokens.add(new Token(lineNum, i + 1, "换行符", "\n"));
                            } else if (String.valueOf(ch).equals("\r")) {                        // 回车符
                                state = 0;
                                displayTokens.add(new Token(lineNum, i + 1, "回车符", "\r"));
                            } else if (String.valueOf(ch).equals("\t")) {                       // 制表符
                                state = 0;
                                displayTokens.add(new Token(lineNum, i + 1, "制表符", "\t"));
                            }
                            break;
                        case 1:            // 加号
                            node.add(new TreeNode("运算符 ： " + ConstVar.PLUS));
                            tokens.add(new Token(lineNum, i, "运算符", ConstVar.PLUS));
                            displayTokens.add(new Token(lineNum, i, "运算符", ConstVar.PLUS));
                            i--;
                            state = 0;
                            break;
                        case 2:            // 减号
                            String temp = tokens.get(tokens.size() - 1).getKind();
                            String c = tokens.get(tokens.size() - 1).getContent();
                            if (temp.equals("整数") || temp.equals("标识符") || temp.equals("实数") || c.equals(")") || c.equals("]")) {
                                node.add(new TreeNode("运算符 ： " + ConstVar.MINUS));
                                tokens.add(new Token(lineNum, i, "运算符", ConstVar.MINUS));
                                displayTokens.add(new Token(lineNum, i, "运算符", ConstVar.MINUS));
                                i--;
                                state = 0;
                            } else if (String.valueOf(ch).equals("\n")) {
                                displayTokens.add(new Token(lineNum, i - 1, "错误", ConstVar.MINUS));
                            } else {
                                begin = i - 1;
                                state = 8;
                            }
                            break;
                        case 3:
                            if (ch == '/') {
                                errorNum++;
                                errorInfo += "  ERROR:第 " + lineNum + " 行,第 " + i
                                        + " 列：" + "运算符\"" + ConstVar.TIMES + "\"使用错误  \n";
                                node.add(new TreeNode(ConstVar.ERROR + "运算符\"" + ConstVar.TIMES + "\"使用错误"));
                                displayTokens.add(new Token(lineNum, i, "错误", cmmText.substring(i - 1, i + 1)));
                            } else {
                                node.add(new TreeNode("运算符 ： " + ConstVar.TIMES));
                                tokens.add(new Token(lineNum, i, "运算符", ConstVar.TIMES));
                                displayTokens.add(new Token(lineNum, i, "运算符", ConstVar.TIMES));
                                i--;
                            }
                            state = 0;
                            break;
                        case 4:
                            if (ch == '/') {
                                node.add(new TreeNode("单行注释 //"));
                                displayTokens.add(new Token(lineNum, i, "单行注释符号", "//"));
                                begin = i + 1;
                                displayTokens.add(new Token(lineNum, i, "注释", cmmText.substring(begin, length - 1)));
                                i = length - 2;
                                state = 0;
                            } else if (ch == '*') {
                                node.add(new TreeNode("多行注释 /*"));
                                displayTokens.add(new Token(lineNum, i, "多行注释开始符号", "/*"));
                                begin = i + 1;
                                isNotation = true;
                                if (offset < sourceText.length()) {
                                    if (!sourceText.substring(offset).contains("*/")) {
                                        errorNum++;
                                        errorInfo += " ERROR:第 " + lineNum + " 行,第 " + i
                                                + " 列：" + "注释*/使用错误 : " + "没有闭合";
                                        node.add(new TreeNode(ConstVar.ERROR + "注释\"" + ConstVar.TIMES + "\"使用错误"));
                                        displayTokens.add(new Token(lineNum, i, "错误", cmmText.substring(i - 1, i + 1)));
                                    }
                                } else {
                                    errorNum++;
                                    errorInfo += " ERROR:第 " + lineNum + " 行,第 " + i
                                            + " 列：" + "注释*/使用错误 : " + "没有闭合";
                                    node.add(new TreeNode(ConstVar.ERROR + "注释\"" + ConstVar.TIMES + "\"使用错误"));
                                    displayTokens.add(new Token(lineNum, i, "错误", cmmText.substring(i - 1, i + 1)));
                                }

                            } else {
                                node.add(new TreeNode("运算符 ： " + ConstVar.DIVIDE));
                                tokens.add(new Token(lineNum, i, "运算符", ConstVar.DIVIDE));
                                displayTokens.add(new Token(lineNum, i, "运算符", ConstVar.DIVIDE));
                                i--;
                                state = 0;
                            }
                            break;
                        case 5:                //==
                            if (ch == '=') {
                                node.add(new TreeNode("运算符 ： " + ConstVar.EQUAL));
                                tokens.add(new Token(lineNum, i, "运算符", ConstVar.EQUAL));
                                displayTokens.add(new Token(lineNum, i, "运算符", ConstVar.EQUAL));
                                state = 0;
                            } else {          // =
                                state = 0;
                                node.add(new TreeNode("运算符 ： " + ConstVar.ASSIGN));
                                tokens.add(new Token(lineNum, i, "运算符", ConstVar.ASSIGN));
                                displayTokens.add(new Token(lineNum, i, "运算符", ConstVar.ASSIGN));
                                i--;
                            }
                            break;
                        case 6:
                            if (ch == '>') {         // <>
                                node.add(new TreeNode("运算符 ： " + ConstVar.NEQUAL));
                                tokens.add(new Token(lineNum, i, "运算符", ConstVar.NEQUAL));
                                displayTokens.add(new Token(lineNum, i, "运算符", ConstVar.NEQUAL));
                                state = 0;
                            } else {                // <
                                state = 0;
                                node.add(new TreeNode("运算符 ： " + ConstVar.LT));
                                tokens.add(new Token(lineNum, i, "运算符", ConstVar.LT));
                                displayTokens.add(new Token(lineNum, i, "运算符", ConstVar.LT));
                                i--;
                            }
                            break;
                        case 7:
                            if (isLetter(ch) || isDigit(ch)) {
                                state = 7;
                            } else {
                                //标识符id从start到end
                                end = i;
                                String id = cmmText.substring(begin, end);
                                if (isKey(id)) {
                                    node.add(new TreeNode("关键字 ： " + id));
                                    tokens.add(new Token(lineNum, begin + 1, "关键字", id));
                                    displayTokens.add(new Token(lineNum, begin + 1, "关键字", id));
                                } else if (matchID(id)) {
                                    node.add(new TreeNode("标识符 ： " + id));
                                    tokens.add(new Token(lineNum, begin + 1, "标识符", id));
                                    displayTokens.add(new Token(lineNum, begin + 1, "标识符", id));
                                } else {
                                    errorNum++;
                                    errorInfo += "  ERROR:第 " + lineNum + " 行,第 " + (begin + 1) + " 列：" + id + "是非法标识符\n";
                                    node.add(new TreeNode(ConstVar.ERROR + id + "是非法标识符"));
                                    displayTokens.add(new Token(lineNum, begin + 1, "错误", id));
                                }
                                i--;
                                state = 0;
                            }
                            break;
                        case 8:
                            if (isDigit(ch) || String.valueOf(ch).equals(".")) {
                                state = 8;
                            } else {
                                if (isLetter(ch)) {
                                    errorNum++;
                                    errorInfo += "  ERROR:第 " + lineNum + " 行,第 " + i + " 列：" + "数字格式错误或者标志符错误\n";
                                    node.add(new TreeNode(ConstVar.ERROR + "数字格式错误或者标志符错误"));
                                    displayTokens.add(new Token(lineNum, i, "错误",
                                            cmmText.substring(begin, find(begin, cmmText) + 1)));
                                    i = find(begin, cmmText);
                                } else {
                                    end = i;
                                    String id = cmmText.substring(begin, end);
                                    if (!id.contains(".")) {
                                        if (matchInteger(id)) {
                                            node.add(new TreeNode("整数    ： " + id));
                                            tokens.add(new Token(lineNum, begin + 1, "整数", id));
                                            displayTokens.add(new Token(lineNum, begin + 1, "整数", id));
                                        } else {
                                            errorNum++;
                                            errorInfo += "  ERROR:第 " + lineNum + " 行,第 " + (begin + 1) + " 列："
                                                    + id + "是非法整数\n";
                                            node.add(new TreeNode(ConstVar.ERROR + id + "是非法整数"));
                                            displayTokens.add(new Token(lineNum, begin + 1, "错误", id));
                                        }
                                    } else {
                                        if (matchReal(id)) {
                                            node.add(new TreeNode("实数    ： " + id));
                                            tokens.add(new Token(lineNum, begin + 1, "实数", id));
                                            displayTokens.add(new Token(lineNum, begin + 1, "实数", id));
                                        } else {
                                            errorNum++;
                                            errorInfo += "  ERROR:第 " + lineNum + " 行,第 " + (begin + 1) + " 列："
                                                    + id + "是非法实数\n";
                                            node.add(new TreeNode(ConstVar.ERROR + id + "是非法实数"));
                                            displayTokens.add(new Token(lineNum, begin + 1, "错误", id));
                                        }
                                    }
                                    i = find(i, cmmText);
                                }
                                state = 0;
                            }
                            break;
                        case 9:
                            node.add(new TreeNode("运算符 ： " + ConstVar.GT));
                            tokens.add(new Token(lineNum, i, "运算符", ConstVar.GT));
                            displayTokens.add(new Token(lineNum, i, "运算符", ConstVar.GT));
                            i--;
                            state = 0;
                            break;
                        case 10:
                            if (ch == '"') {
                                end = i;
                                String string = cmmText.substring(begin, end);
                                node.add(new TreeNode("字符串 ： " + string));
                                tokens.add(new Token(lineNum, begin + 1, "字符串", string));
                                displayTokens.add(new Token(lineNum, begin + 1, "字符串", string));
                                node.add(new TreeNode("分隔符 ： " + ConstVar.DQ));
                                tokens.add(new Token(lineNum, end + 1, "分隔符", ConstVar.DQ));
                                displayTokens.add(new Token(lineNum, end + 1, "分隔符", ConstVar.DQ));
                                state = 0;
                            } else if (i == length - 1) {
                                String string = cmmText.substring(begin);
                                errorNum++;
                                errorInfo += "  ERROR:第 " + lineNum + " 行,第 "
                                        + (begin + 1) + " 列：" + "字符串 " + string + " 缺少引号  \n";
                                node.add(new TreeNode(ConstVar.ERROR + "字符串 " + string + " 缺少引号  \n"));
                                displayTokens.add(new Token(lineNum, i + 1, "错误", string));
                            }
                    }
                } else {//特殊符号对应的ASCII码
                    if (ch > 19967 && ch < 40870 || ch == '\\' || ch == '~'
                            || ch == '`' || ch == '|' || ch == '、' || ch == '^'
                            || ch == '?' || ch == '&' || ch == '^' || ch == '%'
                            || ch == '$' || ch == '@' || ch == '!' || ch == '#'
                            || ch == '；' || ch == '【' || ch == '】' || ch == '，'
                            || ch == '。' || ch == '“' || ch == '”' || ch == '‘'
                            || ch == '’' || ch == '？' || ch == '（' || ch == '）'
                            || ch == '《' || ch == '》' || ch == '·') {
                        errorNum++;
                        errorInfo += "  ERROR:第 " + lineNum + " 行,第 "
                                + (i + 1) + " 列：" + "\"" + ch + "\"是不可识别符号  \n";
                        node.add(new TreeNode(ConstVar.ERROR + "\"" + ch + "\"是不可识别符号"));
                        if (state == 0)
                            displayTokens.add(new Token(lineNum, i + 1, "错误", String.valueOf(ch)));
                    }
                }
            } else {
                if (ch == '*') {
                    state = 3;
                } else if (ch == '/' && state == 3) {
                    node.add(new TreeNode("多行注释 */"));
                    displayTokens.add(new Token(lineNum, begin + 1, "注释", cmmText.substring(begin, i - 1)));
                    displayTokens.add(new Token(lineNum, i, "多行注释结束符号", "*/"));
                    state = 0;
                    isNotation = false;
                } else if (i == length - 2) {
                    displayTokens.add(new Token(lineNum, begin + 1, "注释", cmmText.substring(begin, length - 1)));
                    displayTokens.add(new Token(lineNum, length - 1, "换行符", "\n"));
                    state = 0;
                } else {
                    state = 0;
                }
            }
        }
        return node;
    }

    /**
     * 功能介绍：分析CMM程序，并返回词法分析结果的根结点
     * @param cmmText CMM程序文本
     * @return 分析生成的TreeNode
     */
    public TreeNode execute(String cmmText) {
        setErrorInfo("");
        setErrorNum(0);
        setTokens(new ArrayList<>());
        setDisplayTokens(new ArrayList<>());
        setNotation(false);

        StringReader stringReader = new StringReader(cmmText);
        //每一行的源程序和当前行号
        String eachLine = "";
        int lineNum = 1;
        TreeNode root = new TreeNode("PROGRAM");
        // 读取CMM文件文本
        BufferedReader reader = new BufferedReader(stringReader);
        while (eachLine != null) {
            try {
                eachLine = reader.readLine();
                if (eachLine != null) {
                    if (isNotation() && !eachLine.contains("*/")) {
                        eachLine += "\n";
                        TreeNode temp = new TreeNode(eachLine);
                        temp.add(new TreeNode("多行注释"));
                        displayTokens.add(new Token(lineNum, 1, "注释", eachLine.substring(0, eachLine.length() - 1)));
                        displayTokens.add(new Token(lineNum, eachLine.length() - 1, "换行符", "\n"));
                        root.add(temp);
                        lineNum++;
                        continue;
                    } else {
                        //如果当前行不是注释，则对它进行词法分析
                        root.add((executeLine(eachLine, lineNum)));
                    }
                }
                lineNum++;
            } catch (IOException e) {
                System.err.println("读取文本时出错了！");
            }
        }
        //返回树形结点的root结点
        return root;
    }

    public boolean isNotation() {
        return isNotation;
    }

    public void setNotation(boolean isNotation) {
        this.isNotation = isNotation;
    }

    public int getErrorNum() {
        return errorNum;
    }

    public void setErrorNum(int errorNum) {
        this.errorNum = errorNum;
    }

    public String getErrorInfo() {
        return errorInfo;
    }

    public void setErrorInfo(String errorInfo) {
        this.errorInfo = errorInfo;
    }

    public ArrayList<Token> getTokens() {
        return tokens;
    }

    public void setTokens(ArrayList<Token> tokens) {
        this.tokens = tokens;
    }

    public ArrayList<Token> getDisplayTokens() {
        return displayTokens;
    }

    public void setDisplayTokens(ArrayList<Token> displayTokens) {
        this.displayTokens = displayTokens;
    }

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }
}