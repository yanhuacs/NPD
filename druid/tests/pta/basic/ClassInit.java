package pta.basic;
import static pta.utils.Dummy.mayAlias;
public class ClassInit
{
  public static Object field = new Object();

  public static void main(String[] ps)
  {
    mayAlias(field,field);
  }
}
