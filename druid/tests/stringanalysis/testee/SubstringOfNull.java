package stringanalysis.testee;

import java.lang.reflect.Method;

public class SubstringOfNull {
	public void foo(boolean b, boolean c) {
		String s = "abc";
		if (b) {
			s = null;
		}
		if (c) {
			s = s.substring(1);
		}
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
		new SubstringOfNull().foo(false, true);
		new SubstringOfNull().foo(false, false);
		new SubstringOfNull().foo(true, false);
	}
}
class bc{public void bc(){}}