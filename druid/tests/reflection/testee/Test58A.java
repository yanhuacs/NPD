package reflection.testee;

public class Test58A {
	public Test58A() {}
	public Test58A(String s) {
		System.out.println("Test58A.<init>(String)");
	}
	public Test58A(Test58A o) {
		System.out.println("Test58A.<init>(Test58A)");
	}
}
