package reflection.testee;

import java.lang.reflect.Method;

public class Test30 extends Test30A {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test30");
			Method m = clz.getMethod(args[0], Test30.class);
			m.invoke(new Test30(), new Test30());
			m.invoke(new Test30(), new Test30(), 1);
		} catch(Exception e) {}
	}
	
	public void foo(Test30 o) {
		o.bar();
	}
	
	public void foo(Test30 o, int x) {
		o.bar();
	}
}

class Test30A {
	public void bar() {
	}
}