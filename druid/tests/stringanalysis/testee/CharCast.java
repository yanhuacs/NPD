package stringanalysis.testee;

import java.lang.reflect.Method;

public class CharCast {
	private void foo(String s, int x, int y) {
        char a = (char)x;
        char b = (char)(x + y);
		String test = "stringanalysis.testee."+s+a+b;
	    try {
			Class<?> clz=Class.forName(test);
			Object o = clz.newInstance();
			Method method=clz.getMethod(s+a+b);
			Method dMethod=clz.getDeclaredMethod(s+a+b);
			method.invoke(o);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
    }
    public static void main(String[] args) {
        new CharCast().foo("abcd", 65, 3);
        new CharCast().foo("xyz", 65, 8);
    }
}
class abcdAD{public void abcdAD(){}}
class xyzAI{public void xyzAI(){}}