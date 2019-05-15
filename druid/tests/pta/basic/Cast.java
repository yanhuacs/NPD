package pta.basic;
import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

public class Cast
{
  public static Object o1;
  public static Object o2;
  public static Object o3;
  public static Object o4;

  public static void main(String[] ps)
  {
    o1 = new String();
    o2 = (String) o1;
    castfoo(new Object());
    castfoo(new String());

    castfail(new Integer(5));
    castsucceed(new String());
  }

  public static void castfoo(Object o)
  {
    o4 = o;

    if(o instanceof String)
    {
      o3 = (String) o;
      mayAlias(o3,o);
    }
  }


  public static void castfail(Object o)
  {
    String s = (String) o;
    notAlias(s,o);
  }

  public static void castsucceed(Object o)
  {
    String s = (String) o;
    mayAlias(s,o);
  }
}
