package reflection.testee;

import java.lang.reflect.Constructor;

public class Test52 {
	public static void main(String[] args) {
		try {
			Class clz = Class.forName("reflection.testee.Test52A");
			Constructor ctor = clz.getConstructor(Object.class);
			Object o = ctor.newInstance(new Object());
			System.out.println(o.toString());
		} catch(Exception e) {
		}		
	}
}
