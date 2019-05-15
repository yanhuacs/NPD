package reflection.testee;

import java.lang.reflect.Method;

public class Test48 {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test48");
			Method mtd = clz.getMethod(args[0]);
			mtd.invoke(null);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	public static void foo() {
		bar();
	}
	
	public static void bar() {
		
	}
}
