package stringanalysis.testee;

import java.lang.reflect.Method;

public class ClassWithToString {
	public static void foo(ClassWithToString str) {
		String s = "a" + str;
		String test = "stringanalysis.testee."+s;
	    try {
			Class<?> clz=Class.forName(test);
			Object o = clz.newInstance();
			Method method=clz.getMethod(s);
			Method dMethod=clz.getDeclaredMethod(s);
			method.invoke(o);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
	}
	
	public static void main(String[] args){
		new ClassWithToString().foo(new ClassWithToString());
	}
	
	@Override
	public final String toString() {
		return "xx";
	}
}
class axx{public void axx(){}}