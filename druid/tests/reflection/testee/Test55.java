package reflection.testee;

import java.lang.reflect.Constructor;

public class Test55 {
	public static void main(String[] args) {
		try {
			Class clz = Class.forName("reflection.testee.Test55A");
			Constructor[] ctors = clz.getDeclaredConstructors();
			for(Constructor ctor : ctors) {
				Object o = ctor.newInstance(new Object());
				System.out.println(o.toString());
			}
		} catch(Exception e) {
			
		}		
	}
}
