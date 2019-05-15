/* Java and Android Analysis Framework
 * Copyright (C) 2017 Yifei Zhang, Tian Tan, Yue Li and Jingling Xue
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
package reflection.android;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import pag.WholeProgPAG;
import pag.node.GNode;
import pag.node.alloc.Alloc_Node;
import pag.node.var.LocalVar_Node;
import pta.PTA;
import pta.pts.EmptyPTSet;
import pta.pts.PTSetVisitor;
import reflection.GlobalVariable;
import reflection.InferenceReflectionModel;
import reflection.Util;
import reflection.InferenceReflectionModel.ArgList;
import reflection.InferenceReflectionModel.InvokeCallInfo;
import soot.FastHierarchy;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;

public class ReflectionLibraryModeling {
	/**
	 * Model the return value of invoking target methods of reflective calls
	 * 
	 * Only reachable methods are considered. The methods which are inferred are not considered
	 * Firstly, all the reachable methods are matched with intra-procedural reachable argument lists
	 * Next, the bodies of reachable methods are explored to see the points-to set of return values.
	 * If the points-to sets of all the return values in the target method are empty, then 
	 * the return type need to be modeled.
	 * The left-hand side variable of a reflective call is not considered because it may be used 
	 * multiple times by the caller.
	 */
	public static List<LocalVar_Node> modelInvokeReturnValue() {
		List<LocalVar_Node> newNodes = new ArrayList<>();
		InferenceReflectionModel infer = GlobalVariable.v().getInferenceReflectionModel();
		WholeProgPAG pag = ((PTA)Scene.v().getPointsToAnalysis()).getPag();
		for(Iterator<Set<InvokeCallInfo>> i = infer.getMtd2InvokeCalls().iterator(); i.hasNext(); )
			for(InvokeCallInfo invokeCall : i.next()) {
				Stmt caller = invokeCall.getCaller();
				SootMethod source = invokeCall.getSource();
				if(caller instanceof AssignStmt) {
					Set<SootMethod> matchingMtds = findMatchingMethods(invokeCall);
					Set<RefType> retTypes = getRetRefType(matchingMtds);
					Local retLocal = (Local)((AssignStmt) caller).getLeftOp();
					LocalVar_Node retVarNode = pag.findLocalVarNode(retLocal);
					if(retVarNode == null)
						continue;
					for(RefType retType : retTypes) {
						Set<RefType> possibleTypes = 
								Stream.concat(Util.v().getSubTypes(retType).stream(), Stream.of(retType))
									  .map(RefType::getSootClass)
									  .filter(SootClass::isConcrete)
									  .map(SootClass::getType)
									  .collect(Collectors.toSet());
						
						for(RefType possibleType : possibleTypes) {
							// Keep only one object of each type to keep soundness
							Alloc_Node allocNode = MockAllocNodes.v().get(possibleType);
							if(allocNode == null) {
								allocNode = pag.makeAllocNode(new MockAllocSite(possibleType), possibleType, source);
								MockAllocNodes.v().put(possibleType, allocNode);
							}
							// find or make points to set of return value
							
//							if(retVarNode == null) {
//								retVarNode = pag.makeLocalVarNode(retLocal, retLocal.getType(), source);
//								retVarNode.makeP2Set();
//							}
							// add the allocation node to the points to set of return value
							retVarNode.getP2Set().add(allocNode);
							pag.addAllocEdge(allocNode, retVarNode);
							newNodes.add(retVarNode);
							System.out.println("[InferenceReflectionModel] Modeling return value Add an object with type " + possibleType + 
									" to the points to set of " + retLocal + " in " + caller + ".");
							System.out.println("The reachable methods of this call site are: ");
							for(SootMethod mtd : matchingMtds) {
								System.out.println("\t # " + mtd.getSignature());
							}
						}
					}
				}
			}
		return newNodes;
	}
	
	/**
	 * find the reachable methods that are matched with intra-procedural reachable argument lists
	 */
	private static Set<SootMethod> findMatchingMethods(InvokeCallInfo invokeCall) {
		Set<SootMethod> matchingMtds = new HashSet<>();
		InferenceReflectionModel infer = GlobalVariable.v().getInferenceReflectionModel();
		for(SootMethod targetMtd : invokeCall.getTargetMtds()) {
			List<ArgList> arglists = invokeCall.getArgLists();
			boolean isMatch = false;
			for(ArgList argList : arglists)
				if(!infer.matching(targetMtd, argList).isEmpty()) {
					isMatch = true;
					break;
				}
			if(isMatch)
				matchingMtds.add(targetMtd);
		}
		return matchingMtds;
	}
	
	// if the return value (reference type) of a method is empty, then the type of its return type is recorded
	// we goes into the body of target methods to see whether the methods return a value, rather than
	// see the points-to set of left-hand side variable of reflective calls, because the this variable may be
	// used several times in caller
	private static Set<RefType> getRetRefType(Set<SootMethod> mtds) {
		Set<RefType> retTypes = new HashSet<>();
		WholeProgPAG pag = ((PTA)Scene.v().getPointsToAnalysis()).getPag();
		for(SootMethod mtd : mtds) {
			// only reference type of Android framework
			if(! (mtd.getReturnType() instanceof RefType))
				continue;
			else if(! ((RefType) mtd.getReturnType()).getClassName().startsWith("android"))
				continue;
			RefType retType = (RefType) mtd.getReturnType();
			// if the method is enclosed into a class that is not loaded to BODY level
			// the return type is recorded directly
			if(mtd.getDeclaringClass().resolvingLevel() != SootClass.BODIES) {
				retTypes.add(retType);
				continue;
			}
			List<ReturnStmt> retStmts = mtd.retrieveActiveBody()
										   .getUnits()
										   .stream()
										   .filter(u -> u instanceof ReturnStmt)
										   .map(u -> (ReturnStmt) u)
										   .collect(Collectors.toList());
			// if the method does not contain return statement but it return type is reference type,
			// this is possible, like a method throws a runtime exception and do nothing, then
			// the return type is recorded
			if(retStmts.isEmpty()) {
				retTypes.add(retType);
				continue;
			}
			boolean isRetEmpty = true;
			for(ReturnStmt retStmt : retStmts) {
				Value retOp = retStmt.getOp();
				if(retOp instanceof Local) {
					LocalVar_Node varNode = pag.findLocalVarNode(retOp);
					if(! varNode.getP2Set().isEmpty())
						isRetEmpty = false;
				}
			}
			// if the points-to set of all the return values of a method is empty
			// then its return type is recorded
			if(isRetEmpty)
				retTypes.add((RefType) mtd.getReturnType());
		}
		return retTypes;
	} 
	
	/**
	 * Model the allocation site of receiver object of reflective calls whose target is method
	 * is in Android framework
	 * 
	 * If a method is invoked via reflection and its receiver object is null because of the missing
	 * library code, we model the allocation site of receive object, which includes the concrete 
	 * declaring type of target method and its concrete subtypes
	 */
	public static List<LocalVar_Node> modelInvokeReceiverObject() {
		List<LocalVar_Node> varNodes = new ArrayList<>();
		InferenceReflectionModel infer = GlobalVariable.v().getInferenceReflectionModel();
		WholeProgPAG pag = ((PTA)Scene.v().getPointsToAnalysis()).getPag();
		for(Iterator<Set<InvokeCallInfo>> i = infer.getMtd2InvokeCalls().iterator(); i.hasNext(); ) {
			for(InvokeCallInfo invokeCall : i.next()) {
				SootMethod source = invokeCall.getSource();
				Stmt caller = invokeCall.getCaller();
				Value receiver = caller.getInvokeExpr().getArg(0);
				// ignore static call
				if(receiver instanceof NullConstant)
					continue;
				Local receiverLocal = (Local) receiver;
				LocalVar_Node receiverVarNode = pag.findLocalVarNode(receiverLocal);
				if(receiverVarNode == null)
					continue;
				// collect the types of reachable receiver objects
				Set<SootClass> receiverTypes = new HashSet<>();
				receiverVarNode.getP2Set().forall(new PTSetVisitor() {
					@Override
					public void visit(GNode n) {
						// TODO Auto-generated method stub
						receiverTypes.add(((RefType) n.getType()).getSootClass());
					}
				});
				// find the methods that cannot be dispatched on reachable objects
				// create objects of all their subtypes
				FastHierarchy hierarchy = Scene.v().getFastHierarchy();
				Set<SootClass> missingClasses = new HashSet<>();
				for(SootClass receiverType : receiverTypes)
					for(SootMethod m : findMatchingMethods(invokeCall)) {
						if(m.isStatic() ||
								m.equals(GlobalVariable.v().getUnknownMethod()) ||
								m.getDeclaringClass().equals(GlobalVariable.v().getUnknownClass()))
							continue;
						SootMethod dispatchedMtd = null;
						try {
						dispatchedMtd = hierarchy.resolveConcreteDispatch(receiverType, m);
						} catch(RuntimeException e) {
							System.out.println("Method dispatching failed.");
						}
						if(dispatchedMtd == null) {
							SootClass declaringClass = m.getDeclaringClass();
							if(declaringClass.getName().startsWith("android"))
								for(SootClass c : Util.v().getSubClasses(declaringClass))
									missingClasses.add(c);
						}
					}
				
				for(SootClass c : missingClasses) {
					RefType receiverType = c.getType();
					Alloc_Node allocNode = MockAllocNodes.v().get(receiverType);
					if(allocNode == null) {
						allocNode = pag.makeAllocNode(new MockAllocSite(receiverType), receiverType, source);
						MockAllocNodes.v().put(receiverType, allocNode);
					}
					// find or make points to set of receiver object
					
//					if(receiverVarNode == null) {
//						receiverVarNode = pag.makeLocalVarNode(receiverLocal, receiverLocal.getType(), source);
//						receiverVarNode.makeP2Set();
//					}
					// add the allocation node to the points to set of receiver object
					// receiverVarNode.getP2Set().add(allocNode);
					add(receiverVarNode, allocNode);
					pag.addAllocEdge(allocNode, receiverVarNode);
					varNodes.add(receiverVarNode);
					System.out.println("[InferenceReflectionModel] Modeling reveiver Add an object with type " + receiverType + 
							" to the points to set of " + receiverLocal + " in " + caller + ".");
					System.out.println("The reachable methods of this call site are: ");
					for(SootMethod mtd : invokeCall.getTargetMtds()) {
						System.out.println("\t # " + mtd.getSignature());
					}
				}
		
			}
		}
		return varNodes;
	}
	
	private static void add(LocalVar_Node varNode, Alloc_Node allocNode) {
		if(varNode.getP2Set() instanceof EmptyPTSet)
			varNode.makeP2Set();
		varNode.getP2Set().add(allocNode);
	}
	
	// get the types of the receiver objects of given methods
	private static Set<RefType> getPossibleTypeOfReceiverObject(InvokeCallInfo invokeCall) {
		Set<SootMethod> targetMtds = findMatchingMethods(invokeCall);
		Set<SootClass> receiverClasses = new HashSet<>();
		for(SootMethod mtd : targetMtds) {
			// UNKNOWN method and static method is ignored
			if(targetMtds.equals(GlobalVariable.v().getUnknownMethod()) || mtd.isStatic())
				continue;
			SootClass declaringClass = mtd.getDeclaringClass();
			// currently we only consider class declared in Android framework
			if(declaringClass.getName().startsWith("android")) {
				receiverClasses.add(declaringClass);
				receiverClasses.addAll(Util.v().getSubClasses(declaringClass));
			}
		}
		Set<RefType> receiverTypes = 
				receiverClasses.stream()
							   .filter(SootClass::isConcrete)
							   .map(SootClass::getType)
							   .collect(Collectors.toSet());
		return receiverTypes;
	}
}
