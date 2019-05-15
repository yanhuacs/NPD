package stringanalysis.testee;

import java.lang.reflect.Method;

public class IrrelevantArgs {
	private void foo(IrrelevantArgs arg1, String s, IrrelevantArgs arg2) {
		String x = s;
		String test = "stringanalysis.testee."+x;
		try {
			Class<?> clz=Class.forName(test);
			Object o = clz.newInstance();
			Method method=clz.getMethod(x);
			Method dMethod=clz.getDeclaredMethod(x);
			method.invoke(o);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};		
	}
	
	public static void main(String[] args) {
		new IrrelevantArgs().foo(new IrrelevantArgs(), "abc", new IrrelevantArgs());
	}
}
