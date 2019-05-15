package pta.generalJava;

import static pta.utils.Dummy.notAlias;

import pta.utils.objects.A;
import pta.utils.objects.B;

/*
 * @testcase Null1
 * @description Direct alias to null
 */
public class Null1 {

  public static void main(String[] args) {

    // No allocation site
    A h = new A();
    B a = h.getH();
    B b = a;
    notAlias(b,a);
    /*Benchmark.test("b",
        "{NULLALLOC, mayAlias:[], notMayAlias:[b,a], mustAlias:[b,a], notMustAlias:[i]}");
    Benchmark.use(b);*/
  }
}
