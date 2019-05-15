package testcase;

public class InstanceOfTest {
    public static void main(String[] args) {
        TypeA a = new TypeA();
        InstanceOf ins = new InstanceOf();
        ins.testInstanceOf(a);
    }
}
class Base {
    int field;
    public void func() {
        field = -1;
    }
}
class TypeA extends Base {
    public void func() {
        field = 1;
    }
}

class TypeB extends Base {
    public void func() {
        field = 2;
    }
}

class InstanceOf {
    public void testInstanceOf(Base o) {
        if (!(o instanceof TypeB)) {
            o = null;
        }
        o.func();
    }
}
