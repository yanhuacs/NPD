package reflection.testee;

import java.lang.reflect.Constructor;

public class Test53 {
	public static void main(String[] args) {
		try {
			Class clz = Class.forName("reflection.testee.Test53A");
			Constructor ctor = clz.getDeclaredConstructor(Object.class);
			Object o = ctor.newInstance(new Object());
			System.out.println(o.toString());
		} catch(Exception e) {
			
		}		
	}
}
