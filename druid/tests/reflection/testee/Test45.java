package reflection.testee;

import java.lang.reflect.Method;

public class Test45 {
	static {}
	public static void main(String[] args) {
		try {
			Class clz = Class.forName(args[0]);
			Method mtd = clz.getMethod(args[0]);
			mtd.invoke(new Test45());
		} catch(Exception e) {
		}
	}
	
	public void foo() {
		
	}
}
