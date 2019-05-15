package stringanalysis.testee;

import java.lang.reflect.Method;

public class StringBufferNull {
	public void foo(boolean bool) {
		StringBuffer b;
		if (bool) {
			b = null;
		} else {
			b = new StringBuffer();
		}
		String s = "abc" + b;
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
		new StringBufferNull().foo(true);
		new StringBufferNull().foo(false);
	}
}
class abcnull{public void abcnull(){}}