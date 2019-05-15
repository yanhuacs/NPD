package stringanalysis.testee;

import java.lang.reflect.Method;

public class IfEqualsNotString {
	private static void foo(String s, Object y) {
        if (s.equals(y)) {
        	System.out.println(s);
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
        foo("abc", "abc");
        foo("bar", new Object());
        foo("xyz", new StringBuffer("xyz"));
    }
}
