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
import soot.toolkits.scalar.Pair;
import soot.util.queue.*;

import java.util.*;

import driver.DruidOptions;
import pag.WholeProgPAG;
import pag.node.GNode;
import pag.node.alloc.AllocDotField_Node;
import pag.node.alloc.Alloc_Node;
import pag.node.alloc.ContextAlloc_Node;
import pag.node.var.ContextLocalVar_Node;
import pag.node.var.FieldRef_Node;
import pag.node.var.Var_Node;
import pta.CallGraphBuilder;
import pta.PTA;
import pta.context.EmptyContext;
import pta.pts.PTSetInternal;
import pta.pts.PTSetVisitor;
import reflection.LazyHeapHander;
import reflection.ReflectionOptions;
import reflection.android.HandleUnknownClassMetaObject;
import reflection.android.HandleUnknownMtdMetaObject;
import reflection.android.ReflectionLibraryModeling;

/** Propagates points-to sets along pointer assignment graph using a worklist.
 * @author Ondrej Lhotak
 */

public final class PropWorklist extends Propagator {
    protected final TreeSet<Var_Node> varNodeWorkList = new TreeSet<Var_Node>();

    public PropWorklist( PTA _pta ) {
    	cgb = _pta.getCgb();
    	pag = _pta.getPag();
    	pta = _pta;
    }
    
    public final void solveNewConstraints() {
        do {
            if( DruidOptions.sparkOpts.verbose() ) 
                G.v().out.println( "Worklist has "+varNodeWorkList.size()+" nodes." );
            
            while( !varNodeWorkList.isEmpty() ) {
                Var_Node src = varNodeWorkList.pollFirst();
                handleVarNode( src );
            }
            
            if( DruidOptions.sparkOpts.verbose() ) 
                G.v().out.println( "Now handling field references" );
            
            for (Var_Node src : pag.storeSources()) {
            	FieldRef_Node[] targets = pag.storeLookup( src );
                for (FieldRef_Node target : targets) {
                    target.getBase().makeP2Set().forall( new PTSetVisitor() {
                        public final void visit( GNode n ) {
                            AllocDotField_Node nDotF = pag.makeAllocDotField((Alloc_Node) n, target.getField() );
                            nDotF.makeP2Set().addAll( src.getP2Set(), null );
                        }
                    } );
                }
            }
            
            HashSet<Object[]> edgesToPropagate = new HashSet<Object[]>();
            for (FieldRef_Node src : pag.loadSources()) {
                final Var_Node[] loadTargets = pag.loadLookup( src );
                if( loadTargets.length == 0 ) continue;
                
                src.getBase().getP2Set().forall( new PTSetVisitor() {
                    public final void visit( GNode n ) {
                        AllocDotField_Node nDotF = pag.makeAllocDotField((Alloc_Node) n, src.getField() );
                        if( nDotF != null ) {
                            PTSetInternal p2Set = nDotF.getP2Set();
                            if( !p2Set.getNewSet().isEmpty() ) {
                                for (Var_Node element : loadTargets) {
                                    Object[] pair = { p2Set, element };
                                    edgesToPropagate.add( pair );
                                }
                            }
                        }
                    }
                } );
            }
            Set<PTSetInternal> nodesToFlush = java.util.Collections.newSetFromMap(new IdentityHashMap<PTSetInternal,Boolean>());
            for (Object[] pair : edgesToPropagate) {
                PTSetInternal nDotF = (PTSetInternal) pair[0];
                PTSetInternal newP2Set = nDotF.getNewSet();
                Var_Node loadTarget = (Var_Node) pair[1];
                
                if (thisPtrFilterAddAll(loadTarget, newP2Set, loadTarget.makeP2Set())) {
                    varNodeWorkList.add( loadTarget );
                }
                nodesToFlush.add( nDotF );
            }
            for (PTSetInternal nDotF : nodesToFlush) {
                nDotF.flushNew();
            }
        } while( !varNodeWorkList.isEmpty() );
    }
    
