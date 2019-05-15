package pta.basic;
import static pta.utils.Dummy.mayAlias;
public class ClinitNewExpr {
	static void test() {
		Object x = new Object();
		mayAlias(x,x);
	}	
	
	public static void main(String[] args) {
		new ClinitNewExpr().test();
	}
}
