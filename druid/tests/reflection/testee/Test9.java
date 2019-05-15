package reflection.testee;

import java.lang.reflect.Method;

public class Test9 extends Test9A {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test9A");
			Method mtd = clz.getMethod("foo");
			mtd.invoke(new Test9());
		} catch(Exception e) {
		}
	}
	
	public void foo() {
		bar();
	}
	
	public void bar() {
		
	} 
}

class Test9A {
	public void foo() {
		bar();
	}
	
	public void bar() {
		
	}
}