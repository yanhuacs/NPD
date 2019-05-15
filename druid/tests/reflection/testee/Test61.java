package reflection.testee;

import java.lang.reflect.Constructor;

public class Test61 {
	public static void main(String[] args) {
		String name = args[0];
		Test61 t = new Test61();
		Test61A o1 = (Test61A) t.foo(name);
		System.out.println(o1);
		Test61A o2 = (Test61A) t.bar(name);
		System.out.println(o2);
	}
	
	public Object foo(String name) {
		Object o = null;
		try {
			Class<?> clz = Class.forName(name);
			o = clz.newInstance();
		} catch (Exception e) {}
		return o;
	}
	
	public Object bar(String name) {
		Object o = null;
		try {
			Class<?>clz = Class.forName(name);
			Constructor<?> ctor = clz.getConstructor(String.class);
			o = ctor.newInstance(new String());
		} catch (Exception e) {}
		return o;
	}
}
