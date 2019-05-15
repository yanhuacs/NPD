package stringanalysis.testee;

import java.awt.SystemTray;
import java.lang.reflect.Method;

public class CharAtIntegerString {
    private String secondDigit(int x) {
        String s = "" + x;
        char ch;
        if (s.length() < 2)
            ch = 'A';
        else
            ch = s.charAt(1);
        return "" + ch;
    }
    private void bar(int x) {
        String s = "stringanalysis.testee.x"+secondDigit(x);
        try {
			Class<?> clz=Class.forName(s);
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
        new CharAtIntegerString().bar(-10);
        new CharAtIntegerString().bar(5);
        new CharAtIntegerString().bar(10);
        new CharAtIntegerString().bar(11);
        new CharAtIntegerString().bar(12);
        new CharAtIntegerString().bar(13);
        new CharAtIntegerString().bar(14);
        new CharAtIntegerString().bar(15);
        new CharAtIntegerString().bar(16);
        new CharAtIntegerString().bar(17);
        new CharAtIntegerString().bar(18);
        new CharAtIntegerString().bar(19);
    }
}
class x0{public void x0(){}}
class x1{public void x1(){}}
class x2{public void x2(){}}
class x3{public void x3(){}}
class x4{public void x4(){}}
class x5{public void x5(){}}
class x6{public void x6(){}}
class x7{public void x7(){}}
class x8{public void x8(){}}
class x9{public void x9(){}}
class xA{public void xA(){}}


