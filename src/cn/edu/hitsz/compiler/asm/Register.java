package cn.edu.hitsz.compiler.asm;

/**
 * @author Black
 */
public class Register {
    private int index;
    private String name;

    public Register(int index) {
        this.index = index;
        this.name = "t" + index;
    }

    public int getIndex() {

        return this.index;
    }

    public String getName() {

        return this.name;
    }
}
