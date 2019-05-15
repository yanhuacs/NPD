package stringanalysis.testee;

import java.lang.reflect.Method;

public class StringConstructor{
	public void foo(int x) {
		String s = new String("abcd").toUpperCase() + new String();
		
		StringBuffer buffer = new StringBuffer();
		buffer.append(s); 		// buffer = ABCD
		
		StringBuilder builder = new StringBuilder(buffer); 
								// builder = ABCD
		builder.append("w"); 	// builder = ABCDw
		
		s = new String(buffer) + new String(builder); 
								// s = ABCDABCDw
		
		// modifying these after the constructor calls will not mess up 's'
		buffer.append("X"); 
		builder.setLength(0);
		
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
		new StringConstructor().foo(5);
	}
}
class ABCDABCDw{public void ABCDABCDw(){}}
