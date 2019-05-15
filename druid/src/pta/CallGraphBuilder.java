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

import java.util.*;

import driver.SootUtils;
import pag.node.GNode;
import pag.node.alloc.Alloc_Node;
import pag.node.var.Var_Node;
import pta.context.EmptyContext;
import pta.context.ParameterizedMethod;
import pta.pts.PTSetInternal;
import pta.pts.PTSetVisitor;
import reflection.DefaultReflectionModel;
import reflection.GlobalVariable;
import reflection.ReflectionModel;
import reflection.ReflectionOptions;
import reflection.TraceBasedReflectionModel;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.Filter;
import soot.jimple.toolkits.callgraph.Targets;
import soot.options.CGOptions;
import soot.options.Options;
import soot.util.NumberedString;
import soot.util.queue.ChunkedQueue;
import soot.util.queue.QueueReader;
import util.VirtualInvokeSite;

/**
 * Models the call graph.
 * 
 * @author Ondrej Lhotak
 * @DA modified by Jingbo and Yulei
 */
public abstract class CallGraphBuilder {
	public CallGraph getCICallGraph() {
		final CallGraph cicg = new CallGraph();
		cg.forEach(e->{
			cicg.addEdge(new Edge(e.src(), e.srcUnit(), e.tgt(), e.kind()));
		});
		return cicg;
	}
	
	/** context-insensitive stuff */
	public final CGOptions options= new CGOptions(PhaseOptions.v().getPhaseOptions("cg"));
	protected final ReflectionModel reflectionModel;
	protected final NumberedString sigFinalize = Scene.v().getSubSigNumberer().findOrAdd("void finalize()");
	protected final NumberedString sigStart = Scene.v().getSubSigNumberer().findOrAdd("void start()");
	protected final NumberedString sigRun = Scene.v().getSubSigNumberer().findOrAdd("void run()");
	protected final RefType clRunnable = RefType.v("java.lang.Runnable");

	/** context-sensitive stuff */
	public ReachableParaMethods getReachableParaMethods() {return reachables;}
	protected ReachableParaMethods reachables;
	protected QueueReader<ParameterizedMethod> worklist;
	protected final CallGraph cg= new CallGraph();
	// initialize the receiver to sites map with the number of locals * an estimate for the number of contexts per methods
	public HashMap<Var_Node, List<VirtualInvokeSite>> getReceiverToSitesMap() {return receiverToSites;}
	protected final HashMap<Var_Node, List<VirtualInvokeSite>> receiverToSites = new HashMap<Var_Node, List<VirtualInvokeSite>>(Scene.v().getLocalNumberer().size());
	protected final ChunkedQueue<SootMethod> targetsQueue = new ChunkedQueue<SootMethod>();
	protected final QueueReader<SootMethod> targets = targetsQueue.reader();

	public CallGraph getCallGraph() {return cg;}
	public CallGraphBuilder() {
		if (ReflectionOptions.v().isInferenceReflectionModel()) {
			System.out.println("[InferenceReflectionModel] Inference reflection resolution is unavailable for objectsensitive analysis.");
			reflectionModel = GlobalVariable.v().getInferenceReflectionModel(this);
		} else if (options.reflection_log() == null || options.reflection_log().length() == 0) {
			reflectionModel = new DefaultReflectionModel();
		} else {
			reflectionModel = new TraceBasedReflectionModel(this);
		}
		Scene.v().setCallGraph(cg);
		List<ParameterizedMethod> withNoContext = new LinkedList<ParameterizedMethod>();
		for (SootMethod method : SootUtils.getEntryPoints()) {
			withNoContext.add(ParameterizedMethod.v(method, EmptyContext.v()));
		}
		reachables = new ReachableParaMethods(cg, withNoContext);
		worklist = reachables.listener();
	}
	
	//@{ overwritten by child classes
	/**Virtual&Special*/
	protected abstract void connectVirtualEdge(Var_Node vn, Type type, Alloc_Node n);
	protected abstract Var_Node getReceiverVarNode(Local receiver, ParameterizedMethod m);
	public abstract void addStaticEdge(ParameterizedMethod caller, Unit callStmt, SootMethod callee, Kind kind);		
	public abstract void addVirtualEdge(ParameterizedMethod caller, Unit callStmt, SootMethod callee, Kind kind, Context context);
	//@}
	
