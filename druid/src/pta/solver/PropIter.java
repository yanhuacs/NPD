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

package pta.solver;

import soot.jimple.spark.pag.*;
import soot.jimple.spark.solver.Propagator;
import soot.*;
import soot.util.queue.*;
import java.util.*;

import pag.WholeProgPAG;
import pag.node.GNode;
import pag.node.alloc.AllocDotField_Node;
import pag.node.alloc.Alloc_Node;
import pag.node.var.FieldRef_Node;
import pag.node.var.Var_Node;
import pta.CallGraphBuilder;
import pta.PTA;
import pta.pts.PTSetInternal;
import pta.pts.PTSetVisitor;

/**
 * Propagates points-to sets along pointer assignment graph using iteration.
 * 
 * @author Ondrej Lhotak
 */

public final class PropIter extends Propagator {
	public PropIter(PTA pta) {
		cgb = pta.getCgb();
    	pag = pta.getPag();
    	this.pta = pta;
	}

	/** Actually does the propagation. */
	public final void propagate() {
		new TopoSorter(pag, false).sort();
		for (Alloc_Node object : pag.allocSources()) {
			handleAllocNode( object);
		}
		int iteration = 1;
		boolean change;
		do {
			change = false;
			TreeSet<Var_Node> simpleSources = new TreeSet<Var_Node>(pag.simpleSources());
			if (pag.getOpts().verbose())
				G.v().out.println("Iteration " + (iteration++));
			for (Var_Node object : simpleSources)
				change = handleSimples(object) | change;
			QueueReader<GNode> addedEdges = pag.edgeReader();
			for (Var_Node src : pag.getVarNodeNumberer())
				cgb.updateCallGraph(src);
			pta.buildPAG();
			while (addedEdges.hasNext()) {
				GNode addedSrc = (GNode) addedEdges.next();
				GNode addedTgt = (GNode) addedEdges.next();
				change = true;
				if (addedSrc instanceof Var_Node) {
					PTSetInternal p2set = ((Var_Node) addedSrc).getP2Set();
					if (p2set != null)
						p2set.unFlushNew();
				} else if (addedSrc instanceof Alloc_Node) {
					((Var_Node) addedTgt).makeP2Set().add(addedSrc);
				}
			}
			if (change) {
				new TopoSorter(pag, false).sort();
			}
			
			for (FieldRef_Node object : pag.loadSources()) {
				change = handleLoads(object) | change;
			}
			for (Var_Node object : pag.storeSources()) {
				change = handleStores(object) | change;
			}
			
//			for (NewInstanceNode object : pag.assignInstanceSources()) {
//				change = handleNewInstances(object) | change;
//			}
		} while (change);
	}

	/* End of public methods. */
	/* End of package methods. */

	/**
	 * Propagates new points-to information of node src to all its successors.
	 */
	protected final boolean handleAllocNode(Alloc_Node src) {
		boolean ret = false;
		Var_Node[] targets = pag.allocLookup(src);
		for (Var_Node element : targets) {
			ret = element.makeP2Set().add(src) | ret;
		}
		return ret;
	}

	protected final boolean handleSimples(Var_Node src) {
		boolean ret = false;
		PTSetInternal srcSet = src.getP2Set();
		if (srcSet.isEmpty())
			return false;
		Var_Node[] simpleTargets = pag.simpleLookup(src);
		for (Var_Node element : simpleTargets) {
			ret = element.makeP2Set().addAll(srcSet, null) | ret;
		}
		
//		Node[] newInstances = pag.newInstanceLookup(src);
//		for (Node element : newInstances) {
//			ret = element.makeP2Set().addAll(srcSet, null) | ret;
//		}
		
		return ret;
	}

	protected final boolean handleStores(Var_Node src) {
		boolean ret = false;
		final PTSetInternal srcSet = src.getP2Set();
		if (srcSet.isEmpty())
			return false;
		FieldRef_Node[] storeTargets = pag.storeLookup(src);
		for (FieldRef_Node fr : storeTargets) {
			final SparkField f = fr.getField();
			ret = fr.getBase().getP2Set().forall(new PTSetVisitor() {
				public final void visit(GNode n) {
					AllocDotField_Node nDotF = pag.makeAllocDotField((Alloc_Node) n, f);
					if (nDotF.makeP2Set().addAll(srcSet, null)) {
						returnValue = true;
					}
				}
			}) | ret;
		}
		return ret;
	}

	protected final boolean handleLoads(FieldRef_Node src) {
		boolean ret = false;
		final Var_Node[] loadTargets = pag.loadLookup(src);
		final SparkField f = src.getField();
		ret = src.getBase().getP2Set().forall(new PTSetVisitor() {
			public final void visit(GNode n) {
				AllocDotField_Node nDotF = ((Alloc_Node) n).dot(f);
				if (nDotF == null)
					return;
				PTSetInternal set = nDotF.getP2Set();
				if (set.isEmpty())
					return;
				for (Var_Node target : loadTargets) {
					if (target.makeP2Set().addAll(set, null)) {
						returnValue = true;
					}
				}
			}
		}) | ret;
		return ret;
	}
	
//	protected final boolean handleNewInstances(final NewInstanceNode src) {
//		boolean ret = false;
//		final Node[] newInstances = pag.assignInstanceLookup(src);
//		for (final Node instance : newInstances) {
//			ret = src.getP2Set().forall(new P2SetVisitor() {
//				
//				@Override
//				public void visit(Node n) {
//					if (n instanceof ClassConstantNode) {
//						ClassConstantNode ccn = (ClassConstantNode) n;
//						Type ccnType = RefType.v(ccn.getClassConstant().getValue().replaceAll("/", "."));
//						
//						// If the referenced class has not been loaded, we do this now
//						SootClass targetClass = ((RefType) ccnType).getSootClass();
//						if (targetClass.resolvingLevel() == SootClass.DANGLING)
//							Scene.v().forceResolve(targetClass.getName(), SootClass.SIGNATURES);
//						
//						instance.makeP2Set().add(pag.makeAllocNode(src.getValue(), ccnType, ccn.getMethod()));
//					}
//				}
//				
//			});
//		}
//		return ret;
//	}

	protected WholeProgPAG pag;
    protected PTA pta;
    protected CallGraphBuilder cgb;
}
