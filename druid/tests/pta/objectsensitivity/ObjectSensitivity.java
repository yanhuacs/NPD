package pta.objectsensitivity;

import static pta.utils.Dummy.notAlias;

/**
 * original testcase where object-sens only need a depth of 1 while callsite-sens needs depth 3.
 * @author Ammonia
 *
 */
class ObjectSensitivity{
	Object id(Object a){return a;}
	Object id2(Object a){return id(a);}
	Object id3(Object a){return id2(a);}
	public static void main(String[] argv){
		ObjectSensitivity a=new ObjectSensitivity();
		ObjectSensitivity b=new ObjectSensitivity();
		Object o1=new Object();
		Object o2=new Object();
		Object o3=a.id3(o1);
		Object o4=b.id3(o2);
		notAlias(o3, o4);
	}
}
