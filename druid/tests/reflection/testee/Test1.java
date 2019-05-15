package reflection.testee;

import java.lang.reflect.Method;

public class Test1 {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test1");
			Object o = (Test1)clz.newInstance();
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
