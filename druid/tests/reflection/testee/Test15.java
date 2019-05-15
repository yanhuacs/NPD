package reflection.testee;

import java.lang.reflect.Method;

public class Test15 {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test15");
			Method mtd = clz.getMethod(args[0]);
			double i = (double)mtd.invoke(new Test15());
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