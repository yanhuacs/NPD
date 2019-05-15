package pta.special;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

import pta.utils.objects.A;
import pta.utils.objects.B;

/*
 * @testcase ObjAlias2
 * @description Object sensitive alias from parameter object
 */
public class ObjAlias2 {

  public static void main(String[] args) {

    B b1 = new B();
    //Benchmark.alloc(1);
    B b2 = new B();

    A a = new A();

    B b3 = a.getF();

    B b4 = a.id(b1);
    B b5 = a.id(b2);

    mayAlias(b5,b2);
    mayAlias(b1,b4);    

    B b6 = a.setF(b1);
    B b7 = a.id(b1);

    mayAlias(b1,b6);
    mayAlias(b1,b7);
    //notAlias(b3,b6); CallSite
    /*Benchmark
        .test("b4",
            "{allocId:1, mayAlias:[b4,b2], notMayAlias:[a,b1,b3], mustAlias:[b4,b2], notMustAlias:[a,b1,b3]}");*/
  }
}
