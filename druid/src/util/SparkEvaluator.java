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

package util;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import driver.Config;
import driver.DruidOptions;
import pag.WholeProgPAG;
import pag.node.GNode;
import pag.node.alloc.AllocDotField_Node;
import pag.node.alloc.Alloc_Node;
import pag.node.var.ContextLocalVar_Node;
import pag.node.var.FieldRef_Node;
import pag.node.var.GlobalVar_Node;
import pag.node.var.LocalVar_Node;
import pag.node.var.Var_Node;
import pta.PTA;
import pta.context.ParameterizedMethod;
import pta.pts.PTSetInternal;
import pta.pts.PTSetVisitor;
import soot.Context;
import soot.G;
import soot.Local;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.queue.QueueReader;

/**
 * Gather stats on the performance and precision of a PTA run.
 * 
 * Each new run of Spark will over-write stats
 * 
 * - Wall time (sec) - Memory (max, current) before and after - Reachable
 * methods (context and no-context) - Total reachable casts - Reachable casts
 * that may fail - Call graph edges - Context sensitive call graph edges - Total
 * reachable virtual call sites - Polymorphic virtual call sites (sites with >1
 * target methods) - Number of pointers (local and global) - Total points to
 * sets size (local and global) context insensitive (convert to alloc site)
 *
 */
public class SparkEvaluator {
	private static final int STATLENGTH=50;
	private static final int GB = 1024 * 1024 * 1024;
	/** static singleton */
	private static SparkEvaluator v;
	private Date startTime;
	private StringBuffer report;
	
	/** all method reachable from the harness main */
	private Set<SootMethod> reachableMethods;
	/** add context+methods that are reachable */
	private Set<ParameterizedMethod> reachableParameterizedMethods;

	private PTA pta;
	private WholeProgPAG pag;
	private boolean fails;
	private RefType exceptionType;

	private SparkEvaluator() {
		report = new StringBuffer();
		exceptionType = RefType.v("java.lang.Throwable");
	}

	public static SparkEvaluator v() {
		if (v == null)
			v = new SparkEvaluator();
		return v;
	}

	public static void reset() {
		v = null;
	}

	/**
	 * Note the start of a pta run.
	 */
	public void begin() {
		Runtime runtime = Runtime.getRuntime();// Getting the runtime reference from system
		addLine(" ====== Memory Usage ======");
		addLine(makeUp("Used Memory Before:") + (runtime.totalMemory() - runtime.freeMemory()) / GB + " GB");// Print used memory
		addLine(makeUp("Free Memory Before:") + runtime.freeMemory() / GB + " GB");// Print free memory
		addLine(makeUp("Total Memory Before:") + runtime.totalMemory() / GB + " GB");// Print total available memory
		addLine(makeUp("Max Memory Before:") + runtime.maxMemory() / GB + " GB");// Print Maximum available memory
		addLine(makeUp("K object depth:") + DruidOptions.kobjsens
				+ (DruidOptions.extraArrayContext ? " extra-array-context" : "")
				+ (DruidOptions.typesForContext ? " types-for-context-gt1" : ""));
		
		startTime = new Date();// get current date time with Date()
	}

	private String makeUp(String string) {
		final String makeUpString = " ";
		String ret = "";
		for (int i = 0; i < STATLENGTH - string.length(); i++) {
			ret += makeUpString;
		}
		return string + ret;
	}

	/**
	 * Note the end of a pta run.
	 */
	public void end() {
		// done with processing
		Date endTime = new Date();
		pta = (PTA) Scene.v().getPointsToAnalysis();
		pag = pta.getPag();
		long elapsedTime = endTime.getTime() - startTime.getTime();
		addLine(makeUp("Time (sec):") + (((double) elapsedTime) / 1000.0));
		// memory stats
		Runtime runtime = Runtime.getRuntime();// Getting the runtime reference from system
		addLine(makeUp("Used Memory After:") + (runtime.totalMemory() - runtime.freeMemory()) / GB + " GB");// Print used memory
		addLine(makeUp("Free Memory After:") + runtime.freeMemory() / GB + " GB");// Print free memory
		addLine(makeUp("Total Memory After:") + runtime.totalMemory() / GB + " GB");// Print total available memory
		addLine(makeUp("Max Memory After:") + runtime.maxMemory() / GB + " GB");// Print Maximum available memory
		addLine(" ====== Call Graph ======");
		callGraphProcessing();
		addLine(" ====== Statements ======");
		stmtProcessing();
		addLine(" ====== Nodes ======");
		nodeProcessing();
		countAllocSites();
		addLine(" ====== Assignments ======");
		asmtProcessing();
		addLine(" ====== Classes ======");
		clzProcessing();
	}

