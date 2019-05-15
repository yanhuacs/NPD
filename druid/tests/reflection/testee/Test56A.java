package reflection.testee;

public class Test56A {
	public Test56A() {
		System.out.println("Test56A.<init>()");
		a();
	}
	
	public Test56A(int a, int b) {
		System.out.println("Test56A.<init>(int, int)");
		b();
	}
	
	public Test56A(String s, Object o) {
		System.out.println("Test56A.<init>(String, Object)");
		c();
	}
	
	public Test56A(Integer a, Integer b) {
		System.out.println("Test56A.<init>(Integer, Integer)");
		d();
	}
	
	public Test56A(Test56B x) {
		System.out.println("Test56A.<init>(Test56B)");
		e();
		x.foo();
	}
	
	public void a() {
		
	}
	
	public void b() {
		
	}
	
	public void c() {
		
	}
	
	public void d() {
		
	}
	
	public void e() {
		
	}
}
