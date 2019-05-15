package reflection.testee;

import java.lang.reflect.Method;

public class Test6 {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test6A");
			Test6A o = (Test6A)clz.newInstance();
			Method mtd = clz.getMethod(args[0]);
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

class Test6A {
	public void foo() {
		bar();
	}
	
	public void bar() {
		
	}
}