package pta.basic;
import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;
public class ClassInit3
{
  public static void main(String[] ps)
  {
    // triggers clinit
    new TestNewInstance();

    // triggers clinit
    TestStaticMethod.bar();

    // triggers clinit
    Object x = new Object();
    TestStoreStaticField.x = x;
    TestStoreStaticField.test(x);

    // triggers clinit
    TestStorePrimStaticField.x = 5;
    TestStorePrimStaticField.test();

    // triggers clinit
    Object x1 = null;
    TestStoreNullStaticField.x = x1;
    TestStoreNullStaticField.test(x1);

    // triggers clinit
    Object o = TestLoadStaticField.x;
    TestLoadStaticField.test(o);

    // triggers clinit
    int y = TestLoadPrimStaticField.x;
    TestLoadPrimStaticField.test(y);

    // does not trigger clinit
    TestNewArray[] xs = new TestNewArray[5];

    Class foo = TestClassLiteral.class;
  }
}

class TestNewInstance
{
  static {}
}

class TestStaticMethod
{
  static {}
  public static void bar() {Object x = new Object(); mayAlias(x,x);}
}

class TestStoreStaticField
{
  static {}
  static Object x;
  static void test(Object o) {
  mayAlias(x,o);
  }
}

class TestStoreNullStaticField
{
  static {}
  static Object x;
  static void test(Object o) {
  notAlias(x,o);
  }    

}

class TestStorePrimStaticField
{
  static {}
  static int x;
  static void test() {
  mayAlias(x,5);
  }
}

class TestLoadStaticField
{
  static {}
  static Object x = new Object();
  static void test(Object o) {
  mayAlias(x,o);
  }  


}

class TestLoadPrimStaticField
{
  static {}
  static int x = 3;
  static void test(int y) {
  mayAlias(x,y);
  }


}

class TestNewArray
{
  static {}
}

class TestClassLiteral
{
  static {}
}
