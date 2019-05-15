package reflection.testee;

import java.lang.reflect.Method;

public class Test40 {
	static {}
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test40");
			Method mtd = clz.getDeclaredMethod("foo", int.class);
			mtd.invoke(null, 1);
		} catch(Exception e) {
		}
	}
	
	public static void foo(int i) {
		
	}
	
	protected static void foo(float f) {
		
	}
	
	private static void foo(long l) {
		
	}
}
