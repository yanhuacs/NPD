package reflection.testee;

import java.lang.reflect.Method;

public class Test16 {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test16");
			Method mtd = clz.getMethod(args[0]);
			int i = (int)mtd.invoke(new Test16());
			System.out.println(i);
		} catch(Exception e) {
		}
	}
	
	public Integer foo() {
		bar();
		return 1;
	}
	
	public int x() {
		return 1;
	}
	
	public void bar() {
		
	} 
}