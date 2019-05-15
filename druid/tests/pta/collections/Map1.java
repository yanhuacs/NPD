package pta.collections;

import java.util.HashMap;

import pta.utils.objects.A;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;
/*
 * @testcase Map1
 * @description HashMap
 */
public class Map1 {

  public static void main(String[] args) {

    HashMap<String, A> map = new HashMap<String, A>();
    A a = new A();
    //Benchmark.alloc(1);
    A b = new A();
    map.put("first", a);
    map.put("second", b);
    A c = map.get("second");
    mayAlias(c,b);
    notAlias(a,map);
    /*Benchmark.test("c",
        "{allocId:1, mayAlias:[c,b], notMayAlias:[a,map], mustAlias:[c,b], notMustAlias:[a,map]}");*/
  }
}
