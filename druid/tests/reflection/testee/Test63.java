package reflection.testee;

import java.lang.reflect.Method;

public class Test63 {
	public static void main(String[] args) {
		Test63 test63 = new Test63();
		String name = args[0];
		Object o = test63.a(name);
		Method mtd = test63.b(name);
		try {
			mtd.invoke(o);
		} catch(Exception e) {}
	}
	
	public Object a(String name) {
		Object o = null;
		try {
			Class<?> clz = Class.forName(name);
			o = clz.newInstance();
		} catch(Exception e) {
			
		}
		return o;
	}
	
	public Method b(String name) {
		Method mtd = null;
		try {
			if(name.equals(""))
				mtd = Class.forName("reflection.testee.Test63A").getMethod(name);
			else if(name.equals(""))
				mtd = Class.forName("reflection.testee.Test63B").getMethod(name);
			else
				mtd = Class.forName(new String()).getMethod(name);
		} catch(Exception e) {}
		return mtd;
	}
}
