package pta.objectsensitivity;

import static pta.utils.Dummy.notAlias;

/**
 * a testcase where object-sens need a depth of 2.
 * @author Ammonia
 *
 */
class ObjectSensitivity2{
	class I{Object id(Object a){return a;}}
	Object id2(Object a){
		I i=new I();
		return i.id(a);
	}
	public static void main(String[] argv){
		ObjectSensitivity2 a=new ObjectSensitivity2();
		ObjectSensitivity2 b=new ObjectSensitivity2();
		Object o1=new Object();
		Object o2=new Object();
		Object o3=a.id2(o1);
		Object o4=b.id2(o2);
		notAlias(o3, o4);
	}
}
