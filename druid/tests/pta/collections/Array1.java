package pta.collections;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

import pta.utils.objects.A;
/*
 * @testcase Array1
 * @description Array alias
 */
public class Array1 {

  public static void main(String[] args) {

    A[] array = new A[] {};
    A a = new A();
    //Benchmark.alloc(1);
    A b = new A();
    array[0] = a;
    array[1] = b;
    A c = array[1];
    
    mayAlias(c,b);
    notAlias(a,array);

    /*Benchmark
        .test("c",
            "{allocId:1, mayAlias:[c,b], notMayAlias:[a,array], mustAlias:[c,b], notMustAlias:[a,array]}");*/
  }
}
