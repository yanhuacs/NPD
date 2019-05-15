//test for druid pts evaluator: counts of pts

package pta.special;

public class CounterTest{
	static Object s = new Object();
	Object o = new Object();

    public static void main(String[] args){
    	Object a, b;
    	a=new Object();
    	b=new Object();
    	b=a;
    	a=b;
    	s=new CounterTest().o;
    	s=new CounterTest().o;
    }

}
