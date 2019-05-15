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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import driver.Config;
import javafx.util.Pair;
import pag.WholeProgPAG;
import pag.MtdPAG;
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
import soot.Local;
import soot.SootClass;
import soot.SootField;
import soot.SourceLocator;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.dot.DotGraph;
import soot.util.dot.DotGraphConstants;
import soot.util.dot.DotGraphNode;
import soot.util.queue.QueueReader;

public final class PTAUtils {
	static final String output_dir = SourceLocator.v().getOutputDir();
	static TreeMap<String,GNode> nodes = new TreeMap<>();
	
	// print pts
	public static void printAppPts(PTA pta) {
		WholeProgPAG pag = pta.getPag();
		System.out.println("Globals: ");
		for (Object global : pag.getGlobalPointers()) {
			if (!(global instanceof SootField)||!Config.v().isAppClass(pag.findGlobalVarNode(global).getDeclaringClass()))
				continue;
			System.out.println(global + ":");
			printPts((PTSetInternal) pta.reachingObjects((SootField) global));
		}
		System.out.println("\nLocals: ");
		for (Local local : pag.getLocalPointers()) {
			if (SparkEvaluator.v().isExceptionType(local.getType()))
				continue;
			LocalVar_Node varNode = pag.findLocalVarNode(local);
			if (!Config.v().isAppClass(varNode.getMethod().getDeclaringClass()))
				continue;
			Map<Context, ContextLocalVar_Node> cvns=pta.getContextVarNodeMap().get(varNode);
			if(cvns==null)continue;
			cvns.values().forEach(new Consumer<ContextLocalVar_Node>() {
				public void accept(ContextLocalVar_Node cvn) {
					System.out.println(cvn + ":");
					printPts(cvn.getP2Set());
				}
			});
		}
	}
	private static void printPts(PTSetInternal pts) {
		final StringBuffer ret = new StringBuffer();
		pts.forall(new PTSetVisitor() {
			public final void visit(GNode n) {
				ret.append("\t"+n+"\n");
			}
		});
		System.out.print(ret);
	}
	
	/**
	 * dump callgraph to sootoutput/callgraph.dot
	 */
	public static void dumpCallGraph(CallGraph callgraph) {
		String filename = "callgraph";
		DotGraph canvas = setDotGraph(filename);
		HashSet<Pair<String, String>> set=new HashSet<>();
		callgraph.forEach(edge->{
			ParameterizedMethod srcmtd = (ParameterizedMethod) edge.getSrc();
			ParameterizedMethod dstmtd = (ParameterizedMethod) edge.getTgt();
			String srcName = srcmtd.getClzName() + ":" +  srcmtd.getMtdName();
			String dstName = dstmtd.getClzName() + ":" +  dstmtd.getMtdName();
			DotGraphNode srcDotNode = canvas.drawNode(srcName);
			srcDotNode.setLabel(srcName);
			DotGraphNode dstDotNode = canvas.drawNode(dstName);
			dstDotNode.setLabel(dstName);
			set.add(new Pair<String, String>(srcName, dstName));
		});
		set.forEach(pair->canvas.drawEdge(pair.getKey(), pair.getValue()));
		plotDotGraph(canvas,filename);
	}
	
	/**
	 * dump pts to sootoutput/pts
	 */
	public static void dumpPts(PTA pta, boolean apponly) {
		try {
			PrintWriter file = new PrintWriter(new File(output_dir, "pts"));
			file.println("Points-to results:");
			for (Iterator<Var_Node> vnIt = pta.getPag().getVarNodeNumberer().iterator(); vnIt.hasNext();) {
				final Var_Node vn = vnIt.next();
				SootClass clz;
				if(vn instanceof ContextLocalVar_Node)
					clz = ((ContextLocalVar_Node) vn).getMethod().getDeclaringClass();
				else if(vn instanceof GlobalVar_Node)
					clz = ((GlobalVar_Node)vn).getDeclaringClass();
				else  // AllocDotField_Node (on PAG, but we don't care its pts), LocalVar_Node (not on PAG since it is parameterized)
					continue;
				if(apponly && !Config.v().isAppClass(clz))
					continue;
				
				String label = getNodeLabel(vn);
				nodes.put("["+label+"]", vn);
				file.print(label + " -> {");
				PTSetInternal p2set = vn.getP2Set();
				if (p2set == null || p2set.isEmpty()){
					file.print(" empty }\n");
					continue;
				}
				p2set.forall(new PTSetVisitor() {
					public final void visit(GNode n) {
						String label = getNodeLabel(n);
						nodes.put("["+label+"]", n);
						file.print(" ");
						file.print(label);
					}
				});
				file.print(" }\n");
			}
			dumpNodeNames(file);
			file.close();
		} catch( IOException e ) {
            throw new RuntimeException( "Couldn't dump solution."+e );
        }
		
	}
	/**
	 * dump mPAGs to sootoutput/@filename.dot
	 */
	public static void dumpMPAGs(PTA pta, String filename) {
		DotGraph canvas = setDotGraph(filename);
		Iterator<Edge> iter = pta.getCallGraph().iterator();
		while(iter.hasNext()){
			Edge edge = iter.next();
			ParameterizedMethod srcmtd = (ParameterizedMethod) edge.getSrc();
			MtdPAG mpag = MtdPAG.v(pta.getPag(), srcmtd.method());
			QueueReader<GNode> reader = mpag.getInternalReader().clone();
			while (reader.hasNext()) {
				GNode src = (GNode) reader.next();
				GNode dst = (GNode) reader.next();
				drawNode(canvas, src);
				drawNode(canvas, dst);
				GNode from = src.getReplacement();//TODO ?
		        GNode to = dst.getReplacement();
				String color=
						from instanceof Alloc_Node?"green":	//alloc
						from instanceof FieldRef_Node?"red":	//load
						to instanceof FieldRef_Node?"blue":	//store
						"black";							//simple
				drawEdge(canvas, src, dst, color);
			}
		}
		plotDotGraph(canvas, filename);
	}
	
