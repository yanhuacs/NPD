package testcase;
import java.util.Random;

public class SSA {
    String s;
    String test1() {
        s = null;
        s = "abc";
        return s;
    }
    String test2() {
        s = "abc";
        s = null;
        return s;
    }
    String test3() {
        s = "abc";
        if (s.length() == 0)
            s = null;
        return s;
    }

    public static void main(String argv[]) {
        SSA o = new SSA();
        String s = o.test1();
        System.out.println(s.length());
        if (new Random().nextBoolean()) {
           s = o.test2();
           System.out.println(s.length());
        }
        s = o.test3();
        System.out.println(s.length());

    }
}
