package pta.special;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

import pta.utils.objects.A;

/*
 * @testcase AccessPath1
 * @description Query for access paths
 */
public class AccessPath1 {

  public static void main(String[] args) {

    //Benchmark.alloc(1);
    A a = new A();
    A b = new A();

    a.f = b.f;
    mayAlias(a.f,b.f);
    notAlias(a,b);
    /*Benchmark
        .test("a.f",
            "{allocId:1, mayAlias:[a.f,b.f], notMayAlias:[a,b], mustAlias:[a.f,b.f], notMustAlias:[a,b]}");*/
  }
}
