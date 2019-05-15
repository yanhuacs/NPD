package stringanalysis.testee;

import java.lang.reflect.Method;

public class IfLengthEqualsConst {
	private static void foo(String s) {
        if (s.length() == 6) {
			
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
        foo("abc");
        foo("abcdef");
        foo("abcdefabc");
        String s = "x";
        for (int i=0; i<6; i++) {
            s += "y";
            foo(s);
        }
    }
}
class abcdef{public void abcdef(){}}
class xyyyyy{public void xyyyyy(){}}
