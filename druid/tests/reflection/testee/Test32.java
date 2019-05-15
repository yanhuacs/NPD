package reflection.testee;

import java.lang.reflect.Method;

public class Test32 {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test32");
			Method m = clz.getMethod("foo");
			int i = (int)m.invoke(new Test32());
			System.out.println(i);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public int foo() {
		return 1;
	}
}