	private void countAllocSites() {
		addLine(makeUp("#Alloc site: ") + pag.getAllocNodes().size());//TODO
		addLine(makeUp("#heap objects:") + pta.getHeapContextMap().size());
	}

	private void callGraphProcessing() {
		CallGraph callGraph = Scene.v().getCallGraph();
		// fill reachable methods map
		reachableMethods = new LinkedHashSet<SootMethod>();
		reachableParameterizedMethods = new LinkedHashSet<ParameterizedMethod>();
		Set<ParameterizedMethod> reachableAppParameterizedMethods= new LinkedHashSet<ParameterizedMethod>();
		Set<SootMethod> reachableAppMethods=new LinkedHashSet<SootMethod>();
		
		Set<InsensEdge> insEdges = new HashSet<InsensEdge>();
		int CSStatic = 0;
		int CIStatic = 0;
		 
		for(QueueReader<ParameterizedMethod> qr= pta.getCgb().getReachableParaMethods().listener();qr.hasNext();){
			final ParameterizedMethod pm = qr.next();
			final SootMethod m=pm.method();
			reachableParameterizedMethods.add(pm);
			reachableMethods.add(m);
			if(Config.v().isAppClass(m.getDeclaringClass())){
				reachableAppParameterizedMethods.add(pm);
				reachableAppMethods.add(pm.method());
			}
			
			for(Iterator<Edge> iterator = callGraph.edgesInto(pm);iterator.hasNext();){
				Edge e = iterator.next();
				if (e.isStatic())
					CSStatic++;
				if (insEdges.add(new InsensEdge(e)) && e.isStatic())
					CIStatic++;
			}
		}
		reachableMethods.stream().filter(m->m.isNative()).forEach(m->G.v().out.printf("Warning: %s is a native method!\n", m));

		addLine(makeUp("#Reachable Method (CI):") + reachableMethods.size());
		addLine(makeUp("#Reachable Method (CS):") + reachableParameterizedMethods.size());
		addLine(makeUp("#Reachable App Method (CI):") + reachableAppMethods.size());
		addLine(makeUp("#Reachable App Method (CS):") + reachableAppParameterizedMethods.size());
		addLine(makeUp("#Call Edge(CI):") + insEdges.size());
		addLine(makeUp("#Call Edge(CS):") + callGraph.size());
		addLine(makeUp("#Static Call Edge(CI):") + CIStatic);
		addLine(makeUp("#Static Call Edge(CS):") + CSStatic);
		addLine(makeUp("#vitualcalls:") + pta.getCgb().getReceiverToSitesMap().size());
		addLine(makeUp("#avg p2s size for vitualcalls:") + (callGraph.size()-CSStatic)*1.0/pta.getCgb().getReceiverToSitesMap().size());
	}

