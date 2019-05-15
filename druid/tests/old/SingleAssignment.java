package old;

import static pta.utils.Dummy.call;;

/**
 * basic testcase to show the difference when using or not using "originalname"
 * @author Ammonia
 *
 */
class SingleAssignment{
	public static void main(String[] argv){
		Object a=new Object();
		a=new Object();
		call(a);
	}
}