package reflection.testee;

import java.lang.reflect.Method;

public class Test37 extends Test37A {
	public static int i = 0;
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test37");
			Object o = (Test37)clz.newInstance();
			Method mtd = clz.getMethod(args[0]);
			Object[] argList = null;
			mtd.invoke(o, argList);
		} catch(Exception e) {
		}
	}
	
	public void foo() {
		bar();
	}
	
	public void bar() {
	} 
}

class Test37A {
	static {
		System.out.println("Test37A is loaded.");
	}
}