	private void stmtProcessing() {
		final TypeMask typeManager = pag.getTypeManager();// Get type manager from Soot
		CallGraph callGraph = Scene.v().getCallGraph();

		int totalCasts = 0;
		int appCasts = 0;
		int totalCastsMayFail = 0;
		int appCastsMayFail = 0;
		int totalVirtualCalls = 0;
		int appVirtualCalls = 0;
		int totalPolyCalls = 0;
		int appPolyCalls = 0;

		// loop over all reachable method's statement to find casts, local references, virtual call sites
		for (SootMethod sm : reachableMethods) {
			if (!sm.isConcrete())
				continue;
			if (!sm.hasActiveBody())
				sm.retrieveActiveBody();

			boolean app = Config.v().isAppClass(sm.getDeclaringClass());

			// All the statements in the method
			for (Iterator<Unit> stmts = sm.getActiveBody().getUnits().iterator(); stmts.hasNext();) {
				Stmt st = (Stmt) stmts.next();
				try {
					// casts
					if (st instanceof AssignStmt) {
						Value rhs = ((AssignStmt) st).getRightOp();
						Value lhs = ((AssignStmt) st).getLeftOp();
						if (rhs instanceof CastExpr && lhs.getType() instanceof RefLikeType) {
							final Type targetType = (RefLikeType) ((CastExpr) rhs).getCastType();
							Value v = ((CastExpr) rhs).getOp();
							if (!(v instanceof Local))
								continue;
							totalCasts++;
							if (app)
								appCasts++;
							fails = false;
							((PTSetInternal) pta.reachingObjects((Local) v)).forall(new PTSetVisitor() {
								@Override
								public void visit(GNode n) {
									if (fails)
										return;
									fails = !typeManager.castNeverFails(n.getType(), targetType);
								}
							});

							if (fails) {
								totalCastsMayFail++;
								if (app)
									appCastsMayFail++;
							}
						}
					}
					// virtual calls
					if (st.containsInvokeExpr()) {
						InvokeExpr ie = st.getInvokeExpr();
						if (ie instanceof VirtualInvokeExpr) {
							totalVirtualCalls++;
							if (app)
								appVirtualCalls++;
							// have to check target soot method, cannot just count edges
							Set<SootMethod> targets = new HashSet<SootMethod>();

							for (Iterator<Edge> it = callGraph.edgesOutOf(st); it.hasNext();)
								targets.add(it.next().tgt());
							if (targets.size() > 1) {
								totalPolyCalls++;
								if (app)
									appPolyCalls++;
							}
						}
					}
				} catch (Exception e){
				}
			}
		}
		addLine(makeUp("#Cast (Total):") + totalCasts);
		addLine(makeUp("#Cast (AppOnly):") + appCasts);
		addLine(makeUp("#May Fail Cast (Total):") + totalCastsMayFail);
		addLine(makeUp("#May Fail Cast (AppOnly):") + appCastsMayFail);
		addLine(makeUp("#Virtual Call Site(Total):") + totalVirtualCalls);
		addLine(makeUp("#Virtual Call Site(AppOnly):") + appVirtualCalls);
		addLine(makeUp("#Virtual Call Site(Polymorphic):") + totalPolyCalls);
		addLine(makeUp("#Virtual Call Site(Polymorphic AppOnly):") + appPolyCalls);
	}

