package reflection.testee;

import java.lang.reflect.Method;

public class Test21 extends Test21A {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test21");
			Method mtd = clz.getMethod("foo");
			mtd.invoke(clz.newInstance());
			mtd.invoke(clz.newInstance(), 1);
		} catch(Exception e) {
		}
	}
	
	public void foo() {
		bar();
	}
	
	public void bar() {}
}

class Test21A extends Test21B {
	public void foo() {
		bar();
	}
	
	public void foo(int i) {
	}
	
	public void bar() {}
}

class Test21B {
	public void foo(double d) {}
}