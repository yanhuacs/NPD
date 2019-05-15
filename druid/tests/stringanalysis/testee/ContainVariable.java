package stringanalysis.testee;

import java.lang.reflect.Method;

public class ContainVariable {
	private void foo(String t, String y) {
        boolean b = t.contains(y);
        String s = "" + b;
        String test = "stringanalysis.testee.x"+s;
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
        new ContainVariable().foo("Hello world!", "world");
        new ContainVariable().foo("The world is round!", "wxs");
    }
}
class xtrue{public void xtrue(){}}
class xfalse{public void xfalse(){}}