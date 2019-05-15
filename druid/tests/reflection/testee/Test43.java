package reflection.testee;

import java.lang.reflect.Method;

public class Test43 {
	static {}
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName(null);
			Object o = (Test43) clz.newInstance();
			Method mtd = clz.getDeclaredMethod(args[0], int.class);
			mtd.invoke(o, 1);
		} catch(Exception e) {
		}
	}
	
	public void foo(int i) {
		
	}
}
