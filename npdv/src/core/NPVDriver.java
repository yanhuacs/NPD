package core;
/* Java and Android Analysis Framework - NullPointer Module
 * Copyright (C) 2017 Xinwei Xie, Hua Yan, Jingbo Lu, Yulei Sui and Jingling Xue
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

/* Soot - a J*va Optimization Framework
 * Copyright (C) 2003 Ondrej Lhotak
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

import driver.Config;
//import org.apache.commons.io.output.NullOutputStream;
//import pta.ObjSensitivePTA;
//import pta.PTA;
//import pta.SparkEvaluator;
//import pta.context.StaticInitContext;
import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.LengthExpr;
import soot.jimple.spark.pag.PAG;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.CGOptions;
import soot.options.Options;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thread.NPVerifyTask;

import java.io.*;
import java.util.*;

//import static driver.SootConfig.sparkPTAOpts;

/**
 * Created by Xinwei Xie on 21/7/17.
 */
public class NPVDriver {
    private static CallGraph cg = null;
    //private static PTA pta = null;
    private static PAG pag = null;
    private static final Logger logger = LoggerFactory.getLogger(NPVDriver.class);


    public static void main(String[] args) {
        //prevent printing too much information
        driver.Main.main(args);
        analyzeList(args);
        run();
    }

