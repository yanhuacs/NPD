package pta.basic;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

import pta.utils.objects.A;
import pta.utils.objects.B;
 
public class Interprocedural2 {

  public Interprocedural2() {}

  public void alloc(A x, A y) {
    x.f = y.f;
  }

  public static void main(String[] args) {

    A a = new A();
    A b = new A();
    b.f = new B();
    Interprocedural2 m2 = new Interprocedural2();
    m2.alloc(a, b);

    Object x = a.f;
    Object y = b.f;
    mayAlias(x,y);
    notAlias(a,b);
    notAlias(a,m2);
    notAlias(b,m2);
    
    /*
    Benchmark
        .test("x",
            "{allocId:1, mayAlias:[x,y], notMayAlias:[a,b,m2], mustAlias:[x,y], notMustAlias:[a,b,m2]}");
  	*/
  }
}
