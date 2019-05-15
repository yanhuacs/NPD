package reflection.testee;

import java.lang.reflect.Method;

public class Test41 extends Test41A {
	static {}
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test41");
			Object o = clz.newInstance();
			Method mtd = clz.getDeclaredMethod(args[0], int.class);
			mtd.invoke(o, 1);
		} catch(Exception e) {
		}
	}
	
	public void foo(int i) {
		
	}
	
	protected static void foo(float f) {
		
	}
	
	private static void foo(long l) {
		
	}
}
