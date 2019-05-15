package pta.basic;
import static pta.utils.Dummy.mayAlias;
class Bar
{
  private static int x;

  static
  {
    x = 1;
  }
}

class Foo extends Bar
{
}

public class ClassInit2 extends Foo
{
  private static int x;

  public static void main(String[] ps)
  {
    mayAlias(x,1);
  }

  static
  {
    x = 1;
  }
}
