package reflection.testee;

import java.lang.reflect.Method;

public class Test24 extends Test24A {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName(args[0]);
			Method m = clz.getMethod(args[0], int.class);
			Integer i = new Integer(1);
			m.invoke(new Test24(), i);
		} catch(Exception e) {}
	}
	
	public void foo(int i) {}
}

class Test24A {
	public void bar(int i) {}
}