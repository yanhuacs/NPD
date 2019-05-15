/* Java and Android Analysis Framework
 * Copyright (C) 2017 Jingbo Lu, Yulei Sui
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package pta;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import driver.SootUtils;
import pag.node.var.ContextLocalVar_Node;
import pta.context.ParameterizedMethod;
import soot.Body;
import soot.Context;
import soot.Local;
import soot.PointsToSet;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.NullConstant;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.util.queue.QueueReader;

/**
 * for every testcase in pta run a single test
 * @author Ammonia
 */
@RunWith(Parameterized.class)
public class PTAValidator {
	static final List<String> additionalCMD=new ArrayList<String>();;
	static final String srcPath = "tests/pta";
	static final List<String> mainOrPackList=new ArrayList<String>();
	static final List<String> mainList=new ArrayList<String>();
	
	@Parameters
	public static Collection<String> prepareData() {
		if(mainOrPackList.isEmpty())
			loadFromPath(srcPath);
		for(String mainOrPack:mainOrPackList){
			if(isPack(mainOrPack))
				loadFromPath(packToPath(mainOrPack));
			else
				mainList.add(mainOrPack);
		}
		for(String mainclass:mainList)
			System.out.println(mainclass);
		int size = mainList.size();
		String tail;
		switch (size) {
		case 0:tail=" testcase founded.";break;
		case 1:tail=" testcase founded. Now start testing...";break;
		default:tail=" testcases founded. Now start testing...";
		}
		System.out.println(size + tail);
		return mainList;
	}

	private static String packToPath(String mainOrPack) {
		return "tests/"+mainOrPack.replace('.', '/');
	}

	private static boolean isPack(String mainOrPack) {
		return Character.isLowerCase(mainOrPack.charAt(mainOrPack.lastIndexOf('.')+1));
	}

	private static void loadFromPath(String srcPath) {
		File filePath = new File(srcPath);
		for (File clazz : FileUtils.listFiles(filePath, new String[] { "java" }, true)) {
			String mainclass = SootUtils.fromFileToClass(clazz.toString().substring("tests".length() + 1));
			if(!mainclass.startsWith("pta.utils")){
				mainList.add(mainclass);
			}
		}
	}

	private String mainClassName;
	static boolean result;

	public PTAValidator(String mainClassName) {
		this.mainClassName = mainClassName;
	}

	@Test
	public void test() {
		System.out.println("Testing "+YELLOW+mainClassName+RESET+"...");
		if(!result)switchToDummyOut();
		invokePTA(mainClassName, additionalCMD.toArray(new String[0]));
		if (!result)switchToSystemOut();
		checkAlias(mainClassName.substring(mainClassName.lastIndexOf('.')+1)+".java");
		//printCGEdges("<"+mainClassName+": void main(java.lang.String[])>", 3);
		//printAppPts();
	}

	public static void main(String[] args){
		if (!System.getProperty("os.name").toLowerCase().contains("win"))
			setColor();
		if(args.length>0&&(args[0].equals("-h")||args[0].equals("-help")))
			printUsage();
		else{
			int i=0;
			while(i<args.length&&!args[i].startsWith("-")){
				mainOrPackList.add(args[i]);
				i++;
			}
			while(i<args.length){
				additionalCMD.add(args[i]);
				i++;
			}
			if(additionalCMD.contains("-result")){
				result=true;
				additionalCMD.remove("-result");
			}
			org.junit.runner.JUnitCore.main(new String[] { "pta.PTAValidator" });
		}
	}
	
	private final static void printUsage() {
		System.out.println("Usage for PTAValidator:\n"
				+ option(CYAN, "-h"+RESET+"|"+CYAN+"-help", "\t\tprint this message")
				+ option(CYAN, RESET+"|"+CYAN+"<mainclass|package>* [OPTIONs]", "run all the testcases in PTA or the testcases specified by <mainclass|package>*")
				+ "\te.g. the args \"-result\" will runtests for all testcases in package pta with result information printed.\n"
				+ "\te.g. the args \"pta.special pta.basic.Return\" will runtests for all testcases in package pta.special plus one testcase pta.basic.Return.\n"
				+ "Typical Options for runTests:\n"
				+ option(YELLOW, "-result","\tprint analysis result for each testcase")
				+ option(YELLOW, "-kobjsens <k>","use k-object-sensitive-analysis")
				+ option(YELLOW, "-originalname", "use original Java names in analysis")
				+ "and nearly all other druid options...(find them by \"python runJA -help\")\n"
				+ "Invalid options:\n"
				+ "\tthe following options are compulsory for testcases thus have already been added:\n"
				+ "\t"+option(RED, "-jre", "\t")
				+ "\t"+option(RED, "-apppath", "")
				+ "\t"+option(RED, "-singleentry", "")
				+ "\t"+option(RED, "-bunch", "")
				+ "\t(a few of others containing jni using may not be valid.)\n"
				);
		
	}
	private final static String option(String color, String option, String description){
		return "\t"+color+option+RESET+"\t"+description+".\n";
	}

	//==============================Validator===============================
	public static String RESET="",BOLD="",GREEN="",YELLOW="",CYAN="",WHITE="",RED="";
	public static void setColor() {
		RESET = "\033[0m";
		BOLD   = "\033[1m";
		GREEN = "\033[32m";
		YELLOW = "\033[33m";
		CYAN   = "\033[36m";
		WHITE  = "\033[37m";
		RED = "\033[91m";
	}
	static PTA pta;
	/// output settings
	static PrintStream dummyPrintStream;
	static PrintStream filePrintStream;
	static PrintStream systemOutStream = System.out;

