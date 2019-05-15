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

import polyglot.ast.Assign;
import polyglot.ast.Binary;
import predicate.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import polyglot.ast.NewArray;
import solver.RuleSolver;
import soot.*;
import soot.JastAddJ.Access;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.BriefBlockGraph;

import java.util.*;

/**
 * Created by Xinwei Xie on 21/7/17.
 */
public class CPA {
    private Stack<Context> ctxtStack;
    private Map<SootMethod, Set<State>> entryMap;
    private SootMethod currentMethod;
    private NPVerifier npVerifier;
    private CallGraph cg;
    private PointsToAnalysis pta;
    private boolean enableRecursion = true;
    private boolean fullPathSensitive = true;
    private boolean enableCollection = true;
    private boolean enableLibraryCalls = true;
    private boolean enableVirtualCalls = true;
    private boolean enableCallBacks = true;
    private boolean enableCompressedAPs = true;

    public final static Logger logger = LoggerFactory.getLogger(CPA.class);
    private static Map<Predicate, Unit> safeRootPreds = CollectionFactory.newMap();
    private static Map<Unit, Integer> symObjectMap = CollectionFactory.newMap();
    private static int currentSymObject = 0;

    public CPA(NPVerifier npe) {
        npVerifier = npe;
        cg = npe.getCallGraph();
        pta = npe.getPointsToAnalysis();
    }

    public CPA newInstance() {
        return new CPA(this.npVerifier);
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
                logger.error("Abort analysis of instruction: " + inst + ", since it is not in CFG");
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

    private void printStates(Set<State> states, SootMethod method) {
        logger.debug("after computBlockWP, states: ");
        StringBuffer str = new StringBuffer("");
        for (State s : states) {
            str.append(s + ", ");
        }
        logger.debug(str.toString());
    }

    public Set<State> analyzeMethod(Stack<Context> ctxtStack, Map<SootMethod, Set<State>> entryMap, SootMethod method,
                                    Unit inst, State initState) {
        /*
        if (stackCount > NPVerifier.maxStackCount) {
            //logger.error("!!! StackOverflow !!!");
            stackCount = 0;
            return Collections.singleton(initState);
        }
        */

        logger.debug("\t\tanalyzing method: " + method + ", initState: " + initState);
        this.ctxtStack = ctxtStack;
        this.entryMap = entryMap;
        this.currentMethod = method;
        Queue<Object[]> worklist = new LinkedList<Object[]>();
        Map<Block, Set<State>> visitedMap = CollectionFactory.newMap();
        Set<State> result = CollectionFactory.newSet();
        boolean isMap = false, isCollection = false;

        if (CollectionInfo.isCollectionSubClass(method.getDeclaringClass())) {
            if (initState.hasRootPredicate())
                isCollection = true;
        }

        if (MapInfo.isMapInterface(method)) {
            if (initState.hasRootPredicate())
                isMap = true;
        }

        State startState = State.DummyState;
        if (inst == null) {
            startState = initState;
        }
        ctxtStack.push(new Context(method, startState));

        if ((NPVerifier.boundSteps && initState.getStepCount() > NPVerifier.maxSteps) ||
                (NPVerifier.globalBound && NPVerifier.currentGlobalSteps > NPVerifier.maxGlobalSteps)){
            State trueState = new State();
            //logger.info("reach max step counts.");
            NPVerifier.reachBound = true;
            return Collections.singleton(trueState);
        }
        /*
        EnhancedBlockGraph bbg = new EnhancedBlockGraph(method.getActiveBody());
        UnitGraph cfg = new BriefUnitGraph(method.getActiveBody());
        EnhancedUnitGraph aaa = new EnhancedUnitGraph(method.getActiveBody());
        BriefUnitGraph bbb = new BriefUnitGraph(method.getActiveBody());
        BriefBlockGraph bbg = new BriefBlockGraph(method.getActiveBody());
        Object[] initPair = createInitPair(bbg, inst, initState);
        worklist.add(initPair);
        */
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
                        NPVerifier.earlyTerminate = true;
                        //return Collections.singleton(initState);
                    }
                } else {
                    for (Predicate pred : precond) {
                        if (pred instanceof BinaryPredicate) {
                            BinaryPredicate bp = (BinaryPredicate) pred;
                            if (bp.getLhs().isNullConstant() && !bp.getLhs().isRef()) {
                                NPVerifier.earlyTerminate = true;
                            }
                        }
                    }
                }

