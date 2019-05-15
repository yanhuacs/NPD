package reflection.testee;

public class Test49 {
	public static void main(String[] args) {
		Test49A a = new Test49B();
		Test49A b = new Test49C();
		Test49A c = new Test49D();
		System.out.println(a.toString() + b.toString() + c.toString());
	}
}

