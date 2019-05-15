package reflection.testee;

import java.lang.reflect.Method;

public class Test34 {
	private Test34 t;
	public static void main(String[] args) {
		try {
			StringBuffer buffer =  new StringBuffer();
			Class<?> clz = Class.forName(buffer.toString());
			Test34 t = (Test34) clz.newInstance();
			Method m = clz.getMethod(buffer.toString());
			m.invoke(t);
			m = clz.getMethod(buffer.toString(), Test34.class);
			m.invoke(t, t);
		} catch(Exception e) {
		}
	}
	
	public void setO(Test34 t) {
		this.t = t;
	}
	
	public Test34 getO() {
		return t;
	}
	
	public void foo() {}
}
