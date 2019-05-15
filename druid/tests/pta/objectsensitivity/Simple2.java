/**
 * With a context-insensitive heap abstraction, the fields o1 and o2
 * will point to the same heap object.
 */
package pta.objectsensitivity;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

import pta.utils.objects.B;

public class Simple2
{
  static B o;

  B allocate()
  {
    return new B();
  }
  public static void main(String[] args)
  {
    Simple2 simple1 = new Simple2();
    Simple2 simple2 = new Simple2();
    B o = simple1.allocate();
    B o1 = simple2.allocate();
    notAlias(o,o1);
  }
}
