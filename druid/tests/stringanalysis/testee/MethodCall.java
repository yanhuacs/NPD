package stringanalysis.testee;

import java.lang.reflect.Method;

public class MethodCall {
	public void foo() {
		new GetX(); new GetZ();
		Foo foo = new GetY();
		String s = foo.foo();
		System.out.println(s);
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
	
	public interface Foo {
		String foo();
	}
	public static class GetX implements Foo {	// implements interface directly
		public String foo() {
			return "xtest";
		}
	}
	public static class GetY implements Foo {	// implements, but has a subtype
		public String foo() {
			return "ytest";
		}
	}
	public static class GetZ extends GetY {		// subtype of GetY
		@Override
		public String foo() {
			return "ztest";
		}
	}
	
	public static void main(String[] args) {
		new MethodCall().foo();
	}
}
class ytest{public void ytest(){}}