    /** Actually does the propagation. */
    public final void propagate() {
        new TopoSorter( pag, false ).sort();
        
        for (Alloc_Node object : pag.allocSources()) 
            handleAllocNode( object );
        
        solveNewConstraints();

		if (ReflectionOptions.v().isInferenceReflectionModel() && !ReflectionOptions.v().isConstStringOnly() && ReflectionOptions.v().isAndroid()) {
			if (ReflectionOptions.v().isMetaObjectModel()) {
				varNodeWorkList.addAll(HandleUnknownClassMetaObject.createUnknownClassMetaObj());
				varNodeWorkList.addAll(HandleUnknownMtdMetaObject.createUnknownMtdMetaObj());
			}
			if (ReflectionOptions.v().isLibraryReturnValueModel())
				varNodeWorkList.addAll(ReflectionLibraryModeling.modelInvokeReturnValue());
			if (ReflectionOptions.v().isLibraryReceiverValueModel())
				varNodeWorkList.addAll(ReflectionLibraryModeling.modelInvokeReceiverObject());
			solveNewConstraints();
		}
    }

    /* End of public methods. */
    /* End of package methods. */

    /** Propagates new points-to information of node src to all its
     * successors. */
    // : changed the return type to void
    protected final void handleAllocNode( Alloc_Node src ) {
        for (Var_Node element : pag.allocLookup( src )) {    
            //if( element.makeP2Set().add( src ) ) {
            if (thisPtrFilterAdd(element, src, element.makeP2Set())) {
                varNodeWorkList.add( element );
            }
        }
    }
    
   
    /** Propagates new points-to information of node src to all its successors. */
    protected final void handleVarNode( final Var_Node src ) {
    	/**
    	 * @Ammonia
    	 * why use flush here?
    	 * flush is used  for label any secondary updates of the newP2Set of src. we should not merge this newset to oldset because the secondary updates have not been processed.
    	 * This happens because it interactively uses depthFirst search under the toplevel breadth first search.
    	 * In other words, a pure breadth first search worklist should not change the p2set of src during a single visit to this method.
    	*/
        boolean flush = true;

        if( src.getReplacement() != src ) 
        	throw new RuntimeException("Got bad node "+src+" with rep "+src.getReplacement() );

        final PTSetInternal newP2Set = src.getP2Set().getNewSet();
        if( newP2Set.isEmpty() ) return;
        
        // : simple targets from the input source node whose p2sets will
        // be updated in the while loop below
        Set<Var_Node> processedSimpleTargets = new HashSet<Var_Node>();

        QueueReader<GNode> addedEdges = pag.edgeReader();
        cgb.updateCallGraph( src );
        pta.buildPAG();

		while (addedEdges.hasNext()) {
			GNode addedSrc = (GNode) addedEdges.next();
			GNode addedTgt = (GNode) addedEdges.next();
			if (addedSrc instanceof Var_Node) {
				if (addedTgt instanceof Var_Node) {
					Var_Node edgeSrc = (Var_Node) addedSrc.getReplacement();
					Var_Node edgeTgt = (Var_Node) addedTgt.getReplacement();

					// : cache the new simple targets (from the input source node) whose
					// p2sets are updated to avoid redundant computation
					if (edgeSrc == src)
						processedSimpleTargets.add(edgeTgt);

					// if( edgeTgt.makeP2Set().addAll( edgeSrc.getP2Set(), null) ) {
					if (thisPtrFilterAddAll(edgeTgt, edgeSrc.getP2Set(), edgeTgt.makeP2Set())) {
						varNodeWorkList.add(edgeTgt);
						if (edgeTgt == src)
							flush = false;
					}
				}
			} else if (addedSrc instanceof Alloc_Node) {
				Alloc_Node edgeSrc = (Alloc_Node) addedSrc;
				Var_Node edgeTgt = (Var_Node) addedTgt.getReplacement();
				// if( edgeTgt.makeP2Set().add( edgeSrc ) ) {
				if (thisPtrFilterAdd(edgeTgt, edgeSrc, edgeTgt.makeP2Set())) {
					varNodeWorkList.add(edgeTgt);
					if (edgeTgt == src)
						flush = false;
				}
			}
		}
        for (Var_Node element : pag.simpleLookup( src )) {
            //if( element.makeP2Set().addAll( newP2Set, null ) ) {
            // : Do not re-compute p2sets of simple targets if they were already computed
        	// in the while loop above
        	 // Yifei 
    		// Lazy heap modeling
            if(ReflectionOptions.v().isLazyHeapModeling()) {
            	if((element.getVariable() instanceof Pair<?, ?>) 
            			&& ((Pair<?, ?>)element.getVariable()).getO2().equals(PointsToAnalysis.CAST_NODE)) {
            		LazyHeapHander.handleCast(src, element);
            	}
            }
        	if (!processedSimpleTargets.contains(element)) {
        		if (thisPtrFilterAddAll(element, newP2Set, element.makeP2Set())) {
        			varNodeWorkList.add( element );
        			if(element == src) flush = false;
        		}
        	}
        }

        for (FieldRef_Node fr : pag.storeLookup( src )) {
            final SparkField f = fr.getField();
            fr.getBase().getP2Set().forall( new PTSetVisitor() {
                public final void visit( GNode n ) {
                    AllocDotField_Node nDotF = pag.makeAllocDotField( 
                        (Alloc_Node) n, f );
                    if( nDotF.makeP2Set().addAll( newP2Set, null ) ) {
                        returnValue = true;
                    }
                }
            } );
        }
        //field stuff
        for( final FieldRef_Node fr : src.getAllFieldRefs()) {
            final SparkField field = fr.getField();
            final GNode[] storeSources = pag.storeInvLookup( fr );
            if( storeSources.length > 0 ) {
                newP2Set.forall( new PTSetVisitor() {
                    public final void visit( GNode n ) {
                        AllocDotField_Node nDotF = pag.makeAllocDotField((Alloc_Node) n, field );
                        for (GNode element : storeSources) {
                            nDotF.makeP2Set().addAll( element.getP2Set(), null );
                        }
                    }
                } );
            }
            final Var_Node[] loadTargets = pag.loadLookup( fr );
            if( loadTargets.length > 0 ) {
                newP2Set.forall( new PTSetVisitor() {
                    public final void visit( GNode n ) {
                        AllocDotField_Node nDotF = pag.makeAllocDotField((Alloc_Node) n, field );
                        if( nDotF != null ) {
                            for (Var_Node loadTarget : loadTargets) {
                                if (thisPtrFilterAddAll(loadTarget,  nDotF.getP2Set(), loadTarget.makeP2Set())) {
                                    varNodeWorkList.add( loadTarget );
                                }
                            }
                        }
                    }
                } );
            }
        } 
        if(flush) src.getP2Set().flushNew();
    }

