package stringanalysis.testee;

import java.lang.reflect.Method;

public class FieldConditional {
private boolean b;
	
	public void foo() {
		b = false;
		String s = "ab";
		if (b) {
			s += "c";
		} else {
			s += "d";
		}
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
		new FieldConditional().foo();
	}
}
class abd{public void abd(){}}
