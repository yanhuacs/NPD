package reflection.testee;

import java.lang.reflect.Constructor;

public class Test58 {
	public static void main(String[] args) {
		try {
			Class c = null;
			Constructor ctor = null;
			Object o1 = null;
			Object o2 = null;
			if(args.length == 0) {
				c = Class.forName(args[0]);
				ctor = c.getConstructor(Test58A.class);
				o1 = (Test58A) ctor.newInstance(new Test58A(""));
				o2 = (Test58A) c.newInstance();
			} else {
				c = Class.forName(args[1]);
				ctor = c.getConstructor(Test58B.class);
				o1 = (Test58B) ctor.newInstance(new Test58B(new Object()));
				o2 = (Test58B) c.newInstance();
			}
			System.out.println(o1);
			System.out.println(o2);
		} catch(Exception e) {
			e.printStackTrace();
		}		
	}
}
