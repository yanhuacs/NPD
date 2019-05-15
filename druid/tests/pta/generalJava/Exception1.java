package pta.generalJava;

import static pta.utils.Dummy.mayAlias;

import pta.utils.objects.A;

/*
 * @testcase Exception1
 */
public class Exception1 {

  public static void main(String[] args) {

    //Benchmark.alloc(1);
    A a = new A();
    A b = new A();

    try {
      b = a;
      throw new RuntimeException();

    } catch (RuntimeException e) {
	mayAlias(a,b);
      /*Benchmark.test("b",
          "{allocId:1, mayAlias:[a,b], notMayAlias:[], mustAlias:[a,b], notMustAlias:[]}");*/

    }

  }
}
