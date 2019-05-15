package pta.generalJava;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

import pta.utils.objects.A;
import pta.utils.objects.P;
/*
 * @testcase SuperClass1
 * @description Alias from method in super class
 */
public class SuperClasses1 {

  private static void main(String[] args) {
    //Benchmark.alloc(1);
    A a = new A();
    A b = new A();

    P p = new P(a);
    p.alias(b);
    A h = p.getA();
    mayAlias(h,b);
    notAlias(a,p);
    /*Benchmark.test("h",
        "{allocId:1, mayAlias:[h,b], notMayAlias:[a,p], mustAlias:[b,a], notMustAlias:[p]}");
    Benchmark.use(h);*/
  }

}
