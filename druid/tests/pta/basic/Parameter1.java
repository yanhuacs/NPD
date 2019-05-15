package pta.basic;

import static pta.utils.Dummy.mayAlias;

import pta.utils.objects.A;
/*
 * @testcase ParameterAlias1
 * @description Aliasing through static method parameter
 */
public class Parameter1 {

  public static void test(A x) {
    A b = x;
    mayAlias(b,x);
    /*Benchmark.test("b",
        "{allocId:1, mayAlias:[b,x], notMayAlias:[], mustAlias:[b,x], notMustAlias:[]}");
  	*/
  }

  public static void main(String[] args) {
    A a = new A();
    test(a);
  }
}
