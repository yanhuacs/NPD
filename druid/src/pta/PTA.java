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

import static driver.DruidOptions.sparkOpts;

import driver.*;
import pag.WholeProgPAG;
import pag.MtdPAG;
import pag.builder.MtdPAGBuilder;
import pag.builder.GlobalPAGBuilder;
import pag.node.GNode;
import pag.node.alloc.Alloc_Node;
import pag.node.alloc.ContextAlloc_Node;
import pag.node.var.ContextLocalVar_Node;
import pag.node.var.FieldRef_Node;
import pag.node.var.GlobalVar_Node;
import pag.node.var.LocalVar_Node;
import pag.node.var.Var_Node;
import pta.context.EmptyContext;
import pta.context.ParameterizedMethod;
import pta.pts.EmptyPTSet;
import pta.pts.PTSetInternal;
import pta.pts.PTSetVisitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.Context;
import soot.Kind;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.spark.pag.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.SparkOptions;
import soot.toolkits.scalar.Pair;
import soot.util.queue.QueueReader;

public abstract class PTA implements PointsToAnalysis{
	protected WholeProgPAG pag;
	protected CallGraphBuilder cgb;
	protected QueueReader<ParameterizedMethod> reachablesReader;
	protected QueueReader<Edge> callEdges;
	public Map<InvokeExpr, SootMethod> callToMethod = new HashMap<InvokeExpr, SootMethod>();
	private Set<InvokeExpr> additionalVirtualCalls = new HashSet<InvokeExpr>();
	private Map<InvokeExpr, GNode> virtualCallsToReceivers = new HashMap<InvokeExpr, GNode>();
	
	/// Context-sensitive points-to analysis
    private Map<LocalVar_Node, HashMap<Context, ContextLocalVar_Node>> contextVarNodeMap = new HashMap<LocalVar_Node,HashMap<Context, ContextLocalVar_Node> >();
  	private Map<Alloc_Node, HashMap<Context, ContextAlloc_Node>> contextAllocNodeMap = new HashMap<Alloc_Node,HashMap<Context, ContextAlloc_Node> >();
  	/// Heap-sensitive points-to analysis
  	private Map<Context, ContextAlloc_Node> heapContextMap = new HashMap<Context,ContextAlloc_Node>(10000);
  	
  	public Map<LocalVar_Node, HashMap<Context, ContextLocalVar_Node>> getContextVarNodeMap(){
  		return contextVarNodeMap;
  	}
  	public Map<Alloc_Node, HashMap<Context, ContextAlloc_Node>> getContextAllocNodeMap(){
  		return contextAllocNodeMap;
  	}
  	public Map<Context, ContextAlloc_Node> getHeapContextMap(){
  		return heapContextMap;
  	}  	
  	
	public WholeProgPAG getPag() {return pag;}
	public CallGraphBuilder getCgb() {return cgb;}
	public CallGraph getCallGraph() {return cgb.getCallGraph();}
	
    public PTA() {
    	Scene.v().setPointsToAnalysis(this);
    	pag = new WholeProgPAG();
		cgb = createCallGraphBuilder();
		pag.setGlobalNodeFactory(new GlobalPAGBuilder(pag, this));
		reachablesReader = cgb.reachables.listener();
		callEdges = Scene.v().getCallGraph().listener();
	}

    protected abstract CallGraphBuilder createCallGraphBuilder();
    protected abstract Context heapSelector(Alloc_Node base, Context context);
	protected abstract Context selector(Alloc_Node receiverNode);
	
    public void buildPAG(){
		cgb.build();
		buildMPAGs();
		connectMPAGs();
    }

	protected void buildMPAGs(){
		while (reachablesReader.hasNext()) {
			ParameterizedMethod m = reachablesReader.next();
			MtdPAG mpag = MtdPAG.v(pag, m.method());
			mpag.build();
			addToPAG(mpag,m.context());
		}
	}

