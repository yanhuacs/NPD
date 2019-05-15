package reflection.testee;

import java.lang.reflect.Method;

public class Test8 implements Test8A {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test8A");
			Method mtd = clz.getMethod("foo");
			mtd.invoke(new Test8());
		} catch(Exception e) {
		}
	}
	
	public void foo() {
		bar();
	}
	
	public void bar() {
		
	} 
}

interface Test8A {
	public void foo();
	
	public void bar();
}