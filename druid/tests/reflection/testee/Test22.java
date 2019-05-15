package reflection.testee;

import java.lang.reflect.Method;
import java.util.Arrays;

public class Test22 {
	public static void main(String[] args) {
		try {
			Class<?> clz = Class.forName("reflection.testee.Test22");
			Method mtd = clz.getMethod(args[0], int.class);
			int[] is = (int[])mtd.invoke(clz.newInstance(), 1);
			System.out.println(Arrays.toString(is));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public int[] foo(int i) {
		bar();
		return new int[] {i};
	}
	
	public void bar() {}
}