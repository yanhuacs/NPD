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

import driver.DruidOptions;
import pag.WholeProgPAG;
import pag.node.GNode;
import pag.node.alloc.AllocDotField_Node;
import pag.node.alloc.Alloc_Node;
import pag.node.alloc.ContextAlloc_Node;
import pag.node.var.ContextLocalVar_Node;
import pag.node.var.FieldRef_Node;
import pag.node.var.Var_Node;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import pta.CallGraphBuilder;
import pta.PTA;
import pta.context.EmptyContext;
import pta.pts.PTSetInternal;
import pta.pts.PTSetVisitor;
import soot.Context;
import soot.jimple.spark.pag.SparkField;
import soot.jimple.spark.solver.Propagator;
import soot.util.queue.QueueReader;

public final class Solver extends Propagator {
	protected WholeProgPAG pag;
    protected PTA pta;
    protected CallGraphBuilder cgb;
	protected final TreeSet<Var_Node> varNodeWorkList = new TreeSet<Var_Node>();
    public Solver( PTA _pta ) {
    	cgb = _pta.getCgb();
    	pag = _pta.getPag();
    	pta = _pta;
    }
	@Override
	public final void propagate() {
		new TopoSorter( pag, false ).sort();
		pag.getAlloc().forEach((a,set)-> set.forEach(v->handleAlloc( a, v ))); 
		while( !varNodeWorkList.isEmpty() ) {
        	final Var_Node src = varNodeWorkList.pollFirst();
            //G.v().out.println("handle node: "+ src.toString());
        	if(! (src instanceof AllocDotField_Node)){
        		updateCallGraph(src);
        		handleStoreAndLoadOnBase(src);
        	}
            handleSimpleFromSrc( src );
        }
	}
	
	private void handleAlloc(Alloc_Node src, Var_Node tgt) {
		if(tgt.makeP2Set().add(src))
            varNodeWorkList.add( tgt );
	}

