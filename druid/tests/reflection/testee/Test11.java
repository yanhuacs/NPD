package reflection.testee;

import java.lang.reflect.Method;

public class Test11 extends Test11A {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test11A");
			Method mtd = clz.getMethod("foo");
			mtd.invoke(null);
		} catch(Exception e) {
		}
	}
	
	public static void foo() {
		bar();
	}
	
	public static void bar() {
		
	} 
}

class Test11A {
	public static void foo() {
		bar();
	}
	
	public static void bar() {
		
	}
}