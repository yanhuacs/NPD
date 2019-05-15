package pta.basic;

import static pta.utils.Dummy.notAlias;

import pta.utils.objects.A;
import pta.utils.objects.B;
/*
 * @testcase ReturnValue3
 * @description Alias to a return value from a static method
 */
public class ReturnValue3 {

  public static A id(A x) {
    A y = new A();
    //Benchmark.alloc(1);
    y.f = new B();
    return y;
  }

  public static void main(String[] args) {
    A a = new A();
    A b = id(a);
    B x = b.f;
    B y = a.f;
    notAlias(a,b);
    notAlias(a,y);
    notAlias(b,y);
  }
}
