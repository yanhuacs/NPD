package stringanalysis.testee;

import java.lang.reflect.Method;

public class IfCompoundContain {
	private static void foo(String s, boolean x) {
        boolean z;
        if (x) {
            z = s.equals("Y");
        } else {
            z = s.contains("X");
        }
        if (z) {
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
    private static void bar(String s) {
        foo(s,true);
        foo(s,false);
    }
    public static void main(String[] args) {
        bar("X");
        bar("Y");
        bar("Xbc");
    }
}

class X{public void X(){}}
class Y{public void Y(){}}
class Xbc{public void Xbc(){}}
