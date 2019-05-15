package reflection.testee;

import java.lang.reflect.Method;

public class Test13 {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test13");
			Method mtd = clz.getMethod(args[0]);
			int i = (int)mtd.invoke(new Test13());
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