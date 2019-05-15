package reflection.testee;

import java.lang.reflect.Method;

public class Test3 extends Test3A {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName(args[0]);
			Test3 o = (Test3)clz.newInstance();
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

class Test3A {
	public void foo() {
		
	}
	
	public void bar() {
		
	}
}