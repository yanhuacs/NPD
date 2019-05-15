package stringanalysis.testee;

import java.lang.reflect.Method;

public class FieldCallMethod {
private String field;
    
    public void foo() {
        field = "foo";
        bar();
        String s=field;
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
    
    private String bar() {
        baz();
        return field;
    }
    
    private void baz() {
        field = "baz";
    }
    
    public static void main(String[] args) {
        new FieldCallMethod().foo();
    }
}
class baz{public void baz(){}}