package pta.basic;

import static pta.utils.Dummy.mayAlias;

import pta.utils.objects.A;
  
public class ReturnValue1 {



  public static A id(A x) {
    return x;
  }

  public static void main(String[] args) {

    A a = new A();
    A b = id(a);
    mayAlias(a,b);
  }
}
