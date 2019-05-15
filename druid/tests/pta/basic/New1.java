package pta.basic;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

public class New1
{
  public static String s1;
  public static String s2;
  public static String s3;

  public static int[] a1;
  public static String[] a2;

  public static Object o1;  
  public static Object o2;

  public static void main(String[] ps)
  {
    s1 = "Foo";
    s2 = "Foo";
    s3 = new String(s1);

    a1 = new int[5];
    a2 = new String[5];

    o1 = new Object();
    o2 = new Object();

    /**
     * Finalization
     */
    Object o1 = new TestFinalize1a();
    Object o2 = new TestFinalize2a();

    TestClone1a.test(o1);
    TestClone2a.test(o1);
    TestClone3a.test(o1);
  }
}

class TestFinalize1a
{
}

class TestFinalize2a
{
  protected void finalize()
  {
  }
}

class TestClone1a implements Cloneable
{
  static TestClone1a field;
  static Object object = new Object();

  static void test(Object obj)
  {
    try
    {
      object = obj;
      TestClone1a o = new TestClone1a();
      field = (TestClone1a)((TestClone1a)object).clone();
      //mayAlias(field,object); try/catch not supported
    }
    catch(CloneNotSupportedException exc)
    {
    }
  }
}

class TestClone2a implements Cloneable
{
  static TestClone2a field;
  static Object object = new Object();
  static void test(Object obj)
  {
    object = obj;
    TestClone2a o = new TestClone2a();
    field = (TestClone2a)((TestClone2a)object).clone();
    //mayAlias(field,object); try/catch not supported
  }

  public Object clone()
  {
    try
    {
      return super.clone();
    }
    catch(CloneNotSupportedException exc)
    {
      return null; // impossible
    }
  }
}

class TestClone3a implements Cloneable
{
  static TestClone3a field;
  static Object object = new Object();
  static void test(Object obj)
  {
    object = obj;
    TestClone3a o = new TestClone3a();
    field = (TestClone3a) o.clone(obj);
    notAlias(field,obj);	
  }

  public Object clone(Object obj)
  {
    return new Object();
  }
}
