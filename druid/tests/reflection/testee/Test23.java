package reflection.testee;

import java.lang.reflect.Method;

public class Test23 extends Test23A {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test23");
			Method mtd = clz.getMethod(args[0]);
			mtd.invoke(null);
			mtd.invoke(null, 1);
		} catch(Exception e) {
		}
	}
	
	public static void foo() {
		bar();
	}
	
	public static void bar() {}
}

class Test23A extends Test23B {
	public static void foo() {
		bar();
	}
	
	public static void foo(int i) {
	}
	
	public static void bar() {}
}

class Test23B {
	public static void foo(double d) {}
}