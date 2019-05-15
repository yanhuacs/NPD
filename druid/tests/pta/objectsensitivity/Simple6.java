/**
 * With a context-insensitive heap abstraction, the fields o1 and o2
 * will point to the same heap object.
 */
package pta.objectsensitivity;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

import pta.utils.objects.B;

public class Simple6
{
  static B o;

  Simple6(B b) {
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


  public static void main(String[] args)
  {
    B obj1 = new B();
    Simple6 simple1 = new Simple6(obj1);
    Simple6 simple2 = new Simple6(obj1);
    simple1.o = simple1.call();
    simple2.o = simple2.call();
    B o1 = simple1.o;
    B o2 = simple2.o;
    B o1a = simple1.allocate();
    B o2a = simple2.allocate();

    mayAlias(o1,o2); //Fail if CallSiteSensitive
    mayAlias(o1a,o1);
    mayAlias(o2a,o2);
    notAlias(o2a,o1a);


  }

}