	protected void connectMPAGs(){
		while (callEdges.hasNext()) {
			Edge e = (Edge) callEdges.next();
			processCallEdge(e);
		}
	}
	/**
	 * Adds this method to the main PAG, with all VarNodes parameterized by cxt.
	 */
	private void addToPAG(MtdPAG mpag,Context cxt) {
		if (!mpag.getAddedContexts().add(cxt))
			return;
		QueueReader<GNode> reader = mpag.getInternalReader().clone();
		while (reader.hasNext()) {
			GNode src = (GNode) reader.next();
			src = parameterize(src, cxt);
			GNode dst = (GNode) reader.next();
			dst = parameterize(dst, cxt);
			pag.addEdge(src, dst);
		}
	}
	
	protected ContextLocalVar_Node parameterize(LocalVar_Node vn, Context varNodeParameter) {
		return makeContextVarNode(vn, varNodeParameter);
	}

	protected FieldRef_Node parameterize(FieldRef_Node frn, Context varNodeParameter) {
		return pag.makeFieldRefNode((Var_Node) parameterize(frn.getBase(), varNodeParameter), frn.getField());
	}
	
	protected ContextAlloc_Node parameterize(Alloc_Node node, Context context) {
		if (Config.v().addHeapContext(node))
			return makeContextAllocNode(node, context);
		else
			return makeContextAllocNode(node, EmptyContext.v());
	}

	public GNode parameterize(GNode n, Context varNodeParameter) {
		if (varNodeParameter == null)
			throw new RuntimeException("null context!!!");
		if (n instanceof LocalVar_Node)
			return parameterize((LocalVar_Node) n, varNodeParameter);
		if (n instanceof FieldRef_Node)
			return parameterize((FieldRef_Node) n, varNodeParameter);
		if (n instanceof Alloc_Node)
			return parameterize((Alloc_Node) n, varNodeParameter);
		if (n instanceof GlobalVar_Node)
			return n;
			//return parameterize((GlobalVarNode)n, varNodeParameter);
		throw new RuntimeException("cannot parameterize unknown node!!!");
		//return n;
	}
    /** Finds the ContextVarNode for base variable value and context
     * context, or returns null. */
    public ContextLocalVar_Node findContextVarNode( Local baseValue, Context context ) {
        LocalVar_Node base = pag.findLocalVarNode( baseValue );
        if ( base == null ) 
            return null;
        //the null context is just the local var node
        if (context == null)
            throw new RuntimeException("Context is null when tryin to get a context var node");
        //if context, then get the context sensitive node
    	HashMap<Context, ContextLocalVar_Node> contextMap = contextVarNodeMap.get(base);
    	if(contextMap!=null){
    		return contextMap.get(context);
        }
    	return null;
    }
    /** Finds or creates the ContextVarNode for base variable base and context, of type type. */
    public ContextLocalVar_Node makeContextVarNode( LocalVar_Node base, Context context ) {
    	HashMap<Context, ContextLocalVar_Node> contextMap = contextVarNodeMap.get(base);
    	if(contextMap==null){
        	contextMap = new HashMap<Context, ContextLocalVar_Node>();
        	contextVarNodeMap.put(base, contextMap);
        }
    	ContextLocalVar_Node cxtVarNode = contextMap.get(context);
    	if (cxtVarNode==null) {
    		cxtVarNode = new ContextLocalVar_Node( pag, base, context );
        	contextMap.put(context, cxtVarNode );
            pag.addNodeTag( cxtVarNode, base.getMethod() );
    	}
    	return cxtVarNode;
    }
    
