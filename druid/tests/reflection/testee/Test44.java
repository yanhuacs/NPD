package reflection.testee;

import java.lang.reflect.Method;

public class Test44 {
	static {}
	public static void main(String[] args) {
		try {
			Class<?> clz = Test44A.class;
			Object o = clz.newInstance();
			Method mtd = clz.getMethod("foo");
			mtd.invoke(o);
			mtd = clz.getDeclaredMethod("bar");
			mtd.setAccessible(true);
			mtd.invoke(o);
		} catch(Exception e) {
		}
	}
	
	public void foo(int i) {
		
	}
}
