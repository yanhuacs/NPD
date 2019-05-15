package reflection.testee;

import java.lang.reflect.Method;

public class Test47 {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName(args[0]);
			Object o = (Test47) clz.newInstance();
			Method mtd = clz.getMethod(args[0]);
			mtd.invoke(o);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	public void foo() {
		bar();
	}
	
	public void bar() {
		
	}
}
