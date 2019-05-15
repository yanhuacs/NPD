package pta.collections;

import java.util.HashSet;

import pta.utils.objects.A;

import static pta.utils.Dummy.notAlias;
/*
 * @testcase Set1
 * @description HashSet
 */
public class Set1 {

  public static void main(String[] args) {

    HashSet<A> set = new HashSet<A>();
    A a = new A();
    A c = null;
    //Benchmark.alloc(1);
    A b = new A();
    set.add(a);
    set.add(b);
    for (A i : set) {
      c = i;
      break;
    }
    a = null;

    notAlias(a,b);
    notAlias(b,set);
    notAlias(a,set);
    /*Benchmark.test("c",
        "{allocId:1, mayAlias:[c], notMayAlias:[a,b,set], mustAlias:[c], notMustAlias:[a,b,set]}");
    Benchmark.use(c);*/
  }
}
