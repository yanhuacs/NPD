package reflection.testee;

import java.lang.reflect.Method;

public class Test12 extends Test12A {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test12");
			Method mtd = clz.getMethod("foo");
			mtd.invoke(new Test12());
		} catch(Exception e) {
		}
	}
	
	public static void foo() {
		bar();
	}
	
	public static void bar() {
		
	} 
}

class Test12A {
	public static void foo() {
		bar();
	}
	
	public static void bar() {
		
	}
}