    public ContextAlloc_Node makeContextAllocNode(Alloc_Node allocNode, Context context) {
        if (context == null)
            throw new RuntimeException("Context should not be null when getting context for insensitive node.");
        ContextAlloc_Node contextAllocNode;
        HashMap<Context, ContextAlloc_Node> contextMap = contextAllocNodeMap.get(allocNode);
        if(contextMap==null){
            	contextMap = new HashMap<Context, ContextAlloc_Node>();
            	contextAllocNodeMap.put(allocNode, contextMap);
        }
        contextAllocNode=contextMap.get(context);
        if (contextAllocNode==null) {
        	Context newcxt = heapSelector(allocNode, context);
        	contextAllocNode = heapContextMap.get(newcxt);
            if (contextAllocNode == null) {
            	contextAllocNode = new ContextAlloc_Node(pag, allocNode, newcxt);
            	heapContextMap.put(newcxt, contextAllocNode);
            }   
            contextMap.put(context, contextAllocNode );
        }
        pag.addNodeTag(contextAllocNode, allocNode.getMethod());
        return contextAllocNode;
    }
    
	final protected void processCallEdge(Edge e) {
		if (!e.passesParameters())
			return;
		
		MtdPAG srcmpag = MtdPAG.v(pag, e.src());
		MtdPAG tgtmpag = MtdPAG.v(pag, e.tgt());
		
		if (e.isExplicit() || e.kind() == Kind.THREAD) {
			handleInvoke(srcmpag, tgtmpag, e);
		} else {
			if (e.kind() == Kind.PRIVILEGED) {
				handlePriviledgedEdge(srcmpag,tgtmpag,e);
			} else if (e.kind() == Kind.FINALIZE) {
				handleFinalizeMethod(srcmpag,tgtmpag,e);
			} else if (e.kind() == Kind.NEWINSTANCE) {
				handleNewInstance(srcmpag,tgtmpag,e);
			} else if (e.kind() == Kind.REFL_INVOKE) {
				handleReflectionInvoke(srcmpag,tgtmpag,e);
			} else if (e.kind() == Kind.REFL_CLASS_NEWINSTANCE || e.kind() == Kind.REFL_CONSTR_NEWINSTANCE) {
				handleReflNewInstance(srcmpag,tgtmpag,e);
			} else {
				throw new RuntimeException("Unhandled edge " + e);
			}
		}
	}

	/**
	 * Adds method target as a possible target of the invoke expression in s. If
	 * target is null, only creates the nodes for the call site, without
	 * actually connecting them to any target method.
	 **/
	final private void handleInvoke(MtdPAG srcmpag, MtdPAG tgtmpag, Edge e) {
		Stmt s = (Stmt) e.srcUnit(); 
		Context srcContext = e.srcCtxt();
		Context tgtContext = e.tgtCtxt();
		MtdPAGBuilder srcnf = srcmpag.nodeFactory();
		MtdPAGBuilder tgtnf = tgtmpag.nodeFactory();
		InvokeExpr ie = s.getInvokeExpr();
		// boolean virtualCall = callAssigns.containsKey(ie);
		boolean virtualCall = additionalVirtualCalls.contains(ie) || callToMethod.containsKey(ie);
		int numArgs = ie.getArgCount();
		for (int i = 0; i < numArgs; i++) {
			Value arg = ie.getArg(i);
			if (!(arg.getType() instanceof RefLikeType))
				continue;
			if (arg instanceof NullConstant)
				continue;

			GNode argNode = srcnf.getNode(arg);
			argNode = parameterize(argNode, srcContext);
			argNode = argNode.getReplacement();
			
			GNode parm = tgtnf.caseParm(i);
			parm = parameterize(parm, tgtContext);
			parm = parm.getReplacement();

			pag.addEdge(argNode, parm);
			// : temporary hack to reduce memory-inefficiency
			// Pair pval = addInterproceduralAssignment(argNode, parm, e);
			// callAssigns.put(ie, pval);
			callToMethod.put(ie, srcmpag.getMethod());

		}
		if (ie instanceof InstanceInvokeExpr) {
			InstanceInvokeExpr iie = (InstanceInvokeExpr) ie;

			GNode baseNode = srcnf.getNode(iie.getBase());
			baseNode = parameterize(baseNode, srcContext);
			baseNode = baseNode.getReplacement();

			GNode thisRef = tgtnf.caseThis();
			thisRef = parameterize(thisRef, tgtContext);
			thisRef = thisRef.getReplacement();
			pag.addEdge(baseNode, thisRef);
			// : temporary hack to reduce memory-inefficiency
			// Pair pval = addInterproceduralAssignment(baseNode, thisRef, e);
			// callAssigns.put(ie, pval);
			callToMethod.put(ie, srcmpag.getMethod());
			if (virtualCall && !virtualCallsToReceivers.containsKey(ie)) {
				virtualCallsToReceivers.put(ie, baseNode);
			}
		}
		if (s instanceof AssignStmt) {
			Value dest = ((AssignStmt) s).getLeftOp();
			if (dest.getType() instanceof RefLikeType && !(dest instanceof NullConstant)) {

				GNode destNode = srcnf.getNode(dest);
				destNode = parameterize(destNode, srcContext);
				destNode = destNode.getReplacement();

				GNode retNode = tgtnf.caseRet();
				retNode = parameterize(retNode, tgtContext);
				retNode = retNode.getReplacement();

				pag.addEdge(retNode, destNode);
				// : temporary hack to reduce memory-inefficiency
				// Pair pval = addInterproceduralAssignment( retNode, destNode,
				// e );
				// callAssigns.put(ie, pval);
				callToMethod.put(ie, srcmpag.getMethod());
			}
		}
	}
	
