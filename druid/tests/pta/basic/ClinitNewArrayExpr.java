package pta.basic;
import static pta.utils.Dummy.mayAlias;

public class ClinitNewArrayExpr {
	public static void main(String[] args) {
		ClinitNewArrayExpr[] array = new ClinitNewArrayExpr[1];
		mayAlias(array,array);
	}
}
