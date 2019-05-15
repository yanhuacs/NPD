package pta.special;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

import pta.utils.objects.A;
import pta.utils.objects.B;

/*
 * @testcase ObjAlias1
 * @description Object sensitive alias from caller object
 */
public class ObjAlias1 {

  public static void main(String[] args) {

    B b1 = new B();
    //Benchmark.alloc(1);
    B b2 = new B();

    A a1 = new A(b1);
    A a2 = new A(b2);

    B b3 = a1.getF();
    B b4 = a2.getF();
    mayAlias(b4,b2);
    notAlias(a1,a2);
    mayAlias(b1,b3);
    //notAlias(b1,b4); CallSite
    //notAlias(b2,b3);
  }
}
