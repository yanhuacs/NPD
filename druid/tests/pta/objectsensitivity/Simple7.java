/**
 * With a context-insensitive heap abstraction, the fields o1 and o2
 * will point to the same heap object.
 */
package pta.objectsensitivity;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

import pta.utils.objects.B;

public class Simple7
{
  static B o;

  Simple7(B b) {
    o = b;
  }

  B call()
  {
    return allocate();
  }

  B allocate()
  {
    return new B();
  }

  B call_return(B b) {
    return allocate_return(b);
  }

  B allocate_return(B b) 
  {
    return b;
  }

  void change_o(B b) {
    o = b;
  }

  public static void main(String[] args)
  {
    B obj1 = new B();
    Simple7 simple1 = new Simple7(obj1);
    Simple7 simple2 = new Simple7(obj1);
    simple1.o = simple1.call();
    simple2.o = simple2.call();
    B o1 = simple1.o;
    B o2 = simple2.o;
    simple1.change_o(new B());
    B o1a = simple1.allocate();
    B o2a = simple2.allocate();

    mayAlias(o1,o2); //Fail if callsitesensitive
    mayAlias(o1a,o1);
    mayAlias(o2a,o2);
    notAlias(o2a,o1a);


  }

}
