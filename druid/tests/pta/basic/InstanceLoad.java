package pta.basic;

import static pta.utils.Dummy.mayAlias;

public class InstanceLoad {
	Object a= new Object();
	public static void main(String[] args) {
		InstanceLoad o = new InstanceLoad();
		Object a = o.a;
		mayAlias(a, o.a);
	}
}
