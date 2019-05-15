package stringanalysis.testee;

import java.lang.reflect.Method;

public class ConcatInterprocedual {
	public static String addA(String v)
	{
		return v+"A";
	}
    public static void main(String[] args) {
    	String a="a";
    	String b=addA(a);
    	String s= "stringanalysis.testee."+b;
    	try {
			Class<?> clz=Class.forName(s);
			Object o = clz.newInstance();
			Method method=clz.getMethod(b);
			method.invoke(o);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}

class aA{public void aA(){System.out.println("aA");}}