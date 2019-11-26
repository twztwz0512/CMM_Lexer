package structure;

import java.util.ArrayList;

public class SymbolTable {

    private ArrayList<SymbolTableElement> symbolTable = new ArrayList<>();

    /**
     * 功能介绍：根据索引查找SymbolTableElement对象
     *
     * @param index 提供的索引
     * @return 返回SymbolTableElement对象
     */
    public SymbolTableElement get(int index) {
        return symbolTable.get(index);
    }

    /**
     * 功能介绍：根据SymbolTableElement对象的名字对所有作用域查找
     *
     * @param name  SymbolTableElement名字
     * @param level SymbolTableElement作用域
     * @return 如果存在, 则返回SymbolTableElement对象;否则返回null
     */
    public SymbolTableElement getAllLevel(String name, int level) {
        while (level > -1) {
            for (SymbolTableElement element : symbolTable) {
                if (element.getName().equals(name) && element.getLevel() == level) {
                    return element;
                }
            }
            level--;
        }
        return null;
    }

    /**
     * 功能介绍：根据SymbolTableElement对象的名字对当前作用域查找
     *
     * @param name  SymbolTableElement名字
     * @param level SymbolTableElement作用域
     * @return 如果存在, 则返回SymbolTableElement对象;否则返回null
     */
    public SymbolTableElement getCurrentLevel(String name, int level) {
        for (SymbolTableElement element : symbolTable) {
            if (element.getName().equals(name) && element.getLevel() == level) {
                return element;
            }
        }
        return null;
    }

    /**
     * 功能介绍：向symbolTable中添加SymbolTableElement对象,放在末尾
     *
     * @param element 要添加的元素
     */
    public void add(SymbolTableElement element) {
        symbolTable.add(element);
    }

    /**
     * 功能介绍：在symbolTable中指定的索引处添加SymbolTableElement对象
     *
     * @param index   制定的索引
     * @param element 要添加的元素
     */
    public void add(int index, SymbolTableElement element) {
        symbolTable.add(index, element);
    }

    /**
     * 功能介绍：从symbolTable中移除指定索引处的元素
     *
     * @param index 指定的索引
     */
    public void remove(int index) {
        symbolTable.remove(index);
    }

    /**
     * 功能介绍：从symbolTable中移除指定名字和作用域的元素
     *
     * @param name  指定的名字
     * @param level 指定的作用域
     */
    public void remove(String name, int level) {
        for (int i = 0; i < size(); i++) {
            if (get(i).getName().equals(name) && get(i).getLevel() == level) {
                remove(i);
                return;
            }
        }
    }

    /**
     * 功能介绍：清空symbolTable中的元素,将其大小设为0
     */
    public void removeAll() {
        symbolTable.clear();
    }

    /**
     * 功能介绍：当level减小时更新符号表,去除无用的元素
     */
    public void update(int level) {
        for (int i = 0; i < size(); i++) {
            if (get(i).getLevel() > level) {
                remove(i);
            }
        }
    }

    /**
     * 功能介绍：判断是否包含指定的元素
     *
     * @param element 指定的SymbolTableElement元素
     * @return 如果包含则返回true, 否则返回false
     */
    public boolean contains(SymbolTableElement element) {
        return symbolTable.contains(element);
    }

    /**
     * 功能介绍：判断是否为空
     *
     * @return 如果为空则返回true, 否则返回false
     */
    public boolean isEmpty() {
        return symbolTable.isEmpty();
    }

    /**
     * 功能介绍：计算元素个数
     *
     * @return 返回对象中元素的个数
     */
    public int size() {
        return symbolTable.size();
    }

    public ArrayList<SymbolTableElement> getSymbolTable() {
        return symbolTable;
    }

    public void setSymbolTable(ArrayList<SymbolTableElement> symbolTable) {
        this.symbolTable = symbolTable;
    }
}

