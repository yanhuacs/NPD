package old;

import static pta.utils.Dummy.mayAlias;

public class InstanceStore {
	Object a;
	public static void main(String[] args) {
		InstanceStore i = new InstanceStore();
		Object o=new Object();
		i.a=o;
		mayAlias(i.a, o);
	}
}
