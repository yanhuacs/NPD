package pta.special;

import static pta.utils.Dummy.mayAlias;

import pta.utils.objects.A;
import pta.utils.objects.B;

/*
 * @testcase StrongUpdate1
 * @description Indirect alias of a.f and b.f through alias of a and b
 */
public class StrongUpdate1 {

  public static void main(String[] args) {

    A a = new A();
    A b = a;
    //Benchmark.alloc(1);
    a.f = new B();
    B y = a.f;
    B x = b.f;
    mayAlias(x,y);
    mayAlias(a,b);
    /*Benchmark.test("x",
        "{allocId:1, mayAlias:[x,y], notMayAlias:[a,b], mustAlias:[x,y], notMustAlias:[a,b]}");*/
  }
}
