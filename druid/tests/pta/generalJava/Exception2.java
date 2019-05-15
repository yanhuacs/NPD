package pta.generalJava;

import static pta.utils.Dummy.notAlias;

import pta.utils.objects.A;

/*
 * @testcase Exception2
 * @description No alias in exception (exception never triggered)
 */
public class Exception2 {

  public static void main(String[] args) {

    A a = new A();
    //Benchmark.alloc(1);
    A b = new A();

    try {
      Integer.parseInt("abc");
      a = b;

    } catch (RuntimeException e) {
      notAlias(a,b); //No try/catch support
      /*Benchmark.test("b",
          "{allocId:1, mayAlias:[b], notMayAlias:[a], mustAlias:[b], notMustAlias:[a]}");*/
    }

  }
}