	private void handlePriviledgedEdge(MtdPAG srcmpag, MtdPAG tgtmpag, Edge e){
		// Flow from first parameter of doPrivileged() invocation
		// to this of target, and from return of target to the
		// return of doPrivileged()
		InvokeExpr ie = e.srcStmt().getInvokeExpr();

		GNode parm = srcmpag.nodeFactory().getNode(ie.getArg(0));
		parm = parameterize(parm, e.srcCtxt());
		parm = parm.getReplacement();

		GNode thiz = tgtmpag.nodeFactory().caseThis();
		thiz = parameterize(thiz, e.tgtCtxt());
		thiz = thiz.getReplacement();

		pag.addEdge(parm, thiz);
		// : temporary hack to reduce memory-inefficiency
		// pval = addInterproceduralAssignment(parm, thiz, e);
		// callAssigns.put(ie, pval);
		callToMethod.put(ie, srcmpag.getMethod());

		if (e.srcUnit() instanceof AssignStmt) {
			AssignStmt as = (AssignStmt) e.srcUnit();

			GNode ret = tgtmpag.nodeFactory().caseRet();
			ret = parameterize(ret, e.tgtCtxt());
			ret = ret.getReplacement();

			GNode lhs = srcmpag.nodeFactory().getNode(as.getLeftOp());
			lhs = parameterize(lhs, e.srcCtxt());
			lhs = lhs.getReplacement();

			pag.addEdge(ret, lhs);
			// : temporary hack to reduce memory-inefficiency
			// pval = addInterproceduralAssignment(ret, lhs, e);
			// callAssigns.put(ie, pval);
			callToMethod.put(ie, srcmpag.getMethod());
		}
	}

