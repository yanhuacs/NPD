package pta.objectsensitivity;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

import pta.utils.objects.A;
import pta.utils.objects.B;

/*
 * @testcase ObjectSensitivity7
 * @description Object sensitive alias from caller object, second-order calls.
 */
public class ObjectSensitivity7 {
	B obj = new B();
	B id(B a){return a;}
	B id2(B a){return id(a);}
	B id3(B a){return id2(a);}

	A getA(A a) {
		return a;
	}
	A changeA(A a) {
		a.setF(this.obj);
		return a;	
	}

  public static void main(String[] args) {
	ObjectSensitivity7 o7a = new ObjectSensitivity7();
	ObjectSensitivity7 o7b = new ObjectSensitivity7();
	B b1 = new B();
	B b2 = new B();

	A a1 = new A(b1);

	B b3 = a1.id(b1);
   	B b4 = a1.id(b2);

   	B b5 = o7a.id3(b1);
	B b6 = o7b.id3(b2);

	B b7 = o7a.id3(b3);
	B b8 = o7b.id3(b4);	
	
	B b9 = o7a.getA(a1).getF();
	B b10 = o7b.getA(a1).getF();


	mayAlias(b1,b3);
	//notAlias(b5,b6);
    	//notAlias(b7,b8);
	mayAlias(b9,b10);
	//notAlias(b7,b9);
	//notAlias(b5,b9);



	
	
  }
}
