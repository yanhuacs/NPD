package reflection.testee;

import java.lang.reflect.Method;

public class Test20 extends Test20A {
	public static void main(String[] args) {
		try {
			Class<?> clz = null;
			if(args.length == 1)
				 clz = Class.forName("reflection.testee.Test20");
			else
				clz = Class.forName("reflection.testee.Test20A");
			Method mtd = clz.getMethod("foo");
			mtd.invoke(clz.newInstance());
		} catch(Exception e) {
		}
	}
	
	public void foo() {
		bar();
	}
	
	public static void x() {}
	
	public void bar() {}
}

class Test20A {
	public void foo() {
		bar();
	}
	public void bar() {}
	public static void x() {}
}