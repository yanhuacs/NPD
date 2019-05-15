package pta.collections;

import java.util.ArrayList;

import pta.utils.objects.A;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

/*
 * @testcase List1
 * @description ArrayList
 */
public class List1 {

  public static void main(String[] args) {

    ArrayList<A> list = new ArrayList<A>();
    A a = new A();
    //Benchmark.alloc(1);
    A b = new A();
    list.add(a);
    list.add(b);
    A c = list.get(1);
    
    mayAlias(c,b);
    notAlias(a,list);
    /*
    Benchmark
        .test("b",
            "{allocId:1, mayAlias:[c,b], notMayAlias:[a,list], mustAlias:[c,b], notMustAlias:[a,list]}");*/
  }
}
