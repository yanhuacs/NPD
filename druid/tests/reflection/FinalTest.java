package reflection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.Test;

import pag.node.GNode;
import pta.pts.PTSetInternal;
import pta.pts.PTSetVisitor;
import reflection.JSONFormatter;
import reflection.ReflectionOptions;
import reflection.ReflectionStat;
import soot.Local;
import soot.PointsToAnalysis;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.Edge;

public class FinalTest {

	/**
	 * forName: const clz.newInstance: no cast clz.getMethod: const mtd.invoke()
	 * no argument
	 */
	@Test
	public void Test1() {
		String mainClass = "reflection.testee.Test1";
		String mainMtdName = "<reflection.testee.Test1: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: const clz.newInstance: cast super class clz.getMethod: const
	 */
	@Test
	public void Test2() {
		String mainClass = "reflection.testee.Test2";
		String mainMtdName = "<reflection.testee.Test2: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: var clz.newInstance: cast sub class clz.getMethod: const
	 */
	@Test
	public void Test3() {
		String mainClass = "reflection.testee.Test3";
		String mainMtdName = "<reflection.testee.Test3: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: var clz.newInstance: cast to concrete super class clz.getMethod:
	 * const
	 */
	@Test
	public void Test4() {
		String mainClass = "reflection.testee.Test4";
		String mainMtdName = "<reflection.testee.Test4: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: var clz.newInstance: cast to interface clz.getMethod: const
	 */
	@Test
	public void Test5() {
		String mainClass = "reflection.testee.Test5";
		String mainMtdName = "<reflection.testee.Test5: void main(java.lang.String[])>";
		List<String> classes = Arrays.asList(new String[] {
				"reflection.testee.Test5IA",
				"reflection.testee.Test5IB",
				"reflection.testee.Test5IC",
				"reflection.testee.Test5IX",
				"reflection.testee.Test5IY",
				"reflection.testee.Test5D",
				"reflection.testee.Test5B",
				"reflection.testee.Test5C",
				"reflection.testee.Test5E",
				"reflection.testee.Test5F",
				"reflection.testee.Test5G" });
		invokeSootViaCmdOpt(mainClass, classes);
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
		System.out.println("CG size: " + Scene.v().getCallGraph().size());
	}

	/**
	 * forName: const clz.newInstance: * clz.getMethod: var mtd.invoke:
	 * reflective created, no argument list
	 */
	@Test
	public void Test6() {
		String mainClass = "reflection.testee.Test6";
		String mainMtdName = "<reflection.testee.Test6: void main(java.lang.String[])>";
		List<String> classes = Arrays
				.asList(new String[] { "reflection.testee.Test6A" });
		invokeSootViaCmdOpt(mainClass, classes);
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: const, super class clz.newInstance: * clz.getMethod: const
	 * mtd.invoke: subclass object, no argument list
	 */
	@Test
	public void Test7() {
		String mainClass = "reflection.testee.Test7";
		String mainMtdName = "<reflection.testee.Test7: void main(java.lang.String[])>";

		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: const, interface clz.newInstance: * clz.getMethod: const
	 * mtd.invoke: subclass object, no argument list
	 */
	@Test
	public void Test8() {
		String mainClass = "reflection.testee.Test8";
		String mainMtdName = "<reflection.testee.Test8: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: const, super class clz.newInstance: * clz.getMethod: var
	 * mtd.invoke: subclass object, no argument list
	 */
	@Test
	public void Test9() {
		String mainClass = "reflection.testee.Test9";
		String mainMtdName = "<reflection.testee.Test9: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * This case does not pass. forName: const, super class clz.newInstance: *
	 * clz.getMethod: const, sub class method mtd.invoke: subclass object, no
	 * argument list
	 */
	@Test
	public void Test10() {
		String mainClass = "reflection.testee.Test10";
		String mainMtdName = "<reflection.testee.Test10: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: const, sub class clz.newInstance: * clz.getMethod: const, static
	 * mtd.invoke: null
	 */
	@Test
	public void Test11() {
		String mainClass = "reflection.testee.Test11";
		String mainMtdName = "<reflection.testee.Test11: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: const, super class clz.newInstance: * clz.getMethod: const,
	 * static mtd.invoke: subclass objecf
	 */
	@Test
	public void Test12() {
		String mainClass = "reflection.testee.Test12";
		String mainMtdName = "<reflection.testee.Test12: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: const clz.newInstance: * clz.getMethod: var mtd.invoke: cast,
	 * return primitive type and cast to primitive type
	 */
	@Test
	public void Test13() {
		String mainClass = "reflection.testee.Test13";
		String mainMtdName = "<reflection.testee.Test13: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: const clz.newInstance: * clz.getMethod: var mtd.invoke: cast,
	 * return primitive type and cast to wrapper type
	 */
	@Test
	public void Test14() {
		String mainClass = "reflection.testee.Test14";
		String mainMtdName = "<reflection.testee.Test14: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: const clz.newInstance: * clz.getMethod: var mtd.invoke: cast,
	 * return primitive type and cast to wider primitive type
	 */
	@Test
	public void Test15() {
		String mainClass = "reflection.testee.Test15";
		String mainMtdName = "<reflection.testee.Test15: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: const clz.newInstance: * clz.getMethod: var mtd.invoke: cast,
	 * return wrapper type and cast to primitive type
	 */
	@Test
	public void Test16() {
		String mainClass = "reflection.testee.Test16";
		String mainMtdName = "<reflection.testee.Test16: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: const clz.newInstance: * clz.getMethod: var mtd.invoke: cast,
	 * reference type
	 */
	@Test
	public void Test17() {
		String mainClass = "reflection.testee.Test17";
		String mainMtdName = "<reflection.testee.Test17: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: const clz.newInstance: * clz.getMethod: var mtd.invoke: cast,
	 * super type
	 */
	@Test
	public void Test18() {
		String mainClass = "reflection.testee.Test18";
		String mainMtdName = "<reflection.testee.Test18: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: multiple forName clz.newInstance: * clz.getMethod: const static
	 * method mtd.invoke:
	 */
	@Test
	public void Test19() {
		String mainClass = "reflection.testee.Test19";
		String mainMtdName = "<reflection.testee.Test19: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: multiple forName clz.newInstance: corresponding objects
	 * clz.getMethod: const, instance method mtd.invoke:
	 */
	@Test
	public void Test20() {
		String mainClass = "reflection.testee.Test20";
		String mainMtdName = "<reflection.testee.Test20: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: const clz.newInstance: corresponding objects clz.getMethod:
	 * const, instance method mtd.invoke: multi invoke
	 */
	@Test
	public void Test21() {
		String mainClass = "reflection.testee.Test21";
		String mainMtdName = "<reflection.testee.Test21: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: const clz.newInstance: corresponding objects clz.getMethod: var,
	 * instance method mtd.invoke: cast array type
	 */
	@Test
	public void Test22() {
		String mainClass = "reflection.testee.Test22";
		String mainMtdName = "<reflection.testee.Test22: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: const clz.newInstance: * clz.getMethod: var, static method
	 * mtd.invoke: multi invoke
	 */
	@Test
	public void Test23() {
		String mainClass = "reflection.testee.Test23";
		String mainMtdName = "<reflection.testee.Test23: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: const clz.newInstance: * clz.getMethod: var, static method
	 * mtd.invoke: test argument matching, wrapper type to its primitive type
	 */
	@Test
	public void Test24() {
		String mainClass = "reflection.testee.Test24";
		String mainMtdName = "<reflection.testee.Test24: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: const clz.newInstance: * clz.getMethod: var, static method
	 * mtd.invoke: test argument matching, wrapper type to wider primitive type
	 */
	@Test
	public void Test25() {
		String mainClass = "reflection.testee.Test25";
		String mainMtdName = "<reflection.testee.Test25: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * forName: const clz.newInstance: * clz.getMethod: var, static method
	 * mtd.invoke: test argument matching, sub type to super type
	 */
	@Test
	public void Test26() {
		String mainClass = "reflection.testee.Test26";
		String mainMtdName = "<reflection.testee.Test26: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * Test multiple array definition in a method
	 */
	@Test
	public void Test27() {
		String mainClass = "reflection.testee.Test27";
		String mainMtdName = "<reflection.testee.Test27: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * Test multiple array definition in a method, considering class hierarchy
	 */
	@Test
	public void Test28() {
		String mainClass = "reflection.testee.Test28";
		String mainMtdName = "<reflection.testee.Test28: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * Test uninitialized argument list matching
	 */
	@Test
	public void Test29() {
		String mainClass = "reflection.testee.Test29";
		String mainMtdName = "<reflection.testee.Test29: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * Test argumenet passing
	 */
	@Test
	public void Test30() {
		String mainClass = "reflection.testee.Test30";
		String mainMtdName = "<reflection.testee.Test30: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * Test return value of reflective calls
	 */
	@Test
	public void Test31() {
		String mainClass = "reflection.testee.Test31";
		String mainMtdName = "<reflection.testee.Test31: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * Test return value of reflective calls, primitive type
	 */
	@Test
	public void Test32() {
		String mainClass = "reflection.testee.Test32";
		String mainMtdName = "<reflection.testee.Test32: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * Test getters and setters
	 */
	@Test
	public void Test33() {
		String mainClass = "reflection.testee.Test33";
		String mainMtdName = "<reflection.testee.Test33: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * Test StringBuffer as mehtod name source
	 */
	@Test
	public void Test34() {
		String mainClass = "reflection.testee.Test34";
		String mainMtdName = "<reflection.testee.Test34: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * Test <clinit>
	 */
	@Test
	public void Test35() {
		String mainClass = "reflection.testee.Test35";
		String mainMtdName = "<reflection.testee.Test35: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * Test null arg list
	 */
	@Test
	public void Test36() {
		String mainClass = "reflection.testee.Test36";
		String mainMtdName = "<reflection.testee.Test36: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * Test <init> of super class
	 */
	@Test
	public void Test37() {
		String mainClass = "reflection.testee.Test37";
		String mainMtdName = "<reflection.testee.Test37: void main(java.lang.String[])>";
		String initName = "<reflection.testee.Test37: void <init>()>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		SootMethod init = Scene.v().getMethod(initName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges(init);
		queryCGEdges("foo");
	}

	/**
	 * Test uninitialized arguments
	 */
	@Test
	public void Test38() {
		String mainClass = "reflection.testee.Test38";
		String mainMtdName = "<reflection.testee.Test38: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * Test getDeclaredMethod class is knwon instance method method name:
	 * constant string
	 */
	@Test
	public void Test39() {
		String mainClass = "reflection.testee.Test39";
		String mainMtdName = "<reflection.testee.Test39: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * Test getDeclaredMethod class is known static method method name: constant
	 * string
	 */
	@Test
	public void Test40() {
		String mainClass = "reflection.testee.Test40";
		String mainMtdName = "<reflection.testee.Test40: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * Test getDeclaredMethod instance method class is known method name:
	 * unknown
	 */
	@Test
	public void Test41() {
		String mainClass = "reflection.testee.Test41";
		String mainMtdName = "<reflection.testee.Test41: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * Test getDeclaredMethod instance method class is unknown method name:
	 * unknown
	 */
	@Test
	public void Test42() {
		String mainClass = "reflection.testee.Test42";
		String mainMtdName = "<reflection.testee.Test42: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * Test null Class meta object
	 */
	@Test
	public void Test43() {
		String mainClass = "reflection.testee.Test43";
		String mainMtdName = "<reflection.testee.Test43: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * Test .class
	 */
	@Test
	public void Test44() {
		String mainClass = "reflection.testee.Test44";
		String mainMtdName = "<reflection.testee.Test44: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(
				mainClass,
				Arrays.asList("reflection.testee.Test44A"));
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * Test .class
	 */
	@Test
	public void Test45() {
		String mainClass = "reflection.testee.Test45";
		String mainMtdName = "<reflection.testee.Test45: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * Test trace based reflection model
	 */
	@Test
	public void Test46() {
		String mainClass = "reflection.testee.Test46";
		String mainMtdName = "<reflection.testee.Test46: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		// queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * Test constant string only reflection resolution
	 */
	@Test
	public void Test47() {
		String mainClass = "reflection.testee.Test47";
		String mainMtdName = "<reflection.testee.Test47: void main(java.lang.String[])>";
		// ReflectionOptions.v().setConstStringOnly(true);
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}

	/**
	 * Test inference of static method
	 */
	@Test
	public void Test48() {
		String mainClass = "reflection.testee.Test48";
		String mainMtdName = "<reflection.testee.Test48: void main(java.lang.String[])>";
		// ReflectionOptions.v().setConstStringOnly(true);
		invokeSootViaCmdOpt(mainClass, new ArrayList<>());
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges("main");
		queryCGEdges("foo");
	}
	
	
	@Test
	public void Test49() {
		String mainClass = "reflection.testee.Test49";
		String mainMtdName = "<reflection.testee.Test49: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, new ArrayList<String>());
	}
	
	/**
	 * Test getMethods()
	 */
	@Test 
	public void Test50() {
		String mainClass = "reflection.testee.Test50";
		String mainMtdName = "<reflection.testee.Test50: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, Arrays.asList("reflection.testee.Test50A"));
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges(mainMtd);
	}
	

	/**
	 * Test getDeclaredMethods()
	 */
	@Test 
	public void Test51() {
		String mainClass = "reflection.testee.Test51";
		String mainMtdName = "<reflection.testee.Test51: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, Arrays.asList("reflection.testee.Test51A"));
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges(mainMtd);
	}
	
	/**
	 * Test Class.getConstroctor()
	 */
	@Test
	public void Test52() {
		String mainClass = "reflection.testee.Test52";
		String mainMtdName = "<reflection.testee.Test52: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, Arrays.asList("reflection.testee.Test52A"));
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges(mainMtd);
	}
	
	/**
	 * Test Class.getDeclaredConstroctor()
	 */
	@Test
	public void Test53() {
		String mainClass = "reflection.testee.Test53";
		String mainMtdName = "<reflection.testee.Test53: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, Arrays.asList("reflection.testee.Test53A"));
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges(mainMtd);
	}
	
	/**
	 * Test Class.getConstroctors()
	 */
	@Test
	public void Test54() {
		String mainClass = "reflection.testee.Test54";
		String mainMtdName = "<reflection.testee.Test54: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, Arrays.asList("reflection.testee.Test54A"));
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges(mainMtd);
	}
	
	/**
	 * Test Class.getDeclaredConstroctors()
	 */
	@Test
	public void Test55() {
		String mainClass = "reflection.testee.Test55";
		String mainMtdName = "<reflection.testee.Test55: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, Arrays.asList("reflection.testee.Test55A"));
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges(mainMtd);
	}
	
	/**
	 * Test Constructor.newInstance()
	 */
	@Test
	public void Test56() {
		long begin = System.nanoTime();
		String mainClass = "reflection.testee.Test56";
		String mainMtdName = "<reflection.testee.Test56: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, Arrays.asList("reflection.testee.Test56A", 
													"reflection.testee.Test56B"));
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges(mainMtd);
		for(SootMethod mtd : Scene.v().getSootClass("reflection.testee.Test56A").getMethods())
			if(mtd.getName().equals("<init>"))
				queryCGEdges(mtd);
		for(SootMethod mtd : Scene.v().getSootClass("reflection.testee.Test56B").getMethods())
			if(mtd.getName().equals("foo"))
				queryCGEdges(mtd);
		
		
		ReflectionStat.v().showInferenceReflectionStat();
		JSONFormatter.v().format();
		System.out.println("Analysis has run for " + (System.nanoTime() - begin) / 1E9 + " seconds");
		/*
		SootMethod target = Scene.v().getMethod("<java.security.Provider$Service: java.lang.Object newInstanceGeneric(java.lang.Object)>");
		System.out.println(target.retrieveActiveBody());
		for(Unit u : target.retrieveActiveBody().getUnits()) {
			if(u.toString().equals("r18 = virtualinvoke r2.<java.lang.Class: java.lang.Object newInstance()>()")) {
				Local l = (Local) ((VirtualInvokeExpr) ((Stmt) u).getInvokeExpr()).getBase();
				queryPTS(l);
			}
				
		}
		*/
		
		/*
		SootMethod getImplClass = Scene.v().getMethod("<java.security.Provider$Service: java.lang.Class getImplClass()>");
		for(Unit u : getImplClass.retrieveActiveBody().getUnits())
			if(u.toString().equals("r23 = staticinvoke <java.lang.Class: java.lang.Class forName(java.lang.String)>($r7)")) {
				Local l = (Local) ((StaticInvokeExpr) ((Stmt) u).getInvokeExpr()).getArg(0);
				queryPTS(l);
			}
		*/
		/*
		SootMethod newInstanceGeneric = Scene.v().getMethod("<java.security.Provider$Service: java.lang.Object newInstanceGeneric(java.lang.Object)>");
		System.out.println(newInstanceGeneric.retrieveActiveBody());
		queryPTSOfVarInMtd(newInstanceGeneric);
		*/
	}
	
	@Test
	public void Test58() {
		String mainClass = "reflection.testee.Test58";
		String mainMtdName = "<reflection.testee.Test58: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, Arrays.asList("reflection.testee.Test58A"));
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges(mainMtd);
		
	}
	
	/**
	 * Test multiple forName calls
	 */
	@Test
	public void Test59() {
		String mainClass = "reflection.testee.Test59";
		String mainMtdName = "<reflection.testee.Test59: void main(java.lang.String[])>";
		invokeSootViaCmdOpt(mainClass, Arrays.asList("reflection.testee.Test59A"));
		SootMethod mainMtd = Scene.v().getMethod(mainMtdName);
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges(mainMtd);
	}
	
	@Test
	public void Test60() {
		String mainClass = "reflection.testee.Test60";
		invokeSootViaCmdOpt(mainClass, new ArrayList<String>());
		SootMethod mainMtd = Scene.v().getMainMethod();
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges(mainMtd);
		System.out.println(mainMtd.retrieveActiveBody());
	}
	
	// Test lazy heap modeling Class.newInstance() lazy heap modeling
	@Test
	public void Test61() {
		String mainClass = "reflection.testee.Test61";
		invokeSootViaCmdOpt(mainClass, Arrays.asList("reflection.testee.Test61B"));
		SootMethod mainMtd = Scene.v().getMainMethod();
		System.out.println(mainMtd.retrieveActiveBody());
		queryPTSOfVarInMtd(mainMtd);
		queryCGEdges(mainMtd);
		SootMethod fooMtd = Scene.v().getMethod("<reflection.testee.Test61: java.lang.Object foo(java.lang.String)>");
		queryPTSOfVarInMtd(fooMtd);
		System.out.println(fooMtd.retrieveActiveBody());
	}

	// Test lazy heap modeling Method.invoke()
	@Test
	public void Test63() {
		String mainClass = "reflection.testee.Test63";
		invokeSootViaCmdOpt(mainClass, Arrays.asList("reflection.testee.Test63A",
				"reflection.testee.Test63B"));
	}

	@Test
	public void TestHuawei() {
		String mainClass = "reflection.testee.Test61";
		
		String javaLibs = "C:\\Java\\jdk1.6.0_45\\jre\\lib\\rt.jar;C:\\Java\\jdk1.6.0_45\\jre\\lib\\jce.jar;C:\\Java\\jdk1.6.0_45\\jre\\lib\\jsse.jar";
		String[] sootArgs = new String[] { "-cp", // soot class path
				"C:\\Users\\yifei\\git\\soot\\testclasses" + ";" + javaLibs, //
				"-app", // application mode ?
				"-w", // whole program mode
				"-keep-line-number", "-no-writeout-body-releasing", "-p", // phase
				"cg.spark", // generate call graph
				"enabled:true,string-constants:true",
				// "-p",
				// "cg",
				// "safe-newinstance:true",
				// + ",verbose:false",
				// "-p",
				// "jb",
				// "enabled:true",
				// "-p",
				// "wjtp",
				// "enabled:true",
				"-allow-phantom-refs", // allow phantom class
				"-src-prec", "c", "-f", // output format
				"J", // none
				"-inf-refl",
				"-main-class", // specify main class
				mainClass, // main class
				mainClass, // argument class
		};
		soot.G.reset();
		// loadNecessaryClasses(Arrays.asList("reflection.testee.Test61"));
		// ReflectionOptions.v().setInferenceReflectionModel(true);
		soot.Main.main(sootArgs);
		
		SootMethod mainMtd = Scene.v().getMainMethod();
		queryPTSOfVarInMtd(mainMtd);
	}
	
	// Test Antlr
	@Test
	public void Test57() {
		long begin = System.nanoTime();
		
		// configure SOOT
		String javaLibs = "C:\\Java\\jdk1.6.0_45\\jre\\lib\\rt.jar;C:\\Java\\jdk1.6.0_45\\jre\\lib\\jce0.jar;C:\\Java\\jdk1.6.0_45\\jre\\lib\\jsse.jar";
		String[] sootArgs = new String[] { "-cp", // soot class path
				"C:\\Users\\yifei\\Desktop\\Research\\CODASPY17\\antlr\\antlr-2.7.2.jar" + ";" + javaLibs, //
				"-app", // application mode ?
				"-w", // whole program mode
				"-keep-line-number", "-no-writeout-body-releasing", "-p", // phase
				"cg.spark", // generate call graph
				"enabled:true,string-constants:true",
				"-allow-phantom-refs", // allow phantom class
				"-src-prec", "c", "-f", // output format
				"n", // none
				"-main-class", // specify main class
				"antlr.Tool", // main class
				"antlr.Tool", // argument class
		};
		soot.G.reset();
		ReflectionOptions.v().setInferenceReflectionModel(true);
		soot.Main.main(sootArgs);
		System.out.println("CG: " + Scene.v().getCallGraph().size());
		ReflectionStat.v().showInferenceReflectionStat();
		System.out.println("Analysis has run for " + (System.nanoTime() - begin) / 1E9 + " seconds");
		for(Edge e : Scene.v().getCallGraph()) {
			
		}
	}
	
	@Test
	public void Test62() {
		String mainClass = "reflection.testee.Test62";
		invokeSootViaCmdOpt(mainClass, Arrays.asList("reflection.testee.Test62A", 
				"reflection.testee.Test62B"));
		SootMethod mainMtd = Scene.v().getMainMethod();
		queryPTSOfVarInMtd(mainMtd);
		// queryCGEdges(mainMtd);
		System.out.println(mainMtd.retrieveActiveBody());
	}
	
	public void queryPTSOfVarInMtd(SootMethod mtd) {
		for (Unit u : mtd.retrieveActiveBody().getUnits()) {
			if (u instanceof AssignStmt) {
				AssignStmt assign = (AssignStmt) u;
				if (assign.getRightOp() instanceof StaticInvokeExpr) {
					System.out.println("# Stmt " + assign);
					queryPTS((Local) assign.getLeftOp());
				} else if (assign.getRightOp() instanceof VirtualInvokeExpr) {
					System.out.println("# stmt " + assign);
					VirtualInvokeExpr vInvokeExpr = (VirtualInvokeExpr) assign
							.getRightOp();
					Local base = (Local) vInvokeExpr.getBase();
					System.out.println("# Base var " + base);
					queryPTS(base);
					Local left = (Local) assign.getLeftOp();
					System.out.println("# Left var " + left);
					queryPTS(left);
				} else {
					Value left = assign.getLeftOp();
					if(left instanceof Local) {
						System.out.println("# Assignment " + assign);
						System.out.println("# left op " + left);
						queryPTS((Local) left);
					}
				}
			}
			if (u instanceof InvokeStmt) {
				System.out.println("# stmt " + u);
				InvokeStmt invokeStmt = (InvokeStmt) u;
				InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
				if (invokeExpr instanceof VirtualInvokeExpr) {
					VirtualInvokeExpr vInvokeExpr = (VirtualInvokeExpr) invokeExpr;
					Local base = (Local) vInvokeExpr.getBase();
					System.out.println("# base var " + base);
					queryPTS(base);
				}
			}
		}
	}

	public void queryPTS(Local l) {
		System.out.println("# qurey PTS of " + l);
		PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
		System.out.println(pta.reachingObjects(l).getClass().getName());
		PTSetInternal pts = (PTSetInternal) pta.reachingObjects(l);
		System.out.println("# Var " + l.toString() + " PTS size " + pts.size());
		pts.forall(new PTSetVisitor() {

			@Override
			public void visit(GNode n) {
				System.out.println("# Node type: " + n.getClass().getName());
				System.out.println(n);
			}
		});
		System.out.println();
	}

	public void queryCGEdges(String mtdName) {
		System.out.println("# call edges of method " + mtdName);
		StreamSupport.stream(Scene.v().getCallGraph().spliterator(), false)
				.filter(e -> e.src().getName().equals(mtdName))
				.map(Edge::toString).sorted().forEach(System.out::println);
	}

	public void queryCGEdges(SootMethod mtd) {
		System.out.println("# call edges of method " + mtd.getName());
		StreamSupport.stream(Scene.v().getCallGraph().spliterator(), false)
				.filter(e -> e.src().equals(mtd)).map(Edge::toString).sorted()
				.forEach(System.out::println);
	}

	public void loadNecessaryClasses(List<String> classes) {
		for (String clz : classes) {
			Scene.v().addBasicClass(clz, SootClass.BODIES);
		}
	}

	public void invokeSootViaCmdOpt(String mainClass, List<String> classes) {
		String javaLibs = "C:\\Program Files\\Java\\jre1.6.0.45\\rt.jar;C:\\Program Files\\Java\\jre1.6.0.45\\jce.jar;C:\\Program Files\\Java\\jre1.6.0.45\\jsse.jar";
		String[] sootArgs = new String[] { "-cp", // soot class path
				"D:\\workspace\\cg_android-analysis\\testclasses" + ";" + javaLibs, //
				"-app", // application mode ?
				"-w", // whole program mode
				"-keep-line-number", "-no-writeout-body-releasing", "-p", // phase
				"cg.spark", // generate call graph
				"enabled:true,string-constants:true",
				"-allow-phantom-refs", // allow phantom class
				"-src-prec", "c", "-f", // output format
				"J", // none
				"-inf-refl",
				"-main-class", // specify main class
				mainClass, // main class
				mainClass, // argument class
		};
		String[] ptaArgs = new String[] {
				"-reflection",
				"-lhm",
				"-jre",
				"jre/jre1.6.0_45",
				"-apppath", // app path
				"testclasses",//app path
				"-kobjsens",//specify k
				"0",
				"-mainclass", // specify main class
				mainClass, // main class
		};
		//soot.G.reset();
		//loadNecessaryClasses(classes);
		driver.Main.main(ptaArgs);
		ReflectionStat.v().showInferenceReflectionStat();
		System.out.println("#### CG size: " + Scene.v().getCallGraph().size());
	}
	
	public static void main(String[] args) {
		FinalTest finalTest = new FinalTest();
		finalTest.Test1();
	}
}
