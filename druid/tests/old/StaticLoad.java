package old;

import static pta.utils.Dummy.mayAlias;

public class StaticLoad {
	static Object a= new Object();
	public static void main(String[] args) {
		Object b = a;
		mayAlias(a, b);
	}
}
