package stringanalysis.testee;

import java.lang.reflect.Method;

public class InterfaceMethod {
	private interface Inner {
		String foo();
	}
	private static class InnerClass implements Inner {
		public String foo() {
			return "abc";
		}
	}
	public void foo(Inner inner) {
		String s = inner.foo();
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
		new InterfaceMethod().foo(new InnerClass());
	}
}
class abc{public void abc(){}}