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

package pag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import pag.builder.MtdPAGBuilder;
import pag.node.GNode;
import pag.node.var.Val_Node;
import soot.ArrayType;
import soot.Context;
import soot.EntryPoints;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.VoidType;
import soot.jimple.Stmt;
import soot.util.NumberedString;
import soot.util.SingletonList;
import soot.util.queue.ChunkedQueue;
import soot.util.queue.QueueReader;

/**
 * Part of a pointer assignment graph for a single method.
 * 
 * @author Ondrej Lhotak
 */
public final class MtdPAG {
	public static HashMap<SootMethod, MtdPAG> MethodPAG_methodToPag = new HashMap<SootMethod, MtdPAG>();
	public static void reset() {
		MethodPAG_methodToPag = new HashMap<SootMethod, MtdPAG>();
	}
	
	private WholeProgPAG pag;
	private final ChunkedQueue<GNode> internalEdges = new ChunkedQueue<GNode>();
	private final QueueReader<GNode> internalReader = internalEdges.reader();
	private Set<Context> addedContexts = new HashSet<Context>();
	
	public WholeProgPAG pag() {
		return pag;
	}

	protected MtdPAG(WholeProgPAG pag, SootMethod m) {
		this.pag = pag;
		this.method = m;
		this.nodeFactory = new MtdPAGBuilder(pag, this);
	}

	SootMethod method;

	public SootMethod getMethod() {
		return method;
	}

	protected MtdPAGBuilder nodeFactory;

	public MtdPAGBuilder nodeFactory() {
		return nodeFactory;
	}

	public static MtdPAG v(WholeProgPAG pag, SootMethod m) {
		MtdPAG ret = MethodPAG_methodToPag.get(m);
		if (ret == null) {
			ret = new MtdPAG(pag, m);
			MethodPAG_methodToPag.put(m, ret);
		}
		return ret;
	}

	public void build() {
		if (hasBeenBuilt)
			return;
		hasBeenBuilt = true;
		if (method.isNative()) {
			if (driver.DruidOptions.sparkOpts.simulate_natives()) {
				buildNative();
			}
		} else {
			if (method.isConcrete() && !method.isPhantom()) {
				buildNormal();
			}
		}
		addMiscEdges();
	}
	protected boolean hasBeenBuilt = false;

	protected void buildNormal() {
		for (Iterator<Unit> unitsIt = method.retrieveActiveBody().getUnits().iterator();unitsIt.hasNext();)
			nodeFactory.handleStmt((Stmt)unitsIt.next());
	}

	protected void buildNative() {
		Val_Node thisNode = null;
		Val_Node retNode = null;
		if (!method.isStatic()) {
			thisNode = (Val_Node) nodeFactory.caseThis();
		}
		if (method.getReturnType() instanceof RefLikeType) {
			retNode = (Val_Node) nodeFactory.caseRet();
		}
		Val_Node[] args = new Val_Node[method.getParameterCount()];
		for (int i = 0; i < method.getParameterCount(); i++) {
			if (!(method.getParameterType(i) instanceof RefLikeType))
				continue;
			args[i] = (Val_Node) nodeFactory.caseParm(i);
		}
		pag.nativeMethodDriver.process(method, thisNode, retNode, args);
	}

	protected void addMiscEdges() {
		// Add node for parameter (String[]) in main method
		if (method.getSubSignature().equals(SootMethod.getSubSignature("main",
				new SingletonList(ArrayType.v(RefType.v("java.lang.String"), 1)), VoidType.v()))) {
			addInternalEdge(pag().GlobalNodeFactory().caseArgv(), nodeFactory.caseParm(0));
		} else

		if (method.getSignature().equals("<java.lang.Thread: void <init>(java.lang.ThreadGroup,java.lang.String)>")) {
			addInternalEdge(pag().GlobalNodeFactory().caseMainThread(), nodeFactory.caseThis());
			addInternalEdge(pag().GlobalNodeFactory().caseMainThreadGroup(), nodeFactory.caseParm(0));
		} else

		if (method.getSignature().equals("<java.lang.ref.Finalizer: void <init>(java.lang.Object)>")) {
			addInternalEdge(nodeFactory.caseThis(), pag().GlobalNodeFactory().caseFinalizeQueue());
		} else

		if (method.getSignature().equals("<java.lang.ref.Finalizer: void runFinalizer()>")) {
			addInternalEdge(pag.GlobalNodeFactory().caseFinalizeQueue(), nodeFactory.caseThis());
		} else

		if (method.getSignature().equals("<java.lang.ref.Finalizer: void access$100(java.lang.Object)>")) {
			addInternalEdge(pag.GlobalNodeFactory().caseFinalizeQueue(), nodeFactory.caseParm(0));
		} else

		if (method.getSignature().equals("<java.lang.ClassLoader: void <init>()>")) {
			addInternalEdge(pag.GlobalNodeFactory().caseDefaultClassLoader(), nodeFactory.caseThis());
		} else

		if (method.getSignature().equals("<java.lang.Thread: void exit()>")) {
			addInternalEdge(pag.GlobalNodeFactory().caseMainThread(), nodeFactory.caseThis());
		} else

		if (method.getSignature()
				.equals("<java.security.PrivilegedActionException: void <init>(java.lang.Exception)>")) {
			addInternalEdge(pag.GlobalNodeFactory().caseThrow(), nodeFactory.caseParm(0));
			addInternalEdge(pag.GlobalNodeFactory().casePrivilegedActionException(), nodeFactory.caseThis());
		}

		if (method.getNumberedSubSignature().equals(sigCanonicalize)) {
			SootClass cl = method.getDeclaringClass();
			while (true) {
				if (cl.equals(Scene.v().getSootClass("java.io.FileSystem"))) {
					addInternalEdge(pag.GlobalNodeFactory().caseCanonicalPath(), nodeFactory.caseRet());
				}
				if (!cl.hasSuperclass())
					break;
				cl = cl.getSuperclass();
			}
		}

		boolean isImplicit = false;
		for (SootMethod implicitMethod : EntryPoints.v().implicit()) {
			if (implicitMethod.getNumberedSubSignature().equals(method.getNumberedSubSignature())) {
				isImplicit = true;
				break;
			}
		}
		if (isImplicit) {
			SootClass c = method.getDeclaringClass();
			outer: do {
				while (!c.getName().equals("java.lang.ClassLoader")) {
					if (!c.hasSuperclass()) {
						break outer;
					}
					c = c.getSuperclass();
				}
				if (method.getName().equals("<init>"))
					continue;
				addInternalEdge(pag().GlobalNodeFactory().caseDefaultClassLoader(), nodeFactory.caseThis());
				addInternalEdge(pag().GlobalNodeFactory().caseMainClassNameString(), nodeFactory.caseParm(0));
			} while (false);
		}
	}

	public void addInternalEdge(GNode src, GNode dst) {
		if (src == null)
			return;
		internalEdges.add(src);
		internalEdges.add(dst);
	}
	
	public QueueReader<GNode> getInternalReader() {
		return internalReader;
	}

	public Set<Context> getAddedContexts() {
		return addedContexts;
	}

	protected final NumberedString sigCanonicalize = Scene.v().getSubSigNumberer().findOrAdd("java.lang.String canonicalize(java.lang.String)");

}
