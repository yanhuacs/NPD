package reflection.testee;

import java.lang.reflect.Method;

public class Test2 extends Test2A {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test2");
			Object o = (Test2A)clz.newInstance();
			Method mtd = clz.getMethod("foo");
			mtd.invoke(o);
		} catch(Exception e) {
		}
	}
	
	public void foo() {
		bar();
	}
	
	public void bar() {
		
	} 
}

class Test2A {
	public void foo() {
		
	}
	
	public void bar() {
		
	}
}