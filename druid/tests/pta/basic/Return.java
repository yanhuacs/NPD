package pta.basic;

import static pta.utils.Dummy.mayAlias;

public class Return {
	static Object a,b;
	static Object foo(){
		a=new Object();
		return a;
	}
	public static void main(String[] args) {
		b = foo();
		mayAlias(a, b);
	}
}
