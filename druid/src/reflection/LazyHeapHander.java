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
package reflection;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import pag.node.GNode;
import pag.node.alloc.UnknownObject_Node;
import pag.node.var.Var_Node;
import pta.pts.PTSetVisitor;
import soot.PointsToAnalysis;
import soot.RefType;
import soot.SootClass;
import soot.Type;
import soot.jimple.CastExpr;
import soot.jimple.Expr;
import reflection.InferenceReflectionModel.ClzNewInstCallInfo;
import reflection.InferenceReflectionModel.CtorNewInstCallInfo;
import reflection.InferenceReflectionModel.StmtWithPos;
import soot.toolkits.scalar.Pair;

/**
 * Lazy heap modeling for resolving newInstnace() calls with non-post-nominant cast
 */
public class LazyHeapHander {
	public static void handleCast(Var_Node edgeSrc, Var_Node edgeTgt) {
		assert edgeTgt.getVariable() instanceof Pair<?, ?>;
		Pair<Expr, String> castPair = (Pair<Expr, String>) edgeTgt.getVariable();
		assert castPair.getO2().equals(PointsToAnalysis.CAST_NODE);
		CastExpr castExpr = (CastExpr) castPair.getO1();
		Type type = castExpr.getCastType();
		if(! (type instanceof RefType))
			return;
		RefType castType = (RefType) type; 
		// System.out.println("#### cast type: " + castType);
		
		Set<UnknownObject_Node> unknownObjectNodes = new HashSet<>();
		edgeSrc.getP2Set().forall(new PTSetVisitor() {
			@Override
			public void visit(GNode n) {
				if(n instanceof UnknownObject_Node)
					unknownObjectNodes.add((UnknownObject_Node) n);
			}
		});
		
		Set<SootClass> targetClasses = Util.v().getSubClasses(castType.getSootClass())
											   .stream()
											   .filter(SootClass::isConcrete)
											   .collect(Collectors.toSet());
		
		for(UnknownObject_Node unknownObjectNode : unknownObjectNodes) {
			UnknownObjectWrapper wrapper = unknownObjectNode.getUnknownObjectWrapper();
			StmtWithPos newInstCall = wrapper.getNewInstCall();
			InferenceReflectionModel inferenceReflectionModel = GlobalVariable.v().getInferenceReflectionModel();
			if(newInstCall instanceof ClzNewInstCallInfo)
				inferenceReflectionModel.handleClzNewInstSideEffect((ClzNewInstCallInfo) newInstCall, targetClasses);
			else if(newInstCall instanceof CtorNewInstCallInfo)
				inferenceReflectionModel.handleCtorNewInstSideEffect((CtorNewInstCallInfo) newInstCall, wrapper.getConstructorNode(), targetClasses);
		}
	}
	
	public static void handleMethodInvoke(UnknownObject_Node unknownObjectNode, Set<SootClass> methodClassTypes) {
		UnknownObjectWrapper wrapper = unknownObjectNode.getUnknownObjectWrapper();
		StmtWithPos newInstCall = wrapper.getNewInstCall();
		InferenceReflectionModel inferenceReflectionModel = GlobalVariable.v().getInferenceReflectionModel();
		if(newInstCall instanceof ClzNewInstCallInfo)
			inferenceReflectionModel.handleClzNewInstSideEffect((ClzNewInstCallInfo) newInstCall, methodClassTypes);
		else if(newInstCall instanceof CtorNewInstCallInfo)
			inferenceReflectionModel.handleCtorNewInstSideEffect((CtorNewInstCallInfo) newInstCall, wrapper.getConstructorNode(), methodClassTypes);
	}
}