	private void handleSimpleFromSrc(Var_Node src) {
		final PTSetInternal pts=src.getP2Set();
		final PTSetInternal newset = pts.getNewSet();
		Set<Var_Node> targets = pag.getSimple().get(src);
        if(targets!=null)
        	targets.forEach(element->{
        		if(thisPtrFilterAddAll(element, newset))
        			varNodeWorkList.add(element);
        	});
        pts.flushNew();
	}
	private void handleStoreAndLoadOnBase(Var_Node src) {
		final PTSetInternal newP2Set = src.getP2Set().getNewSet();
		if(newP2Set.isEmpty())
			return;
        for(final FieldRef_Node fr : src.getAllFieldRefs()) {
        	final SparkField fld = fr.getField();
    		/// foreach src.fld = v do add simple from v-->o.fld where o\in pts(src)
        	for (final Var_Node v :pag.storeInvLookup( fr )) {
            	newP2Set.forall( new PTSetVisitor() {
                    public final void visit( GNode n ) {
                        final AllocDotField_Node oDotF = pag.makeAllocDotField((Alloc_Node) n, fld );
                        pag.addSimpleEdge(v, oDotF);
                        if (thisPtrFilterAddAll(oDotF, v.getP2Set().getOldSet()))
                    		varNodeWorkList.add( oDotF );
                    }
                } );
            }
    		/// foreach v = src.fld do add simple from o.fld-->v where o\in pts(src)
            for (final Var_Node element : pag.loadLookup( fr ))
            	if(element==src){
                	Set<AllocDotField_Node> set=new HashSet<AllocDotField_Node>();
                	newP2Set.forall( new PTSetVisitor() {
                		public final void visit( GNode n ) {
                			final AllocDotField_Node oDotF = pag.makeAllocDotField((Alloc_Node) n, fld );
                			pag.addSimpleEdge(oDotF, element);
                			set.add(oDotF);
                		}
                	} );
                	for(AllocDotField_Node oDotF:set)
                		if (thisPtrFilterAddAll(element, oDotF.getP2Set().getOldSet()))
            				varNodeWorkList.add( element );
                }
                else
                	newP2Set.forall( new PTSetVisitor() {
                		public final void visit( GNode n ) {
                			final AllocDotField_Node oDotF = pag.makeAllocDotField((Alloc_Node) n, fld );
                			pag.addSimpleEdge(oDotF, element);
                			if (thisPtrFilterAddAll(element, oDotF.getP2Set().getOldSet()))
                				varNodeWorkList.add( element );
                		}
                	} );
        }
	}
	private void updateCallGraph(Var_Node src) {
		final QueueReader<GNode> addedEdges = pag.edgeReader();
        cgb.updateCallGraph( src );
        pta.buildPAG();
        while (addedEdges.hasNext()) {
			final GNode addedSrc = addedEdges.next();
			final GNode addedTgt = addedEdges.next();
			if (addedSrc instanceof Alloc_Node)
				handleAlloc((Alloc_Node)addedSrc, (Var_Node) addedTgt);
			else if (addedSrc instanceof FieldRef_Node) {
				final FieldRef_Node srcfrn = (FieldRef_Node)addedSrc;
				final SparkField fld = srcfrn.getField();
				final Var_Node tgtv = (Var_Node)addedTgt;
				srcfrn.getBase().getP2Set().forall(new PTSetVisitor() {
					public void visit(GNode n) {
						final AllocDotField_Node oDotF = pag.makeAllocDotField((Alloc_Node) n, fld);
						pag.addSimpleEdge(oDotF, tgtv);
						if (thisPtrFilterAddAll(tgtv, oDotF.getP2Set().getOldSet()))
							varNodeWorkList.add( tgtv );
					}
				});
			}
			else if(addedTgt instanceof FieldRef_Node){
				final Var_Node srcv = (Var_Node)addedSrc;
				final FieldRef_Node tgtfrn = (FieldRef_Node)addedTgt;
				final SparkField fld = tgtfrn.getField();
				tgtfrn.getBase().getP2Set().forall(new PTSetVisitor() {
					public void visit(GNode n) {
						final AllocDotField_Node oDotF = pag.makeAllocDotField((Alloc_Node) n, fld);
						pag.addSimpleEdge(srcv, oDotF);
						//Only GlobalVarNode?
						if (thisPtrFilterAddAll(oDotF, srcv.getP2Set().getOldSet()))
							varNodeWorkList.add( oDotF );
					}
				});
			}else{
				final Var_Node tgtv = (Var_Node)addedTgt;
				if (thisPtrFilterAddAll(tgtv, addedSrc.getP2Set().getOldSet()))
					varNodeWorkList.add(tgtv);
			}
        }
	}

	//===================================================================================================================

	private boolean thisPtrShouldAdd(ContextAlloc_Node allocNode, Context thisRefContext) {
		// if the this pointer has no context, then only add obj sens alloc nodes with no context beyond new expr
		if (thisRefContext instanceof EmptyContext) 
			return allocNode.noContext();
		else
			return thisRefContext==allocNode;
		
	}
	private boolean thisPtrFilterAddAll(final Var_Node pointer, PTSetInternal other) {
		final PTSetInternal addTo = pointer.makeP2Set();
		// : removed call to prunePTSetForThisPtr to improve performance
		// PointsToSetInternal newSet = pag.prunePTSetForThisPtr(pointer, other);
		// return addTo.addAll(newSet, null);
		// : moved part of the test out of the loop for efficiency
		if (DruidOptions.kobjsens>0 && pointer.isThisPtr() && pointer instanceof ContextLocalVar_Node) {
			final Context thisPtrCtxt = ((ContextLocalVar_Node) pointer).context();
			return other.forall(new PTSetVisitor() {
				public final void visit(GNode n) {
					if (thisPtrShouldAdd((ContextAlloc_Node) n, thisPtrCtxt))
						if (addTo.add(n))
							returnValue = true;
				}
			});
		}
		//deprecated because not equal to forall->add
		//return addTo.addAll(other, null);
		return other.forall(new PTSetVisitor() {
			public final void visit(GNode n) {
				if (addTo.add(n))
					returnValue = true;
			}
		});
	}
}