	public void build() {
		while(true){
			if(!worklist.hasNext()){
				reachables.update();
				if(!worklist.hasNext())
					return;
			}
			ParameterizedMethod momc = worklist.next();
			SootMethod method = momc.method();
			if (method.isNative() || method.isPhantom()) 
				continue;
			try {
				Body b = method.retrieveActiveBody();
				getImplicitTargets(momc);
				handleInvoke(momc, b);
			} catch (Exception e) {
				System.out.println("cannot retrieve method body: " + method);
			}
		}
	}

	private void handleInvoke(ParameterizedMethod m, Body b) {
		for (Iterator<Unit> sIt = b.getUnits().iterator(); sIt.hasNext();) {
			final Stmt s = (Stmt) sIt.next();
			if (s.containsInvokeExpr()) {
				InvokeExpr ie = s.getInvokeExpr();

				if (ie instanceof InstanceInvokeExpr) {
					InstanceInvokeExpr iie = (InstanceInvokeExpr) ie;
					Local receiver = (Local) iie.getBase();
					Var_Node recNode = getReceiverVarNode(receiver, m);
					NumberedString subSig = iie.getMethodRef().getSubSignature();
					recordVirtualCallSite(s, m, recNode, iie, subSig, Edge.ieToKind(iie));
					if (subSig == sigStart) {
						recordVirtualCallSite(s, m, recNode, iie, sigRun, Kind.THREAD);
					}
				} else {
					SootMethod tgt = ie.getMethod();
					if (tgt != null) {
						// static invoke or dynamic invoke
						addStaticEdge(m, s, tgt, Edge.ieToKind(ie));
					} else {
						if (!Options.v().ignore_resolution_errors()) {
							throw new InternalError("Unresolved target " + ie.getMethod()
									+ ". Resolution error should have occured earlier.");
						}
					}
				}
			}
		}
	}

	
	
	/* End of public methods. */
	private void recordVirtualCallSite(Stmt s, ParameterizedMethod m, Var_Node receiver, InstanceInvokeExpr iie,NumberedString subSig, Kind kind) {
		if (receiver == null)
			throw new RuntimeException("Null receiver node in OnFlyCallGraphBuilder");

		List<VirtualInvokeSite> sites = (List<VirtualInvokeSite>) receiverToSites.get(receiver);
		if (sites == null) {
			receiverToSites.put(receiver, sites = new ArrayList<VirtualInvokeSite>());
		}
		sites.add(new VirtualInvokeSite(s, m, iie, subSig, kind));
	}

	private void getImplicitTargets(ParameterizedMethod source) {
		SootMethod sourceMethod = source.method();
		if (sourceMethod.method().isNative() || sourceMethod.isPhantom())
			return;
		Body b = sourceMethod.retrieveActiveBody();
		for (Iterator<Unit> sIt = b.getUnits().iterator(); sIt.hasNext();) {
			final Stmt s = (Stmt) sIt.next();
			if (s.containsInvokeExpr()) {
				InvokeExpr ie = s.getInvokeExpr();
				reflectionModel.handleInvokeExpr(ie, source, s);
				if (ie instanceof StaticInvokeExpr) {
					SootClass cl = ie.getMethodRef().declaringClass();
					for (SootMethod clinit : EntryPoints.v().clinitsOf(cl)) {
						addStaticEdge(source, s, clinit, Kind.CLINIT);
					}
				}
			}
			if (s.containsFieldRef()) {
				FieldRef fr = s.getFieldRef();
				if (fr instanceof StaticFieldRef) {
					SootClass cl = fr.getFieldRef().declaringClass();
					for (SootMethod clinit : EntryPoints.v().clinitsOf(cl)) {
						addStaticEdge(source, s, clinit, Kind.CLINIT);
					}
				}
			}
			if (s instanceof AssignStmt) {
				Value rhs = ((AssignStmt) s).getRightOp();
				if (rhs instanceof NewExpr) {
					NewExpr r = (NewExpr) rhs;
					SootClass cl = r.getBaseType().getSootClass();
					for (SootMethod clinit : EntryPoints.v().clinitsOf(cl)) {
						addStaticEdge(source, s, clinit, Kind.CLINIT);
					}
				} else if (rhs instanceof NewArrayExpr || rhs instanceof NewMultiArrayExpr) {
					Type t = rhs.getType();
					if (t instanceof ArrayType)
						t = ((ArrayType) t).baseType;
					if (t instanceof RefType) {
						SootClass cl = ((RefType) t).getSootClass();
						for (SootMethod clinit : EntryPoints.v().clinitsOf(cl)) {
							addStaticEdge(source, s, clinit, Kind.CLINIT);
						}
					}
				}
			}
		}
	}

