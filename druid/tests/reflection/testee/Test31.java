package reflection.testee;

import java.lang.reflect.Method;

public class Test31 {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test31");
			Method m = clz.getMethod(args[0], Test31.class);
			Test31 t = (Test31)m.invoke(new Test31(), new Test31());
			t.foo(t);
			t = (Test31)m.invoke(new Test31());
			t.bar();
		} catch(Exception e) {}
	}
	
	public Test31 foo(Test31 o) {
		return o;
	}
	
	public Test31 bar() {
		return new Test31();
	}
}
