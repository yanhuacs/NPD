package old;

import static pta.utils.Dummy.mayAlias;

public class InstanceParameter {
	static Object o=new Object();
	void foo(Object a){
		mayAlias(a, o);
	}
	public static void main(String[] args) {
		new InstanceParameter().foo(o);
	}
}
