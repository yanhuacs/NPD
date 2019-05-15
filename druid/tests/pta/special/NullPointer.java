//pag process for null pointer
//link: MethodPAG.addInternalEdge(src, dst)

package pta.special;

public class NullPointer{ 

    public static void main(String[] args) throws Exception {
    	Object a = getNull();
    	Object b = a;
    }
    static Object getNull(){
    	
    	return null;
    }
}
