package stringanalysis.testee;

import java.lang.reflect.Method;

public class FieldNested {
	private String apples;
	private String oranges;
	
	public void foo() {
		apples = oranges = "foo";
		bar();
		System.out.println(oranges);
		String s=apples+oranges;
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
	private void bar() {
		baz();
		bong();
	}
	private void baz() {
		apples = "baz";
	}
	private void bong() {
		oranges = apples;
	}
	
	public static void main(String[] args) {
		new FieldNested().foo();
	}
}
class bazbaz{public void bazbaz(){}}
