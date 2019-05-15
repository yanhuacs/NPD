package reflection.testee;

import java.lang.reflect.Method;

public class Test38 {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test38");
			Object o = (Test38)clz.newInstance();
			Method mtd = clz.getMethod("bar", String.class, String.class);
			mtd.invoke(o, new Object[] {new String(), null});
			// mtd.invoke(o, "s", null);
		} catch(Exception e) {
		}
	}
	
	public void foo(String s, Integer i) {
	}
	
	public void bar(String s, String j) {
	}
}
