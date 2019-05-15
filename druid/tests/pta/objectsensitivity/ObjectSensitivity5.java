package pta.objectsensitivity;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

import pta.utils.objects.A;
import pta.utils.objects.B;

/*
 * @testcase ObjectSensitivity5
 * @description Object sensitive alias from caller object
 */
public class ObjectSensitivity5 {
	B obj = new B();
	A getA(A a) {
		A temp = new A(a.getF());
		return temp;
	}
	A changeA(A a) {
		a.setF(this.obj);
		return a;	
	}

  public static void main(String[] args) {
	ObjectSensitivity5 o5a = new ObjectSensitivity5();
	ObjectSensitivity5 o5b = new ObjectSensitivity5();
	B b1 = new B();
	B b2 = new B();
	
	A a1 = new A(b1);
	A a2 = new A(b2);

	notAlias(o5a.obj,o5b.obj);
	notAlias(a1,a2);

	A a3 = o5a.changeA(a1);
	A a4 = o5b.changeA(a1);
	
	mayAlias(a3,a4);

	
	
  }
}
