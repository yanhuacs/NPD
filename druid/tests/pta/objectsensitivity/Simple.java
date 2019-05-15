/**
 * With a context-insensitive heap abstraction, the fields o1 and o2
 * will point to the same heap object.
 */
package pta.objectsensitivity;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

import pta.utils.objects.B;

public class Simple
{
  static B o;
  B allocate()
  {
    return new B();
  }

  public static void main(String[] args)
  {
    Simple simple = new Simple();
    o = simple.allocate();

    B o1 = simple.allocate();
    mayAlias(o,o1);

  }

}