	public void updateCallGraph(final Var_Node vn) {
		if (!(vn.getVariable() instanceof Local))
			return;

		PTSetInternal p2set = vn.getP2Set().getNewSet();
		if (receiverToSites.get(vn) != null) {
			p2set.forall(new PTSetVisitor() {
				public final void visit(GNode n) {
					connectVirtualEdge(vn, n.getType(), (Alloc_Node) n);
				}
			});
		}
		reflectionModel.updateNode(vn, p2set);
	}
	
	public static class ReachableParaMethods
	{ 
	    private CallGraph cg;
	    private Iterator<Edge> edgeSource;
	    private final ChunkedQueue<ParameterizedMethod> reachables = new ChunkedQueue<ParameterizedMethod>();
	    private final Set<ParameterizedMethod> set = new HashSet<ParameterizedMethod>();
	    private QueueReader<ParameterizedMethod> unprocessedMethods;
	    private final QueueReader<ParameterizedMethod> allReachables = reachables.reader();
	    private Filter filter;
	    public ReachableParaMethods( CallGraph graph, Iterator<ParameterizedMethod> entryPoints ) {
	        this( graph, entryPoints, null );
	    }
	    public ReachableParaMethods( CallGraph graph, Iterator<ParameterizedMethod> entryPoints, Filter filter ) {
	        this.filter = filter;
	        this.cg = graph;
	        addMethods( entryPoints );
	        unprocessedMethods = reachables.reader();
	        this.edgeSource = graph.listener();
	        if( filter != null ) this.edgeSource = filter.wrap( this.edgeSource );
	    }
	    public ReachableParaMethods( CallGraph graph, Collection<ParameterizedMethod> entryPoints ) {
	    	this(graph, entryPoints.iterator());
	    }
	    private void addMethods( Iterator<?extends MethodOrMethodContext> methods ) {
	        while( methods.hasNext() )
	            addMethod((ParameterizedMethod) methods.next() );
	    }
	    private void addMethod( ParameterizedMethod m ) {
	            if( set.add( m ) ) {
	                reachables.add( m );
	            }
	    }
	    /** Causes the QueueReader objects to be filled up with any methods
	     * that have become reachable since the last call. */
	    public void update() {
	        while(edgeSource.hasNext()) {
	            Edge e = edgeSource.next();
	            if( set.contains( e.getSrc() ) ) addMethod( (ParameterizedMethod) e.getTgt() );
	        }
	        while(unprocessedMethods.hasNext()) {
	        	ParameterizedMethod m = unprocessedMethods.next();
	            Iterator<Edge> targets = cg.edgesOutOf( m );
	            if( filter != null ) targets = filter.wrap( targets );
	            addMethods( new Targets( targets ) );
	        }
	    }
	    /** Returns a QueueReader object containing all methods found reachable
	     * so far, and which will be informed of any new methods that are later
	     * found to be reachable. */
	    public QueueReader<ParameterizedMethod> listener() {
	        return allReachables.clone();
	    }
	    /** Returns a QueueReader object which will contain ONLY NEW methods
	     * which will be found to be reachable, but not those that have already
	     * been found to be reachable.
	     */
	    public QueueReader<ParameterizedMethod> newListener() {
	        return reachables.reader();
	    }
	    /** Returns true iff method is reachable. */
	    public boolean contains( ParameterizedMethod m ) {
	        return set.contains( m );
	    }
	    /** Returns the number of methods that are reachable. */
	    public int size() {
	    	return set.size();
	    }
	}
}
