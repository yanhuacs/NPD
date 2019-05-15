package stringanalysis.testee;

import java.lang.reflect.Method;

public class ExpreSensitivity {
	public void foo(final boolean b) {
		String s = "sens1";
		if (b) {
			if (!b) {
				s = "sens2";
			}
		}
		// the usefulness of this test is arguable, since real code is almost always reachable
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
		boolean b=false;
		if(args[0]!=null) b=true;
		new ExpreSensitivity().foo(b);
	}
}
class sens1{public void sens1(){}}
class sens2{public void sens2(){}}