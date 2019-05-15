package pta.basic;
import static pta.utils.Dummy.mayAlias;
public class ArrayElement {
	public static void main(String[] args) {
		int[] array = new int[1];
		array[0]=1;
		Object[] array2 = new Object[1];
		Object x = new Object();
		array2[0]= x;
                mayAlias(array[0],1);
		mayAlias(array2[0], x);
	}
}
