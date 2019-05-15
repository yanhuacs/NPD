package stringanalysis.testee;

import java.lang.reflect.Method;

public class ArrayUpdate {
	public void foo() {
		String[] array = new String[10];
		array[1] = "foo";
		array[2] = "bar";
		String s=array[1];
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
	public static void main(String[] args) {
		new ArrayUpdate().foo();
	}
}
