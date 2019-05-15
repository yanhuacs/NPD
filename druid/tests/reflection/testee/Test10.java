package reflection.testee;

import java.lang.reflect.Method;

public class Test10 extends Test10A {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test10");
			Method mtd = clz.getMethod("foo");
			mtd.invoke(new Test10A());
		} catch(Exception e) {
		}
	}
	
	public void foo() {
		bar();
	}
	
	public void bar() {
		
	} 
}

class Test10A {
	public void foo() {
		bar();
	}
	
	public void bar() {
		
	}
}