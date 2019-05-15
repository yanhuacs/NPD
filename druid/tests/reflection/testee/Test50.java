package reflection.testee;

import java.lang.reflect.Method;

public class Test50 {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test50A");
			Object o = clz.newInstance();
			for(Method m : clz.getMethods())
				if(m.getName().equals("foo"))
					m.invoke(o);
		} catch(Exception e) {
			
		}
	}
}
