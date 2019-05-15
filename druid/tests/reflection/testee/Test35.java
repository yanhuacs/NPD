package reflection.testee;

import java.lang.reflect.Method;

public class Test35 {
	static {
		System.out.println("I am loaded.");
	}
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test35");
			Object o = clz.newInstance();
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
