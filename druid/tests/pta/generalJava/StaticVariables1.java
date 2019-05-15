package pta.generalJava;

import static pta.utils.Dummy.mayAlias;

import pta.utils.objects.A;

/*
 * @testcase StaticVariables1
 * @description Alias to a static variable, allocation site at the static variable site
 */
public class StaticVariables1 {

  private static A a;

  public static void main(String[] args) {
    //Benchmark.alloc(1);
    a = new A();
    A b = a;
    A c = a;
    mayAlias(b,c);
    /*Benchmark.test("b",
        "{allocId:1, mayAlias:[b,c], notMayAlias:[], mustAlias:[b,c], notMustAlias:[]}");*/
  }
}
