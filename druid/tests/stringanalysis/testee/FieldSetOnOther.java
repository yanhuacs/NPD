package stringanalysis.testee;

import java.lang.reflect.Method;

public class FieldSetOnOther {
	private String field;
    public void foo(FieldSetOnOther o1, FieldSetOnOther o2) {
        o1.field = "foo";
        bar(o1, o2);
		String s = "stringanalysis.testee."+o1.field;
	    try {
			Class<?> clz=Class.forName(s);
			Object o = clz.newInstance();
			Method method=clz.getMethod(o1.field);
			Method dMethod=clz.getDeclaredMethod(o1.field);
			method.invoke(o);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};		
    }
    
    private void bar(FieldSetOnOther o1, FieldSetOnOther o2) {
        o2.field = "bar";
    }
    
    public static void main(String[] args) {
        new FieldSetOnOther().foo(new FieldSetOnOther(), new FieldSetOnOther());
        
    }
}

