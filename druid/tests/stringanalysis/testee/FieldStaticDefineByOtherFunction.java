package stringanalysis.testee;

import java.lang.reflect.Method;

public class FieldStaticDefineByOtherFunction {
private static String field;
	
	public static void foo() {
		String s = "xyz";
		bar();
		s += field;
		String string = "stringanalysis.testee."+s;
		try {
			Class<?> clz=Class.forName(string);
			Object o = clz.newInstance();
			Method method=clz.getMethod(s);
			Method dMethod=clz.getDeclaredMethod(s);
			method.invoke(o);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
	}
	
	private static void bar() {
		field = "abc";
	}
	
	public static void main(String[] args) {
		foo();
	}
}

class xyzabc{public void xyzabc(){}}