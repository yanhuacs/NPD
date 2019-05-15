package reflection.testee;

import java.lang.reflect.Method;

public class Test26 extends Test26A {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName(args[0]);
			Method m = clz.getMethod(args[0], Test26A.class);
			Test26 i = new Test26();
			m.invoke(new Test26(), i);
		} catch(Exception e) {}
	}
	
	public void foo(Test26A i) {}
}

class Test26A {
	public void bar(Test26A i) {}
	public void x(Test26A f) {}
}