 /*
 * With a context-insensitive heap abstraction, the fields o1 and o2
 * will point to the same heap object.
 */
package pta.special;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

import pta.utils.objects.B;

public class FlowSensitivity1
{
  static Object o;
  Object allocate()
  {
    o = new Object();
    return o;
  }

  public static void main(String[] args)
  {
    FlowSensitivity1 flow1 = new FlowSensitivity1();
    FlowSensitivity1 flow2 = new FlowSensitivity1();
    Object o1 = flow1.allocate();
    Object o2 = flow2.allocate();
    mayAlias(o1,o2);
  }

}
