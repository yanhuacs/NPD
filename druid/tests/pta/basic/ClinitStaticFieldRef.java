package pta.basic;
import static pta.utils.Dummy.mayAlias;
public class ClinitStaticFieldRef {
	static int a;
	public static void main(String[] args) {
		a=0;
		mayAlias(a,0);
	}
}
