package pta.basic;

import static pta.utils.Dummy.mayAlias;

public class StaticParameter {
	static Object o;
	static void foo(Object a){
		mayAlias(a, o);
	}
	public static void main(String[] args) {
		o=new Object();
		foo(o);
	}
}
