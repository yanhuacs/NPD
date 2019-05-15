package reflection.testee;

import java.lang.reflect.Method;

public class Test7 extends Test7A {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test7A");
			Method mtd = clz.getMethod("foo");
			mtd.invoke(new Test7());
		} catch(Exception e) {
		}
	}
	
	public void foo() {
		bar();
	}
	
	public void bar() {
		
	} 
}

class Test7A {
	public void foo() {
		bar();
	}
	
	public void bar() {
		
	}
}