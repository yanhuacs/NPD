package reflection.testee;

import java.lang.reflect.Constructor;

public class Test54 {
	public static void main(String[] args) {
		try {
			Class clz = Class.forName("reflection.testee.Test54A");
			Constructor[] ctors = clz.getConstructors();
			for(Constructor ctor : ctors) {
				ctor.setAccessible(true);
				Object o = ctor.newInstance(new Object());
				System.out.println(o.toString());
			}
		} catch(Exception e) {
		}		
	}
}
