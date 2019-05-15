package reflection.testee;

public class Test {
	public static void main(String[] args) {
		 String clzName = args[0];
		 String methodName = args[1];
		 try {
		 	Class<?> clz = Class.forName(clzName);
		 	Object o = (Target) clz.newInstance();
		 	clz.getMethod(methodName, String.class).invoke(o, new String());
		 	clz.getMethod(methodName, Integer.class).invoke(o, new Integer(1));
		 } catch (Exception e) {}

		/*String clzName = "reflection.testee.Target";
		try {
			Class clz = Class.forName(clzName);
			Method mtd = clz.getMethod("foo", String.class);
			Object o = clz.newInstance();
			mtd.invoke(o, "str");
		} catch (Exception e) {} */
	}
}
