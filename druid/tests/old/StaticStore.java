package old;

import static pta.utils.Dummy.mayAlias;

public class StaticStore {
	static Object a;
	public static void main(String[] args) {
		Object o = new Object();
		a=o;
		mayAlias(a, o);
	}
}
