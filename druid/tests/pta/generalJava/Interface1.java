package pta.generalJava;

import static pta.utils.Dummy.mayAlias;
import static pta.utils.Dummy.notAlias;

import pta.utils.objects.A;
import pta.utils.objects.G;
import pta.utils.objects.H;

/*
 * @testcase Interface1
 * @description Alias from method in interface
 */
public class Interface1 {

  public static void main(String[] args) {

    A a = new A();
    //Benchmark.alloc(1);
    A b = new A();

    G g = new G();
    H h = new H();
    g.foo(a);
    A c = h.foo(b);
    mayAlias(c,b);
    notAlias(a,g);
    notAlias(a,h);
    notAlias(g,h);
    /*Benchmark.test("c",
        "{allocId:1, mayAlias:[c,b], notMayAlias:[a,g,h], mustAlias:[c,b], notMustAlias:[a,g,h]}");

    Benchmark.use(c);*/
  }

}
