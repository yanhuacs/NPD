/**
 * With a context-insensitive heap abstraction, the fields o1 and o2
 * will point to the same heap object.
 */
package pta.objectsensitivity;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

import pta.utils.objects.B;

public class Simple4
{
  static B o;
  B call()
  {
    return allocate();
  }

  B allocate()
  {
    return new B();
  }

  public static void main(String[] args)
  {
    Simple4 simple1 = new Simple4();
    Simple4 simple2 = new Simple4();
    o = simple1.allocate();
    B o1 = simple2.call();
    B o2 = new B();
    notAlias(o,o1);
    notAlias(o,o2);
    notAlias(o1,o2);

  }

}
