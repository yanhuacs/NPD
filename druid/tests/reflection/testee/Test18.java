package reflection.testee;

import java.lang.reflect.Method;

public class Test18 extends Test18A {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test18");
			Method mtd = clz.getMethod(args[0]);
			Test18A i = (Test18A)mtd.invoke(new Test18());
			System.out.println(i);
		} catch(Exception e) {
		}
	}
	
	public Test18 foo() {
		return new Test18();
	}
}

class Test18A {
	
}