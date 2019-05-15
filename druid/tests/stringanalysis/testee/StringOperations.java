package stringanalysis.testee;

import java.lang.reflect.Method;

public class StringOperations {
	public static void foo() {
		String s = "Abc";
		s = s.toUpperCase() +s.toLowerCase();
		String test = "stringanalysis.testee."+s;
		try {
			Class<?> clz=Class.forName(test);
			Object o = clz.newInstance();
			Method method=clz.getMethod(s);
			Method dMethod=clz.getDeclaredMethod(s);
			method.invoke(o);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
	}
	public static void main(String[] args) {
		foo();
	}
}

class ABCabc{public void ABCabc(){}}