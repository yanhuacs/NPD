package thread;

import core.CollectionFactory;
import core.ModRef;
import driver.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class NPExecutor {

    private static final Logger logger = LoggerFactory.getLogger(NPExecutor.class);
    private static int nthreads = 1; //Runtime.getRuntime().availableProcessors();

    public static void main(String[] args) {
        String num = System.getProperty("thread.NPExecutor.nthreads");
        if (num != null) {
            try {
                nthreads = Integer.parseInt(num);
                logger.info("Running with " + nthreads + " threads...");
            } catch (NumberFormatException e) {
                logger.info("Illegal thread number, running with 1 thread...");
            }
        }

        driver.Main.main(args);
        analyzeList(args);
        init();
        run();
    }

    private static void analyzeList(String[] args) {
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
                    NPVerifyTask.analyzeList.add(line);
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

    public static void init() {
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
                    NPAlgo.setClass.addAll(fh.getAllImplementersOfInterface(klass));
                else if (klassName.equals("java.util.Map"))
                    NPAlgo.mapClass.addAll(fh.getAllImplementersOfInterface(klass));
                else if (klassName.equals("java.util.Collection"))
                    NPAlgo.collectionClass.addAll(fh.getAllImplementersOfInterface(klass));
                else if (klassName.equals("java.util.Iterator"))
                    NPAlgo.iteratorClass.addAll(fh.getAllImplementersOfInterface(klass));
                else if (klassName.equals("java.util.Enumeration"))
                    NPAlgo.enumClass.addAll(fh.getAllImplementersOfInterface(klass));
                else if (klassName.equals("java.util.List"))
                    NPAlgo.listClass.addAll(fh.getAllImplementersOfInterface(klass));
            }
            //if (klass.isApplicationClass()) {
            if (Config.v().isAppClass(klass)) {
                for (SootMethod m : klass.getMethods()) {
                    applicationMethods.add(m);
                    if (m.isStaticInitializer()
                            && !m.getDeclaringClass().isJavaLibraryClass()
                            && cg.edgesInto(m).hasNext()) {
                        NPAlgo.classInitMethods.add(m);
                    }
                }
            }
        }

        ModRef modRef = new ModRef(cg, pta);
        NPAlgo.modMap = modRef.computeMod();
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

            if (NPVerifyTask.analyzeList.size() > 0) {
                boolean needCheck = false;
                // package
                String packageName = method.getDeclaringClass().getPackageName();
                //packageName = packageName.substring(0, packageName.lastIndexOf('.'));
                if (NPVerifyTask.analyzeList.contains(packageName))
                    needCheck = true;
                // class
                String className = method.getDeclaringClass().toString();
                if (NPVerifyTask.analyzeList.contains(className))
                    needCheck = true;
                // method
                String methodName = NPUtil.getMethodSignature(method);
                //logger.error("method: " + methodName);
                if (NPVerifyTask.analyzeList.contains(methodName))
                    needCheck = true;
                if (!needCheck)
                    continue;
            }

            List<Unit> deref = NPVerifyTask.dereferenceMap.get(method);
            if (deref == null) {
                deref = new ArrayList<Unit>();
                NPVerifyTask.dereferenceMap.put(method, deref);
            }
            PatchingChain<Unit> units = activeBody.getUnits();
            Iterator<Unit> unitIter = units.iterator();
            while(unitIter.hasNext()) {
                Unit inst = unitIter.next();
                Value base = NPUtil.getInstBase(inst);
                if (base == null || NPUtil.isThisReference(units, base) || NPUtil.isArrayInst(inst)) continue;
                deref.add(inst);
                NPVerifyTask.totalDeref++;
            }
        }

    }

    public static void run() {

        Date start = new Date();

        ExecutorService executor = Executors.newFixedThreadPool(nthreads);
        List<Future<Boolean>> taskList = new ArrayList<>();

        PointsToAnalysis pta = driver.Main.pta;
        CallGraph cg = driver.Main.pta.getCgb().getCICallGraph();

        for(SootMethod method: NPVerifyTask.dereferenceMap.keySet()) {
            List<Unit> deref = NPVerifyTask.dereferenceMap.get(method);
            logger.debug("ANALYSING METHOD: " + method + "==================");
            for (Unit inst : deref) {
                Value base = NPUtil.getInstBase(inst);
                int currentDeref = NPVerifyTask.currentDeref.incrementAndGet();
                NPVerifyTask task = new NPVerifyTask(method, inst, base, cg, pta);
                //CompletableFuture<Boolean> completableFuture =
                CompletableFuture.supplyAsync(task, executor).thenAccept(safe -> {
                    logger.info("\t[" + currentDeref + " / " + NPVerifyTask.totalDeref
                            + " | " + NPVerifyTask.numOfWrong
                            + "], base: " + base + " , inst: " + inst
                            + " in line: " + inst.getTag("LineNumberTag")
                            + ", steps: " + task.getCurrentGlobalSteps()
                            + (task.isReachBound() ? ", reach max steps" : ""));

                    if (!safe) {
                        logger.error("\t!!! WRONG !!! null pointer deref: " + base +
                                " in inst: " + inst +
                                " of method: " + method +
                                " of line: " + inst.getTag("LineNumberTag"));
                    }
                });
            }
        }
        executor.shutdown();
        while (!executor.isTerminated())
            ;

        Date end = new Date();
        long time = end.getTime() - start.getTime();
        String error = String.format("%.3f", (((double) NPVerifyTask.numOfWrong.get())/((double) NPVerifyTask.totalDeref)));
        String elapse = time / 1000 + "." + (time / 100) % 10;
        logger.info("Whole program analysis completed in " + elapse + " seconds: "
                + " #Derefs: " + NPVerifyTask.totalDeref + ", #NPEs: " + NPVerifyTask.numOfWrong
                + ", #Safe Derefs: " + (NPVerifyTask.numOfSafe.get() + NPVerifyTask.numOfUnknown.get())
                //+ ", Unknown: " + NPVerifyTask.numOfWrong
                + ", Error Rate: " +  error);

        if (NPVerifyTask.enableStatistic) {
            System.out.println("==================== Statistics ====================");
            /*
            System.out.println("\tvirtual calls count: ");
            for (Integer size : NPVerifyTask.virtualCallCounts.keySet()) {
                System.out.println("\t\t" + size + "    :    " + NPVerifyTask.virtualCallCounts.get(size));
            }
            */
            System.out.println("\tWrong instructions: ");
            for (SootMethod m : NPVerifyTask.wrongMap.keySet()) {
                System.out.println("\t\tMethod: " + m); // + NPVerifyTask.wrongMap.get(m).size());
                for (Unit u : NPVerifyTask.wrongMap.get(m)) {
                    System.out.println("\t\t\t" + u + ", line: " + u.getTag("LineNumberTag"));
                }
            }
            /*
            System.out.println("\tMAYBE INSTRUCTIONS: ");
            for (SootMethod m : NPVerifyTask.unknownMap.keySet()) {
                System.out.println("\t\tmethod: " + m); // + NPVerifyTask.wrongMap.get(m).size());
                for (Unit u : NPVerifyTask.unknownMap.get(m)) {
                    System.out.println("\t\t\t" + u + ", line: " + u.getTag("LineNumberTag"));
                }
            }
            */
            System.out.println("====================================================");
        }
    }
}
