package pta.objectsensitivity;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;
import pta.utils.objects.A;
import pta.utils.objects.B;

/*
 * @testcase ObjectSensitivity3
 * @description Object sensitive alias from parameter object, uses both custom class objects and -objsens 2
 */
public class ObjectSensitivity3 {
	B id(B a){return a;}
	B id2(B a){return id(a);}
	B id3(B a){return id2(a);}
  public static void main(String[] args) {

    ObjectSensitivity3 o3a = new ObjectSensitivity3();
    ObjectSensitivity3 o3b = new ObjectSensitivity3();

    B b1 = new B();
    //Benchmark.alloc(1);
    B b2 = new B();

    A a = new A();

    B b3 = a.id(b1);
    B b4 = a.id(b2);

    B b5 = o3a.id3(b1);
    B b6 = o3b.id3(b2);

    B b7 = o3a.id3(b3);
    B b8 = o3b.id3(b4);

    mayAlias(b4,b2);
    notAlias(a,b1);
    notAlias(a,b3);
    mayAlias(b1,b3);
    //notAlias(b5,b6); //CallSite
    //notAlias(b7,b8);

  }
}
