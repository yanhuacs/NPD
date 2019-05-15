package reflection.testee;

import java.lang.reflect.Constructor;

public class Test56 {
	public static void main(String[] args) {
		try {
			Class clz = Class.forName("reflection.testee.Test56A");
			Constructor<Test56A> ctor1 = clz.getConstructor();
			Test56A o1 =  ctor1.newInstance();
			o1.a();
			
			Constructor<Test56A> ctor2 = clz.getConstructor(int.class, int.class);
			Test56A o2 = ctor2.newInstance(new Object[]{1, 2});
			o2.b();
			
			Constructor<Test56A> ctor3 = clz.getConstructor(String.class, Object.class);
			Test56A o3 = ctor3.newInstance("", new Object());
			o3.c();
			
			Constructor<Test56A> ctor4 = clz.getConstructor(Integer.class, Integer.class);
			Test56A o4 = ctor4.newInstance(new Object[]{1, 2});
			o4.d();
			
			Constructor<Test56A> ctor5 = clz.getConstructor(Test56B.class);
			Test56A o5 = ctor5.newInstance(new Test56B());
			o5.e();
		} catch(Exception e) {
			
		}		
	}
}
