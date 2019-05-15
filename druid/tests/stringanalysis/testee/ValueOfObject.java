package stringanalysis.testee;

import java.lang.reflect.Method;

public class ValueOfObject {
	public void foo(boolean b){ 
		String x = "abc";
		String y = "xyz";
		Object o1;
		if (b) {
			o1 = x;
		} else {
			o1 = y;
		}
		String s = String.valueOf(o1);
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
		new ValueOfObject().foo(true);
		new ValueOfObject().foo(false);
	}
}
class xyz{public void xyz(){}}
