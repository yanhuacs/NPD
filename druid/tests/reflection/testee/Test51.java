package reflection.testee;

import java.lang.reflect.Method;

public class Test51 {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test51A");
			Object o = clz.newInstance();
			for(Method m : clz.getDeclaredMethods())
				if(m.getName().equals("bar"))
					m.invoke(o);
		} catch(Exception e) {
			
		}
	}
}
