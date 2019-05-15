package pta.generalJava;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

import pta.utils.objects.A;
/*
 * @testcase OuterClass1
 * @description Alias from method in inner class
 */
public class OuterClass1 {

  public OuterClass1() {}

  public class InnerClass {
    private A a;

    public InnerClass(A a) {
      this.a = a;
    }

    public void alias(A x) {
      this.a = x;
    }
  }

  private void test() {
    //Benchmark.alloc(1);
    A a = new A();
    A b = new A();

    InnerClass i = new InnerClass(a);
    i.alias(b);
    A h = i.a;
    mayAlias(b,h);
    notAlias(i,a);
    /*Benchmark.test("h",
        "{allocId:1, mayAlias:[b,h], notMayAlias:[i,a], mustAlias:[b,a], notMustAlias:[i]}");
    */
  }

  private static void main(String[] args) {
    OuterClass1 oc1 = new OuterClass1();
    oc1.test();
  }

}
