package testcase;

import java.util.Random;

class AAA {
    String f = null;
}
class B {
    String g = "abc";
}

public class SSA2 {
    void test1(AAA a, B b) {
    }
    void test2(AAA a, B b) {
        a.f = "xyz";
        b.g = null;
    }


    public static void main(String argv[]) {
        AAA a = new AAA();
        B b = new B();
        SSA2 o = new SSA2();
        if (new Random().nextBoolean()) {
            o.test1(a, b);
            a.f.length();
            b.g.length();
        }
        //System.out.println("a.f's length = " + a.f.length());
        //System.out.println("b.g's length = " + b.g.length());
        o.test2(a, b);
        //System.out.println("a.f's length = " + a.f.length());
        //System.out.println("b.g's length = " + b.g.length());
        a.f.length();
        b.g.length();
    }
}
