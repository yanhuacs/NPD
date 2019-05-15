package reflection.testee;

public class Test58B {
	public Test58B() {}
	
	public Test58B(Object o) {
		System.out.println("Test58B.<init>(Object)");
	}
	
	public Test58B(Test58B o) {
		System.out.println("Test58B.<init>(Test58B)");
	}
}
