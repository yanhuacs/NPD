package stringanalysis.testee;

import java.lang.reflect.Method;


public class CharArgument {
	
	private void foo(char ch){
        String s = "stringanalysis.testee.x";
        s += ch;
        s += ch;
        try {
			Class<?> clz=Class.forName(s);
			Object o = clz.newInstance();
			Method method=clz.getMethod(s);
			method.invoke(o);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    public static void main(String[] args){
        new CharArgument().foo('A');
        new CharArgument().foo('B');
    }
	
}

class xAA{
	public void xAA(){
		System.out.println("A");
	}	
}

class xBB{
	public void xBB(){
		System.out.println("B");
	}
}
