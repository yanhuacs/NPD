package pta.generalJava;
import static pta.utils.Dummy.mayAlias;

public class Super {
  private static Object field = new Object();

  public static void main(String[] ps) {
    SuperBase b = new SuperExtension();
    Object newfield = new Object();
    newfield = b.foo(newfield,field);
    mayAlias(field, newfield);
  }

}

class SuperBase {
  public Object foo(Object f, Object b) {
    f = b;
    return f;
  }
}

class SuperExtension extends SuperBase {
  public Object foo(Object f, Object b) {
    return super.foo(f,b);
  }
}
