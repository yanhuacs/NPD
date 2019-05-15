package reflection.testee;

import java.lang.reflect.Method;

public class Test29 {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test29");
			Method m = clz.getMethod(args[0]);
			Object[] paraList = null;
			paraList = new Object[1];
			paraList[0] = null;
			m.invoke(new Test29(), paraList);
		} catch(Exception e) {}
	}
	
	public void a(String s) {
		
	}
	
	public void b(double d) {
		
	}
	
	public void b(Double d) {
		
	}
	
}