package testcase;

public class ClinitTest {
    public static void main(String[] args) {
        if (StaticInst.getInst() != null) {
            int len = StaticInst.getInst().getStr().length();
        }
    }
}

class StaticInst {
    private static StaticInst inst;
    private String str;
    public StaticInst() {
        str = new String("");
    }
    public String getStr() {
        return str;
    }

    public static StaticInst getInst() {
        if (inst == null) {
            inst = new StaticInst();
        }
        return inst;
    }
}
