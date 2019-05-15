package stringanalysis.testee;

import java.lang.reflect.Method;

public class IfEndwith {
	private static void foo(String s) {
        if (s.endsWith("oo")) {
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
    }
    public static void main(String[] args) {
        foo("bfooX");
        foo("fo");
        foo("ffoo");
    }
}
class ffoo{public void ffoo(){}}