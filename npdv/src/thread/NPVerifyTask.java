package thread;

import core.*;
import predicate.BinaryPredicate;
import soot.*;
import soot.jimple.NullConstant;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.toolkits.callgraph.CallGraph;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class NPVerifyTask implements Supplier<Boolean> {
    private boolean earlyTerminate = false;
    private long currentGlobalSteps = 0L;
    private boolean reachBound = false;
    private boolean analyzingClinit = false;

    public static final boolean boundSteps = true;
    public static final boolean globalBound = true;
    public static final int maxSteps = 10000;
    public static final int maxTargets = 100;
    public static final long maxGlobalSteps = 200000L;
    public static int totalDeref = 0;
    public static AtomicInteger currentDeref = new AtomicInteger(0);
    public static AtomicInteger numOfWrong = new AtomicInteger(0);
    public static AtomicInteger numOfUnknown = new AtomicInteger(0);
    public static AtomicInteger numOfSafe = new AtomicInteger(0);

    public static final boolean enableRecursion = true;
    public static final boolean fullPathSensitive = true;
    public static final boolean enableCollection = true;
    public static final boolean enableStatistic = true;

    private final SootMethod method;
    private final Unit inst;
    private final Value base;
    private final CallGraph cg;
    private final PointsToAnalysis pta;


    public static final Set<Unit> collectionDerefs = new ConcurrentHashSet<>();
    public static final Set<Unit> mapDerefs = new ConcurrentHashSet<>();
    public static final Set<String> analyzeList = CollectionFactory.newSet();
    public static final Map<SootMethod, List<Unit>> dereferenceMap = CollectionFactory.newMap();
    public static final Map<SootMethod, List<Unit>> safeMap = new ConcurrentHashMap<>();
    public static final Map<SootMethod, List<Unit>> wrongMap = new ConcurrentHashMap<>();
    public static final Map<SootMethod, List<Unit>> unknownMap = new ConcurrentHashMap<>();
    public static final Map<SootMethod, List<Unit>> reachBoundMap = new ConcurrentHashMap<>();
    public static final Map<Integer, AtomicInteger> virtualCallCounts = new ConcurrentHashMap<>();

    public NPVerifyTask(SootMethod m, Unit i, Value b, CallGraph c, PointsToAnalysis p) {
        method = m;
        inst = i;
        base = b;
        cg = c;
        pta = p;
    }

    public static void reset() {
        totalDeref = 0;
        currentDeref.set(0);
        numOfWrong.set(0);
        numOfSafe.set(0);
        numOfUnknown.set(0);
        dereferenceMap.clear();
        safeMap.clear();
        wrongMap.clear();
        unknownMap.clear();
        reachBoundMap.clear();
        analyzeList.clear();
    }

    public void setAnalyzingClinit(boolean b) {
        analyzingClinit = b;
    }
    public boolean isAnalyzingClinit() {
        return analyzingClinit;
    }
    public void setReachBound(boolean b) {
        reachBound = b;
    }
    public boolean isReachBound() {
        return reachBound;
    }
    public long getCurrentGlobalSteps() {
        return currentGlobalSteps;
    }
    public void incCurrentGlobalSteps() {
        this.currentGlobalSteps++;
    }
    public void setEarlyTerminate(boolean earlyTerminate) {
        this.earlyTerminate = earlyTerminate;
    }
    public boolean isEarlyTerminate() {
        return earlyTerminate;
    }
    public PointsToAnalysis getPta() {
        return pta;
    }
    public CallGraph getCg() {
        return cg;
    }

    @Override
    public Boolean get() {
        State initState = new State();
        this.reachBound = false;
        AccessPath lhs = new AccessPath((Value) base, method);
        AccessPath rhs = new AccessPath((Value) NullConstant.v(), method);
        BinaryPredicate pred = new BinaryPredicate(lhs, rhs, Operator.EQ, false, true);
        initState.addPredicate(pred);
        Set<State> result = new NPAlgo(this).analyzeMethod(method, inst, initState);

        if (this.reachBound) {
            NPUtil.updateMap(method, inst, reachBoundMap);
        }

        if (!result.isEmpty()) {
            result = NPUtil.removeInvalidStates(result);
        }

        if (result.isEmpty()) {
            NPUtil.updateMap(method, inst, safeMap);
            numOfSafe.incrementAndGet();
            return true;
        } else {
            boolean allEmpty = true;
            for (State st : result) {
                if (!st.getAllPreds().isEmpty())
                    allEmpty = false;
            }
            if (allEmpty && this.earlyTerminate) {
                NPUtil.updateMap(method, inst, wrongMap);
                numOfWrong.incrementAndGet();
                return false;
            } else {
                NPUtil.updateMap(method, inst, unknownMap);
                numOfUnknown.incrementAndGet();
                return true;
            }
        }
    }
}