    private boolean thisPtrShouldAdd(Alloc_Node allocNode, Context thisRefContext) {
        if (allocNode instanceof ContextAlloc_Node) {
            ContextAlloc_Node osan = (ContextAlloc_Node)allocNode;        
            //if the this pointer has no context, then only add obj sens alloc nodes with no context beyond new expr
            if (thisRefContext instanceof EmptyContext) {
                return osan.noContext();
            } else if (thisRefContext instanceof ContextAlloc_Node) {
                return thisRefContext.equals((Context)allocNode);
            } else {
            	
                throw new RuntimeException("This filter has strange type of context on this ref: " + thisRefContext.getClass()); 
            }
        } else {
            //not an obj sens node, so if there is context in the this ref, then alloc node should not be added
            return thisRefContext instanceof EmptyContext;
        }
    }
    
     private boolean thisPtrFilterAdd(Var_Node pointer, Alloc_Node other, PTSetInternal addTo) {
         if (DruidOptions.kobjsens>0 && pointer.isThisPtr() && pointer instanceof ContextLocalVar_Node) {
             Context thisRefContext = ((ContextLocalVar_Node) pointer).context();
             if (thisPtrShouldAdd(other, thisRefContext)) 
                 return addTo.add(other);
             else
                 return false;
         }
         return addTo.add(other);
     }
    
    private boolean thisPtrFilterAddAll(final Var_Node pointer, 
                                     PTSetInternal other, 
                                     final PTSetInternal addTo) {
        // : removed call to prunePTSetForThisPtr to improve performance
        // PointsToSetInternal newSet = pag.prunePTSetForThisPtr(pointer, other);
        // return addTo.addAll(newSet, null);
        // : moved part of the test out of the loop for efficiency
        if (DruidOptions.kobjsens>0 && pointer.isThisPtr() && pointer instanceof ContextLocalVar_Node) {
            final Context thisPtrCtxt = ((ContextLocalVar_Node) pointer).context();
            return other.forall( new PTSetVisitor() {
                public final void visit( GNode n ) {
                    if (thisPtrShouldAdd((Alloc_Node)n, thisPtrCtxt))
                        if (addTo.add(n))
                            returnValue = true;
                }} );
        }
        return addTo.addAll(other, null);
    }
    
    protected WholeProgPAG pag;
    protected PTA pta;
    protected CallGraphBuilder cgb;
}