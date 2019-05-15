package reflection.testee;

import java.lang.reflect.Method;

public class Test5 {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName(args[0]);
			Test5IA o = (Test5IA)clz.newInstance();
			Method mtd = clz.getMethod("foo");
			mtd.invoke(o);
		} catch(Exception e) {
		}
	}
	
	public void foo() {
		bar();
	}
	
	public void bar() {
		
	} 
}

interface Test5IA {
	public void foo();
	
	public void bar();
}

interface Test5IB extends Test5IA {
	
}

interface Test5IX extends Test5IB {
	
}

interface Test5IY extends Test5IB {
	
}

interface Test5IC extends Test5IA {
	
}

class Test5D implements Test5IA {
	@Override
	public void foo() {}

	@Override
	public void bar() {}
}

class Test5G extends Test5D {
	
}

class Test5B implements Test5IX {
	@Override
	public void foo() {}

	@Override
	public void bar() {}
}

class Test5E extends Test5B {
	
}

class Test5C implements Test5IY {
	@Override
	public void foo() {}

	@Override
	public void bar() {}
}

class Test5F extends Test5C {
}