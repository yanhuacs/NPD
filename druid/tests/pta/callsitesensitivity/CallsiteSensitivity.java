package pta.callsitesensitivity;

import static pta.utils.Dummy.notAlias;

/**
 * static call.
 * @author Ammonia
 *
 */
class CallsiteSensitivity{
	static Object id(Object a){return a;}
	public static void main(String[] argv){
		Object o1=new Object();
		Object o2=new Object();
		Object o3=id(o1);
		Object o4=id(o2);
		notAlias(o3, o4);
	}
}