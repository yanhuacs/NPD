package pta.objectsensitivity;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

import pta.utils.objects.A;
import pta.utils.objects.B;

/*
 * @testcase ObjectSensitivity6
 * @description Object sensitive alias from caller object, second-order calls.
 */
public class ObjectSensitivity6 {
	B obj = new B();
	B id(B a){return a;}
	B id2(B a){return id(a);}
	B id3(B a){return id2(a);}

	A getA(A a) {
		A temp = new A(a.getF());
		return temp;
	}
	A changeA(A a) {
		a.setF(this.obj);
		return a;	
	}

  public static void main(String[] args) {
	ObjectSensitivity6 o6a = new ObjectSensitivity6();
	ObjectSensitivity6 o6b = new ObjectSensitivity6();
	B b1 = new B();
	B b2 = new B();

	A a1 = new A(b1);

	B b3 = a1.id(b1);
   	B b4 = a1.id(b2);

   	B b5 = o6a.id3(b1);
	B b6 = o6b.id3(b2);

	B b7 = o6a.id3(b3);
	B b8 = o6b.id3(b4);	

	
	
	B b9 = o6a.getA(a1).getF();
	B b10 = o6b.getA(a1).getF();


	mayAlias(b1,b3);
	notAlias(b1,b2);
	//notAlias(b5,b6);
    	//notAlias(b7,b8);
	mayAlias(b9,b10);
	//notAlias(b7,b9);
	//notAlias(b5,b9);


	
	
  }
}
