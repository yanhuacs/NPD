package thread;

import core.*;
import core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import predicate.BinaryPredicate;
import predicate.InstanceOfPredicate;
import predicate.KeyNotExistPredicate;
import predicate.Predicate;
import solver.RuleSolver;
import solver.SATSolver;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.BriefBlockGraph;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class NPAlgo {

    private final Map<SootMethod, Map<State, Set<State>>> methodSummary = CollectionFactory.newMap();
    private final Map<SootMethod, Map<State, Set<State>>> entryStateSummary = CollectionFactory.newMap();
    private final Stack<Context> ctxtStack = CollectionFactory.newStack();
    private final Map<SootMethod, Set<State>> entryMap = CollectionFactory.newMap();
    private final SATSolver solver = new SATSolver();
    private NPVerifyTask npTask;
    private CallGraph cg;
    private PointsToAnalysis pta;

    private final static Logger logger = LoggerFactory.getLogger(NPAlgo.class);

    public static final Map<Unit, Integer> symObjectMap = new ConcurrentHashMap<>();
    public static final AtomicInteger currentSymObject = new AtomicInteger(0);
    public static final Set<SootClass> mapClass = new ConcurrentHashSet<>();
    public static final Set<SootClass> collectionClass = new ConcurrentHashSet<>();

    public static Map<SootMethod, Map<PointsToSet, SootField>> modMap;
    public static final Set<SootMethod> classInitMethods = CollectionFactory.newSet();
    public static final Set<SootClass> iteratorClass = CollectionFactory.newSet();
    public static final Set<SootClass> enumClass = CollectionFactory.newSet();
    public static final Set<SootClass> setClass = CollectionFactory.newSet();
    public static final Set<SootClass> listClass = CollectionFactory.newSet();

    public NPAlgo(NPVerifyTask task) {
       npTask = task;
       cg = npTask.getCg();
       pta = npTask.getPta();
    }

    public void reset() {
        methodSummary.clear();
        entryStateSummary.clear();
        entryMap.clear();
        ctxtStack.clear();
        //solver.reset();
    }

    public Set<State> analyzeMethod(//Stack<Context> ctxtStack,
                                     //Map<SootMethod, Set<State>> entryMap,
                                     SootMethod method,
                                     Unit inst,
                                     State initState) {

        logger.debug("\t\tanalyzing method: " + method + ", initState: " + initState);

        Queue<Object[]> worklist = new LinkedList<Object[]>();
        Map<Block, Set<State>> visitedMap = CollectionFactory.newMap();
        Set<State> result = CollectionFactory.newSet();
        boolean isMap = false, isCollection = false;

        if (NPUtil.isCollectionSubClass(method.getDeclaringClass())) {
            if (initState.hasRootPredicate())
                isCollection = true;
        }

        if (NPUtil.isMapInterface(method)) {
            if (initState.hasRootPredicate())
                isMap = true;
        }

        State startState = State.DummyState;
        if (inst == null) {
            startState = initState;
        }
        ctxtStack.push(new Context(method, startState));

        if ((NPVerifyTask.boundSteps && initState.getStepCount() > NPVerifyTask.maxSteps) ||
                (NPVerifyTask.globalBound && npTask.getCurrentGlobalSteps() > NPVerifyTask.maxGlobalSteps)){
            State trueState = new State();
            //logger.info("reach max step counts.");
            npTask.setReachBound(true);
            return Collections.singleton(trueState);
        }

        if (method.getDeclaringClass().isPhantom() || !method.hasActiveBody())
            return Collections.singleton(initState);
        BriefBlockGraph bbg = new BriefBlockGraph(method.getActiveBody());
        createInitPair(bbg, inst, initState, worklist);

        while(!worklist.isEmpty()) {
            Object[] tuple = worklist.remove();
            Block block = (Block)tuple[0];
            Set<State> wpre = computeBlockWP(method, (Block)tuple[0], (Unit)tuple[1], (State)tuple[2], (Block)tuple[3], bbg);
            //printStates(wpre, method);
            for(Iterator<State> precondIter = wpre.iterator(); precondIter.hasNext();) {
                State precond = precondIter.next();

                //precond.increaseStepCount();

                // null == null
                BinaryPredicate root = (BinaryPredicate) precond.getRootPredicate();
                if (root != null) {
                    if (root.getLhs().isNullConstant() && !root.getLhs().isRef()) {
                        //logger.error("early terminate");
                        npTask.setEarlyTerminate(true);
                        //return Collections.singleton(initState);
                    }
                /*
                } else {
                    for (Predicate pred : precond) {
                        if (pred instanceof BinaryPredicate) {
                            BinaryPredicate bp = (BinaryPredicate) pred;
                            if (bp.getLhs().isNullConstant() && !bp.getLhs().isRef()) {
                                npTask.setEarlyTerminate(true);
                            }
                        }
                    }
                */
                }

                //SAT solver
                logger.debug("\t\t\t\tBEFORE SATSolver, precond: " + precond);
                if (!this.solver.isSatisfied(precond)) {
                    precondIter.remove();
                    logger.debug("\t\t\t\tPRECOND: " + precond + " is !!!!UNSATISFIED!!!!");
                    continue;
                }
                logger.debug("\t\t\t\tAFTER SATSolver, precond: " + precond);

                Set<State> visitedSet = visitedMap.get(block);
                if (visitedSet == null) {
                    visitedSet = CollectionFactory.newSet();
                    visitedSet.add(precond);
                    visitedMap.put(block, visitedSet);
                } else if (State.hasSubset(visitedSet, precond)) { // hasSubset
                    precondIter.remove();
                    continue;
                } else {
                    visitedSet.add(precond);
                }
            }
            //handle entry statement
            List<Block> predBlocks = bbg.getPredsOf(block);
            if (predBlocks.isEmpty()) {
                result.addAll(wpre);
                continue;
            }

            for (Iterator<Block> predBlockIter = predBlocks.iterator(); predBlockIter.hasNext();) {
                Block predBlock = predBlockIter.next();
                Set<State> oldWPre = null;
                Set<State> newWPre = wpre;

                for(State p : newWPre) {
                    worklist.offer(new Object[] {predBlock, null, p, block});
                }
            }
        }

        //Set<State> remove = CollectionFactory.newSet();
        logger.debug("\t\t\t\tworklist is empty, result is: " + result);
        for (State st : result) {
            if (!st.hasRootPredicate()) {
                if (isCollection)
                    NPVerifyTask.collectionDerefs.add(inst);
                if (isMap)
                    NPVerifyTask.mapDerefs.add(inst);
            }
        }
        //result.removeAll(remove);

        Context ctx = ctxtStack.pop();
        // TODO: handle context (recursion)
        if (ctx.recursion) {
            assert inst == null;

            Set<State> oldSummary = this.getSummary(method, initState);
            if (oldSummary != null && result.equals(oldSummary))
                ctx.recursion = false;
            else {
                for (Context recurCtx : ctx.contextsInRecursion) {
                    Map<State, Set<State>> entry = this.methodSummary.get(recurCtx.method);
                    if (entry != null)
                        entry.remove(recurCtx.state);
                }
                ctx.contextsInRecursion = CollectionFactory.newList();
                addToSummary(method, initState, result);
                //ctx.recursion = false;
                // FIXME
                /*
                if (stackCount < NPVerifier.maxStackCount)
                    result = analyzeMethod(ctxtStack, entryMap, method, null, initState);
                    */
            }
        }

        if (inst == null) {
            addToSummary(method, initState, result);
        }

        logger.debug("\t\tfinished analyzing method: " + method + ", resultState: " + result);
        if (method.getName().equals("main")) {
            npTask.setAnalyzingClinit(true);
            Set<State> outStates = CollectionFactory.newSet();

            for (SootMethod init: NPAlgo.classInitMethods) {
                for (State st : result) {
                    Set<Predicate> preds = st.getAllPreds();
                    if (preds != null) {
                        for (Predicate pred : preds) {
                            AccessPath[] allAP = pred.getAllAccessPaths();
                            for (AccessPath ap : allAP) {
                                ap.setMethod(init);
                            }
                        }
                        outStates.addAll(analyzeMethod(init, null, st));
                    }
                }
                result = outStates;
            }
        }

        if (!ctxtStack.isEmpty() || npTask.isAnalyzingClinit())
            return result;
        else
            return analyzePredecessors(method, result); //, entryMap);
    }

    private Set<State> analyzePredecessors(SootMethod method, Set<State> result) { //, Map<SootMethod, Set<State>> entryMap) {

        logger.debug("\t\t\t|||analyzing predecessor|||: " + method + ", result: " + result);
        if (!this.entryStateSummary.containsKey(method)) {
            this.entryStateSummary.put(method, CollectionFactory.newMap());
        }

        Set<State> reachSet = entryMap.get(method);
        if (reachSet == null)
            reachSet = CollectionFactory.newSet();
        Set<State> newStates = CollectionFactory.newSet();
        for (State r : result) {
            // hasSubSet
            if (!NPUtil.hasSubset(reachSet, r)) {
                newStates.add(r);
                reachSet.add(r);
            }
        }
        entryMap.put(method, reachSet);

        if (NPVerifyTask.boundSteps) {
            for (State s : result) {
                if (s.getStepCount() > NPVerifyTask.maxSteps) {
                    return Collections.singleton(new State());
                }
            }
        }

        Set<Edge> callingEdgeSet = NPUtil.getCallGraphNode(method, cg);
        // if no method calling the current method
        if (callingEdgeSet.isEmpty())
            return newStates;

        Iterator<Edge> callerIter = callingEdgeSet.iterator();
        boolean enteredPred = false;
        Set<State> entryStates = CollectionFactory.newSet();
        outerMost: while(callerIter.hasNext()) {
            Edge edge = (Edge)callerIter.next();
            SootMethod caller = (SootMethod) edge.getSrc().method();
            // TODO: ignore native methods
            if (caller.isNative()) continue;

            // TODO: handle call-backs

            Unit inst = edge.srcUnit();
            if (inst == null) continue;
            /*
            InvokeStmt invokeStmt = null;
            if (inst instanceof InvokeStmt) {
                invokeStmt = (InvokeStmt) inst;
            } else if (inst instanceof AssignStmt) {
                invokeStmt = Jimple.v().newInvokeStmt(((AssignStmt)inst).getRightOp());
            }
            */
            //invokeStmt.getInvokeExpr().getArgs();
            for (State st : newStates) {
                State callerState = NPUtil.getCallerStates(method, caller, inst, st);
                //special handle android dummyMain
                if (caller.getName().equals("main") && caller.getDeclaringClass().getName().equals("dummyMainClass")) {
                    for (Predicate newPred : callerState) {
                        AccessPath[] allAP = newPred.getAllAccessPaths();
                        if (allAP[0].isNullConstant() && allAP[1].isNullConstant()) {
                            Predicate foundPred = null;
                            for (Predicate oldPred : st) {
                                AccessPath[] oldAP = oldPred.getAllAccessPaths();
                                if (oldAP[1].equals(allAP[1])) {
                                    foundPred = oldPred;
                                    break;
                                }
                            }
                            if (foundPred != null) {
                                Value value = foundPred.getAllAccessPaths()[0].getLocal();
                                if (value instanceof ParameterRef) {
                                    Type type = ((ParameterRef) value).getType();
                                    Local symObject = Jimple.v().newLocal("local_" + type, type);
                                    symObject.setName("symObj_" + NPUtil.getOrCreateSymObject(inst));
                                    // a symbolic object is a compressed AP
                                    AccessPath symObjAP = new AccessPath(symObject, method, inst, true);
                                    newPred.replace(allAP[0], symObjAP);
                                }
                            }
                        }
                    }
                }

                if (callerState.getPreds().isEmpty()) {
                    entryStates.add(callerState);
                } else {
                    //Set<State> resultStates = new npVerifier(this.cg).analyzeMethod(ctxtStack, entryMap, caller, inst, callerState);
                    State hitState = NPUtil.hasSubsetAndRootPred(this.entryStateSummary.get(method).keySet(), callerState);
                    Set<State> resultStates = null;
                    if (hitState == null) {
                        //resultStates = this.newInstance().analyzeMethod(ctxtStack, entryMap, caller, inst, callerState);
                        //npVerifier.entryStateSummary.get(currentMethod).put(callerState, resultStates);
                        resultStates = analyzeMethod(caller, inst, callerState);
                        this.entryStateSummary.get(method).put(callerState, resultStates);
                    } else {
                        resultStates = this.entryStateSummary.get(method).get(hitState);
                        assert (resultStates != null);
                    }
                    entryStates.addAll(resultStates);
                }
                enteredPred = true;
                if (!entryStates.isEmpty())
                    break outerMost;
            }
        }

        logger.debug("\t\t\t|||finish analyzing predecessor|||: " + method + ", result: " + (enteredPred? entryStates : newStates));
        if (enteredPred)
            return entryStates;
        else
            return newStates;
    }

    private void createInitPair(BlockGraph bbg, Unit inst, State initState, Queue<Object[]> workList) {
        if (inst != null) {
            Block instBlock = null;
            for (Block bb : bbg) {
                for (Iterator<Unit> unitIter = bb.iterator(); unitIter.hasNext(); ) {
                    if (inst == (Unit) unitIter.next()) {
                        instBlock = bb;
                        break;
                    }
                }
            }
            if (instBlock == null) {
                throw new IllegalStateException("Abort analysis of instruction: " + inst + ", since it is not in CFG");
            }
            Object[] ret = new Object[4];
            ret[0] = instBlock;
            ret[1] = inst;
            ret[2] = initState;
            ret[3] = null;
            workList.add(ret);
        } else {
            List<Block> tails = bbg.getTails();
            for (Block bb : tails) {
                Object[] ret = new Object[4];
                ret[0] = bb;
                ret[1] = null;
                ret[2] = initState;
                ret[3] = null;
                workList.add(ret);
            }
        }
    }

    private Set<State> getSummary(SootMethod method, State state) {
        Set<State> outStates = null;
        Map<State, Set<State>> summaryFunction = this.methodSummary.get(method);
        if (summaryFunction != null) {
            outStates = summaryFunction.get(state);
        }
        return outStates;
    }

    private void addToSummary(SootMethod method, State st, Set<State> result) {
        Map<State, Set<State>> summaryFunction = this.methodSummary.get(method);
        if (summaryFunction == null) {
            summaryFunction = CollectionFactory.newMap();
            summaryFunction.put(st, result);
            this.methodSummary.put(method, summaryFunction);
        } else {
            summaryFunction.put(st, result);
        }
    }

    private Set<State> computeBlockWP(SootMethod method, Block block, Unit inst, State state, Block succBlock, BriefBlockGraph bbg) {
        Set<State> result;
        int instIndex = -1;
        Vector<Unit> instVec = new Vector<Unit>();
        for (Unit u : block) instVec.add(u);

        if (inst != null) {
            for (int i = instVec.size() - 1; i >= 0; i--) {
                Unit tempInst = instVec.elementAt(i);
                if (inst == tempInst) {
                    instIndex = i - 1;
                    break;
                }
            }
        } else {
            instIndex = instVec.size() - 1;
        }

        if (instIndex < 0) {
            result = CollectionFactory.newSet();
            result.add(state);
            return result;
        }

        Set<State> inputPosts = CollectionFactory.newSet();
        Set<State> outputPres = CollectionFactory.newSet();
        inputPosts.add(state);

        //logger.info("\t\t\tcomputeBlockWP, METHOD: " + method.getName());
        for (int i = instIndex; i >= 0; i--) {
            Unit tempInst = instVec.elementAt(i);
            if (tempInst != null) {
                outputPres = CollectionFactory.newSet();
                for (State inputPost : inputPosts) {
                    inputPost.increaseStepCount();

                    npTask.incCurrentGlobalSteps();
                    outputPres.addAll(computeInstWP(method, block, tempInst, inputPost, succBlock));
                }
            } else {
                outputPres = inputPosts;
            }
            logger.debug("\t\t\t\tBLOCK " + block.getIndexInMethod() + " (" + method.getName() + ") INST: " + tempInst
                    + ", IN: " + inputPosts + ", OUT: " + outputPres
                    + ", current global steps: " + npTask.getCurrentGlobalSteps());
            inputPosts = outputPres;
        }
        result = outputPres;

        // deal with <init>
        if (method.getName().equals("<init>") && bbg.getPredsOf(block).isEmpty()) {
            logger.debug("\t\t\t\tSPECIAL <init> handle, before: " + result);
            SootClass klass = method.getDeclaringClass();
            Iterator<SootField> iter = klass.getFields().iterator();
            Set<SootField> fieldSet = CollectionFactory.newSet();
            while (iter.hasNext()) {
                SootField ref = iter.next();
                fieldSet.add(ref);
            }
            if (fieldSet.size() > 0) {
                for (State st : result) {
                    for (Predicate pred : st.getAllPreds()) {
                        AccessPath[] aps = pred.getAllAccessPaths();
                        for (int i = 0; i < aps.length; ++i) {
                            if (aps[i].numOfFields() == 1) {
                                FieldRef ref = (FieldRef) aps[i].getFieldAt(1);
                                SootField sootField = ref.getField();
                                if (fieldSet.contains(sootField)) {
                                    AccessPath nullAP = new AccessPath(NullConstant.v(), method);
                                    //aps[i].replace(aps[i], nullAP);
                                }
                            }
                        }
                    }
                }
            }
            logger.debug("\t\t\t\tSPECIAL <init> handle, after: " + result);
        }
        return result;
    }

    private Set<State> computeInstWP(SootMethod method, Block block, Unit inst, State postcond, Block succBlock) {
        Set<State> result = CollectionFactory.newSet();
        if (NPUtil.isRootPredProvedSafe(postcond))
            return result;

        //if (inst instanceof InvokeStmt)
        if (inst instanceof IdentityStmt) {
            Value lhs = ((IdentityStmt)inst).getLeftOp();
            Value rhs = ((IdentityStmt)inst).getRightOp();
            State precond = postcond.clone();
            AccessPath defAP, useAP = null;
            defAP = new AccessPath(lhs, method);
            if (lhs instanceof Local) {
                if (rhs instanceof ThisRef) {
                    //Value def = Jimple.v().newThisRef((RefType)rhs.getType());
                    useAP = new AccessPath(rhs, method);
                } else if (rhs instanceof ParameterRef) {
                    useAP = new AccessPath(rhs, method);
                } else {
                    useAP = new AccessPath(rhs, method);
                    //throw new IllegalArgumentException("IdentityStmt is: " + inst);
                }

            }
            /*

            Value rhs = identityStmt.getRightOp();
            if (rhs instanceof ThisRef) {
                ThisRef thisRef = (ThisRef)rhs;
            }
            */
            result.addAll(NPUtil.replace(postcond, defAP, useAP, pta));
            //result.add(postcond.clone());
            //result.addAll(identityStmtTransFunction(method, block, inst, postcond, succBlock));
        } else if (inst instanceof AssignStmt) {
            Value lval = ((AssignStmt) inst).getLeftOp();
            Value rval = ((AssignStmt) inst).getRightOp();
            NPVerifier.FieldAccessType type = NPVerifier.FieldAccessType.GETFIELD;
            Value base = null;
            FieldRef ref = null;
            if (rval instanceof StaticFieldRef) {
                ref = (StaticFieldRef) rval;
                type = NPVerifier.FieldAccessType.GETFIELD;
                //base = ClassConstant.fromType(method.getDeclaringClass().getType());
                base = ClassConstant.fromType(ref.getField().getDeclaringClass().getType());
                result.addAll(fieldAccessTransFunc(method, type, base, ref, (AssignStmt)inst, postcond));
            } else if (lval instanceof StaticFieldRef) {
                ref = (StaticFieldRef) lval;
                type = NPVerifier.FieldAccessType.PUTFIELD;
                //base = ClassConstant.fromType(method.getDeclaringClass().getType());
                base = ClassConstant.fromType(ref.getField().getDeclaringClass().getType());
                result.addAll(fieldAccessTransFunc(method, type, base, ref, (AssignStmt)inst, postcond));
            } else if (rval instanceof InstanceFieldRef) {
                base = ((InstanceFieldRef) rval).getBase();
                type = NPVerifier.FieldAccessType.GETFIELD;
                ref = (FieldRef) rval;
                result.addAll(fieldAccessTransFunc(method, type, base, ref, (AssignStmt) inst, postcond));
            } else if (lval instanceof InstanceFieldRef) {
                base = ((InstanceFieldRef) lval).getBase();
                type = NPVerifier.FieldAccessType.PUTFIELD;
                ref = (FieldRef) lval;
                result.addAll(fieldAccessTransFunc(method, type, base, ref, (AssignStmt) inst, postcond));
            } else if (lval instanceof Local) {
                if (rval instanceof Local) {
                    // example: r4 = r0
                    AccessPath useAP = new AccessPath(rval, method);
                    AccessPath defAP = new AccessPath(lval, method);

                    //result.add(precond);
                    result.addAll(NPUtil.replace(postcond, defAP, useAP, pta));
                } else if (rval instanceof InvokeExpr) {
                    //
                    result.addAll(invokeExprTransFunc(method, inst, (InvokeExpr) rval, postcond));
                } else if (rval instanceof BinopExpr) {
                    AccessPath defAP = new AccessPath(lval, method);
                    State precond = postcond.clone();
                    Iterator<Predicate> predIter = precond.iterator();
                    while (predIter.hasNext()) {
                        Predicate pred = predIter.next();
                        if (pred.containsAP(defAP)) {
                            predIter.remove();
                        }
                    }
                    result.add(precond);
                } else if (rval instanceof NewExpr || rval instanceof NewArrayExpr) {
                    State outState = postcond.clone();
                    Local symObject = (Local) lval.clone();
                    symObject.setName("symObj_" + NPUtil.getOrCreateSymObject(inst));
                    // a symbolic object is a compressed AP
                    AccessPath symObjAP = new AccessPath(symObject, method, inst, true);
                    AccessPath defAP = new AccessPath(lval, method);
                    result.addAll(NPUtil.replace(outState, defAP, symObjAP, pta));
                } else if (rval instanceof NullConstant) {
                    AccessPath useAP = new AccessPath(NullConstant.v(), method);
                    AccessPath defAP = new AccessPath(lval, method);

                    //result.add(precond);
                    result.addAll(NPUtil.replace(postcond, defAP, useAP, pta));
                } else if (rval instanceof StringConstant) {

                    AccessPath useAP = new AccessPath(rval, method);
                    AccessPath defAP = new AccessPath(lval, method);
                    result.addAll(NPUtil.replace(postcond, defAP, useAP, pta));

                } else if (rval instanceof NumericConstant) {
                    result.add(postcond.clone());

                } else if (rval instanceof LengthExpr) {
                    /*
                    LengthExpr lengthExpr = (LengthExpr) rval;
                    Value arrayBase = lengthExpr.getOp();
                    AccessPath arrayAP = new AccessPath(arrayBase, method);
                    State precond = postcond.clone();
                    if (aliasWithAPInRootPredicate(precond, arrayAP))
                        precond.addPredicate(BinaryPredicate.createNonNullPred(arrayAP, method));

                    result.add(precond);
                    */
                    result.add(postcond.clone());
                } else if (rval instanceof CastExpr) {
                    CastExpr castExpr = (CastExpr) rval;
                    AccessPath useAP = new AccessPath(castExpr.getOp(), method, inst);
                    AccessPath defAP = new AccessPath(lval, method);
                    State predcond = postcond.clone();
                    result.addAll(NPUtil.replace(predcond, defAP, useAP, pta));
                } else if (rval instanceof InstanceOfExpr) {
                    InstanceOfExpr ioe = (InstanceOfExpr) rval;
                    boolean neg = false;
                    for (Unit u : block) {
                        if (u instanceof IfStmt) {
                            IfStmt ifStmt = (IfStmt) u;
                            Expr cond = (Expr) ifStmt.getCondition();
                            if (cond instanceof NeExpr) {
                                if (((NeExpr) cond).getOp1().equivTo(lval)) {
                                    neg = true;
                                }
                            }
                        }
                    }

                    InstanceOfPredicate newAP = new InstanceOfPredicate(new AccessPath(ioe.getOp(), method, inst),
                            ioe.getCheckType(), neg);
                    State precond = postcond.clone();
                    precond.addPredicate(newAP);
                    result.add(precond);
                } else {
                    result.add(postcond.clone());
                }
            } else {
                result.add(postcond.clone());
            }
        } else if (inst instanceof ReturnStmt) {
            Value ret = ((ReturnStmt) inst).getOp();
            AccessPath acturalRetAP = new AccessPath(ret, method, inst);
            AccessPath dummyRetAP = null;
            if (ret instanceof NullConstant)
                dummyRetAP = new AccessPath(Jimple.v().newLocal("retObj", method.getReturnType()), method);
            else
                dummyRetAP = new AccessPath(Jimple.v().newLocal("retObj", ret.getType()), method);
            //AccessPath dummyRetAP = new AccessPath(Jimple.v().newLocal("retObj", ret.getType()), method);
            State precond = postcond.clone();
            for (Predicate pred : postcond) {
                Predicate newPred = pred.clone();
                newPred.replace(dummyRetAP, acturalRetAP);
                precond.removePredicate(pred);
                precond.addPredicate(newPred);
            }
            result.add(precond);

        } else if (inst instanceof InvokeStmt) {
            //
            result.addAll(invokeInstTransFunc(method, inst, (InvokeStmt) inst, postcond));
        } else if (inst instanceof IfStmt) {
            IfStmt ifStmt = (IfStmt) inst;

            result.add(conditionInstTransFunc(method, block, ifStmt, postcond, succBlock));
            // exception, throw, arraystore, arrayload, arraylength, checkcast,
        } else if (inst instanceof ThrowStmt) {
            /*
            AccessPath returnAP = new AccessPath(Jimple.v().newLocal("retObj", method.getReturnType()), method);
            for (Predicate pred : postcond) {
                AccessPath[] aps = pred.getAllAccessPaths();
                for (int i = 0; i < aps.length; ++i) {
                    if (aps[i].contains(returnAP))
                        return result;
                }
            }
            */
            State precond = postcond.clone();
            if (precond.getAllPreds().size() > 0) {
                for (Predicate pred : precond) {
                    if (pred instanceof BinaryPredicate) {
                        BinaryPredicate bp = (BinaryPredicate)pred.clone();
                        bp.unNegate();
                        bp.setOp(RuleSolver.getComplementOp(bp.getOp()));
                        precond.addPredicate(bp);
                        break;
                    }
                }
                result.add(precond);
            }
        } else {
            result.add(postcond.clone());
        }
        return result;
    }

    private Set<State> fieldAccessTransFunc(SootMethod method, NPVerifier.FieldAccessType type, Value base, FieldRef ref,
                                                   AssignStmt inst, State postcond) {
        AccessPath useAP = null, defAP = null;
        if (type == NPVerifier.FieldAccessType.GETFIELD) {
            useAP = new AccessPath(base, ref, method, inst);
            defAP = new AccessPath(inst.getLeftOp(), method);
        } else if (type == NPVerifier.FieldAccessType.PUTFIELD) {
            useAP = new AccessPath(inst.getRightOp(), method, inst);
            defAP = new AccessPath(base, ref, method);
        } else
            throw new IllegalArgumentException("Invalid field access instruction: ");
        State precond = postcond.clone();
        AccessPath baseAP = new AccessPath(base, method, inst);
        if (npTask.fullPathSensitive) {
            precond.addPredicate(BinaryPredicate.createNonNullPred(baseAP, method));
        } else {
            if (NPUtil.aliasWithAPInRootPredicate(precond, baseAP, pta))
                precond.addPredicate(BinaryPredicate.createNonNullPred(baseAP, method));
        }
        return NPUtil.replace(precond, defAP, useAP, pta);
    }

    private Set<State> invokeInstTransFunc(SootMethod method, Unit inst, InvokeStmt invokeStmt, State postcond) {
        InvokeExpr expr = invokeStmt.getInvokeExpr();
        return invokeExprTransFunc(method, inst, expr, postcond);
    }

    private Set<State> invokeExprTransFunc(SootMethod method, Unit inst, InvokeExpr expr, State postcond) {
        logger.debug("\t\t\t\t*INVOKEEXPR*: " + inst + " in method: " + method + ", state: " + postcond);
        Set<State> outStates = CollectionFactory.newSet();
        State metaState = postcond.clone();
        Value base = null;
        if (!(expr instanceof StaticInvokeExpr)) {
            //inst.get
            if (expr instanceof SpecialInvokeExpr) {
                base = ((SpecialInvokeExpr) expr).getBase();
            } else if (expr instanceof VirtualInvokeExpr) {
                base = ((VirtualInvokeExpr) expr).getBase();
            } else if (expr instanceof InterfaceInvokeExpr) {
                base = ((InterfaceInvokeExpr) expr).getBase();
            }
            assert (base != null);
            AccessPath recvAP = new AccessPath(base, method, inst);
            if (npTask.fullPathSensitive) {
                metaState.addPredicate(BinaryPredicate.createNonNullPred(recvAP, method));
            } else {
                if (NPUtil.aliasWithAPInRootPredicate(metaState, recvAP, pta))
                    metaState.addPredicate(BinaryPredicate.createNonNullPred(recvAP, method));
            }

        }

        SootMethod m;
        try {
            m = expr.getMethod();
        } catch (Exception e) {
            outStates.add(metaState);
            return outStates;
        }
        String name = NPUtil.getMethodSignature(m);

        if (inst instanceof AssignStmt) {
            if (NPUtil.isNotNull(m.getDeclaringClass().getName(), m.getName())) {
                State out = postcond.clone();
                AssignStmt stmt = (AssignStmt) inst;
                AccessPath lhs = new AccessPath(stmt.getLeftOp(), method, inst);
                out.addPredicate(BinaryPredicate.createNonNullPred(lhs, method));
                outStates.add(out);
                return outStates;
            }
        }

        if (!NPUtil.invokeInstPossible(m)) {
            outStates.add(metaState);
            return outStates;
        }

        // TODO: get all possible targets due to virtual call
        Set<Edge> targetSet = NPUtil.getPossibleTargets(inst, cg);
        if (targetSet.size() == 0) {

            outStates.add(metaState);
            return outStates;
        } else {
            if (NPVerifyTask.enableStatistic) {
                Integer size = targetSet.size();
                NPVerifyTask.virtualCallCounts.computeIfAbsent(size, k -> new AtomicInteger(1))
                        .incrementAndGet();
            }
        }

        if (NPVerifyTask.enableCollection) {
            if (NPUtil.isLibraryCollection(targetSet)) {
                SootMethod target = targetSet.iterator().next().tgt();
                return invokeCollectionInstTransFunc(target, inst, base, method, metaState);
            }
            if (NPUtil.isLibraryMap(targetSet)) {
                SootMethod target = targetSet.iterator().next().tgt();
                return invokeMapInstTransFunc(target, inst, base, method, metaState);
            }
        }

        int targetCount = 0;
        for (Edge e : targetSet) {
            SootMethod target = e.tgt();
            if (target.isNative() || !NPUtil.invokeInstPossible(target) || target.getName().equals("<clinit>")) {
                //outStates.add(metaState);
                continue;
            }
            Map<Value, Value> acturalToFormal = NPUtil.mapActuralToFormal(inst, expr);
            Map<Value, Value> formalToActural = NPUtil.invertMap(acturalToFormal);

            Set<Predicate> modPreds = CollectionFactory.newSet();
            Set<Predicate> retPreds = CollectionFactory.newSet();
            Set<Predicate> unModPreds = CollectionFactory.newSet();
            computeModPreds(method, target, inst, metaState.getPreds(), modPreds, retPreds, unModPreds);

            String targetName = NPUtil.getMethodSignature(target);
            if (target.isStaticInitializer() ||
                    analyzeLibCallList.contains(targetName)) {
                retPreds.addAll(modPreds);
            } else if (target.isJavaLibraryMethod()) {
                metaState.removeAllPredicates(modPreds);
            } else {
                retPreds.addAll(modPreds);
            }

            if (!metaState.hasRootPredicate() && postcond.hasRootPredicate()) {

            }

            if (retPreds.isEmpty()) {
                outStates.add(metaState);
                continue;
            }
            // FIXME no mod ref
            /*
            for (Predicate pred : metaState) {
                retPreds.add(pred);
            }
            */

            Set<Predicate> modMappedPreds = CollectionFactory.newSet();
            Set<Predicate> modUnmappedPreds = CollectionFactory.newSet();
            State calleeState = new State();
            calleeState.resetState(metaState);
            NPUtil.mapPredicates(retPreds, acturalToFormal, method, target, modMappedPreds, modUnmappedPreds);
            //mapActuralToFormal(retPreds, acturalToFormal, currentMethod, target, modMappedPreds, modUnmappedPreds);
            calleeState.addAllPredicates(modMappedPreds);
            calleeState.addAllPredicates(modUnmappedPreds);
            if (!calleeState.hasRootPredicate() && postcond.hasRootPredicate())
                calleeState.addPredicate(postcond.getRootPredicate());


            State hitState = null;
            Set<State> resultStates = CollectionFactory.newSet();
            if (this.methodSummary.containsKey(target) &&
                    ((hitState = NPUtil.hasSubsetAndRootPred(this.methodSummary.get(target).keySet(), calleeState)) != null)) {
                Set<State> summaryStates = this.methodSummary.get(target).get(hitState);
                assert (summaryStates != null);

                for (State summaryState : summaryStates) {
                    State summaryClone = summaryState.clone();
                    summaryClone.resetState(metaState);
                    summaryClone.increaseStepCount();
                    resultStates.add(summaryClone);
                }
            } else if (targetSet.size() > NPVerifyTask.maxTargets) {
                State trueState = new State();
                trueState.resetState(calleeState);
                resultStates.add(trueState);

            } else {
                Context callContext = new Context(target, calleeState);
                boolean ctxFound = false;
                Context ctx = null;
                if (NPUtil.getMethodSignature(target).contains("print(java.lang.StringBuilder,java.util.Calendar,char,java.util.Locale)")) {
                    ctx = null;//throw new IllegalArgumentException("");
                }
                for (Context c : this.ctxtStack) {
                    if (c.equalsStateSubset(callContext)) {
                        ctxFound = true;
                        ctx = c;
                    }
                }
                if (!ctxFound) {
                    //resultStates = new npVerifier(this.cg).analyzeMethod(ctxtStack, entryMap, target, null, calleeState);
                    logger.debug("\t\t\t\tANALYZING target " + (targetCount++) + " : " + target + " of method " + method + ", state: " + calleeState);
                    //resultStates = this.newInstance().analyzeMethod(ctxtStack, entryMap, target, null, calleeState);
                    resultStates = analyzeMethod(target, null, calleeState);

                } else {
                    if (!npTask.enableRecursion) {
                        State trueState = new State();
                        trueState.resetState(calleeState);
                        resultStates.add(trueState);
                    } else {

                        ctx.recursion = true;
                        resultStates.clear();
                        Set<State> summaryStates = getSummary(method, calleeState);
                        if (summaryStates != null) {
                            for (State summary : summaryStates) {
                                State clone = summary.clone();
                                clone.resetState(metaState);
                                clone.increaseStepCount();
                                resultStates.add(clone);
                            }
                        }
                        int index = ctxtStack.size() - 1;
                        Context elem;
                        while ((elem = ctxtStack.elementAt(index)) != ctx) {
                            ctx.contextsInRecursion.add(elem);
                            index--;
                        }
                    }
                }
            }
            for (State resultState : resultStates) {
                Set<Predicate> unMapCalleePreds = CollectionFactory.newSet();
                Set<Predicate> mapCalleePreds = CollectionFactory.newSet();
                NPUtil.mapPredicates(resultState.getPreds(), formalToActural, target, method, mapCalleePreds, unMapCalleePreds);
                State callerState = new State();
                callerState.resetState(resultState);
                callerState.addAllPredicates(mapCalleePreds);

                for (Predicate unMap : unMapCalleePreds) {
                    if (unMap.hasNoLocalAP(target))
                        callerState.addPredicate(unMap);
                }
                callerState.addAllPredicates(unModPreds);
                if (resultState.getRootPredicate() == null) {
                    //callerState.addPredicate(postcond.getRootPredicate());
                    //callerState.setRootPredicate(postcond.getRootPredicate());
                }
                outStates.add(callerState);
            }
        }
        return outStates;
    }

    private State conditionInstTransFunc(SootMethod method, Block block, IfStmt inst, State postcond, Block succBlock) {
        boolean fallThrough = false;
        Unit target = inst.getTarget();
        Iterator<Unit> instIter = succBlock.iterator();
        while (instIter.hasNext()) {
            Unit tempInst = instIter.next();
            if (tempInst == target)
                fallThrough = true;
        }

        ConditionExpr cond = (ConditionExpr) inst.getCondition();
        AccessPath lhs = new AccessPath(cond.getOp1(), method, inst);
        AccessPath rhs = new AccessPath(cond.getOp2(), method, inst);

        // TODO partial path sensitivity
        if (!NPVerifyTask.fullPathSensitive) {
            if ((lhs.isConstant() || rhs.isConstant())
                    && NPUtil.aliasWithAPInRootPredicate(postcond, lhs.isConstant() ? rhs : lhs, pta)) {
                BinaryPredicate bp = new BinaryPredicate(lhs, rhs, NPUtil.getConditionOperator(cond), !fallThrough, false);
                State precond = postcond.clone();
                precond.addPredicate(bp);
                return precond;
            }
            return postcond;
        } else {
            if (rhs.getLocal() instanceof NullConstant) {
                BinaryPredicate bp = new BinaryPredicate(lhs, rhs, NPUtil.getConditionOperator(cond), !fallThrough, false);
                State precond = postcond.clone();
                precond.addPredicate(bp);
                return precond;
            } else if (lhs.getLocal() instanceof NullConstant) {
                BinaryPredicate bp = new BinaryPredicate(rhs, lhs, NPUtil.getConditionOperator(cond), !fallThrough, false);
                State precond = postcond.clone();
                precond.addPredicate(bp);
                return precond;
            } else {
                return postcond;
            }
        }
    }

    private void computeModPreds(SootMethod currentMethod, SootMethod target, Unit inst, Set<Predicate> inputPreds, Set<Predicate> modPreds,
                                 Set<Predicate> retPreds, Set<Predicate> unModPreds) {
        Value def = null;
        if (inst instanceof AssignStmt) {
            def = ((AssignStmt) inst).getLeftOp();
        }

        Map<PointsToSet, SootField> modMap = this.modMap.get(target);
        if (modMap == null)
            modMap = CollectionFactory.newMap();

        //TODO: skipList
        if (NPAlgo.skipMethodList.contains(NPUtil.getMethodSignature(target))) {
            modMap.clear();
        }

        for (Predicate pred : inputPreds) {
            AccessPath[] allAPs = pred.getAllAccessPaths();
            Set<PointsToSet> predPts = CollectionFactory.newSet();
            boolean isModified = false;

            for (int i = 0; i < allAPs.length; ++i) {
                AccessPath ap = allAPs[i];
                if (def != null) {
                    AccessPath targetAP = new AccessPath(def, currentMethod);
                    if (ap.contains(targetAP)) {
                        retPreds.add(pred);
                        isModified = true;
                        break;
                    }
                }
                if (ap.isElemFieldContained()) {
                    ap = ap.getAliasedAP();
                }
                if (ap.isRef() && !ap.isElemFieldContained()) {
                    Map<Integer, PointsToSet> apPts = ap.getPointsToSet(pta);
                    for (Integer fieldNum : apPts.keySet()) {
                        predPts.add(apPts.get(fieldNum));
                    }
                } else {
                    if (def != null) {
                        AccessPath targetAP = new AccessPath(def, currentMethod);
                        if (ap.contains(targetAP)) {
                            retPreds.add(pred);
                            isModified = true;
                            break;
                        }
                    }
                }
            }

            if (!retPreds.contains(pred)) {
                AccessPath[] aps = pred.getAllAccessPaths();
                for (int i = 0; i < aps.length; ++i) {
                    AccessPath ap = allAPs[i];
                    if (ap.isElemFieldContained() &&
                            !(NPAlgo.skipMethodList.contains(NPUtil.getMethodSignature(target)))) {
                        Map<Integer, PointsToSet> elemPredPts = ap.getPointsToSet(pta);
                        // TODO:

                    }
                }

                out: for (PointsToSet pts : predPts) {
                    for (PointsToSet modPts : modMap.keySet()) {
                        if (modPts.hasNonEmptyIntersection(pts)) {
                            modPreds.add(pred);
                            isModified = true;
                            break out;
                        }
                    }
                }
                /*
                modPreds.add(pred);
                isModified = true;
                */
            }
            if (!isModified)
                unModPreds.add(pred);
        }
        // bypass modref
        //modPreds.addAll(inputPreds);
    }

    private Set<State> invokeCollectionInstTransFunc(SootMethod target, Unit inst, Value base,
                                                     SootMethod method, State postcond) {
        Set<State> outStates = CollectionFactory.newSet();
        AccessPath defAP = null;
        AccessPath receiveAP = null;
        InvokeExpr expr = null;
        if (inst instanceof AssignStmt) {
            defAP = new AccessPath(((AssignStmt) inst).getLeftOp(), method);
            expr = (InvokeExpr)((AssignStmt) inst).getRightOp();
        } else if (inst instanceof InvokeExpr) {
            expr = (InvokeExpr) inst;
        } else if (inst instanceof InvokeStmt) {
            expr = ((InvokeStmt) inst).getInvokeExpr();
        }
        if (expr == null)
            throw new IllegalArgumentException("expr is null");
        assert expr != null;
        if (!(expr instanceof StaticInvokeExpr)) {
            receiveAP = new AccessPath(base, method);
        }
        ElementRef elementRef = new ElementRef();
        IteratorFieldRef iteratorFieldRef = new IteratorFieldRef();
        outStates.add(postcond);
        String targetName = target.getName();
        if (CollectionInfo.isReadIterator(targetName)) {

            if (defAP == null) {
                outStates.add(postcond);
                return outStates;
            }
            elementRef.setAliasAP(defAP);
            AccessPath receiveAPElem = getAPIterElem(receiveAP, elementRef, method, inst);
            State metaState = postcond.clone();
            metaState.addPredicate(BinaryPredicate.createNonNullPred(receiveAP, method));
            outStates.clear();
            outStates.addAll(NPUtil.replace(metaState, defAP, receiveAPElem, pta));

        } else if (CollectionInfo.isGetIterator(targetName)) {
            if (defAP == null) {
                outStates.add(postcond);
                return outStates;
            }
            AccessPath defApIter = new AccessPath(defAP.getLocal(), iteratorFieldRef, method);
            State metaState = postcond.clone();
            for (Predicate pred : metaState) {
                AccessPath[] allAPs = pred.getAllAccessPaths();
                for (int i = 0; i < allAPs.length; ++i) {
                    AccessPath predAP = allAPs[i];
                    if (predAP.isInstanceRef()) {
                        if (predAP.contains(defApIter)) {
                            NPUtil.updateState(postcond, pred, defApIter, receiveAP, i);
                        }
                    }
                }
            }
            outStates.clear();
            postcond.addPredicate(BinaryPredicate.createNonNullPred(receiveAP, method));
            postcond.addPredicate(BinaryPredicate.createNonNullPred(defAP, method));
            outStates.add(postcond);

        } else if (CollectionInfo.isReadCollection(targetName)) {
            elementRef.setAliasAP(defAP);
            AccessPath receiveAPElem = new AccessPath(base, elementRef, method, defAP.getConsumerStmt());
            State metaState = postcond.clone();
            metaState.addPredicate(BinaryPredicate.createNonNullPred(receiveAP, method));
            outStates.clear();
            outStates.addAll(NPUtil.replace(metaState, defAP, receiveAPElem, pta));

        } else if (CollectionInfo.isAddCollection(targetName) &&
                !CollectionInfo.isIterator(target)) {
            AccessPath receiveAPElem = new AccessPath(base, elementRef, method);
            AccessPath paramAP;
            if (expr.getArgCount() == 1) {
                paramAP = new AccessPath(expr.getArg(0), method);
            } else
                paramAP = new AccessPath(expr.getArg(1), method);
            outStates.clear();
            postcond.addPredicate(BinaryPredicate.createNonNullPred(receiveAP, method));
            outStates.addAll(NPUtil.replace(postcond, receiveAPElem, paramAP, pta));

        } else if (CollectionInfo.isAddIterator(targetName) &&
                CollectionInfo.isIterator(target)) {
            AccessPath receiveAPElem = new AccessPath(base, elementRef, method);
            AccessPath receiveIterAP = new AccessPath(base, iteratorFieldRef, method);
            receiveAPElem.replace(receiveAP, receiveIterAP);
            AccessPath paramAP;
            if (expr.getArgCount() == 1) {
                paramAP = new AccessPath(expr.getArg(0), method);
            } else
                paramAP = new AccessPath(expr.getArg(1), method);
            outStates.clear();
            postcond.addPredicate(BinaryPredicate.createNonNullPred(receiveAP, method));
            outStates.addAll(NPUtil.replace(postcond, receiveAPElem, paramAP, pta));

        } else if (CollectionInfo.isAddAllCollection(targetName)) {
            AccessPath receiveAPElem = new AccessPath(base, elementRef, method);
            AccessPath paramAPElem;
            if (expr.getArgCount() == 1) {
                paramAPElem = new AccessPath(expr.getArg(0), elementRef, method);
            } else
                paramAPElem = new AccessPath(expr.getArg(1), elementRef, method);
            outStates.clear();
            postcond.addPredicate(BinaryPredicate.createNonNullPred(receiveAP, method));
            outStates.addAll(NPUtil.replace(postcond, receiveAPElem, paramAPElem, pta));

        } else if (targetName.contains("<init>") &&
                !targetName.contains("iterator")) {
            AccessPath receiveAPElem = new AccessPath(base, elementRef, method);
            postcond.addPredicate(BinaryPredicate.createNonNullPred(receiveAP, method));
            outStates.add(postcond);
            for (Predicate pred : postcond) {
                if (pred.containsAP(receiveAPElem)) {
                    outStates.clear();
                    break;
                }
            }

        } else if (CollectionInfo.isCollectionToArray(targetName)) {
            AccessPath receiveAPElem = new AccessPath(base, elementRef, method);
            State metaState = postcond.clone();
            for (Predicate pred : postcond) {
                if (pred.containsAP(receiveAPElem)) {
                    metaState.removePredicate(pred);
                    break;
                }
            }
            outStates.clear();
            metaState.addPredicate(BinaryPredicate.createNonNullPred(receiveAP, method));
            outStates.add(metaState);

        } else if (CollectionInfo.isSkipCollection(targetName)) {

        }
        return outStates;
    }

    private Set<State> invokeMapInstTransFunc(SootMethod target, Unit inst, Value base, SootMethod currentMethod, State postcond) {
        Set<State> outStates = CollectionFactory.newSet();
        ValueFieldRef valueField = new ValueFieldRef();
        KeyFieldRef keyField = new KeyFieldRef();
        ElementRef elementRef = new ElementRef();
        String methodName = target.getName();
        InvokeExpr expr = null;
        if (inst instanceof AssignStmt) {
            expr = (InvokeExpr) ((AssignStmt) inst).getRightOp();
        } else if (inst instanceof InvokeStmt) {
            expr = (InvokeExpr) ((InvokeStmt) inst).getInvokeExpr();
        }
        if (MapInfo.isPut(methodName)) {
            AccessPath receiveAPKeyElem = new AccessPath(base, elementRef, currentMethod);
            AccessPath receiveAPValElem = new AccessPath(base, elementRef, currentMethod);
            AccessPath receiveAP = new AccessPath(base, currentMethod);
            AccessPath receiveKeyAP = new AccessPath(base, keyField, currentMethod);
            AccessPath receiveValueAP = new AccessPath(base, valueField, currentMethod);
            receiveAPValElem.replace(receiveAP, receiveValueAP);
            receiveAPKeyElem.replace(receiveAP, receiveKeyAP);
            AccessPath param1AP = new AccessPath(expr.getArg(0), currentMethod);
            AccessPath param2AP = new AccessPath(expr.getArg(1), currentMethod);
            /**
             * Invalidate the disjunct if key and map used in put instruction
             * is same as the key and map in KeyNotExistPredicate.
             * This means that the KeyNotExistPredicate is false as there exists
             * mapping in this map of the key
             */
            for (Predicate pred : postcond) {
                if (pred instanceof KeyNotExistPredicate) {
                    KeyNotExistPredicate keyPred = (KeyNotExistPredicate) pred;
                    if (keyPred.getMap().equals(receiveAP) &&
                            keyPred.getKey().equals(param1AP)) {
                        outStates.clear();
                        return outStates;
                    }
                }
            }
            Set<State> intermediateState = CollectionFactory.newSet();
            intermediateState.addAll(NPUtil.replace(postcond, receiveAPKeyElem, param1AP, pta));
            for (State intermediateCond : intermediateState) {
                outStates.addAll(NPUtil.replace(intermediateCond, receiveAPValElem, param2AP, pta));
            }


        } else if (MapInfo.isGetKeySet(methodName)) {

            AssignStmt assignStmt = (AssignStmt) inst;
            Value def = (assignStmt).getLeftOp();
            AccessPath defAPElem = new AccessPath(def, elementRef, currentMethod);
            AccessPath receiveAPKeyElem = new AccessPath(base, elementRef, currentMethod);
            AccessPath receiveAP = new AccessPath(base, currentMethod);
            AccessPath receiveKeyAP = new AccessPath(base, keyField, currentMethod);
            receiveAPKeyElem.replace(receiveAP, receiveKeyAP);
            postcond.addPredicate(BinaryPredicate.createNonNullPred(new AccessPath(def, currentMethod), currentMethod));
            strongUpdate(postcond, defAPElem, receiveAPKeyElem);
            outStates.add(postcond);

        } else if (MapInfo.isGetValueSet(methodName)) {

            AssignStmt assignStmt = (AssignStmt) inst;
            Value def = (assignStmt).getLeftOp();
            AccessPath defAPElem = new AccessPath(def, elementRef, currentMethod);
            AccessPath receiveAPValueElem = new AccessPath(base, elementRef, currentMethod);
            AccessPath receiveAP = new AccessPath(base, currentMethod);
            AccessPath receiveValAP = new AccessPath(base, valueField, currentMethod);
            receiveAPValueElem.replace(receiveAP, receiveValAP);
            postcond.addPredicate(BinaryPredicate.createNonNullPred(new AccessPath(def, currentMethod), currentMethod));
            strongUpdate(postcond, defAPElem, receiveAPValueElem);
            outStates.add(postcond);

        } else if (MapInfo.isClear(methodName)) {
            AccessPath receiveAPKeyElem = new AccessPath(base, elementRef, currentMethod);
            AccessPath receiveAPValElem = new AccessPath(base, elementRef, currentMethod);
            AccessPath receiveAP = new AccessPath(base, currentMethod);
            AccessPath receiveKeyAP = new AccessPath(base, keyField, currentMethod);
            AccessPath receiveValAP = new AccessPath(base, valueField, currentMethod);
            receiveAPValElem.replace(receiveAP, receiveValAP);
            receiveAPKeyElem.replace(receiveAP, receiveKeyAP);
            outStates.add(postcond);
            for (Predicate pred : postcond) {
                if (pred instanceof KeyNotExistPredicate) {
                    KeyNotExistPredicate keyPred = (KeyNotExistPredicate) pred;
                    if (keyPred.getMap().equals(receiveAP))
                        outStates.iterator().next().removePredicate(keyPred);
                }
                if (pred.containsAP(receiveAPValElem) || pred.containsAP(receiveAPKeyElem)) {
                    outStates.clear();
                    break;
                }
            }

        } else if (methodName.contains("<init>")) {
            AccessPath receiveAPKeyElem = new AccessPath(base, elementRef, currentMethod);
            AccessPath receiveAPValElem = new AccessPath(base, elementRef, currentMethod);
            AccessPath receiveAP = new AccessPath(base, currentMethod);
            AccessPath receiveKeyAP = new AccessPath(base, keyField, currentMethod);
            AccessPath receiveValAP = new AccessPath(base, valueField, currentMethod);
            receiveAPValElem.replace(receiveAP, receiveValAP);
            receiveAPKeyElem.replace(receiveAP, receiveKeyAP);
            outStates.add(postcond.clone());

            if (expr.getArgCount() == 1) {
                for (Predicate pred : postcond) {
                    if (pred instanceof KeyNotExistPredicate) {
                        KeyNotExistPredicate keyPred = (KeyNotExistPredicate) pred;
                        if (keyPred.getMap().equals(receiveAP))
                            outStates.iterator().next().removePredicate(keyPred);
                    }
                    if (pred.containsAP(receiveValAP) || pred.containsAP(receiveKeyAP)) {
                        outStates.clear();
                        break;
                    }
                }
            } else if (expr.getArgCount() == 2) {
                State tempPostCond = postcond.clone();
                for (Predicate pred : tempPostCond) {
                    if (pred instanceof KeyNotExistPredicate) {
                        if (((KeyNotExistPredicate) pred).getMap().equals(receiveAP))
                            postcond.removePredicate(pred);
                    }
                }
                AccessPath defAPValElem = receiveAPValElem.clone();
                defAPValElem.replace(receiveAP, new AccessPath(expr.getArg(0), currentMethod));
                AccessPath defAPKeyElem = receiveAPKeyElem.clone();
                defAPKeyElem.replace(receiveAP, new AccessPath(expr.getArg(0), currentMethod));

                strongUpdate(postcond, receiveAPValElem, defAPValElem);
                strongUpdate(postcond, receiveAPKeyElem, defAPKeyElem);
                outStates.add(postcond);
            }

        } else if (MapInfo.isGetValue(methodName)) {
            if (!(inst instanceof AssignStmt)) {
                outStates.add(postcond);
                return outStates;
            }
            AssignStmt assignStmt = (AssignStmt) inst;
            AccessPath receiverAP = new AccessPath(base, currentMethod);
            State metaState = postcond.clone();
            AccessPath nullAP = new AccessPath(NullConstant.v(), currentMethod);
            AccessPath defAP = new AccessPath(assignStmt.getLeftOp(), currentMethod);
            boolean addKeyNotExit = false;
            for (Predicate pred : postcond.getAllPreds()) {
                if (pred.containsAP(defAP))
                    addKeyNotExit = true;
            }
            outStates.addAll(NPUtil.replace(postcond, defAP, nullAP, pta));
            if (addKeyNotExit) {
                //InvokeExpr expr = (InvokeExpr) assignStmt.getRightOp();
                if (expr == null)
                    throw new IllegalArgumentException("");
                if (expr.getArgCount() > 0) {
                    AccessPath param = new AccessPath(expr.getArg(0), currentMethod);
                    KeyNotExistPredicate pred = new KeyNotExistPredicate(receiverAP, param);
                    outStates.iterator().next().addPredicate(pred);
                }
            }
            elementRef.setAliasAP(defAP);
            AccessPath receiveAPValElem = new AccessPath(base, elementRef, currentMethod, defAP.getConsumerStmt());
            AccessPath receiveValueAP = new AccessPath(base, valueField, currentMethod);
            receiveAPValElem.replace(receiverAP, receiveValueAP);
            outStates.addAll(NPUtil.replace(metaState, defAP, receiveAPValElem, pta));

        } else if (MapInfo.isEnum(methodName)) {
            AssignStmt assignStmt = (AssignStmt) inst;
            Value def = (assignStmt).getLeftOp();
            AccessPath receiveAP = new AccessPath(base, currentMethod);
            AccessPath receiveAPValElem = getMapValElem(receiveAP, currentMethod);
            AccessPath defAP = new AccessPath(def, currentMethod);
            AccessPath defAPIterElem = getAPIterElem(defAP, elementRef, currentMethod, inst);
            strongUpdate(postcond, defAPIterElem, receiveAPValElem);
            postcond.addPredicate(BinaryPredicate.createNonNullPred(receiveAP, currentMethod));
            postcond.addPredicate(BinaryPredicate.createNonNullPred(defAP, currentMethod));
            outStates.add(postcond);

        } else if (MapInfo.isId(methodName)) {
            if (methodName.equals("remove")) {
                AccessPath receiveAP = new AccessPath(base, currentMethod);
                State tempPostCond = postcond.clone();
                for (Predicate pred : tempPostCond) {
                    if (pred instanceof KeyNotExistPredicate) {
                        if (((KeyNotExistPredicate) pred).getMap().equals(receiveAP))
                            postcond.removePredicate(pred);
                    }
                }
            }
            outStates.add(postcond);
        }
        return outStates;
    }

    private AccessPath getAPIterElem(AccessPath ap, ElementRef elementRef, SootMethod method, Unit inst) {
        IteratorFieldRef iteratorFieldRef = new IteratorFieldRef();
        AccessPath apElem = new AccessPath(ap.getLocal(), elementRef, method, inst);
        AccessPath apIter = new AccessPath(ap.getLocal(), iteratorFieldRef, method);
        apElem.replace(ap, apIter);
        return apElem;
    }

    private AccessPath getMapValElem(AccessPath map, SootMethod method) {
        ValueFieldRef valueFieldRef = new ValueFieldRef();
        ElementRef elementRef = new ElementRef();
        AccessPath mapAPValElem = new AccessPath(map.getLocal(), elementRef, method);
        AccessPath mapValueAP = new AccessPath(map.getLocal(), valueFieldRef, method);
        mapAPValElem.replace(map, mapValueAP);
        return mapAPValElem;
    }

    private void strongUpdate(State postcond, AccessPath replaceElem, AccessPath beReplacedElem) {
        State metaState = postcond.clone();
        for (Predicate pred : metaState) {
            AccessPath[] allAPs = pred.getAllAccessPaths();
            for (int i = 0; i < allAPs.length; i++) {
                AccessPath predAP = allAPs[i];
                if (predAP.isInstanceRef()) {
                    if (predAP.contains(replaceElem)) {
                        NPUtil.updateState(postcond, pred, replaceElem, beReplacedElem, i);
                    }
                }
            }
        }
    }

    public static final Map<String, String> notNullList = new HashMap<String, String>(){{
        put("android.content.Context", "getSystemService");
        put("android.app.Activity", "getSystemService");
        put("android.view.ContextThemeWrapper", "getSystemService");
        put("java.lang.String", "substring");
        put("java.lang.StringBuilder", "toString");
    }};

    public static final Set<String> exclusionList = new HashSet<String>(
        Arrays.asList(
            "java.lang.Object.equals(java.lang.Object)boolean",
            "java.lang.Object.hashCode()int",
            "java.lang.Object.<clinit>()void",
            "java.lang.Object.<init>()void",
            "java.lang.Object.registerNatives()void"
        )
    );

    public static final Set<String> skipMethodList = new HashSet<String>(
        Arrays.asList(
            "java.beans.XMLDecoder.getHandler()com.sun.beans.ObjectHandler",
            "java.beans.XMLDecoder.readObject()java.lang.Object",
            "java.beans.XMLEncoder.close()void",
            "java.io.ByteArrayOutputStream.toString()java.lang.String",
            "java.io.DataInputStream.readByte()byte",
            "java.io.DataInputStream.readFully(byte[])void",
            "java.io.DataInputStream.readInt()int",
            "java.io.File.canRead()boolean",
            "java.io.File.isFile()boolean",
            "java.io.File.lastModified()long",
            "java.io.File.mkdirs()boolean",
            "java.io.FileSystem.resolve(java.lang.String,java.lang.String)java.lang.String",
            "java.io.ObjectStreamClass.toString()java.lang.String",
            "java.io.ObjectStreamField.toString()java.lang.String",
            "java.io.PrintStream.print(char)void",
            "java.io.PrintStream.print(int)void",
            "java.io.PrintStream.print(java.lang.String)void ",
            "java.io.PrintStream.println(java.lang.Object)void",
            "java.io.PrintStream.println(java.lang.String)void ",
            "java.io.PrintWriter.print(char)void",
            "java.io.PrintWriter.print(java.lang.String)void",
            "java.io.PrintWriter.println(int)void",
            "java.io.PrintWriter.println(java.lang.String)void ",
            "java.io.PrintWriter.println()void",
            "java.lang.Byte.toString()java.lang.String",
            "java.lang.Character.toString()java.lang.String",
            "java.lang.Class.enumConstantDirectory()java.util.Map",
            "java.lang.Class.getClassLoader()java.lang.ClassLoader",
            "java.lang.Class.getField0(java.lang.String)java.lang.reflect.Field",
            "java.lang.Class.toString()java.lang.String",
            "java.lang.Double.toString()java.lang.String",
            "java.lang.Enum.toString()java.lang.String",
            "java.lang.Enum.valueOf(java.lang.Class,java.lang.String)java.lang.Enum",
            "java.lang.Float.toString()java.lang.String",
            "java.lang.Object.clone()java.lang.Object",
            "java.lang.Object.toString()java.lang.String",
            "java.lang.reflect.Method.toString()java.lang.String",
            "java.lang.Short.toString()java.lang.String",
            "java.lang.StringCoding.encode(char[],int,int)byte[]",
            "java.lang.String.equals(java.lang.Object)boolean",
            "java.lang.String.matches(java.lang.String)boolean",
            "java.lang.String.substring(int)java.lang.String",
            "java.lang.System.getProperties()java.util.Properties",
            "java.lang.Thread.getContextClassLoader()java.lang.ClassLoader",
            "java.lang.ThreadLocal.get()java.lang.Object",
            "java.lang.Thread.toString()java.lang.String",
            "java.lang.Throwable.printStackTrace(java.io.PrintWriter)void ",
            "java.lang.Throwable.printStackTrace()void",
            "java.math.BigInteger.toString()java.lang.String",
            "java.net.DatagramSocket.getImpl()java.net.DatagramSocketImpl",
            "java.net.InetAddress.getAllByName(java.lang.String)java.net.InetAddress[]",
            "java.net.InetAddress.getByName(java.lang.String)java.net.InetAddress",
            "java.net.MulticastSocket.getInterface()java.net.InetAddress",
            "java.net.Socket.getLocalAddress()java.net.InetAddress",
            "java.net.Socket.toString()java.lang.String",
            "java.net.URI.defineSchemeSpecificPart()void",
            "java.net.URI.getPath()java.lang.String",
            "java.net.URI.toURL()java.net.URL",
            "java.net.URLConnection.getContentType()java.lang.String",
            "java.net.URLEncoder.encode(java.lang.String,java.lang.String)java.lang.String",
            "java.nio.charset.Charset.name()java.lang.String",
            "java.sql.Connection.prepareStatement(java.lang.String)java.sql.PreparedStatement",
            "java.sql.PreparedStatement.executeQuery()java.sql.ResultSet",
            "java.sql.ResultSet.getString(java.lang.String)java.lang.String",
            "java.sql.ResultSetMetaData.getColumnCount()int",
            "java.sql.ResultSetMetaData.getColumnName(int)java.lang.String",
            "java.text.AttributedCharacterIterator$Attribute.toString()java.lang.String",
            "java.text.AttributeEntry.toString()java.lang.String",
            "java.text.DateFormat.format(java.util.Date)java.lang.String",
            "java.text.FieldPosition.toString()java.lang.String",
            "java.util.AbstractCollection.toString()java.lang.String",
            "java.util.Calendar.get(int)int",
            "java.util.Collection.iterator()java.util.Iterator",
            "java.util.Currency.toString()java.lang.String",
            "java.util.Date.toString()java.lang.String",
            "java.util.Formatter$FixedString.toString()java.lang.String",
            "java.util.Formatter$FormatSpecifier.toString()java.lang.String",
            "java.util.HashSet.contains(java.lang.Object)boolean",
            "java.util.Iterator.next()java.lang.Object",
            "java.util.jar.Attributes$Name.toString()java.lang.String",
            "java.util.LinkedHashMap.get(java.lang.Object)java.lang.Object",
            "java.util.LinkedList.clear()void",
            "java.util.LinkedList.clone()java.lang.Object",
            "java.util.List.get(int)java.lang.Object",
            "java.util.Locale.getDisplayName()java.lang.String",
            "java.util.Locale.getDisplayName(java.util.Locale)java.lang.String",
            "java.util.Locale.getInstance(java.lang.String,java.lang.String,java.lang.String)java.util.Locale",
            "java.util.logging.Level.toString()java.lang.String",
            "java.util.Map.get(java.lang.Object)java.lang.Object",
            "java.util.Map.keySet()java.util.Set",
            "java.util.Map.values()java.util.Collection",
            "java.util.Scanner.hasNextLine()boolean",
            "java.util.Vector.listIterator()java.util.ListIterator",
            "java.util.Vector.toString()java.lang.String",
            "java.util.zip.ZipInputStream.getNextEntry()java.util.zip.ZipEntry",
            "javax.xml.parsers.DocumentBuilderFactory.newInstance()javax.xml.parsers.DocumentBuilderFactory",
            "javax.xml.stream.XMLInputFactory.newInstance()javax.xml.stream.XMLInputFactory",
            "javax.xml.stream.XMLOutputFactory.newInstance()javax.xml.stream.XMLOutputFactory",
            "javax.xml.transform.TransformerFactory.newInstance()javax.xml.transform.TransformerFactory",
            "javolution.util.FastMap.get(java.lang.Object)java.lang.Object",
            "org.w3c.dom.Document.getFirstChild()org.w3c.dom.Node",
            "org.w3c.dom.NamedNodeMap.getNamedItem(java.lang.String)org.w3c.dom.Node",
            "org.w3c.dom.Node.getAttributes()org.w3c.dom.NamedNodeMap",
            "org.w3c.dom.Node.getNodeValue()java.lang.String",
            "java.io.File.isDirectory()boolean",
            "java.io.File.listFiles()java.io.File[]",
            "java.io.File.toString()java.lang.String",
            "java.io.File.toURI()java.net.URI",
            "java.io.ObjectInputStream.readClassDesc(boolean)java.io.ObjectStreamClass",
            "java.io.PrintWriter.println(java.lang.String)void",
            "java.lang.Boolean.toString()java.lang.String",
            "java.lang.Character$Subset.toString()java.lang.String",
            "java.lang.Class.getDeclaredField(java.lang.String)java.lang.reflect.Field",
            "java.lang.Class.getField(java.lang.String)java.lang.reflect.Field",
            "java.lang.Integer.toString()java.lang.String",
            "java.lang.Long.toString()java.lang.String",
            "java.lang.StackTraceElement.toString()java.lang.String",
            "java.lang.String.toString()java.lang.String",
            "java.lang.Throwable.printStackTrace(java.io.PrintStream)void",
            "java.lang.Throwable.printStackTrace(java.io.PrintWriter)void",
            "java.lang.Throwable.toString()java.lang.String",
            "java.net.Socket.getImpl()java.net.SocketImpl",
            "java.security.PrivilegedActionException.toString()java.lang.String",
            "java.util.AbstractList.listIterator()java.util.ListIterator",
            "java.util.AbstractMap.toString()java.lang.String",
            "java.util.HashMap.get(java.lang.Object)java.lang.Object",
            "java.util.Locale.toString()java.lang.String",
            "java.io.File.exists()boolean",
            "java.io.PrintStream.println()void",
            "java.lang.String.getBytes()byte[]",
            "java.lang.String.valueOf(java.lang.Object)java.lang.String",
            "java.util.Locale.getDefault()java.util.Locale",
            "java.io.PrintStream.println(java.lang.String)void",
            "java.util.AbstractSequentialList.iterator()java.util.Iterator",
            "java.io.PrintStream.print(java.lang.String)void"
        )
    );

    public static final Set<String> analyzeLibCallList = new HashSet<String>(
        Arrays.asList(
            "java.beans.XMLDecoder.close()void",
            "java.io.BufferedInputStream.read()int",
            "java.io.BufferedWriter.<init>(java.io.Writer)void",
            "java.io.ByteArrayInputStream.read()int",
            "java.io.DataInputStream.<init>(java.io.InputStream)void",
            "java.io.File.<init>(java.io.Filejava.lang.String)void",
            "java.io.FileInputStream.<init>(java.io.File)void",
            "java.io.FileOutputStream.<init>(java.io.File)void",
            "java.io.FileReader.<init>(java.io.File)void",
            "java.io.ObjectInputStream$BlockDataInputStream.read()int",
            "java.io.PrintStream.<init>(java.io.OutputStream)void",
            "java.io.PrintWriter.<init>(java.io.OutputStream)void",
            "java.io.RandomAccessFile.<init>(java.io.Filejava.lang.String)void",
            "java.io.StreamTokenizer.nextToken()int",
            "java.lang.ConditionalSpecialCasing.toLowerCaseCharArray(java.lang.String,int,java.util.Locale)char[]",
            "java.lang.ConditionalSpecialCasing.toLowerCaseEx(java.lang.String,int,java.util.Locale)int",
            "java.lang.StringBuffer.append(java.lang.Object)java.lang.StringBuffer",
            "java.lang.StringBuilder.append(java.lang.Object)java.lang.StringBuilder",
            "java.lang.String.<init>(byte[],int,int,java.lang.String)void",
            "java.lang.String.<init>(byte[],int,int)void",
            "java.lang.String.<init>(byte[],java.lang.String)void",
            "java.lang.String.<init>(byte[])void",
            "#java.lang.String.matches(java.lang.String)boolean",
            "java.lang.String.toLowerCase()java.lang.String",
            "java.lang.String.toLowerCase(java.util.Locale)java.lang.String",
            "java.lang.System.exit(int)void",
            "java.lang.System.setProperties(java.util.Properties)void",
            "java.lang.ThreadLocal.setInitialValue()java.lang.Object",
            "java.lang.ThreadLocal$ThreadLocalMap.access$000(java.lang.ThreadLocal$ThreadLocalMapjava.lang.ThreadLocal)java.lang.ThreadLocal$ThreadLocalMap$Entry",
            "java.lang.Thread.start()void",
            "java.net.Authenticator.setDefault(java.net.Authenticator)void",
            "java.net.DatagramSocket.receive(java.net.DatagramPacket)void",
            "java.net.DatagramSocket.send(java.net.DatagramPacket)void",
            "java.net.URL.<init>(java.lang.String)void",
            "java.text.SimpleDateFormat.<init>(java.lang.Stringjava.util.Locale)void",
            "java.util.Arrays.sort(java.lang.Object[],int,int)void",
            "java.util.concurrent.locks.ReentrantLock.lock()void",
            "java.util.concurrent.ScheduledThreadPoolExecutor.delayedExecute(java.util.concurrent.RunnableScheduledFuture)void",
            "java.util.concurrent.ScheduledThreadPoolExecutor.scheduleAtFixedRate(java.lang.RunnableJJjava.util.concurrent.TimeUnit)java.util.concurrent.ScheduledFuture",
            "java.util.concurrent.ScheduledThreadPoolExecutor.schedule(java.lang.RunnableJjava.util.concurrent.TimeUnit)java.util.concurrent.ScheduledFuture",
            "java.util.concurrent.ThreadPoolExecutor.execute(java.lang.Runnable)void",
            "#java.util.HashMap.get(java.lang.Object)java.lang.Object",
            "java.util.jar.JarOutputStream.<init>(java.io.OutputStreamjava.util.jar.Manifest)void",
            "java.util.jar.JarOutputStream.putNextEntry(java.util.zip.ZipEntry)void",
            "#java.util.LinkedList.add(int,java.lang.Object)void",
            "java.util.LinkedList.addLast(java.lang.Object)void",
            "java.util.LinkedList.add(java.lang.Object)boolean",
            "java.util.LinkedList.<init>()void",
            "java.util.LinkedList.listIterator(int)java.util.ListIterator",
            "java.util.LinkedList$ListItr.add(java.lang.Object)void",
            "java.util.LinkedList$ListItr.next()java.lang.Object",
            "java.util.LinkedList$ListItr.previous()java.lang.Object",
            "java.util.LinkedList.removeFirst()java.lang.Object",
            "java.util.LinkedList.remove(java.lang.Object)boolean",
            "java.util.logging.Logger.fine(java.lang.String)void",
            "java.util.logging.Logger.info(java.lang.String)void",
            "java.util.logging.Logger.warning(java.lang.String)void",
            "java.util.logging.LogManager.readPrimordialConfiguration()void",
            "java.util.Vector$Itr.next()java.lang.Object",
            "java.util.zip.DeflaterOutputStream.close()void",
            "java.util.zip.ZipEntry.<init>(java.lang.String)void",
            "java.util.zip.ZipFile$2.nextElement()java.lang.Object",
            "java.util.zip.ZipOutputStream.putNextEntry(java.util.zip.ZipEntry)void",
            "java.lang.StringCoding.decode(byte[],int,int)char[]",
            "java.lang.Thread.run()void",
            "java.lang.System.setProperty(java.lang.Stringjava.lang.String)java.lang.String",
            "java.lang.StringCoding.decode(java.lang.String,byte[],int,int)char[]",
            "java.lang.Runtime.exit(int)void",
            "java.lang.Shutdown.exit(int)void",
            "java.nio.charset.Charset.defaultCharset()java.nio.charset.Charset",
            "java.lang.StringCoding.lookupCharset(java.lang.String)java.nio.charset.Charset",
            "java.lang.Shutdown.sequence()void",
            "java.nio.charset.Charset.forName(java.lang.String)java.nio.charset.Charset",
            "java.security.AccessController.doPrivileged(java.security.PrivilegedAction)java.lang.Object",
            "java.lang.SecurityManager.checkExit(int)void",
            "java.nio.charset.Charset.lookup(java.lang.String)java.nio.charset.Charset",
            "java.nio.charset.Charset.isSupported(java.lang.String)boolean",
            "java.lang.SecurityManager.checkPermission(java.security.Permission)void",
            "java.lang.Shutdown.runHooks()void",
            "javax.xml.transform.SecuritySupport$1.run()java.lang.Object",
            "javax.xml.transform.SecuritySupport$2.run()java.lang.Object",
            "javax.xml.transform.SecuritySupport$3.run()java.lang.Object",
            "javax.xml.transform.SecuritySupport$4.run()java.lang.Object",
            "javax.xml.transform.SecuritySupport$5.run()java.lang.Object",
            "java.util.List.add(java.lang.Object)boolean"
        )
    );
}
