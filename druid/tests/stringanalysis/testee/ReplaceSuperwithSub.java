package stringanalysis.testee;

import java.lang.reflect.Method;

public class ReplaceSuperwithSub {
	public void foo() {
		String s = "abcbcabcb";
		s = s.replace("abc", "b");
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
		new ReplaceSuperwithSub().foo();
	}
}
class bbcbb{public void bbcbb(){}}