package reflection.testee;

public class Test62 {
	public String[] list;
	public Test62() {
		list = new String[2];
		list[0] = "reflection.testee.Test62A";
	}
	public static void main(String[] args) {
		Test62 t = new Test62();
		try {
			Class c = Class.forName(t.list[0]);
			Object obj = c.newInstance();
			System.out.println(obj.toString());
			t.foo();
		} catch(Exception e) {}
	}
	
	public void foo() {
		list[1] = "reflection.testee.Test62B";
	}
}