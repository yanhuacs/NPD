package reflection.testee;

import java.lang.reflect.Method;

public class Test19 extends Test19A {
	public static void main(String[] args) {
		try {
			Class<?> clz = null;
			if(args.length == 1)
				 clz = Class.forName("reflection.testee.Test19");
			else
				clz = Class.forName("reflection.testee.Test19A");
			Method mtd = clz.getMethod(args[0]);
			mtd.invoke(null);
		} catch(Exception e) {
		}
	}
	
	public static void foo() {
		bar();
	}
	
	public static void bar() {}
}

class Test19A {
	public static void foo() {
		bar();
	}
	public static void bar() {}
}