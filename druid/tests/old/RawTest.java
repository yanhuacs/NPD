package old;

import static pta.utils.Dummy.call;
/**
 * Same Parameterized Mtd invoked with different arguments
 * @author Ammonia
 *
 */
class RawTest{
	Object a;
	
	public static void main(String[] argv){
		new RawTest().run();
	}
	private void run() {
		Object a=new Object();
		Object b=this.a;
		this.a=a;
		
		call(b);
	}
}