package pta.callsitesensitivity;

import static pta.utils.Dummy.notAlias;

/**
 * virtual call.
 * @author Ammonia
 *
 */
class CallsiteSensitivity2{
	Object id(Object a){return a;}
	public static void main(String[] argv){
		CallsiteSensitivity2 c=new CallsiteSensitivity2();
		Object o1=new Object();
		Object o2=new Object();
		Object o3=c.id(o1);
		Object o4=c.id(o2);
		notAlias(o3, o4);
	}
}
