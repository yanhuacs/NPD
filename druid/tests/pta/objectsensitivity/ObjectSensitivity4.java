package pta.objectsensitivity;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

import pta.utils.objects.A;
import pta.utils.objects.B;

/*
 * @testcase ObjectSensitivity4
 * @description Object sensitive alias from caller object
 */
public class ObjectSensitivity4 {
	B obj = new B();
	A getA(A a) {
		A temp = new A(a.getF());
		return temp;
	}

  public static void main(String[] args) {
	ObjectSensitivity4 o4a = new ObjectSensitivity4();
	ObjectSensitivity4 o4b = new ObjectSensitivity4();
	B b1 = new B();
	B b2 = new B();
	
	A a1 = new A(b1);
	A a2 = new A(b2);

	notAlias(o4a.obj,o4b.obj);
	notAlias(a1,a2);

	A a3 = o4a.getA(a1);
	A a4 = o4b.getA(a1);
	A a5 = o4a.getA(a2);
	A a6 = o4b.getA(a2);
	
	notAlias(a3,a4);
	//notAlias(a3,a5); CallSite
	notAlias(a3,a6);


	
	
  }
}