	private void nodeProcessing() {
		Map<LocalVar_Node, HashMap<Context, ContextLocalVar_Node>> contextVarNodeMap=pta.getContextVarNodeMap();
		
		int totalGlobalPointers = 0;
		int totalGlobalPointsToCi = 0;
		int totalGlobalPointsToCs = 0;
		int appGlobalPointers = 0;
		int appGlobalPointsToCi = 0;
		int appGlobalPointsToCs = 0;

		int totalLocalPointersCi = 0;
		int totalLocalPointersCs = 0;
		int totalLocalCiToCi = 0;
		int totalLocalCiToCs = 0;
		int totalLocalCsToCi = 0;
		int totalLocalCsToCs = 0;
		int appLocalPointersCi = 0;
		int appLocalPointersCs = 0;
		int appLocalCiToCi = 0;
		int appLocalCiToCs = 0;
		int appLocalCsToCi = 0;
		int appLocalCsToCs = 0;
		
		// globals
		for (Object global : pag.getGlobalPointers()) {
			try {
				if (!(global instanceof SootField))
					continue;
				GlobalVar_Node gvn=pag.findGlobalVarNode(global);
				boolean app = Config.v().isAppClass(gvn.getDeclaringClass());
				
				totalGlobalPointers++;
				if (app)
					appGlobalPointers++;

				final Set<Object> allocSites = new HashSet<Object>();

				PTSetInternal pts = gvn.getP2Set();
				pts.forall(new PTSetVisitor() {
					public void visit(GNode n) {
						allocSites.add(((Alloc_Node) n).getNewExpr());
					}
				});

				totalGlobalPointsToCi += allocSites.size();
				totalGlobalPointsToCs += pts.size();
				if (app){
					appGlobalPointsToCi += allocSites.size();
					appGlobalPointsToCs += pts.size();
				}
			} catch (Exception e) {
			}
		}
		// locals exclude Exceptions
		for (Local local : pag.getLocalPointers()) {
			try {
				if (isExceptionType(local.getType()))
					continue;
				LocalVar_Node lvn=pag.findLocalVarNode(local);
				boolean app = Config.v().isAppClass(lvn.getMethod().getDeclaringClass());
				HashMap<Context, ContextLocalVar_Node> contextMap=contextVarNodeMap.get(lvn);
				if(contextMap==null)continue;
				int contexts=contextMap.size();
				
				totalLocalPointersCi++;
				totalLocalPointersCs+=contexts;
				if (app){
					appLocalPointersCi++;
					appLocalPointersCs+=contexts;
				}
				
				final Set<Object> allocSites = new HashSet<Object>();
				PTSetInternal pts = (PTSetInternal) pta.reachingObjects(local);
				pts.forall(new PTSetVisitor() {
					@Override
					public void visit(GNode n) {
						allocSites.add(((Alloc_Node) n).getNewExpr());
					}
				});
				totalLocalCiToCi += allocSites.size();
				totalLocalCiToCs+=pts.size();
				if (app) {
					appLocalCiToCi += allocSites.size();
					appLocalCiToCs+=pts.size();
				}
				
				for(ContextLocalVar_Node cvn:contextMap.values()){
					final Set<Object> callocSites = new HashSet<Object>();
					PTSetInternal cpts=cvn.getP2Set();
					cpts.forall(new PTSetVisitor() {
						@Override
						public void visit(GNode n) {
							callocSites.add(((Alloc_Node) n).getNewExpr());
						}
					});
					totalLocalCsToCi += callocSites.size();
					totalLocalCsToCs+=cpts.size();
					if (app) {
						appLocalCsToCi += callocSites.size();
						appLocalCsToCs+=cpts.size();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		addLine(makeUp("#Global Pointer (lib + app):") + totalGlobalPointers);
		addLine(makeUp("#Global Avg Points-To Target(CI):")+ ((double) totalGlobalPointsToCi) / ((double) totalGlobalPointers));
		addLine(makeUp("#Global Avg Points-To Target(CS):")+ ((double) totalGlobalPointsToCs) / ((double) totalGlobalPointers));
		addLine(makeUp("#App Global Pointer:") + appGlobalPointers);
		addLine(makeUp("#App Global Avg Points-To Target(CI):")+ ((double) appGlobalPointsToCi) / ((double) appGlobalPointers));
		addLine(makeUp("#App Global Avg Points-To Target(CS):")+ ((double) appGlobalPointsToCs) / ((double) appGlobalPointers));
		addLine(makeUp("#Local Pointer (lib + app):") + totalLocalPointersCi);
		addLine(makeUp("#Local Avg Points-To Target(CI):") + ((double) totalLocalCiToCi) / ((double) totalLocalPointersCi));
		addLine(makeUp("#Local Avg Points-To Target(CS):") + ((double) totalLocalCiToCs) / ((double) totalLocalPointersCi));
		addLine(makeUp("#App Local Pointer:") + appLocalPointersCi);
		addLine(makeUp("#App Local Avg Points-To Target(CI):") + ((double) appLocalCiToCi) / ((double) appLocalPointersCi));
		addLine(makeUp("#App Local Avg Points-To Target(CS):") + ((double) appLocalCiToCs) / ((double) appLocalPointersCi));
		addLine(makeUp("#Context Local Pointer (lib + app):") + totalLocalPointersCs);
		addLine(makeUp("#Context Local Avg Points-To Target(CI):") + ((double) totalLocalCsToCi) / ((double) totalLocalPointersCs));
		addLine(makeUp("#Context Local Avg Points-To Target(CS):") + ((double) totalLocalCsToCs) / ((double) totalLocalPointersCs));
		addLine(makeUp("#App Context Local Pointer:") + appLocalPointersCs);
		addLine(makeUp("#App Context Local Avg Points-To Target(CI):") + ((double) appLocalCsToCi) / ((double) appLocalPointersCs));
		addLine(makeUp("#App Context Local Avg Points-To Target(CS):") + ((double) appLocalCsToCs) / ((double) appLocalPointersCs));
	}

	private void asmtProcessing() {
		int a=0,sp=0,ov=0,vo=0,st=0,l=0;
		for(Set<Var_Node> s:pag.getAlloc().values())
			a+=s.size();
		for(Entry<Var_Node,Set<Var_Node>> e:pag.getSimple().entrySet()){
			Set<Var_Node> tagets = e.getValue();
			int nt=tagets.size();
			sp+=nt;
			if(e.getKey() instanceof AllocDotField_Node)
				ov+=nt;
			else
				for(Var_Node v:tagets)
					if(v instanceof AllocDotField_Node)
						vo++;
		}
		for(Set<FieldRef_Node> s:pag.getStore().values())
			st+=s.size();
		for(Set<Var_Node> s:pag.getLoad().values())
			l+=s.size();
		
		addLine(makeUp("#Alloc-pag-edge:") + a);
		addLine(makeUp("#Simple-pag-edge:") + sp);
			addLine(makeUp("\t#Local-to-Local:") + (sp-ov-vo));
			addLine(makeUp("\t#Field-to-Local:") + ov);
			addLine(makeUp("\t#Local-to-Field:") + vo);
		addLine(makeUp("#Store-pag-edge:") + st);
		addLine(makeUp("#Load-pag-edge:") + l);
	}

	private void clzProcessing() {
		Set<SootClass> reachableClasses = new HashSet<SootClass>();
		reachableMethods.forEach(new Consumer<SootMethod>() {
			public void accept(SootMethod mtd) {
				reachableClasses.add(mtd.getDeclaringClass());
			}
		});
		
		Set<SootClass> reachableAppClasses = new HashSet<SootClass>();
		reachableClasses.forEach(new Consumer<SootClass>() {
			public void accept(SootClass clz) {
				if (Config.v().isAppClass(clz))
					reachableAppClasses.add(clz);
			}
		});
		
		addLine(makeUp("#Class:") + Scene.v().getClasses().size());
		addLine(makeUp("#Appclass:") + Scene.v().getApplicationClasses().size());
		addLine(makeUp("#Libclass:") + (Scene.v().getClasses().size()-Scene.v().getApplicationClasses().size()-Scene.v().getPhantomClasses().size()));
		addLine(makeUp("#Phantomclass:") + Scene.v().getPhantomClasses().size());
		addLine(makeUp("#Class(reachable):") + reachableClasses.size());
		addLine(makeUp("#Appclass(reachable):") + reachableAppClasses.size());
		addLine(makeUp("#Libclass(reachable):") + (reachableClasses.size()-reachableAppClasses.size()));
	}

	public boolean isExceptionType(Type type) {
		if (type instanceof RefType) {
			SootClass sc = ((RefType) type).getSootClass();
			if (!sc.isInterface()&& Scene.v().getActiveHierarchy().isClassSubclassOfIncluding(sc, exceptionType.getSootClass())) {
				return true;
			}
		}
		return false;
	}

	private void addLine(String str) {
		report.append(str + '\n');
	}

	public String toString() {
		return report.toString();
	}

	class InsensEdge {
		SootMethod src;
		SootMethod dst;
		Unit srcUnit;
		public InsensEdge(Edge edge) {
			this.src = edge.src();
			this.dst = edge.tgt();
			srcUnit = edge.srcUnit();
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((dst == null) ? 0 : dst.hashCode());
			result = prime * result + ((src == null) ? 0 : src.hashCode());
			result = prime * result + ((srcUnit == null) ? 0 : srcUnit.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			InsensEdge other = (InsensEdge) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (dst == null) {
				if (other.dst != null)
					return false;
			} else if (!dst.equals(other.dst))
				return false;
			if (src == null) {
				if (other.src != null)
					return false;
			} else if (!src.equals(other.src))
				return false;
			if (srcUnit == null) {
				if (other.srcUnit != null)
					return false;
			} else if (!srcUnit.equals(other.srcUnit))
				return false;
			return true;
		}
		private SparkEvaluator getOuterType() {
			return SparkEvaluator.this;
		}
	}
}
