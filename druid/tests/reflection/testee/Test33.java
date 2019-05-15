package reflection.testee;

import java.lang.reflect.Method;

public class Test33 {
	private Test33 t;
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test33");
			Test33 t = (Test33) clz.newInstance();
			Method m = clz.getMethod("setO", Test33.class);
			m.invoke(t, t);
			m = clz.getMethod("getO");
			Test33 o = (Test33) m.invoke(t);
			o.foo();
		} catch(Exception e) {
		}
	}
	
	public void setO(Test33 t) {
		this.t = t;
	}
	
	public Test33 getO() {
		return t;
	}
	
	public void foo() {}
}