	private void handleFinalizeMethod(MtdPAG srcmpag, MtdPAG tgtmpag, Edge e){
		GNode srcThis = srcmpag.nodeFactory().caseThis();
		srcThis = parameterize(srcThis, e.srcCtxt());
		srcThis = srcThis.getReplacement();

		GNode tgtThis = tgtmpag.nodeFactory().caseThis();
		tgtThis = parameterize(tgtThis, e.tgtCtxt());
		tgtThis = tgtThis.getReplacement();

		pag.addEdge(srcThis, tgtThis);
	}
	
	
	private void handleNewInstance(MtdPAG srcmpag, MtdPAG tgtmpag, Edge e){
		Stmt s = (Stmt) e.srcUnit();
		InstanceInvokeExpr iie = (InstanceInvokeExpr) s.getInvokeExpr();

		GNode cls = srcmpag.nodeFactory().getNode(iie.getBase());
		cls = parameterize(cls, e.srcCtxt());
		cls = cls.getReplacement();
		GNode newObject = pag.GlobalNodeFactory().caseNewInstance((Var_Node) cls);

		GNode initThis = tgtmpag.nodeFactory().caseThis();
		initThis = parameterize(initThis, e.tgtCtxt());
		initThis = initThis.getReplacement();

		pag.addEdge(newObject, initThis);
		if (s instanceof AssignStmt) {
			AssignStmt as = (AssignStmt) s;
			GNode asLHS = srcmpag.nodeFactory().getNode(as.getLeftOp());
			asLHS = parameterize(asLHS, e.srcCtxt());
			asLHS = asLHS.getReplacement();
			pag.addEdge(newObject, asLHS);
		}

		// : temporary hack to reduce memory-inefficiency
		// pval = addInterproceduralAssignment(newObject, initThis, e);
		// callAssigns.put(s.getInvokeExpr(), pval);
		callToMethod.put(s.getInvokeExpr(), srcmpag.getMethod());
	}
	
	private void handleReflectionInvoke(MtdPAG srcmpag, MtdPAG tgtmpag, Edge e){
		// Flow (1) from first parameter of invoke(..) invocation
		// to this of target, (2) from the contents of the second
		// (array) parameter
		// to all parameters of the target, and (3) from return of
		// target to the
		// return of invoke(..)

		// (1)
		InvokeExpr ie = e.srcStmt().getInvokeExpr();

		Value arg0 = ie.getArg(0);
		// if "null" is passed in, omit the edge
		if (arg0 != NullConstant.v()) {
			GNode parm0 = srcmpag.nodeFactory().getNode(arg0);
			parm0 = parameterize(parm0, e.srcCtxt());
			parm0 = parm0.getReplacement();

			GNode thiz = tgtmpag.nodeFactory().caseThis();
			thiz = parameterize(thiz, e.tgtCtxt());
			thiz = thiz.getReplacement();

			pag.addEdge(parm0, thiz);
			// : temporary hack to reduce memory-inefficiency
			// pval = addInterproceduralAssignment(parm0, thiz, e);
			// callAssigns.put(ie, pval);
			callToMethod.put(ie, srcmpag.getMethod());
		}

		// (2)
		Value arg1 = ie.getArg(1);
		SootMethod tgt = e.getTgt().method();
		// if "null" is passed in, or target has no parameters, omit the edge
		if (arg1 != NullConstant.v() && tgt.getParameterCount() > 0) {
			GNode parm1 = srcmpag.nodeFactory().getNode(arg1);
			parm1 = parameterize(parm1, e.srcCtxt());
			parm1 = parm1.getReplacement();
			FieldRef_Node parm1contents = pag.makeFieldRefNode((Var_Node) parm1, ArrayElement.v());

			for (int i = 0; i < tgt.getParameterCount(); i++) {
				// if no reference type, create no edge
				if (!(tgt.getParameterType(i) instanceof RefLikeType))
					continue;

				GNode tgtParmI = tgtmpag.nodeFactory().caseParm(i);
				tgtParmI = parameterize(tgtParmI, e.tgtCtxt());
				tgtParmI = tgtParmI.getReplacement();

				pag.addEdge(parm1contents, tgtParmI);
				// : temporary hack to reduce memory-inefficiency
				// pval = addInterproceduralAssignment(parm1contents,tgtParmI, e);
				// callAssigns.put(ie, pval);
				additionalVirtualCalls.add(ie);
			}
		}

		// (3) only create return edge if we are actually assigning the
		// return value and the return type of the callee is actually a reference type
		if (e.srcUnit() instanceof AssignStmt && (tgt.getReturnType() instanceof RefLikeType)) {
			AssignStmt as = (AssignStmt) e.srcUnit();

			GNode ret = tgtmpag.nodeFactory().caseRet();
			ret = parameterize(ret, e.tgtCtxt());
			ret = ret.getReplacement();

			GNode lhs = srcmpag.nodeFactory().getNode(as.getLeftOp());
			lhs = parameterize(lhs, e.srcCtxt());
			lhs = lhs.getReplacement();

			pag.addEdge(ret, lhs);
			// : temporary hack to reduce memory-inefficiency
			// pval = addInterproceduralAssignment(ret, lhs, e);
			// callAssigns.put(ie, pval);
			additionalVirtualCalls.add(ie);
		}
	}
	
