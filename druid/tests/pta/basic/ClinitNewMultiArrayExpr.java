package pta.basic;
import static pta.utils.Dummy.mayAlias;

public class ClinitNewMultiArrayExpr {
	public static void main(String[] args) {
		ClinitNewMultiArrayExpr[][] array = new ClinitNewMultiArrayExpr[1][];
		mayAlias(array,array);
	}
}
