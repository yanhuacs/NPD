package reflection.testee;

import java.lang.reflect.Method;

public class Test17 {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test17");
			Method mtd = clz.getMethod(args[0]);
			Test17 i = (Test17)mtd.invoke(new Test17());
			System.out.println(i);
		} catch(Exception e) {
		}
	}
	
	public Test17 foo() {
		return new Test17();
	}
}