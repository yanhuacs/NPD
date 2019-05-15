package reflection.testee;

public class Test60 {
	public static void main(String[] args) {
		String clzName = args[0];
		try {
			Class<?> clz = Class.forName(clzName);
			Test60A o = (Test60A) clz.newInstance();
			o.o = new Object();
			System.out.println(o.o);
		} catch (Exception e) {}
	}
}
