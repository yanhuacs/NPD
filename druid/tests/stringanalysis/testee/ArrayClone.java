package stringanalysis.testee;

import java.lang.reflect.Method;

public class ArrayClone {
	public void foo() {
		String[] array = new String[] {"foo"};
		String[] clone = array.clone();
		array[0] = "bar";
		
		String s=clone[0];
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
		new ArrayClone().foo();
	}
}
