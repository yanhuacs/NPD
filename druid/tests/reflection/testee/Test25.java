package reflection.testee;

import java.lang.reflect.Method;

public class Test25 extends Test25A {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName(args[0]);
			Method m = clz.getMethod(args[0], double.class);
			Integer i = new Integer(1);
			m.invoke(new Test25(), i);
		} catch(Exception e) {}
	}
	
	public void foo(double i) {}
}

class Test25A {
	public void bar(double i) {}
	public void x(float f) {}
}