                //SAT solver
                logger.debug("\t\t\t\tBEFORE SATSolver, precond: " + precond);
                if (!npVerifier.solver.isSatisfied(precond)) {
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
        logger.debug("\t\t\t\tworklist is empty, result is: " + result);
        Set<State> remove = CollectionFactory.newSet();
        for (State st : result) {
            if (st.hasRootPredicate()) {
                if (isRootPredProvedSafe(st))
                    remove.add(st);
            } else {
                if (isCollection)
                    npVerifier.collectionDerefs.add(inst);
                if (isMap)
                    npVerifier.mapDerefs.add(inst);
            }
        }
        result.removeAll(remove);

        Context ctx = ctxtStack.pop();
        // TODO: handle context (recursion)
        if (ctx.recursion) {
            assert inst == null;

            Set<State> oldSummary = this.getSummary(method, initState);
            if (oldSummary != null && result.equals(oldSummary))
                ctx.recursion = false;
            else {
                for (Context recurCtx : ctx.contextsInRecursion) {
                    Map<State, Set<State>> entry = npVerifier.methodSummary.get(recurCtx.method);
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
            NPVerifier.analyzingClinit = true;
            Set<State> outStates = CollectionFactory.newSet();

            /*
            for (SootMethod init: NPVerifier.classInitMethods) {
                for (State st : result) {
                    Set<Predicate> preds = st.getAllPreds();
                    if (preds != null) {
                        for (Predicate pred : preds) {
                            AccessPath[] allAP = pred.getAllAccessPaths();
                            for (AccessPath ap : allAP) {
                                ap.setMethod(init);
                            }
                        }
                        outStates.addAll(analyzeMethod(ctxtStack, entryMap, init, null, st));
                    }
                }
                result = outStates;
            }
            */
        }

        if (!ctxtStack.isEmpty() || NPVerifier.analyzingClinit)
            return result;
        else
            return analyzePredecessors(method, result); //, entryMap);
    }

    private Set<State> analyzePredecessors(SootMethod method, Set<State> result) { //, Map<SootMethod, Set<State>> entryMap) {

        logger.debug("\t\t\t|||analyzing predecessor|||: " + method + ", result: " + result);
        if (!npVerifier.entryStateSummary.containsKey(method)) {
            npVerifier.entryStateSummary.put(method, CollectionFactory.newMap());
        }

        Set<State> reachSet = entryMap.get(method);
        if (reachSet == null)
            reachSet = CollectionFactory.newSet();
        Set<State> newStates = CollectionFactory.newSet();
        for (State r : result) {
            // hasSubSet
            if (!hasSubset(reachSet, r)) {
                newStates.add(r);
                reachSet.add(r);
            }
        }
        entryMap.put(method, reachSet);

        if (npVerifier.boundSteps) {
            for (State s : result) {
                if (s.getStepCount() > npVerifier.maxSteps) {
                    return Collections.singleton(new State());
                }
            }
        }

        Set<Edge> callingEdgeSet = getCallGraphNode(method);
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
                State callerState = getCallerStates(method, caller, inst, st);
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
                                    symObject.setName("symObj_" + getOrCreateSymObject(inst));
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
                    State hitState = hasSubsetAndRootPred(npVerifier.entryStateSummary.get(method).keySet(), callerState);
                    Set<State> resultStates = null;
                    if (hitState == null) {
                        //resultStates = this.newInstance().analyzeMethod(ctxtStack, entryMap, caller, inst, callerState);
                        //npVerifier.entryStateSummary.get(currentMethod).put(callerState, resultStates);
                        resultStates = analyzeMethod(ctxtStack, entryMap, caller, inst, callerState);
                        npVerifier.entryStateSummary.get(method).put(callerState, resultStates);
                    } else {
                        resultStates = npVerifier.entryStateSummary.get(method).get(hitState);
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

                    NPVerifier.currentGlobalSteps++;
                    outputPres.addAll(computeInstWP(method, block, tempInst, inputPost, succBlock));
                }
            } else {
                outputPres = inputPosts;
            }
            logger.debug("\t\t\t\tBLOCK " + block.getIndexInMethod() + " (" + method.getName() + ") INST: " + tempInst
                    + ", IN: " + inputPosts + ", OUT: " + outputPres);
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
        if (isRootPredProvedSafe(postcond))
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
            result.addAll(replace(postcond, defAP, useAP));
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
                    result.addAll(replace(postcond, defAP, useAP));
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
                    symObject.setName("symObj_" + getOrCreateSymObject(inst));
                    // a symbolic object is a compressed AP
                    AccessPath symObjAP = new AccessPath(symObject, method, inst, true);
                    AccessPath defAP = new AccessPath(lval, method);
                    result.addAll(replace(outState, defAP, symObjAP));
                } else if (rval instanceof NullConstant) {
                    AccessPath useAP = new AccessPath(NullConstant.v(), method);
                    AccessPath defAP = new AccessPath(lval, method);

                    //result.add(precond);
                    result.addAll(replace(postcond, defAP, useAP));
                } else if (rval instanceof StringConstant) {

                    AccessPath useAP = new AccessPath(rval, method);
                    AccessPath defAP = new AccessPath(lval, method);
                    result.addAll(replace(postcond, defAP, useAP));

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
                    result.addAll(replace(predcond, defAP, useAP));
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
        if (this.fullPathSensitive) {
            precond.addPredicate(BinaryPredicate.createNonNullPred(baseAP, method));
        } else {
            if (this.aliasWithAPInRootPredicate(precond, baseAP))
                precond.addPredicate(BinaryPredicate.createNonNullPred(baseAP, method));
        }
        return replace(precond, defAP, useAP);
    }

    private Set<State> invokeInstTransFunc(SootMethod method, Unit inst, InvokeStmt invokeStmt, State postcond) {
        InvokeExpr expr = invokeStmt.getInvokeExpr();
        return invokeExprTransFunc(method, inst, expr, postcond);
    }

    // TODO: for virtualinvoke and interfaceinvoke may have many callees
    private boolean invokeInstPossible(SootMethod method) {
        //if (exclusionList.contains())
        //String signature = expr.getMethod().getDeclaringClass().getName();
        String methodName = NPVerifier.getMethodSignature(method);
        String klass = method.getDeclaringClass().getName();
        if (npVerifier.exclusionList.contains(methodName)
                || klass.startsWith("java")
                || klass.startsWith("javax")
                || klass.startsWith("sun"))
            return false;

        return true;
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
        if (!this.fullPathSensitive) {
            if ((lhs.isConstant() || rhs.isConstant())
                    && aliasWithAPInRootPredicate(postcond, lhs.isConstant() ? rhs : lhs)) {
                BinaryPredicate bp = new BinaryPredicate(lhs, rhs, getConditionOperator(cond), !fallThrough, false);
                State precond = postcond.clone();
                precond.addPredicate(bp);
                return precond;
            }
            return postcond;
        } else {
            if (rhs.getLocal() instanceof NullConstant) {
                BinaryPredicate bp = new BinaryPredicate(lhs, rhs, getConditionOperator(cond), !fallThrough, false);
                State precond = postcond.clone();
                precond.addPredicate(bp);
                return precond;
            } else if (lhs.getLocal() instanceof NullConstant) {
                BinaryPredicate bp = new BinaryPredicate(rhs, lhs, getConditionOperator(cond), !fallThrough, false);
                State precond = postcond.clone();
                precond.addPredicate(bp);
                return precond;
            } else {
                return postcond;
            }
        }
    }

    private boolean isNotNull(String className, String methodName) {
        String method = NPVerifier.notNullList.get(className);
        if (method != null) {
            return method.equals(methodName);
        } else
            return false;
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
            if (this.fullPathSensitive) {
                metaState.addPredicate(BinaryPredicate.createNonNullPred(recvAP, method));
            } else {
                if (aliasWithAPInRootPredicate(metaState, recvAP))
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
        String name = NPVerifier.getMethodSignature(m);

        if (inst instanceof AssignStmt) {
            if (isNotNull(m.getDeclaringClass().getName(), m.getName())) {
                State out = postcond.clone();
                AssignStmt stmt = (AssignStmt) inst;
                AccessPath lhs = new AccessPath(stmt.getLeftOp(), method, inst);
                out.addPredicate(BinaryPredicate.createNonNullPred(lhs, method));
                outStates.add(out);
                return outStates;
            }
        }

        if (!invokeInstPossible(m)) {
            outStates.add(metaState);
            return outStates;
        }

        // TODO: get all possible targets due to virtual call
        Set<Edge> targetSet = this.getPossibleTargets(inst);
        if (targetSet.size() == 0) {

            outStates.add(metaState);
            return outStates;
        } else {
            if (NPVerifier.enableStatistic) {
                Integer size = targetSet.size();
                Integer count = NPVerifier.virtualCallCounts.get(size);
                if (count == null) {
                    NPVerifier.virtualCallCounts.put(size, new Integer(1));
                } else {
                    NPVerifier.virtualCallCounts.put(size, ++count);
                }
            }
        }

        if (enableCollection) {
            if (isLibraryCollection(targetSet)) {
                SootMethod target = targetSet.iterator().next().tgt();
                return invokeCollectionInstTransFunc(target, inst, base, method, metaState);
            }
            if (isLibraryMap(targetSet)) {
                SootMethod target = targetSet.iterator().next().tgt();
                return invokeMapInstTransFunc(target, inst, base, method, metaState);
            }
        }

        int targetCount = 0;
        for (Edge e : targetSet) {
            SootMethod target = e.tgt();
            if (target.isNative() || !invokeInstPossible(target) || target.getName().equals("<clinit>")) {
                //outStates.add(metaState);
                continue;
            }
            Map<Value, Value> acturalToFormal = mapActuralToFormal(inst, expr);
            Map<Value, Value> formalToActural = invertMap(acturalToFormal);

            Set<Predicate> modPreds = CollectionFactory.newSet();
            Set<Predicate> retPreds = CollectionFactory.newSet();
            Set<Predicate> unModPreds = CollectionFactory.newSet();
            computeModPreds(method, target, inst, metaState.getPreds(), modPreds, retPreds, unModPreds);

            String targetName = NPVerifier.getMethodSignature(target);
            if (target.isStaticInitializer() ||
                    NPVerifier.analyzeLibCallList.contains(targetName)) {
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
            mapPredicates(retPreds, acturalToFormal, method, target, modMappedPreds, modUnmappedPreds);
            //mapActuralToFormal(retPreds, acturalToFormal, currentMethod, target, modMappedPreds, modUnmappedPreds);
            calleeState.addAllPredicates(modMappedPreds);
            calleeState.addAllPredicates(modUnmappedPreds);


            State hitState = null;
            Set<State> resultStates = CollectionFactory.newSet();
            if (npVerifier.methodSummary.containsKey(target) &&
                    ((hitState = hasSubsetAndRootPred(npVerifier.methodSummary.get(target).keySet(), calleeState)) != null)) {
                Set<State> summaryStates = npVerifier.methodSummary.get(target).get(hitState);
                assert (summaryStates != null);

                for (State summaryState : summaryStates) {
                    State summaryClone = summaryState.clone();
                    summaryClone.resetState(metaState);
                    summaryClone.increaseStepCount();
                    resultStates.add(summaryClone);
                }
            } else if (targetSet.size() > npVerifier.maxTargets) {
                State trueState = new State();
                trueState.resetState(calleeState);
                resultStates.add(trueState);

            } else {
                Context callContext = new Context(target, calleeState);
                boolean ctxFound = false;
                Context ctx = null;
                if (NPVerifier.getMethodSignature(target).contains("print(java.lang.StringBuilder,java.util.Calendar,char,java.util.Locale)")) {
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
                    logger.debug("\t\t\t\tANALYZING target " + (targetCount++) + " : " + target + " of method " + method + ", state: " + postcond);
                    //resultStates = this.newInstance().analyzeMethod(ctxtStack, entryMap, target, null, calleeState);
                    resultStates = analyzeMethod(ctxtStack, entryMap, target, null, calleeState);

                } else {
                    if (!enableRecursion) {
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
                mapPredicates(resultState.getPreds(), formalToActural, target, method, mapCalleePreds, unMapCalleePreds);
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

    private boolean isLibraryCollection(Set<Edge> targetSet) {
        for (Edge e : targetSet) {
            SootMethod m = e.tgt();
            if (!CollectionInfo.isCollectionSubClass(m.getDeclaringClass()))
                return false;
        }
        return true;
    }

    private boolean isLibraryMap(Set<Edge> targetSet) {
        for (Edge e : targetSet) {
            SootMethod m = e.tgt();
            if (!MapInfo.isMapInterface(m))
                return false;
        }
        return true;
    }

    private Set<State> invokeCollectionInstTransFunc(SootMethod target, Unit inst, Value base, SootMethod method, State postcond) {
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
            outStates.addAll(replace(metaState, defAP, receiveAPElem));

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
                            updateState(postcond, pred, defApIter, receiveAP, i);
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
            outStates.addAll(replace(metaState, defAP, receiveAPElem));

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
            outStates.addAll(replace(postcond, receiveAPElem, paramAP));

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
            outStates.addAll(replace(postcond, receiveAPElem, paramAP));

        } else if (CollectionInfo.isAddAllCollection(targetName)) {
            AccessPath receiveAPElem = new AccessPath(base, elementRef, method);
            AccessPath paramAPElem;
            if (expr.getArgCount() == 1) {
                paramAPElem = new AccessPath(expr.getArg(0), elementRef, method);
            } else
                paramAPElem = new AccessPath(expr.getArg(1), elementRef, method);
            outStates.clear();
            postcond.addPredicate(BinaryPredicate.createNonNullPred(receiveAP, method));
            outStates.addAll(replace(postcond, receiveAPElem, paramAPElem));

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
            intermediateState.addAll(replace(postcond, receiveAPKeyElem, param1AP));
            for (State intermediateCond : intermediateState) {
                outStates.addAll(replace(intermediateCond, receiveAPValElem, param2AP));
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
            outStates.addAll(replace(postcond, defAP, nullAP));
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
            outStates.addAll(replace(metaState, defAP, receiveAPValElem));

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
                        updateState(postcond, pred, replaceElem, beReplacedElem, i);
                    }
                }
            }
        }
    }

    private Set<State> replace(State postcond, AccessPath defAP, AccessPath useAP) {
        if (defAP == null || useAP == null)
            throw new IllegalArgumentException("AccessPath is null");
        if (!defAP.isInstanceRef()) {
            State st = postcond.clone();
            Set<Predicate> newPreds = CollectionFactory.newSet();
            Set<Predicate> toRemovePreds = CollectionFactory.newSet();
            for (Iterator<Predicate> predIter = st.iterator(); predIter.hasNext();) {
                Predicate pred = predIter.next();
                if (!(pred instanceof BinaryPredicate)) continue;
                if (pred.containsAP(defAP)) {
                    Predicate newPred = pred.clone();
                    newPred.replace(defAP, useAP);
                    newPreds.add(newPred);
                    toRemovePreds.add(pred);
                }
            }
            st.removeAllPredicates(toRemovePreds);
            st.addAllPredicates(newPreds);
            return Collections.singleton(st);
        } else {
            Set<State> precond = CollectionFactory.newSet();
            Set<State> disjuncts = CollectionFactory.newSet();
            Set<AccessPath> visitedAPs = CollectionFactory.newSet();
            disjuncts.add(postcond);
            for (Predicate pred : postcond) {
                AccessPath[] aps = pred.getAllAccessPaths();
                for (int i = 0; i < aps.length; ++i) {
                    AccessPath predAP = aps[i];
                    if (predAP.isInstanceRef()) {
                        if (!predAP.isCompressed() && visitedAPs.contains(predAP)) continue;
                        Set<State> newDisjuncts = CollectionFactory.newSet();
                        for (State cond : disjuncts) {
                            newDisjuncts.addAll(createDisjuncts(cond, pred, i, predAP, defAP, useAP));
                        }
                        disjuncts.clear();
                        disjuncts.addAll(newDisjuncts);
                        if (!predAP.isCompressed() && !predAP.isElemFieldContained()) {
                            for (int k = 0; predAP.hasFieldAt(k); ++k) {
                                visitedAPs.add(predAP.getPartAP(k));
                            }
                        }
                    }
                }
            }
            precond.addAll(disjuncts);
            return precond;
        }
    }

    private void computeModPreds(SootMethod currentMethod, SootMethod target, Unit inst, Set<Predicate> inputPreds, Set<Predicate> modPreds,
                                 Set<Predicate> retPreds, Set<Predicate> unModPreds) {
        Value def = null;
        if (inst instanceof AssignStmt) {
            def = ((AssignStmt) inst).getLeftOp();
        }

        Map<PointsToSet, SootField> modMap = npVerifier.modMap.get(target);
        if (modMap == null)
            modMap = CollectionFactory.newMap();

        //TODO: skipList
        if (npVerifier.skipMethodList.contains(NPVerifier.getMethodSignature(target))) {
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
                            !(npVerifier.skipMethodList.contains(NPVerifier.getMethodSignature(target)))) {
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

    private boolean isRootPredProvedSafe(State pred) {
        if (!pred.hasRootPredicate())
            return false;
        AccessPath rootAP = pred.getRootPredicate().getAllAccessPaths()[0];
        if (rootAP.hasSpecialFields() || rootAP.getConsumerStmt() == null)
            return false;

        return safeRootPreds.containsKey(pred.getRootPredicate()) &&
               safeRootPreds.get(pred.getRootPredicate()).equals(rootAP.getConsumerStmt());
    }

    private void addToSummary(SootMethod method, State st, Set<State> result) {
        Map<State, Set<State>> summaryFunction = npVerifier.methodSummary.get(method);
        if (summaryFunction == null) {
            summaryFunction = CollectionFactory.newMap();
            summaryFunction.put(st, result);
            npVerifier.methodSummary.put(method, summaryFunction);
        } else {
            summaryFunction.put(st, result);
        }
    }

    private Set<State> getSummary(SootMethod method, State state) {
        Set<State> outStates = null;
        Map<State, Set<State>> summaryFunction = npVerifier.methodSummary.get(method);
        if (summaryFunction != null) {
            outStates = summaryFunction.get(state);
        }
        return outStates;
    }

    private boolean hasSubset(Set<State> states, State sub) {
        for (State st : states) {
            if (st != State.DummyState && st.isSubset(sub))
                return true;
        }
        return false;
    }

    public Set<Edge> getCallGraphNode(SootMethod method) {
        Set<Edge> callingEdgeSet = CollectionFactory.newSet();
        Iterator<Edge> callingEdgeIter = cg.iterator();
        while(callingEdgeIter.hasNext()) {
            Edge e = (Edge)callingEdgeIter.next();
            if (e.getTgt().method() == method)
                callingEdgeSet.add(e);
        }
        return callingEdgeSet;
    }

    private State getCallerStates(SootMethod currentMethod, SootMethod caller, Unit inst, State st) {
        InvokeExpr expr = null;
        if (inst instanceof InvokeStmt) {
            expr = ((InvokeStmt) inst).getInvokeExpr();
        } else if (inst instanceof AssignStmt) {
            Value rhs = ((AssignStmt) inst).getRightOp();
            if (rhs instanceof InvokeExpr) {
                expr = (InvokeExpr) rhs;
            } else {
                //throw new IllegalArgumentException("instruction: " + inst + " does not contain invoke expression");
                State ret = new State();
                ret.resetState(st);
                return ret;
            }
        }
        Map<Value, Value> formalToActural = invertMap(mapActuralToFormal(inst, expr));

        Set<Predicate> unMapCalleePreds = CollectionFactory.newSet();
        Set<Predicate> mapCalleePreds = CollectionFactory.newSet();
        mapPredicates(st.getPreds(), formalToActural, currentMethod, caller, mapCalleePreds, unMapCalleePreds);

        State callerState = new State();
        callerState.resetState(st);
        callerState.addAllPredicates(mapCalleePreds);

        for (Predicate pred : unMapCalleePreds) {
            if (pred.hasNoLocalAP(currentMethod))
                callerState.addPredicate(pred);
        }
        return callerState;
    }

    private void mapActuralToFormal(Set<Predicate> inputPreds, Map<Value, Value> varMap, SootMethod current, SootMethod target,
                               Set<Predicate> mappedPreds, Set<Predicate> unMappedPreds) {
         for (Predicate oldPred : inputPreds) {
             boolean replaced = false;
             Predicate newPred = null;
             for (Value var : varMap.keySet()) {
                 AccessPath oldAP = new AccessPath(var, current);
                 if (oldPred.containsAP(oldAP)) {
                     Value newVal = varMap.get(var);
                     AccessPath newAP = null;

                     if (!(newVal instanceof ThisRef || newVal instanceof ParameterRef)) {
                         newAP = new AccessPath(varMap.get(var), target);
                         if (newPred == null)
                             newPred = oldPred.clone();
                         if (newAP != null) {
                             newPred.replace(oldAP, newAP);
                             replaced = true;
                         }
                     }
                 }
             }
             if (replaced) {
                 mappedPreds.add(newPred);
             } else {
                 unMappedPreds.add(oldPred);
             }
         }
    }

    private void mapPredicates(Set<Predicate> inputPreds, Map<Value, Value> varMap, SootMethod current, SootMethod target,
                               Set<Predicate> mappedPreds, Set<Predicate> unMappedPreds) {
        if (!target.hasActiveBody()) return;
        for (Predicate oldPred : inputPreds) {
            boolean replaced = false;
            Predicate newPred = null;
            for (Value var : varMap.keySet()) {
                AccessPath oldAP = new AccessPath(var, current);
                if (oldPred.containsAP(oldAP)) {
                    Value newVal = varMap.get(var);
                    AccessPath newAP = null;
                    if (newVal instanceof ThisRef) {
                        Iterator<Unit> instIter = target.getActiveBody().getUnits().iterator();
                        while (instIter.hasNext()) {
                            Unit inst = instIter.next();
                            if (inst instanceof IdentityStmt) {
                                IdentityStmt identityStmt = (IdentityStmt) inst;
                                Value rhs = identityStmt.getRightOp();
                                if (rhs instanceof ThisRef) {
                                    newAP = new AccessPath(identityStmt.getLeftOp(), target);
                                }
                            }
                        }
                    } else if (newVal instanceof ParameterRef) {
                        Iterator<Unit> instIter = target.getActiveBody().getUnits().iterator();
                        while (instIter.hasNext()) {
                            Unit inst = instIter.next();
                            if (inst instanceof IdentityStmt) {
                                IdentityStmt identityStmt = (IdentityStmt) inst;
                                Value rhs = identityStmt.getRightOp();
                                if (rhs instanceof ParameterRef && rhs.equivTo(newVal)) {
                                    newAP = new AccessPath(identityStmt.getLeftOp(), target);
                                }
                            }
                        }
                    } else {
                        newAP = new AccessPath(varMap.get(var), target);
                    }

                    //assert newAP != null;

                    if (newAP == null) {
                        //logger.error("newAP is null, inst: " + newVal);
                        //throw new IllegalArgumentException("new AP is null");
                        continue;
                    }
                    if (newPred == null)
                        newPred = oldPred.clone();
                    newPred.replace(oldAP, newAP);
                    replaced = true;
                } else if (var instanceof ClassConstant && oldPred.getAllAccessPaths()[0].isStaticRef()) {
                    if (newPred == null)
                        newPred = oldPred.clone();
                    AccessPath lhs = newPred.getAllAccessPaths()[0];
                    lhs.setMethod(target);
                    replaced = true;
                }
            }
            if (replaced) {
                mappedPreds.add(newPred);
            } else {
                unMappedPreds.add(oldPred);
            }
        }
    }

    private State hasSubsetAndRootPred(Set<State> subSetStates, State superState) {
        boolean superRoot = superState.hasRootPredicate();
        for (State st : subSetStates) {
            boolean stRoot = st.hasRootPredicate();
            if ((superRoot && stRoot) || (!superRoot && !stRoot)) {
                if (st != State.DummyState &&
                        (!superRoot || st.getRootPredicate().equals(superState.getRootPredicate())) &&
                        st.isSubset(superState)) {
                    return st;
                }
            }
        }
        return null;
    }

    private <K, V> Map<V, K> invertMap(Map<K, V> m) {
        Map<V, K> result = CollectionFactory.newMap(); //new HashMap<V, K>(m.size());
        for (Map.Entry<K, V> entry : m.entrySet()) {
            K key = entry.getKey();
            V val = entry.getValue();
            if (result.containsKey(val)) {
                throw new IllegalArgumentException("input map is not one-to-one");
            }
            result.put(val, key);
        }
        return result;
    }

    private Map<Value, Value> mapActuralToFormal(Unit inst, InvokeExpr expr) {
        if (expr == null) {
            throw new IllegalArgumentException("expr is null");
        }
        SootMethod target = expr.getMethod();
        List<Value> args = expr.getArgs();
        Map<Value, Value> argsMap = CollectionFactory.newMap();
        Value base = null;//expr.getBase();
        if (expr instanceof SpecialInvokeExpr)
            base = ((SpecialInvokeExpr)expr).getBase();
        else if (expr instanceof VirtualInvokeExpr)
            base = ((VirtualInvokeExpr)expr).getBase();
        /*
        else if (expr instanceof StaticInvokeExpr) {
            StaticInvokeExpr staticInvokeExpr = (StaticInvokeExpr) expr;
            Type type = staticInvokeExpr.getMethod().getDeclaringClass().getType();
            Value classConst = ClassConstant.fromType(type);
            argsMap.put(classConst, classConst);
        }
        */
        Type type = expr.getMethod().getDeclaringClass().getType();
        Value classConst = ClassConstant.fromType(type);
        argsMap.put(classConst, classConst);

        //argsMap.put(base, retValue);
        //Value formalThis = Jimple.v().newLocal("this", RefType.v(target.getDeclaringClass()));
        if (base != null) {
            if ((base.getType() instanceof RefType)) {
                //throw new IllegalArgumentException("illegal ref type, inst: " + inst + ", base type: " + base.getType());
                Value formalThis = Jimple.v().newThisRef((RefType) base.getType());
                argsMap.put(base, formalThis);
            }
            //Jimple.v().newArrayRef()
        }
        int count = 0;
        Iterator<Type> parTypeIter = target.getParameterTypes().iterator();
        while(parTypeIter.hasNext()) {
            Type t = parTypeIter.next();
            Local l = Jimple.v().newLocal("parameter" + count, t);
            ParameterRef ref = Jimple.v().newParameterRef(l.getType(), count);
            argsMap.put(expr.getArg(count), ref);
            count++;
        }

        if (inst != null && inst instanceof AssignStmt) {
            Value def = ((AssignStmt)inst).getLeftOp();
            argsMap.put(def, Jimple.v().newLocal("retObj", def.getType()));
        }
        return argsMap;
    }

    public static int getOrCreateSymObject(Unit inst) {
        Integer symObj = symObjectMap.get(inst);
        if (symObj != null) {
            return symObj;
        } else {
            int ret = currentSymObject++;
            symObjectMap.put(inst, currentSymObject);
            return ret;
        }
    }

    public static boolean isSymObject(Value local) {
        if (!(local instanceof Local)) return false;

        return ((Local)local).getName().contains("symObj_");
    }

    public boolean aliasWithAPInRootPredicate(State state, AccessPath ap) {
        if (!state.hasRootPredicate())
            return false;
        Predicate rootPred = state.getRootPredicate();
        AccessPath[] aps = rootPred.getAllAccessPaths();
        for (int i = 0; i < aps.length; ++i) {
            if (!aps[i].isConstant() && mayAliases(aps[i], ap)) {
                return true;
            }
        }
        return false;
    }

    private boolean mayAliases(AccessPath ap1, AccessPath ap2) {
        if (ap1.equals(ap2))
            return true;
        // TODO:
        //PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
        if (!(ap1.getLocal() instanceof Local) || !(ap2.getLocal() instanceof Local)) {
            throw new IllegalArgumentException("ap's local isn't Local");
        }
        PointsToSet pts1 = null, pts2 = null;
        if (ap1.isInstanceRef()) {
            if (ap1.getFieldAt(1) instanceof FieldRef)
                pts1 = pta.reachingObjects((Local) ap1.getLocal(), ((FieldRef)ap1.getFieldAt(1)).getField());
        } else {
            pts1 = pta.reachingObjects((Local) ap1.getLocal());
        }

        if (ap2.isInstanceRef()) {
            if (ap2.getFieldAt(1) instanceof FieldRef)
                pts2 = pta.reachingObjects((Local) ap2.getLocal(), ((FieldRef)ap2.getFieldAt(1)).getField());
        } else {
            pts2 = pta.reachingObjects((Local) ap2.getLocal());
        }

        if (pts1 != null && pts2 != null && pts1.hasNonEmptyIntersection(pts2))
            return true;
        else
            return false;
    }

    private Operator getConditionOperator(Expr expr) {
        if (expr instanceof EqExpr)
            return Operator.EQ;
        else if (expr instanceof NeExpr)
            return Operator.NE;
        else if (expr instanceof LeExpr)
            return Operator.LE;
        else if (expr instanceof LtExpr)
            return Operator.LT;
        else if (expr instanceof GeExpr)
            return Operator.GE;
        else if (expr instanceof GtExpr)
            return Operator.GT;
        else
            return null;
    }

    public Set<Edge> getPossibleTargets(Unit inst) {
        Set<Edge> targetSet = CollectionFactory.newSet();
        Iterator<Edge> targetIter = cg.edgesOutOf(inst);
        while(targetIter.hasNext()) {
            Edge e = (Edge) targetIter.next();
            targetSet.add(e);
        }
        return targetSet;
    }

    private void updateState(State state, Predicate replacePred, AccessPath replaceAP,
                             AccessPath newAP, int predAPIndex) {
        if (!replaceAP.isCompressed()) {
            Set<Predicate> rmPreds = CollectionFactory.newSet();
            Set<Predicate> newPreds = CollectionFactory.newSet();
            for (Predicate pred : state) {
                AccessPath[] aps = pred.getAllAccessPaths();
                Predicate newPred = null;
                for (int i = 0; i < aps.length; ++i) {
                    if (!aps[i].isCompressed() && aps[i].contains(replaceAP)) {
                        if (newPred == null)
                            newPred = pred.clone();
                        newPred.replaceAt(i, replaceAP, newAP);
                    }
                }
                if (newPred != null) {
                    rmPreds.add(pred);
                    newPreds.add(newPred);
                }
            }
            state.removeAllPredicates(rmPreds);
            state.addAllPredicates(newPreds);
        } else {

        }
    }

    private Set<State> createDisjuncts(State postcond, Predicate pred, int predAPIndex,
                                       AccessPath predAP, AccessPath defAP, AccessPath useAP) {
        AccessPath defBase = defAP.getBase();
        Ref defField = defAP.getLastField();
        Map<Integer, Set<Integer>> aliasSet = CollectionFactory.newMap();
        int fieldNum;
        if (predAP.isElemFieldContained()) {
            if (!(defField instanceof ElementRef)) {
                fieldNum = 2;
                while (predAP.hasFieldAt(fieldNum)) {
                    if (predAP.getFieldAt(fieldNum).equals(defField) &&
                        predAP.getPartAP(fieldNum - 1).isElemFieldContained()) {
                        AccessPath aliasPredAP = predAP.getPartAP(fieldNum - 1).getAliasedAP();
                        AccessPath aliasDefBase = defBase.getAliasedAP();
                        if (!aliasPredAP.isElemFieldContained() &&
                            !aliasDefBase.isElemFieldContained() &&
                            !mayAliases(aliasPredAP, aliasDefBase)) {
                            fieldNum++;
                            continue;
                        }
                        Set<Integer> set = CollectionFactory.newSet();
                        set.add(0);
                        aliasSet.put(fieldNum - 1, set);
                    }
                    fieldNum++;
                }
            } else {
                fieldNum = 1;
                while (predAP.hasFieldAt(fieldNum)) {
                    if (predAP.getFieldAt(fieldNum).equals(defField)) {
                        AccessPath aliasPredAP = predAP.getPartAP(fieldNum - 1).getAliasedAP();
                        AccessPath aliasDefBase = defBase.getAliasedAP();
                        if (!aliasPredAP.isElemFieldContained() &&
                            !aliasDefBase.isElemFieldContained() &&
                            !mayAliases(aliasPredAP, aliasDefBase)) {
                            fieldNum++;
                            continue;
                        }
                        Set<Integer> set = CollectionFactory.newSet();
                        set.add(0);
                        aliasSet.put(fieldNum - 1, set);
                    }
                    fieldNum++;
                }
            }

        }

        aliasSet.putAll(getMayAliasAPs(predAP, defBase));

        Set<AccessPath[]> aliasAPs = CollectionFactory.newSet();
        for (Integer predFieldNum : aliasSet.keySet()) {
            Set<Integer> baseFieldNum = aliasSet.get(predFieldNum);
            if (baseFieldNum != null) {
                AccessPath partAP = predAP.getPartAP(predFieldNum.intValue());
                if (predAP.hasFieldAt(predFieldNum.intValue() + 1)) {
                    Ref nextField = predAP.getFieldAt(predFieldNum.intValue() + 1);
                    //if (defField.equivTo(nextField)) {
                    //TODO: need revised
                    boolean checkEqual = false;
                    if (defField instanceof InstanceFieldRef && nextField instanceof InstanceFieldRef) {
                        InstanceFieldRef instDefField = (InstanceFieldRef) defField;
                        InstanceFieldRef instNextField = (InstanceFieldRef) nextField;
                        if (instDefField.getFieldRef().equals(instNextField.getFieldRef()))
                            checkEqual = true;
                        else {
                            SootFieldRef lhs = instDefField.getFieldRef();
                            SootFieldRef rhs = instNextField.getFieldRef();
                            FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();
                            if (lhs.name().equals(rhs.name()) && lhs.type().equals(rhs.type()) &&
                                    (fh.isSubclass(lhs.declaringClass(), rhs.declaringClass()) || fh.isSubclass(rhs.declaringClass(), lhs.declaringClass()))) {
                                checkEqual = true;
                            }
                        }
                    } else if (defField instanceof StaticFieldRef && nextField instanceof StaticFieldRef) {
                        StaticFieldRef staticDefField = (StaticFieldRef) defField;
                        StaticFieldRef staticNextField = (StaticFieldRef) nextField;
                        if (staticDefField.getFieldRef().equals(staticNextField.getFieldRef()))
                            checkEqual = true;
                    }
                    //if (defField.equivHashCode() == nextField.equivHashCode()) {
                    if (checkEqual) {
                        AccessPath[] partAParray = new AccessPath[2];
                        AccessPath partAPdotF = predAP.getPartAP(predFieldNum.intValue() + 1);
                        partAParray[0] = partAP;
                        partAParray[1] = partAPdotF;
                        aliasAPs.add(partAParray);
                    }
                }
            }
        }

        Set<State> outStates = CollectionFactory.newSet();
        if (aliasAPs.size() == 1) {
            AccessPath[] aap = aliasAPs.iterator().next();
            if (!aap[0].isCompressed()
                    && !defBase.isCompressed()
                    && aap[0].equals(defBase)) {
                State newState = postcond.clone();
                updateState(newState, pred, aap[1], useAP, predAPIndex);
                outStates.add(newState);
                return outStates;
            }
        }

        for (AccessPath[] aap : aliasAPs) {
            State newState = postcond.clone();
            updateState(newState, pred, aap[1], useAP, predAPIndex);
            for (AccessPath[] allAAPs : aliasAPs) {
                if (!predAP.isCompressed()) {
                    if (!allAAPs.equals(aap)) {
                        newState.addPredicate(new AliasPredicate(defBase, allAAPs[0], true));
                    } else {
                        newState.addPredicate(new AliasPredicate(defBase, aap[0], false));
                    }
                }
            }
            outStates.add(newState);
        }

        State originalState = postcond.clone();
        if (!predAP.isCompressed()) {
            for (AccessPath[] allAAPs : aliasAPs) {
                if (!(defField instanceof ElementRef)) {
                    originalState.addPredicate(new AliasPredicate(defBase, allAAPs[0], true));
                }
            }
        }
        outStates.add(originalState);

        return outStates;
    }

    private Map<Integer, Set<Integer>> getMayAliasAPs(AccessPath ap1, AccessPath ap2) {
        Map<Integer, Set<Integer>> aliasSet = CollectionFactory.newMap();

        Map<Integer, PointsToSet> pts1 = ap1.getPointsToSet(pta);
        Map<Integer, PointsToSet> pts2 = ap2.getPointsToSet(pta);

        for (Integer ap1Field : pts1.keySet()) {
            PointsToSet ap1Pts = pts1.get(ap1Field);
            for (Integer ap2Field : pts2.keySet()) {
                PointsToSet ap2Pts = pts2.get(ap2Field);
                if (ap1Pts.hasNonEmptyIntersection(ap2Pts)) {
                    Set<Integer> s = CollectionFactory.newSet();
                    s.add(ap2Field);
                    aliasSet.put(ap1Field, s);
                }
            }
        }
        return aliasSet;
    }

}
