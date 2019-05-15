package pta.basic;
import static pta.utils.Dummy.mayAlias;
/**
 * clinit for static invoke expr
 * @author Ammonia
 *
 */
public class ClinitStaticInvoke {
	static void foo(){Object A = new Object(); mayAlias(A,A);}
	public static void main(String[] args) {
		foo();
	}
}