    private static void analyzeList(String[] args) {
        /*
        Properties result = new Properties();
        for (int i = 0; i < args.length; ++i) {
            if (args[i] == null)
                continue;
            String key = args[i];
            String property = null;
            if (key.charAt(0) == '-') {
                if (key.contains("=")) {
                    property = key.substring(1, key.indexOf('='));
                } else {
                    property = key.substring(1);
                }
            }
            if (property != null) {
                if (key.contains("=")) {
                    result.put(property, args[i].substring(args[i].indexOf('=') + 1));
                } else {
                    if ((i + 1) > args.length || args[i + 1].charAt(0) == '-') {
                        throw new IllegalArgumentException("Malformed command-line.  Must be of form -key=value or -key valu");
                    }
                    result.put(property, args[i + 1]);
                    i++;
                }
            }
        }
        */

        String analyzeList = "list.txt";
        if (analyzeList != null) {
            File file = new File(analyzeList);
            try {
                FileInputStream fis = new FileInputStream(file);
                BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                String line = null;
                logger.info("Analyze list: ");
                while((line = br.readLine()) != null) {
                    if (line.startsWith("#") || line.equals(""))
                        continue;
                    NPVerifier.analyzeList.add(line);
                    logger.info("\t" + line);
                }
            } catch (FileNotFoundException e) {
                logger.error("Analyze List File: " + analyzeList + " does not exist!");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
        }
    }

    public static void processCmdLine(String[] args) {
        if(!Options.v().parse(args)) {
            throw new OptionsParseException("Option parse error");
        } else {
            if(PackManager.v().onlyStandardPacks()) {
                for(Pack pack : PackManager.v().allPacks()) {
                    Options.v().warnForeignPhase(pack.getPhaseName());
                    for(Transform tr : pack) {
                        Options.v().warnForeignPhase(tr.getPhaseName());
                    }
                }
            }

            Options.v().warnNonexistentPhase();
            if(Options.v().help()) {
                G.v().out.println(Options.v().getUsage());
                throw new CompilationDeathException(1);
            } else if(Options.v().phase_list()) {
                G.v().out.println(Options.v().getPhaseList());
                throw new CompilationDeathException(1);
            } else if(!Options.v().phase_help().isEmpty()) {
                for (String phase : Options.v().phase_help()) {
                    G.v().out.println(Options.v().getPhaseHelp(phase));
                }
                throw new CompilationDeathException(1);
            } else if((Options.v().unfriendly_mode() || args.length != 0) && !Options.v().version()) {
                if(Options.v().on_the_fly()) {
                    Options.v().set_whole_program(true);
                    PhaseOptions.v().setPhaseOption("cg", "off");
                }
            } else {
                throw new CompilationDeathException(1);
            }
        }
    }
    public static void autoSetOptions() {
        if(Options.v().no_bodies_for_excluded()) {
            Options.v().set_allow_phantom_refs(true);
        }

        CGOptions cgOptions = new CGOptions(PhaseOptions.v().getPhaseOptions("cg"));
        String log = cgOptions.reflection_log();
        if(log != null && log.length() > 0) {
            Options.v().set_allow_phantom_refs(true);
        }

        if(Options.v().allow_phantom_refs()) {
            Options.v().set_wrong_staticness(3);
        }

        Options.v().set_keep_line_number(true);
        Options.v().setPhaseOption("cg.spark", "on");
    }
    public static void sootInit(String[] args) {
        try {
            processCmdLine(args);
            autoSetOptions();
            Scene.v().loadNecessaryClasses();
            PackManager.v().runPacks();
        } catch (CompilationDeathException var7) {
            if(var7.getStatus() != 1) {
                throw var7;
            }
            return;
        }
    }

    public static void run() {
        //Iterator<SootClass> appClassIter = Config.v().appClasses.iterator();
        Date start = new Date();
        Set<SootMethod> applicationMethods = CollectionFactory.newSet();
        Iterator<SootClass> classIter = Scene.v().getClasses().iterator();
        FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();

        PointsToAnalysis pta = driver.Main.pta;
        CallGraph cg = driver.Main.pta.getCgb().getCICallGraph();

        while (classIter.hasNext()) {
            SootClass klass = classIter.next();
            String klassName = klass.getName();
            if (klass.isInterface()) {
                if (klassName.equals("java.util.Set"))
                    NPVerifier.setClass.addAll(fh.getAllImplementersOfInterface(klass));
                else if (klassName.equals("java.util.Map"))
                    NPVerifier.mapClass.addAll(fh.getAllImplementersOfInterface(klass));
                else if (klassName.equals("java.util.Collection"))
                    NPVerifier.collectionClass.addAll(fh.getAllImplementersOfInterface(klass));
                else if (klassName.equals("java.util.Iterator"))
                    NPVerifier.iteratorClass.addAll(fh.getAllImplementersOfInterface(klass));
                else if (klassName.equals("java.util.Enumeration"))
                    NPVerifier.enumClass.addAll(fh.getAllImplementersOfInterface(klass));
                else if (klassName.equals("java.util.List"))
                    NPVerifier.listClass.addAll(fh.getAllImplementersOfInterface(klass));
            }
            //if (klass.isApplicationClass()) {
            if (Config.v().isAppClass(klass)) {
                for (SootMethod m : klass.getMethods()) {
                   applicationMethods.add(m);
                   if (m.isStaticInitializer()
                           && !m.getDeclaringClass().isJavaLibraryClass()
                           && cg.edgesInto(m).hasNext()) {
                       NPVerifier.classInitMethods.add(m);
                   }
                }
            }
        }
        //Iterator<SootClass> appClassIter = Scene.v().getApplicationClasses().iterator();

        NPVerifier npe = new NPVerifier(cg, pta); //, pta);

        //cg = Scene.v().getCallGraph();
        //NPVerifier.applicationMethods = applicationMethods;

        ModRef modref = new ModRef(cg, pta);
        NPVerifier.modMap = modref.computeMod();

        logger.info("Finish modref analysis...");

        Iterator<SootMethod> methodIter = applicationMethods.iterator();
        while (methodIter.hasNext()) {
                SootMethod method = methodIter.next();

                Iterator<Edge> inIter = cg.edgesInto(method);
                Iterator<Edge> outIter = cg.edgesOutOf(method);
                if (!inIter.hasNext() || !outIter.hasNext()) {
                    if (!method.isMain() || method.getDeclaringClass().isJavaLibraryClass())
                        continue;
                }


                if (method.isAbstract()) continue;
                Body activeBody = null;
                try {
                    activeBody = method.retrieveActiveBody();
                } catch (Exception e) {
                    logger.debug("method: " + method + " does not have active body!!!");
                }
                if (activeBody == null) {
                    logger.debug("method: " + method + " does not have active body!!!");
                    continue;
                }

                if (NPVerifier.analyzeList.size() > 0) {
                    boolean needCheck = false;
                    // package
                    String packageName = method.getDeclaringClass().getPackageName();
                    //packageName = packageName.substring(0, packageName.lastIndexOf('.'));
                    if (NPVerifier.analyzeList.contains(packageName))
                        needCheck = true;
                    // class
                    String className = method.getDeclaringClass().toString();
                    if (NPVerifier.analyzeList.contains(className))
                        needCheck = true;
                    // method
                    String methodName = NPVerifier.getMethodSignature(method);
                    //logger.error("method: " + methodName);
                    if (NPVerifier.analyzeList.contains(methodName))
                        needCheck = true;
                    if (!needCheck)
                        continue;
                }

                List<Unit> deref = NPVerifier.dereferenceMap.get(method);
                if (deref == null) {
                    deref = new ArrayList<Unit>();
                    NPVerifier.dereferenceMap.put(method, deref);
                }
                PatchingChain<Unit> units = activeBody.getUnits();
                Iterator<Unit> unitIter = units.iterator();
                while(unitIter.hasNext()) {
                    Unit inst = unitIter.next();
                    Value base = npe.getInstBase(inst);
                    if (base == null || npe.isThisReference(units, base) || npe.isArrayInst(inst)) continue;
                    deref.add(inst);
                    NPVerifier.totalDref++;
                }
        }

        for (SootMethod method : NPVerifier.dereferenceMap.keySet()) {
            List<Unit> deref = NPVerifier.dereferenceMap.get(method);
            logger.debug("ANALYSING METHOD: " + method + "==================");
            for (Unit inst : deref) {

                //if (NPVerifier.byPass(inst))
                //    continue;
                Value base = npe.getInstBase(inst);
                NPVerifier.currentDref++;
                logger.info("\t[" + NPVerifier.currentDref + " / " + NPVerifier.totalDref + " | " + NPVerifier.numOfWrong + "], base: " + base + " , inst: " + inst
                            + " in line: " + inst.getTag("LineNumberTag"));
                boolean safe = npe.isDerefSafe(method, inst, base);
                if (!safe) {
                    logger.error("\t!!! WRONG !!! null pointer deref: " + base +
                            " in inst: " + inst +
                            " of method: " + method +
                            " of line: " + inst.getTag("LineNumberTag")
                            + ", steps: " + NPVerifier.currentGlobalSteps);
                    //NPVerifier.numOfWrong++;
                    //NPVerifier.numOfSafe++;
                }
            }
        }
        /*
        //iterate all application classes
        while(methodIter.hasNext()) {
            //SootClass appClass = (SootClass)(appClassIter.next());
            //Iterator<SootMethod> methodIter = appClass.methodIterator();
            //iterate all class's methods
            while(methodIter.hasNext()) {
                SootMethod method = methodIter.next();
                Iterator<Edge> inIter = cg.edgesInto(method);
                Iterator<Edge> outIter = cg.edgesOutOf(method);
                if (!inIter.hasNext() || !outIter.hasNext()) continue;
                if (method.isAbstract()) continue;
                Body activeBody = method.retrieveActiveBody();
                if (activeBody == null) continue;

                PatchingChain<Unit> units = activeBody.getUnits();
                Iterator<Unit> unitIter = units.iterator();
                logger.info("ANALYSING METHOD: " + method + "==================");
                while(unitIter.hasNext()) {
                    Unit inst = unitIter.next();
                    Value base = npe.getInstBase(inst);
                    if (base == null || npe.isThisReference(units, base)) continue;
                    logger.info("\tbase: " + base + " , inst: " + inst);
                    NPVerifier.currentDref++;
                    boolean safe = npe.isDerefSafe(method, inst, base);
                    if (!safe) {
                        logger.error("\t\t!!! WRONG !!! null pointer deref: " + base +
                                " in inst: " + inst +
                                " in method: " + method +
                                " in line: " + inst.getTag("LineNumberTag"));
                        NPVerifier.numOfWrong++;
                    }
                }
                //logger.info("FINISH" + method + "==================");
            }
        }
        */
        Date end = new Date();
        long time = end.getTime() - start.getTime();
        String error = String.format("%.3f", (((double) NPVerifier.numOfWrong)/((double) NPVerifier.totalDref)));
        String elapse = time / 1000 + "." + (time / 100) % 10;
        logger.info("Whole program analysis completed in " + elapse + " seconds: "
                + " #Derefs: " + NPVerifier.totalDref + ", #NPEs: " + NPVerifier.numOfWrong
                + ", #Safe Derefs: " + (NPVerifier.numOfSafe + NPVerifier.numOfUnknown)
                //+ ", Unknown: " + NPVerifier.numOfWrong
                + ", Error Rate: " +  error);

        if (NPVerifier.enableStatistic) {
            System.out.println("==================== Statistics ====================");
            /*
            System.out.println("\tvirtual calls count: ");
            for (Integer size : NPVerifier.virtualCallCounts.keySet()) {
                System.out.println("\t\t" + size + "    :    " + NPVerifier.virtualCallCounts.get(size));
            }
            */
            System.out.println("\tWrong instructions: ");
            for (SootMethod m : NPVerifier.wrongMap.keySet()) {
                System.out.println("\t\tMethod: " + m); // + NPVerifier.wrongMap.get(m).size());
                for (Unit u : NPVerifier.wrongMap.get(m)) {
                    System.out.println("\t\t\t" + u + ", line: " + u.getTag("LineNumberTag"));
                }
            }
            /*
            System.out.println("\tMAYBE INSTRUCTIONS: ");
            for (SootMethod m : NPVerifier.unknownMap.keySet()) {
                System.out.println("\t\tmethod: " + m); // + NPVerifier.wrongMap.get(m).size());
                for (Unit u : NPVerifier.unknownMap.get(m)) {
                    System.out.println("\t\t\t" + u + ", line: " + u.getTag("LineNumberTag"));
                }
            }
            */
            System.out.println("====================================================");
        }
    }
}
