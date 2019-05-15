package reflection.testee;

import java.lang.reflect.Method;

public class Test46 {
	static {}
	public static void main(String[] args) {
		try {
			Class clz = Class.forName("reflection.testee.Test46");
			Method mtd = clz.getMethod("foo", String.class, int.class);
			String s = (String) mtd.invoke(null, "s", 1);
			System.out.println(s.toUpperCase());
		} catch(Exception e) {
		}
	}
	
	public static String foo(String s, int i) {
		System.out.println("Test46.foo()");
		System.out.println(s.toLowerCase());
		return bar(i);
	}
	
	public static String bar(int i) {
		return new String(Integer.valueOf(i).toString());
	}
}