	/**
	 * dump pag to sootoutput/@filename.dot
	 */
	public static void dumpPAG(WholeProgPAG pag, String filename) {
		DotGraph canvas = setDotGraph(filename);
		/// dump allocNodes
        for (Object object : pag.allocSources()) {
            final Alloc_Node n = (Alloc_Node) object;
            drawNode(canvas,n);
            if( n.getReplacement() != n ) continue;//TODO why after drawNode?
            for (GNode element : pag.allocLookup( n )) {
            	drawNode(canvas, element);
            	drawEdge(canvas, n, element, "green");
            }
        }
        /// dump simpleNodes
        for (Object object : pag.simpleSources()) {
            final Var_Node n = (Var_Node) object;
            drawNode(canvas,n);
            if( n.getReplacement() != n ) continue;
            for (GNode element : pag.simpleLookup( n )) {
            	drawNode(canvas, element);
            	drawEdge(canvas, n, element, "black");
            }
        }
        /// dump loadNodes
        for (Object object : pag.loadSources()) {
            final FieldRef_Node n = (FieldRef_Node) object;
            drawNode(canvas, n);
            for (GNode element : pag.loadLookup( n )) {
            	drawNode(canvas, element);
            	drawEdge(canvas, n, element, "red");
            }
        } 
        /// dump storeNodes
        for (Object object : pag.storeSources()) {
            final Var_Node n = (Var_Node) object;
            drawNode(canvas, n);
            if( n.getReplacement() != n ) continue;
            for (GNode element : pag.storeLookup( n )) {
            	drawNode(canvas, element);
            	drawEdge(canvas, n, element, "blue");
            }
        }
		plotDotGraph(canvas,filename);
	}
	
	private static void plotDotGraph(DotGraph canvas, String filename) {
		canvas.plot(output_dir + "/" + filename + ".dot");
	}
	private static DotGraph setDotGraph(String fileName) {
		DotGraph canvas = new DotGraph(fileName);
		canvas.setNodeShape(DotGraphConstants.NODE_SHAPE_BOX);
		canvas.setGraphLabel(fileName);
		return canvas;
	}
	private static String getNodeLabel(GNode node){
		int num = node.getNumber();
		if(node instanceof LocalVar_Node)
			return "L" + num;
		else if(node instanceof GlobalVar_Node)
			return "G" + num;
		else if(node instanceof AllocDotField_Node)
			return "OF" + num;
		else if(node instanceof FieldRef_Node)
			return "VF" + num;
		else if(node instanceof Alloc_Node)
			return "O" + num;
		else
			throw new RuntimeException("no such node type exists!");
	}
	private static void drawNode(DotGraph canvas, GNode node) {
		DotGraphNode dotNode = canvas.drawNode(node.toString());
    	dotNode.setLabel("[" + getNodeLabel(node) + "]");
    	nodes.put("[" + getNodeLabel(node) + "]", node);
	}
	private static void drawEdge(DotGraph canvas, GNode src, GNode dst, String color) {
		canvas.drawEdge(src.toString(),dst.toString()).setAttribute("color",color);
	}

	//TODO no caller
//	public static void CollectPointerDeferences(WholeProgPAG pag) {
//		int mass = 0;
//		int varMass = 0;
//		int adfs = 0;
//		int scalars = 0;
//		for (Iterator<Var_Node> vIt = pag.getVarNodeNumberer().iterator(); vIt.hasNext();) {
//			final Var_Node v = vIt.next();
//			scalars++;
//			PTSetInternal set = v.getP2Set();
//			if (set != null)
//				mass += set.size();
//			if (set != null)
//				varMass += set.size();
//		}
//		for (Iterator<Alloc_Node> anIt = pag.allocSourcesIterator(); anIt.hasNext();) {
//			final Alloc_Node an = anIt.next();
//			for (Iterator<AllocDotField_Node> adfIt = an.getFields().iterator(); adfIt.hasNext();) {
//				final AllocDotField_Node adf =  adfIt.next();
//				PTSetInternal set = adf.getP2Set();
//				if (set != null)
//					mass += set.size();
//				if (set != null && set.size() > 0) {
//					adfs++;
//				}
//			}
//		}
//		G.v().out.println("Set mass: " + mass);
//		G.v().out.println("Variable mass: " + varMass);
//		G.v().out.println("Scalars: " + scalars);
//		G.v().out.println("adfs: " + adfs);
//		// Compute points-to set sizes of dereference sites BEFORE
//		// trimming sets by declared type
//		int[] deRefCounts = new int[30001];
//		for (Var_Node v : pag.getDereferences()) {
//			PTSetInternal set = v.getP2Set();
//			int size = 0;
//			if (set != null)
//				size = set.size();
//			deRefCounts[size]++;
//		}
//		int total = 0;
//		for (int element : deRefCounts)
//			total += element;
//		G.v().out.println("Dereference counts BEFORE trimming (total = " + total + "):");
//		for (int i = 0; i < deRefCounts.length; i++) {
//			if (deRefCounts[i] > 0) {
//				G.v().out.println("" + i + " " + deRefCounts[i] + " " + (deRefCounts[i] * 100.0 / total) + "%");
//			}
//		}
//	}
	private static void dumpNodeNames(PrintWriter file) {
		nodes.forEach((l,n)->file.println(l+n));
	}
	
	public static void dumpNodeNames(String fileName){
		try {
			PrintWriter out=new PrintWriter(new File(output_dir, fileName));
			dumpNodeNames(out);
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}