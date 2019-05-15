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
import java.util.Map.Entry;

import pag.WholeProgPAG;
import pag.node.alloc.Alloc_Node;
import pag.node.var.LocalVar_Node;
import reflection.GlobalVariable;
import reflection.InferenceReflectionModel;
import reflection.InferenceReflectionModel.ForNameCallInfo;

import java.util.Set;

import pta.PTA;
import soot.Local;
import soot.Scene;
import soot.SootMethod;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Stmt;

public class HandleUnknownClassMetaObject {
	/**
	 * Handling the imperfect analysis environment
	 * Because the imperfect analysis environment of Android, some paths between application code and
	 * Android framework are broken, which causes many reflective calls are missed.
	 * 
	 * If the points-to set of the class name in the Class.forName call is empty, then we put a
	 * UNKNOWN class meta-object into the points-to set the meta-object, which may enable inference
	 * in the subsequent reflective calls.
	 */
	public static List<LocalVar_Node> createUnknownClassMetaObj() {
		List<LocalVar_Node> newVarNodes = new ArrayList<>();
		InferenceReflectionModel infer = GlobalVariable.v().getInferenceReflectionModel();
		if(infer == null)
			return newVarNodes;
		WholeProgPAG pag = ((PTA)Scene.v().getPointsToAnalysis()).getPag();
		for(Iterator<Entry<Value, Set<ForNameCallInfo>>> i = infer.getClzName2ForNameCalls().entrySet().iterator(); i.hasNext(); ) {
			for(ForNameCallInfo forNameCall : i.next().getValue()) {
				SootMethod source = forNameCall.getSource();
				Stmt caller = forNameCall.getCaller();
				if(caller instanceof AssignStmt) {
					AssignStmt assign = (AssignStmt) caller;
					Local metaClass = (Local) assign.getLeftOp();
					LocalVar_Node metaClassNode = pag.findLocalVarNode(metaClass);
					if(metaClassNode == null)
						continue;
					// if meta class returned by Class.forName() call does not return any class meta object,
					// then we put a unknown method into its points to set
//					if(metaClassNode == null) {
//						metaClassNode = pag.makeLocalVarNode(metaClass, metaClass.getType(), source);
//						metaClassNode.makeP2Set();
//					}
					if(metaClassNode.getP2Set().isEmpty()) {
						Alloc_Node classConstant = pag.makeClassConstantNode(GlobalVariable.v().getUnknownClzConst());
						metaClassNode.getP2Set().add(classConstant);
						newVarNodes.add(metaClassNode);
						pag.addAllocEdge(classConstant, metaClassNode);
						System.out.println("[InferenceReflectionModel] Class meta-object Add an unknown class to the points to set of local variable " + 
								metaClass + " in " + assign + ", the containing method is " + source.getSignature() + ".");
					}
				}
			}
		}
		return newVarNodes;
	}
}
