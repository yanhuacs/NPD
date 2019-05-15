package reflection.testee;

import java.lang.reflect.Method;

public class Test36 {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test36");
			Object o = (Test36)clz.newInstance();
			Method mtd = clz.getMethod(args[0]);
			Object[] argList = null;
			mtd.invoke(o, argList);
		} catch(Exception e) {
		}
	}
	
	public void foo() {
		bar();
	}
	
	public void bar() {
	} 
}
