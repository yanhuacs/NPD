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

package driver;

import pta.context.ParameterizedMethod;
import pta.context.StaticInitContext;

import static driver.DruidOptions.sparkOpts;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import pag.MtdPAG;
import pag.WholeProgPAG;
import pta.ObjSensitivePTA;
import pta.PTA;
import reflection.JSONFormatter;
import reflection.ReflectionOptions;
import reflection.ReflectionStat;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.jimple.ReachingTypeDumper;
import string.JsaExe;
import util.PTAUtils;
import util.SparkEvaluator;

public class Main {
	public static PTA pta = null;
	public static void main(String[] args) {
		long time = System.currentTimeMillis();
		reset();

		Config.v().init(args);
		
		if (DruidOptions.stringAnalysis) {
			System.out.println("Now performing string-analysis..");
			JsaExe.run(Config.v());
			ReflectionOptions.v().setStringAnalysis(true);
		}
		if (DruidOptions.dumpJimple) {
			String jimplePath = DruidOptions.APP_PATH.replace(".jar", "");
			SootUtils.dumpJimple(jimplePath);
			System.out.println("Jimple files have been dumped to: " + jimplePath);
		}
		
		SparkEvaluator.v().begin();
		
		run();
		
		SparkEvaluator.v().end();
		System.out.println(SparkEvaluator.v().toString());
		System.out.println("Total time(including class loading):" + (System.currentTimeMillis() - time) / 1000);
		
		//PTAUtils.printAppPts(pta);
	}
	
	private static void reset(){
		//G.reset();
		ParameterizedMethod.reset();
		MtdPAG.reset();
		SparkEvaluator.reset();
	}

	private static void run() {
		// always reset the obj sens universe because we might switch back and forth
		// between obj sens and no sens, definitely ugly, but better than passing around global state
		StaticInitContext.reset();

		// Build pointer assignment graph
		if (sparkOpts.pre_jimplify())
			preJimplify();

		Date startBuild = new Date();
		pta = new ObjSensitivePTA();
		pta.buildPAG();
		final WholeProgPAG pag = pta.getPag();
		pag.setInitialReader();
		Date endBuild = new Date();
		reportTime("Pointer Assignment Graph", startBuild, endBuild);
			
		// Build type masks
		Date startTM = new Date();
		pag.getTypeManager().makeTypeMask();
		Date endTM = new Date();
		reportTime("Type masks", startTM, endTM);

		//pag.cleanUpMerges();
		if (sparkOpts.force_gc())
			doGC();

		// Propagate
		Date startProp = new Date();
		G.v().out.println("Start solving constraints... ");
		//new pta.solver.PropIter(pta).propagate();
		//new pta.solver.PropWorklist(pta).propagate();
		new pta.solver.Solver(pta).propagate();
        G.v().out.println("End solving constraints... ");

		Date endProp = new Date();
		reportTime("Points-to resolution time:", startProp, endProp);

		if (!sparkOpts.on_fly_cg() || sparkOpts.vta()) {
			soot.jimple.toolkits.callgraph.CallGraphBuilder cicgb = new soot.jimple.toolkits.callgraph.CallGraphBuilder(pta);
			cicgb.build();
		}
		
		dumpStats(pta,pag);
	}

	private static void dumpStats(PTA pta, WholeProgPAG pag){
		if (ReflectionOptions.v().isInferenceReflectionModel()) {
			ReflectionStat.v().showInferenceReflectionStat();
			JSONFormatter.v().format();
		}
		final String output_dir = SourceLocator.v().getOutputDir();
		if (sparkOpts.dump_answer())
			new ReachingTypeDumper(pta, output_dir).dump();
		if (DruidOptions.dumppts)
			PTAUtils.dumpPts(pta,!DruidOptions.dumplibpts);
		if (DruidOptions.dumpCallGraph)
			PTAUtils.dumpCallGraph(pta.getCallGraph());
		if (DruidOptions.dumppag){
			PTAUtils.dumpPAG(pta.getPag(),"final_pag");
			PTAUtils.dumpMPAGs(pta,"mpags");
			PTAUtils.dumpNodeNames("nodeNames");
		}
//		if (DruidOptions.dumphtml)
//			new PAG2HTML(pag, output_dir).dump();
	}
	
	private static void preJimplify() {
		boolean change = true;
		while (change) {
			change = false;
			for (Iterator<SootClass> cIt = new ArrayList<SootClass>(Scene.v().getClasses()).iterator(); cIt
					.hasNext();) {
				final SootClass c = cIt.next();
				for (Iterator<?> mIt = c.methodIterator(); mIt.hasNext();) {
					final SootMethod m = (SootMethod) mIt.next();
					if (!m.isConcrete())
						continue;
					if (m.isNative())
						continue;
					if (m.isPhantom())
						continue;
					if (!m.hasActiveBody()) {
						change = true;
						m.retrieveActiveBody();
					}
				}
			}
		}
	}

	private static void doGC() {
		// Do 5 times because the garbage collector doesn't seem to always collect everything on the first try.
		System.gc();
		System.gc();
		System.gc();
		System.gc();
		System.gc();
	}

	private static void reportTime(String desc, Date start, Date end) {
		long time = end.getTime() - start.getTime();
		G.v().out.println("[PTA] " + desc + " in " + time / 1000 + "." + (time / 100) % 10 + " seconds.");
	}
}