	private void handleReflNewInstance(MtdPAG srcmpag, MtdPAG tgtmpag, Edge e) {
		// (1) create a fresh node for the new object
		// (2) create edge from this object to "this" of the constructor
		// (3) if this is a call to Constructor.newInstance and not
		// Class.newInstance,
		// create edges passing the contents of the arguments array of
		// the call
		// to all possible parameters of the target
		// (4) if we are inside an assign statement,
		// assign the fresh object from (1) to the LHS of the assign
		// statement
		Stmt s = (Stmt) e.srcUnit();
		InstanceInvokeExpr iie = (InstanceInvokeExpr) s.getInvokeExpr();

		// (1)
		GNode cls = srcmpag.nodeFactory().getNode(iie.getBase());
		cls = parameterize(cls, e.srcCtxt());
		cls = cls.getReplacement();
		if (cls instanceof ContextLocalVar_Node)
			cls = pag.findLocalVarNode(((Var_Node) cls).getVariable());

		Var_Node newObject = pag.makeGlobalVarNode(cls, RefType.v("java.lang.Object"));
		SootClass tgtClass = e.getTgt().method().getDeclaringClass();
		RefType tgtType = tgtClass.getType();
		Alloc_Node site = pag.makeAllocNode(new Pair<GNode, SootClass>(cls, tgtClass), tgtType, null);
		pag.addEdge(site, newObject);

		// (2)
		GNode initThis = tgtmpag.nodeFactory().caseThis();
		initThis = parameterize(initThis, e.tgtCtxt());
		initThis = initThis.getReplacement();
		pag.addEdge(newObject, initThis);
		// : temporary hack to reduce memory-inefficiency
		// addInterproceduralAssignment(newObject, initThis, e);

		// (3)
		if (e.kind() == Kind.REFL_CONSTR_NEWINSTANCE) {
			Value arg = iie.getArg(0);
			SootMethod tgt = e.getTgt().method();
			// if "null" is passed in, or target has no parameters, omit the edge
			if (arg != NullConstant.v() && tgt.getParameterCount() > 0) {
				GNode parm0 = srcmpag.nodeFactory().getNode(arg);
				parm0 = parameterize(parm0, e.srcCtxt());
				parm0 = parm0.getReplacement();
				FieldRef_Node parm1contents = pag.makeFieldRefNode((Var_Node) parm0, ArrayElement.v());

				for (int i = 0; i < tgt.getParameterCount(); i++) {
					// if no reference type, create no edge
					if (!(tgt.getParameterType(i) instanceof RefLikeType))
						continue;

					GNode tgtParmI = tgtmpag.nodeFactory().caseParm(i);
					tgtParmI = parameterize(tgtParmI, e.tgtCtxt());
					tgtParmI = tgtParmI.getReplacement();

					pag.addEdge(parm1contents, tgtParmI);
					// : temporary hack to reduce memory-inefficiency
					// pval =
					// addInterproceduralAssignment(parm1contents,
					// tgtParmI, e);
					// callAssigns.put(iie, pval);
					additionalVirtualCalls.add(iie);
				}
			}
		}

		// (4)
		if (s instanceof AssignStmt) {
			AssignStmt as = (AssignStmt) s;
			GNode asLHS = srcmpag.nodeFactory().getNode(as.getLeftOp());
			asLHS = parameterize(asLHS, e.srcCtxt());
			asLHS = asLHS.getReplacement();
			pag.addEdge(newObject, asLHS);
		}

		// : temporary hack to reduce memory-inefficiency
		// pval = addInterproceduralAssignment(newObject, initThis, e);
		// callAssigns.put(s.getInvokeExpr(), pval);
		callToMethod.put(s.getInvokeExpr(), srcmpag.getMethod());
	}
	
	
	/** Returns the set of objects pointed to by variable l. */
    public PointsToSet reachingObjects( Local l ) {
        LocalVar_Node n = pag.findLocalVarNode( l );
        if( n == null ) {
            return EmptyPTSet.v();
        }
        //find all context nodes, and collect their answers

        final PTSetInternal ret = pag.setFactory.newSet(l.getType(), pag );

        //just in case is no context
        ret.addAll(n.getP2Set(), null);
    	HashMap<Context, ContextLocalVar_Node> contextMap = contextVarNodeMap.get(n);
        if (contextMap != null) {
            //add all context nodes
            for (Map.Entry<Context,ContextLocalVar_Node> entry : contextMap.entrySet()) {
                ret.addAll((PTSetInternal)reachingObjects((Context) entry.getKey(), l), null);
            }
        }
        return ret;
    }
    /** Returns the set of objects pointed to by variable l in context c. */
    public PointsToSet reachingObjects( Context c, Local l ) {

        Var_Node n = findContextVarNode( l, c );
        if( n == null ) {
            return EmptyPTSet.v();
        }
        return n.getP2Set();
    }
    /** Returns the set of objects pointed to by static field f. */
    public PointsToSet reachingObjects( SootField f ) {
        if( !f.isStatic() )
            throw new RuntimeException( "The parameter f must be a *static* field." );
        Var_Node n = pag.findGlobalVarNode( f );
        if( n == null ) {
            return EmptyPTSet.v();
        }
        return n.getP2Set();
    }
    /** Returns the set of objects pointed to by instance field f
     * of the objects in the PointsToSet s. */
    public PointsToSet reachingObjects( PointsToSet s, final SootField f ) {
        if( f.isStatic() )
            throw new RuntimeException( "The parameter f must be an *instance* field." );

        return reachingObjectsInternal( s, f );
    }
    /** Returns the set of objects pointed to by instance field f
     * of the objects pointed to by l. */
    public PointsToSet reachingObjects( Local l, SootField f ) {
        return reachingObjects( reachingObjects(l), f );
    }

