package pta.basic;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

import pta.utils.objects.A;
import pta.utils.objects.B;

/*
 * @testcase Method1
 * @description Alias in a static method
 */
public class Interprocedural1 {

  public static void alloc(A x, A y) {
    x.f = y.f;
  }

  public static void main(String[] args) {

    A a = new A();
    A b = new A();

    //Benchmark.alloc(1);
    b.f = new B();
    alloc(a, b);

    B x = a.f;
    B y = b.f;
    mayAlias(x,y);
    notAlias(a,b);
    /*Benchmark.test("x",
        "{allocId:1, mayAlias:[x,y], notMayAlias:[a,b], mustAlias:[x,y], notMustAlias:[a,b]}");*/
   }
}
