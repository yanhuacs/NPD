package stringanalysis.testee;

import java.lang.reflect.Method;

public class BasicMethodCall {
	public String constant() {
		return "abc";
	}
	public void foo() {
		String s = constant();
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
		new BasicMethodCall().foo();
	}
}
