package reflection.testee;

import java.lang.reflect.Method;

public class Test39 extends Test39A {
	static {}
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test39");
			Test39 o = (Test39)clz.newInstance();
			Method mtd = clz.getDeclaredMethod("foo", float.class);
			System.out.println(mtd.toString());
			mtd.invoke(o, 1.1);
		} catch(Exception e) {
		}
	}
	
	public void foo(int i) {
		
	}
	
	protected void foo(float f) {
		
	}
	
	private void foo(long l) {
		
	}
}
