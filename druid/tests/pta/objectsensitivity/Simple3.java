
package pta.objectsensitivity;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

import pta.utils.objects.B;

public class Simple3
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
    Simple3 simple1 = new Simple3();
    Simple3 simple2 = new Simple3();
    B o = simple1.call();
    B o1 = simple2.call();
    notAlias(o,o1);
  }
}
