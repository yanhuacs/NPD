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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import pag.WholeProgPAG;
import pag.node.alloc.Alloc_Node;
import pag.node.var.LocalVar_Node;
import pta.PTA;
import reflection.GetMethodWrapper;
import reflection.GlobalVariable;
import reflection.InferenceReflectionModel;
import reflection.InferenceReflectionModel.GetDeclaredMtdCallInfo;
import reflection.InferenceReflectionModel.GetMtdCallInfo;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Stmt;

public class HandleUnknownMtdMetaObject {
	/**
	 * Handling the imperfect analysis environment
	 * Because the imperfect analysis environment of Android, some paths between application code and
	 * Android framework are broken, which causes many reflective calls are missed.
	 * 
	 * If the points-to set of the class name in the Class.forName call is empty, then we put a
	 * UNKNOWN method meta-object into the points-to set the meta-object, which may enable inference
	 * in the subsequent reflective calls.
	 */
	// handle null method name in Class.getMethod and Class.getDeclaredMethod call
	public static List<LocalVar_Node> createUnknownMtdMetaObj() {
		List<LocalVar_Node> newVarNodes = new ArrayList<>();
		InferenceReflectionModel infer = GlobalVariable.v().getInferenceReflectionModel();
		for(Iterator<Set<GetMtdCallInfo>> i = infer.getMtdName2GetMtdCalls().iterator(); i.hasNext(); ) {
			for(GetMtdCallInfo getMtdCall : i.next()) {
				LocalVar_Node varNode = updateMtdMetaObjVarNode(getMtdCall.getSource(),
						getMtdCall.getCaller(), getMtdCall.getMetaClzs());
				if(varNode != null)
					newVarNodes.add(varNode);
			}
		}
		for(Iterator<Set<GetDeclaredMtdCallInfo>> i = infer.getMtdName2GetDeclaredMtdCalls().iterator(); i.hasNext(); ) {
			Set<GetDeclaredMtdCallInfo> getDeclaredMtdCalls = i.next();
			for(GetDeclaredMtdCallInfo getDeclaredMtdCall : getDeclaredMtdCalls) {
				LocalVar_Node varNode = updateMtdMetaObjVarNode(getDeclaredMtdCall.getSource(), 
						getDeclaredMtdCall.getCaller(), getDeclaredMtdCall.getMetaClzs());
				if(varNode != null)
					newVarNodes.add(varNode);
			}
		}
		return newVarNodes;
	}

	// get updated local variable of method meta object
	private static LocalVar_Node updateMtdMetaObjVarNode(SootMethod source, Stmt caller, Set<SootClass> metaClzs) {
		WholeProgPAG pag = ((PTA)Scene.v().getPointsToAnalysis()).getPag();
		Value mtdName = caller.getInvokeExpr().getArg(0);
		if(mtdName instanceof Local && caller instanceof AssignStmt) {
			Value mtdMetaObjLocal = ((AssignStmt) caller).getLeftOp();
			LocalVar_Node mtdMetaObjVarNode = pag.findLocalVarNode(mtdMetaObjLocal);
			if(mtdMetaObjVarNode == null) {
//				mtdMetaObjVarNode = pag.makeLocalVarNode(mtdMetaObjLocal, mtdMetaObjLocal.getType(), source);
//				mtdMetaObjVarNode.makeP2Set();
				return null;
			}
			// if points to set of method name is empty, which means the method name may be get from
			// library, then we put a unknown method into the points to set of method meta object
			if(mtdMetaObjVarNode.getP2Set().isEmpty() && ! metaClzs.isEmpty()) {
				for(SootClass metaClass : metaClzs) {
					SootMethod unknownMtd = GlobalVariable.v().getUnknownMethod();
					GetMethodWrapper wrapper = new GetMethodWrapper(metaClass, unknownMtd, caller);
					if(caller.getInvokeExpr().getMethod().getName().equals("getMethod"))
						wrapper.fromGetMethod();
					else
						wrapper.fromGetDeclaredMethod();
					Alloc_Node metaMtdAllocNode = GlobalVariable.v().getInferenceReflectionModel().makeMethodMetaObjectNode(wrapper, source);
					mtdMetaObjVarNode.getP2Set().add(metaMtdAllocNode);
					pag.addAllocEdge(metaMtdAllocNode, mtdMetaObjVarNode);
					System.out.println("[InferenceReflectionModel] Mthod meta-object Add an unkown method to local variable " + mtdMetaObjLocal + 
							" in " + caller + ", the containing method is " + source.getSignature() + ".");
				}
				return mtdMetaObjVarNode;
			}
		}
		return null;
	}
}
