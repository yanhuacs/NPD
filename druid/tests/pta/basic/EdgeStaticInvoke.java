package pta.basic;
import static pta.utils.Dummy.mayAlias;

/**
 * CallEdge for static invoke
 * @author Ammonia
 *
 */
public class EdgeStaticInvoke{ 
	static void foo(){Object A = new Object(); mayAlias(A,A);}
    public static void main(String[] args) throws Exception {
    	foo();
    }
}
