package pta.basic;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

import pta.utils.objects.A;
import pta.utils.objects.B;

/*
 * @testcase FieldSensitivity2
 * @description Field Sensitivity without static method
 */
public class FieldSensitivity2 {

  public FieldSensitivity2() {}

  private void assign(A x, A y) {
    y.f = x.f;
  }

  private void test() {
    //Benchmark.alloc(1);
    B b = new B();
    A a = new A(b);
    A c = new A();
    assign(a, c);
    B d = c.f;
    mayAlias(d,b);
    notAlias(a,c);
    /*Benchmark.test("d",
        "{allocId:1, mayAlias:[d,b], notMayAlias:[a,c], mustAlias:[d,b], notMustAlias:[a,c]}");*/
  }

  public static void main(String[] args) {

    FieldSensitivity2 fs2 = new FieldSensitivity2();
    fs2.test();
  }

}
