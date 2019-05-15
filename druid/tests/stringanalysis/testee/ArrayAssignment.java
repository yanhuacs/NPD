package stringanalysis.testee;

import java.lang.reflect.Method;

public class ArrayAssignment {
	public static void test(int i, int j) {
		String[] array = new String[5];
		
		array[i] = "foo";
		array[j] = "bar";
		
		String s = "stringanalysis.testee."+array[i];
	    try {
			Class<?> clz=Class.forName(s);
			Object o = clz.newInstance();
			Method method=clz.getMethod(array[i]);
			Method dMethod=clz.getDeclaredMethod(array[i]);
			method.invoke(o);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
	}
	
	public static void main(String[] args) {
		test(2, 2);
		test(1, 2);
	}
}

class foo{public void foo(){}}
class bar{public void bar(){}}
