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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import reflection.GlobalVariable;
import reflection.InferenceReflectionModel;
import reflection.InferenceReflectionModel.InvokeCallInfo;
import soot.Body;
import soot.IntType;
import soot.Local;
import soot.PatchingChain;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.EqExpr;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JEqExpr;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JStaticInvokeExpr;

/**
 * Handling target invocation of reflective calls
 * 
 * Because several methods can be invoked at a Method.invoke() call site,
 * and FlowDroid performs flow-sensitive analysis, the order of the target method invocation
 * is an important issue. 
 * 
 * Because FlowDroid filter out call graph edges with kind REFL_INVOKE and flow function does not
 * handle this kind of call edge, in order not to modify the core code in FlowDroid, the target methods
 * are inserted as a series of if statement.
 * 
 * @author yifei
 */
public class HandleTgtMtdInvocation {
	public static void handleTgtMtdInvocation(InvokeCallInfo invokeCall, Unit target) {
		PatchingChain<Unit> units = invokeCall.getSource().retrieveActiveBody().getUnits();
		Local ifCond = invokeCall.getIfCond();
		if(ifCond == null) {
			/**
			 * 1. Generate local variable of if condition
			 * 2. Add this local variable into the local variable declaration
			 * 3. Generate and insert the initialization
			 * 
			 * In order not to be removed by constant value propagation, the value is initialized by a method call.
			 * For convenience, this method is static (no receiver object), returns a integer primitive type (no unboxing)
			 * , parameter is primitive type and supported by old version Java
			 * Signature: public static int signum(int i)
			 * Since: Java 1.5
			 */
			ifCond = Jimple.v().newLocal(GlobalVariable.v().getIfCondVarName(), IntType.v());
			invokeCall.getSource().retrieveActiveBody().getLocals().add(ifCond);
			SootMethod intInitMtd = Scene.v().getMethod("<java.lang.Integer: int signum(int)>");
			InvokeExpr invokeIntInit = new JStaticInvokeExpr(intInitMtd.makeRef(), Arrays.asList(IntConstant.v(0)));
			AssignStmt init = new JAssignStmt(ifCond, invokeIntInit);
			// AssignStmt init = new JAssignStmt(ifCond, IntConstant.v(0));
			units.insertAfter(init, invokeCall.getCaller());
			invokeCall.setIfCond(ifCond);
			invokeCall.setLastPos(init);
		}
		
		// old method
//		// 4. Generate if statement
//		EqExpr eqExpr = new JEqExpr(ifCond, IntConstant.v(invokeCall.getCaseValue()));
//		IfStmt ifStmt = new JIfStmt(eqExpr, target);
//		units.insertAfter(ifStmt, invokeCall.getLastPos());
//		// 5. Insert target again
//		units.insertAfter(target, ifStmt);
//		invokeCall.setLastPos(target);
		
		/**
		 * 4. Generate if statement
		 * 	if cond_1 == 0 goto label 1
		 * 	reflective calls
		 * 	label 1
		 * 	if cond_1 == 1 goto label 2
		 * 	reflective calls
		 * 	label 2
		 * 
		 * 1. insert if statement first
		 * 2. then reflective target method call
		 * 3. then the goto target of if statement, which is a nop in this case
		 */
		NopStmt nop = Jimple.v().newNopStmt();
		EqExpr eqExpr = new JEqExpr(ifCond, IntConstant.v(invokeCall.getCaseValue()));
		IfStmt ifStmt = new JIfStmt(eqExpr, nop);
		units.insertAfter(ifStmt, invokeCall.getLastPos());
		units.insertAfter(target, ifStmt);
		units.insertAfter(nop, target);
		invokeCall.setLastPos(nop);
	}
	
	/**
	 * Remove the if statement if the reflection call site has only one target
	 * This can maintain the same semantics as normal call
	 * 
	 * Find all the reachable Method.invoke() call site
	 * Find the mono-target reflective calls
	 * remove the initialization of if guard and if statement
	 */
	public static void handleMonoTgtReflCall() {
		InferenceReflectionModel infer = GlobalVariable.v().getInferenceReflectionModel();
		if(infer == null)
			return;
		
		for(Iterator<Set<InvokeCallInfo>> i = infer.getMtd2InvokeCalls().iterator(); i.hasNext(); ) {
			for(InvokeCallInfo invokeCall : i.next()) {
				if(invokeCall.getInvokeExpr().size() == 1) {
					Body body = invokeCall.getSource().retrieveActiveBody();
					PatchingChain<Unit> units = body.getUnits();
					System.out.println("## handling mono-target reflective call site");
					System.out.println("## Before: ");
					System.out.println(body);
					Unit caller = invokeCall.getCaller();
					Unit init = units.getSuccOf(caller);
					Unit ifStmt = units.getSuccOf(init);
					Unit targetCall = units.getSuccOf(ifStmt);
					assert init instanceof AssignStmt;
					assert ifStmt instanceof IfStmt;
					assert targetCall instanceof AssignStmt || targetCall instanceof InvokeStmt;
					// remove the initialization
					// remove the if statement
					// remove the the variable declaration used in if condition
					units.remove(init);
					units.remove(ifStmt);
					body.getLocals().remove(((AssignStmt) init).getLeftOp());
					System.out.println("## After: ");
					System.out.println(body);
				}
			}
		}
	}
}