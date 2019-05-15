package reflection.testee;

import java.lang.reflect.Method;

public class Test4 extends Test4A {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName(args[0]);
			Test4A o = (Test4A)clz.newInstance();
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

class Test4A {
	public void foo() {
		
	}
	
	public void bar() {
		
	}
}