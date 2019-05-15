package pta.collections;

import java.util.LinkedList;

import pta.utils.objects.A;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

/*
 * @testcase List2
 * @description LinkedList
 */
public class List2 {

  public static void main(String[] args) {

    LinkedList<A> list = new LinkedList<A>();
    A a = new A();
    //Benchmark.alloc(1);
    A b = new A();
    list.add(a);
    list.add(b);
    A c = list.get(1);

    mayAlias(c,b);
    notAlias(a,list);
    /*Benchmark
        .test("b",
            "{allocId:1, mayAlias:[c,b], notMayAlias:[a,list], mustAlias:[c,b], notMustAlias:[a,list]}");*/
  }
}
