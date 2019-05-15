package pta.basic;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

import pta.utils.objects.A;
import pta.utils.objects.B;

/*
 * @testcase FieldSensitivity1
 * 
 * @version 1.0
 * 
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 * 
 * @description Field Sensitivity with static method
 */
public class FieldSensitivity1 {

  private static void assign(A x, A y) {
    y.f = x.f;
  }

  public static void main(String[] args) {

    //Benchmark.alloc(1);
    B b = new B();
    A a = new A(b);
    A c = new A();
    assign(a, c);
    B d = c.f;
    mayAlias(d,b);
    notAlias(a,c);
    /*Benchmark.test("d",
        "{allocId:1, mayAlias:[d,b], notMayAlias:[a,c], mustAlias:[d,b], notMustAlias:[a,c]}");
*/
  }

}