	public static void switchToDummyOut() {
		dummyPrintStream = new PrintStream(new OutputStream(){
			@Override
			public void write(int b) throws IOException {}
		});
		System.setOut(dummyPrintStream);
	}

	public static void switchToSystemOut() {
		dummyPrintStream.close();
		System.setOut(systemOutStream);
	}

	public static void switchToFileOut() {
		System.setOut(filePrintStream);
	}
	
	/// invokers
	private static void invokePTA(String mainClass) {
		invokePTA(mainClass, new String[0]);
	}
	
	private static void invokePTA(String mainClass, String[] additionalArgs) {
		String[] mainArgs = new String[] { "-mainclass", // specify main class
				mainClass, // main class
		};
		additionalArgs = concat(mainArgs, additionalArgs);
		invokePTA(additionalArgs);
	}

	private static void invokePTA(String[] additionalArgs) {
		String[] ptaArgs = new String[] {
				"-jre",
				"jre/jre1.6.0_45",
//				"-libpath",
//				"aafe/android-lib",
				"-apppath",
				"testclasses",
				"-singleentry",
				"-bunch"
		};
		ptaArgs = concat(ptaArgs, additionalArgs);
		driver.Main.main(ptaArgs);
		pta =  (PTA)Scene.v().getPointsToAnalysis();
	}
	
	public static String[] concat(String[] a, String[] b) {
		String[] c = new String[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);  
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}
	
	private static void invokeExternalPTA(String... cmd){
		try {
			ProcessBuilder pbuilder = new ProcessBuilder(cmd);
			pbuilder.redirectErrorStream(true);
			Process process;
			process = pbuilder.start();
			InputStream inputStream = process.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			String line;
			while ((line = reader.readLine()) != null)
				System.out.println(line);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private static void invokeExternalPTA(List<String> cmd){
		try {
			ProcessBuilder pbuilder = new ProcessBuilder(cmd);
			pbuilder.redirectErrorStream(true);
			Process process;
			process = pbuilder.start();
			InputStream inputStream = process.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			String line;
			while ((line = reader.readLine()) != null)
				System.out.println(line);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/// checkers
	/**
	 * Are the two pointers an alias with context insensitive points-to information?
	 */
	private static boolean isMayAlias(Value value, Value value2) {
		if(value instanceof NullConstant||value2 instanceof NullConstant)
			return false;
		PointsToSet pts1 = pta.reachingObjects((Local) value);
		PointsToSet pts2 = pta.reachingObjects((Local) value2);
		return soot.jimple.toolkits.pointer.Union.hasNonEmptyIntersection(pts1, pts2);
	}
	
	private static void checkAlias(String fileName) {
		String mayAliasSig = "<pta.utils.Dummy: void mayAlias(java.lang.Object,java.lang.Object)>";
		String notAliasSig = "<pta.utils.Dummy: void notAlias(java.lang.Object,java.lang.Object)>";
		
		QueueReader<ParameterizedMethod> worklist= pta.cgb.reachables.listener();
		Set<SootMethod> reachableMethods = new LinkedHashSet<SootMethod>();
		while(worklist.hasNext()){
			ParameterizedMethod momc = worklist.next();
			SootMethod method = momc.method();
			if(reachableMethods.add(method)){
				if (method.isNative() || method.isPhantom()) continue;
				Body b = method.retrieveActiveBody();
				for (Iterator<Unit> sIt = b.getUnits().iterator(); sIt.hasNext();) {
					final Stmt s = (Stmt) sIt.next();
					if (s.containsInvokeExpr()) {
						InvokeExpr ie = s.getInvokeExpr();
						if (ie instanceof StaticInvokeExpr) {
							final String methRefSig = ie.getMethodRef().getSignature();
							if( methRefSig.equals( mayAliasSig )||methRefSig.equals( notAliasSig ) ){
								Value arg0=ie.getArg(0);
								Value arg1=ie.getArg(1);
								String args="("+arg0+", "+arg1+") "+" "+getId(arg0)+", "+getId(arg1);
								String invokeInfo=RESET+"%s"+args+" "+fileName+": "+s.getTag("LineNumberTag")+"\n";
								String seccessInfo=GREEN + "\tSUCCESS: "+invokeInfo;
								String failInfo=RED + "\tFAIL:    "+ invokeInfo;
								if( methRefSig.equals( mayAliasSig )){
									if(isMayAlias(arg0, arg1))
										System.out.printf(seccessInfo, "mayAlias");
									else
										System.out.printf(failInfo, "mayAlias");
								}else{
									if(isMayAlias(arg0, arg1))
										System.out.printf(failInfo, "notAlias");
									else
										System.out.printf(seccessInfo, "notAlias");
								}
							}
						}
					}
				}
			}
		}
	}
	
	private static String getId(Value arg) {
		if(arg instanceof NullConstant)
			return arg.toString();
		HashMap<Context, ContextLocalVar_Node> m=pta.getContextVarNodeMap().get(pta.getPag().findLocalVarNode(arg));
		if(m.size()!=1)
			throw new RuntimeException("One and Only one version of each arg in stub function is permitted!!Please check your code!");
		ContextLocalVar_Node n=m.values().iterator().next();
		return arg+"="+n.getNumber();
	}
}