    /** Returns the set of objects pointed to by instance field f
     * of the objects pointed to by l in context c. */
    public PointsToSet reachingObjects( Context c, Local l, SootField f ) {
        return reachingObjects( reachingObjects(c, l), f );
    }
    /** Returns the set of objects pointed to by elements of the arrays
     * in the PointsToSet s. */
    public PointsToSet reachingObjectsOfArrayElement( PointsToSet s ) {
        return reachingObjectsInternal( s, ArrayElement.v() );
    }

    private PointsToSet reachingObjectsInternal( PointsToSet s, final SparkField f ) {
        if( sparkOpts.field_based() || sparkOpts.vta() ) {
            Var_Node n = pag.findGlobalVarNode( f );
            if( n == null ) {
                return EmptyPTSet.v();
            }
            return n.getP2Set();
        }
        if( sparkOpts.propagator() == SparkOptions.propagator_alias ) {
            throw new RuntimeException( "The alias edge propagator does not compute points-to information for instance fields! Use a different propagator." );
        }
        PTSetInternal bases = (PTSetInternal) s;
        final PTSetInternal ret = pag.setFactory.newSet( 
            (f instanceof SootField) ? ((SootField)f).getType() : null, pag );
        bases.forall( new PTSetVisitor() {
            public final void visit( GNode n ) {
                GNode nDotF = ((Alloc_Node) n).dot( f );
                if(nDotF != null) ret.addAll( nDotF.getP2Set(), null );
            }} );
        return ret;
    }
}
