package structure;

public class Token {
    private String kind;         //token类型
    private int line;            // token所在行
    private int culomn;          // token所在列
    private String content;      //token内容
    private String idKind;       // 标识符类型

    public Token(int l, int c, String k, String con) {
        this.line = l;
        this.culomn = c;
        this.kind = k;
        this.content = con;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getCulomn() {
        return culomn;
    }

    public void setCulomn(int culomn) {
        this.culomn = culomn;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getIdKind() {
        return idKind;
    }

    public void setIdKind(String idKind) {
        this.idKind = idKind;
    }

    @Override
    public String toString() {
        return "Token{" +
                "kind='" + kind + '\'' +
                ", line=" + line +
                ", column=" + culomn +
                ", content='" + content + '\'' +
                '}';
    }
}
