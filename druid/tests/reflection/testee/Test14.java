package reflection.testee;

import java.lang.reflect.Method;

public class Test14 {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test14");
			Method mtd = clz.getMethod(args[0]);
			Integer i = (Integer)mtd.invoke(new Test14());
			System.out.println(i);
		} catch(Exception e) {
		}
	}
	
	public int foo() {
		bar();
		return 1;
	}
	
	public void bar() {
		
	} 
}