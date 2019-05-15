package stringanalysis.testee;

import java.lang.reflect.Method;

public class SwitchChar {
	private static void foo(char c) {
		String s = "";
		switch (c) {
		case '<':
		case '>':
		case '&':
			s += "t";
			break;
		default:
			s += c;
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
	public static void main(String[] args) {
		foo('a');
		foo('b');
		foo('&');
	}
}
class a{public void a(){}}
class b{public void b(){}}
class t{public void t(){}}