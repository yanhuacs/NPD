package reflection.testee;

import java.lang.reflect.Method;

public class Test27 {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test27");
			Method m = clz.getMethod(args[0]);
			Object[] paraList = null;
			if(args.length == 1) {
				paraList = new Object[2];
				paraList[0] = new String();
				paraList[1] = new Integer(0);
			} else if (args.length == 2) {
				paraList = new Object[1];
				paraList[0] = new Double(0.5);
			} else {
				paraList = new Object[0];
			}
			m.invoke(new Test27(), paraList);
		} catch(Exception e) {}
	}
	
	public void a(String s) {
		
	}
	
	public void b(double d) {
		
	}
	
	public void c(String s, int i) {
		
	}
	
	public void d(double s, int d) {
		
	}
	
	public void e() {
		
	}
}
