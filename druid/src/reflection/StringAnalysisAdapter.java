/* Java and Android Analysis Framework
 * Copyright (C) 2017 Yifei Zhang, Tian Tan, Yue Li and Jingling Xue
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
call2ClassNamesringSetnse as published by the Free Software call2ClassNamescall2Stricall2ClassNameser
 * call2StringSet 2.1 of thecall2ClassNamese, or (at yourcall2StringSet) any later version.
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

package reflection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RunAutomaton;
import reflection.InferenceReflectionModel.GetDeclaredMtdCallInfo;
import reflection.InferenceReflectionModel.GetMtdCallInfo;
import reflection.InferenceReflectionModel.StmtWithPos;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.InvokeExpr;
import soot.toolkits.scalar.Pair;
import string.JsaWrapper;

/**
 * Take the string analysis results and use them in reflection analysis
 */
public class StringAnalysisAdapter {
	private static StringAnalysisAdapter adapter;
	private static Map<StmtWithPos, Set<String>> call2ClassNames;
	private static Map<Pair<StmtWithPos, SootClass>, Set<String>> call2MethodNames;
	private JsaWrapper jsaWrapper;
	
	private SootMethod forNameMethod;
	private Set<SootMethod> getMtdMethods;
	
	// all the class and method names in current program
	private Set<String> reachableClassNames;
	
	private StringAnalysisAdapter() {
		jsaWrapper = JsaWrapper.v();
		call2ClassNames = new HashMap<>();
		call2MethodNames = new HashMap<>();
		
		forNameMethod = Scene.v().getMethod("<java.lang.Class: java.lang.Class forName(java.lang.String)>");
		getMtdMethods = Arrays.asList(
				"<java.lang.Class: java.lang.reflect.Method getMethod(java.lang.String,java.lang.Class[])>",
				"<java.lang.Class: java.lang.reflect.Method getDeclaredMethod(java.lang.String,java.lang.Class[])>"
				).stream().map(Scene.v()::getMethod).collect(Collectors.toSet());
		
		reachableClassNames = Scene.v().getClasses().stream().map(SootClass::getName).collect(Collectors.toSet());
	}
	
	public static StringAnalysisAdapter v() {
		if(adapter == null)
			adapter = new StringAnalysisAdapter();
		return adapter;
	}
	
	/**
	 * Give a Class.forName() call, finding possible string constants of the first argument, which denotes a class name.
	 */
	public Set<String> getPossibleClassNames(StmtWithPos call) {
		Set<String> analysisResult = call2ClassNames.get(call);
		if(analysisResult != null)
			return analysisResult;
		InvokeExpr invokeExpr = call.getCaller().getInvokeExpr();
		Set<String> stringConstants = null;
		Automaton automaton = jsaWrapper.getAutomaton(invokeExpr.getArgBox(0));
		if(! automaton.isFinite())
			return new HashSet<>();
		RunAutomaton argAutomaton = new RunAutomaton(automaton);
		if(forNameMethod.equals(invokeExpr.getMethod())) {
			stringConstants = reachableClassNames.stream().filter(argAutomaton::run).collect(Collectors.toSet());
		} else {
			throw new RuntimeException("Caller does not contain refletion APIs.");
		}
		call2ClassNames.put(call, stringConstants);
		return stringConstants;
	}
	
	/**
	 * Give Class.getMethod() or Class.getDeclaredMethod() call, finding possible string constants of the first argument, 
	 * which denotes a method names.
	 */	
	public Set<String> getPossibleMethodNames(StmtWithPos call, SootClass methodClassType) {
		Pair<StmtWithPos, SootClass> callBaseClassPair = new Pair<>(call, methodClassType);
		Set<String> analysisResult = call2MethodNames.get(callBaseClassPair);
		if(analysisResult != null)
			return analysisResult;
		InvokeExpr invokeExpr = call.getCaller().getInvokeExpr();
		Set<String> stringConstants = null;
		Automaton automaton = jsaWrapper.getAutomaton(invokeExpr.getArgBox(0));
		if(! automaton.isFinite())
			return new HashSet<>();
		RunAutomaton argAutomaton = new RunAutomaton(automaton);
		if(getMtdMethods.contains(invokeExpr.getMethod())) {
			if(call instanceof GetMtdCallInfo)
				stringConstants = findMethodNamesInClassHierarchy(methodClassType).stream().filter(argAutomaton::run).collect(Collectors.toSet());
			else if(call instanceof GetDeclaredMtdCallInfo)
				stringConstants = findMethodNamesInClass(methodClassType).stream().filter(argAutomaton::run).collect(Collectors.toSet());
		} else {
			throw new RuntimeException("Caller does not contain refletion APIs.");
		}
		call2MethodNames.put(callBaseClassPair, stringConstants);
		return stringConstants;
	}

	private Set<String> findMethodNamesInClassHierarchy(SootClass base) {
		if(base.resolvingLevel() < SootClass.SIGNATURES || base.equals(GlobalVariable.v().getUnknownClass()))
			return new HashSet<>();
		Set<String> methodNames = base.getMethods().stream().map(SootMethod::getName).collect(Collectors.toSet());
		if(! base.getName().equals("java.lang.Object"))
			methodNames.addAll(findMethodNamesInClassHierarchy(base.getSuperclass()));
		for(SootClass i : base.getInterfaces())
			methodNames.addAll(findMethodNamesInClassHierarchy(i));
		return methodNames;
	}
	
	private Set<String> findMethodNamesInClass(SootClass base) {
		if(base.resolvingLevel() < SootClass.SIGNATURES)
			return new HashSet<>();
		return base.getMethods().stream().map(SootMethod::getName).collect(Collectors.toSet());
	}
}
