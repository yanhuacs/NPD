package old;

import static pta.utils.Dummy.mayAlias;

public class InstanceIdent {
	static InstanceIdent o;
	void foo(){
		mayAlias(this, o);
	}
	public static void main(String[] args) {
		o = new InstanceIdent();
		o.foo();
	}
}
