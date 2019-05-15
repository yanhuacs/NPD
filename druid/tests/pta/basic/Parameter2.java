package pta.basic;

import static pta.utils.Dummy.mayAlias;

import pta.utils.objects.A;
/*
 * @testcase ParameterAlias2
 * @description Aliasing through non static method parameter
 */
public class Parameter2 {

  public Parameter2() {}

  public void test(A x) {
    A b = x;
    mayAlias(b,x);
  }

  public static void main(String[] args) {
    A a = new A();
    Parameter2 p2 = new Parameter2();
    p2.test(a);
  }
}
