package thread;

import core.*;
import predicate.AliasPredicate;
import predicate.BinaryPredicate;
import predicate.Predicate;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class NPUtil {

    public static boolean hasSubset(Set<State> states, State sub) {
        for (State st : states) {
            if (st != State.DummyState && st.isSubset(sub))
                return true;
        }
        return false;
    }

    public static Set<Edge> getCallGraphNode(SootMethod method, CallGraph cg) {
        Set<Edge> callingEdgeSet = CollectionFactory.newSet();
        Iterator<Edge> callingEdgeIter = cg.iterator();
        while(callingEdgeIter.hasNext()) {
            Edge e = (Edge)callingEdgeIter.next();
            if (e.getTgt().method() == method)
                callingEdgeSet.add(e);
        }
        return callingEdgeSet;
    }

    public static State getCallerStates(SootMethod currentMethod, SootMethod caller, Unit inst, State st) {
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

    public static <K, V> Map<V, K> invertMap(Map<K, V> m) {
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

    public static Map<Value, Value> mapActuralToFormal(Unit inst, InvokeExpr expr) {
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

    public static void mapPredicates(Set<Predicate> inputPreds, Map<Value, Value> varMap, SootMethod current, SootMethod target,
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

    public static int getOrCreateSymObject(Unit inst) {
        Integer symObj = NPAlgo.symObjectMap.get(inst);
        if (symObj == null) {
            NPAlgo.symObjectMap.computeIfAbsent(inst, v -> NPAlgo.currentSymObject.incrementAndGet());
            return NPAlgo.symObjectMap.get(inst);
        }
        return symObj;
    }

    public static State hasSubsetAndRootPred(Set<State> subSetStates, State superState) {
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

    public static boolean isRootPredProvedSafe(State pred) {
        if (!pred.hasRootPredicate())
            return false;
        AccessPath rootAP = pred.getRootPredicate().getAllAccessPaths()[0];
        if (rootAP.hasSpecialFields() || rootAP.getConsumerStmt() == null)
            return false;
        return false;
    }

    public static Set<State> replace(State postcond, AccessPath defAP, AccessPath useAP,
                                     PointsToAnalysis pta) {
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
                            newDisjuncts.addAll(createDisjuncts(cond, pred, i, predAP, defAP, useAP, pta));
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

    public static Set<State> createDisjuncts(State postcond, Predicate pred, int predAPIndex,
                                             AccessPath predAP, AccessPath defAP, AccessPath useAP,
                                             PointsToAnalysis pta) {
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
                                !mayAliases(aliasPredAP, aliasDefBase, pta)) {
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
                                !mayAliases(aliasPredAP, aliasDefBase, pta)) {
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

        aliasSet.putAll(getMayAliasAPs(predAP, defBase, pta));

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

    public static void updateState(State state, Predicate replacePred, AccessPath replaceAP,
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

    public static Map<Integer, Set<Integer>> getMayAliasAPs(AccessPath ap1,
                                                            AccessPath ap2,
                                                            PointsToAnalysis pta) {
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

    public static boolean aliasWithAPInRootPredicate(State state, AccessPath ap,
                                                     PointsToAnalysis pta) {
        if (!state.hasRootPredicate())
            return false;
        Predicate rootPred = state.getRootPredicate();
        AccessPath[] aps = rootPred.getAllAccessPaths();
        for (int i = 0; i < aps.length; ++i) {
            if (!aps[i].isConstant() && mayAliases(aps[i], ap, pta)) {
                return true;
            }
        }
        return false;
    }

    public static boolean mayAliases(AccessPath ap1, AccessPath ap2, PointsToAnalysis pta) {
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

    public static Operator getConditionOperator(Expr expr) {
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

    public static String getMethodSignature(SootMethod method) {
        StringBuffer ret = new StringBuffer();
        ret.append(method.getDeclaringClass().toString());
        ret.append(".");
        ret.append(method.getName());
        List<Type> params = method.getParameterTypes();
        ret.append("(");
        for(int i = 0; i < params.size(); ++i) {
            ret.append(((Type)params.get(i)).getEscapedName());
            if (i < params.size() - 1) {
                ret.append(",");
            }
        }
        ret.append(")");
        ret.append(method.getReturnType().toString());
        return ret.toString().intern();
    }

    public static boolean invokeInstPossible(SootMethod method) {
        //if (exclusionList.contains())
        //String signature = expr.getMethod().getDeclaringClass().getName();
        String methodName = NPVerifier.getMethodSignature(method);
        String klass = method.getDeclaringClass().getName();
        if (NPAlgo.exclusionList.contains(methodName)
                || klass.startsWith("java")
                || klass.startsWith("javax")
                || klass.startsWith("sun"))
            return false;

        return true;
    }

    public static Set<Edge> getPossibleTargets(Unit inst, CallGraph cg) {
        Set<Edge> targetSet = CollectionFactory.newSet();
        Iterator<Edge> targetIter = cg.edgesOutOf(inst);
        while(targetIter.hasNext()) {
            Edge e = (Edge) targetIter.next();
            targetSet.add(e);
        }
        return targetSet;
    }

    public static boolean isNotNull(String className, String methodName) {
        String method = NPAlgo.notNullList.get(className);
        if (method != null) {
            return method.equals(methodName);
        } else
            return false;
    }

    public static boolean isLibraryCollection(Set<Edge> targetSet) {
        for (Edge e : targetSet) {
            SootMethod m = e.tgt();
            if (!NPUtil.isCollectionSubClass(m.getDeclaringClass()))
                return false;
        }
        return true;
    }

    public static boolean isLibraryMap(Set<Edge> targetSet) {
        for (Edge e : targetSet) {
            SootMethod m = e.tgt();
            if (!NPUtil.isMapInterface(m))
                return false;
        }
        return true;
    }

    public static boolean isMapInterface(SootMethod method) {
        String methodName = method.getName();
        SootClass declaringClass = method.getDeclaringClass();
        if (declaringClass.isJavaLibraryClass()) {
            if (NPAlgo.mapClass.contains(declaringClass)) {
                if (MapInfo.isPut(methodName) ||
                        MapInfo.isGetKeySet(methodName) ||
                        MapInfo.isGetValueSet(methodName) ||
                        MapInfo.isClear(methodName) ||
                        MapInfo.isGetValue(methodName) ||
                        MapInfo.isId(methodName) ||
                        MapInfo.isEnum(methodName) ||
                        methodName.equals("<init>"))
                    return true;
            } else {
                Set<String> methodNames = new HashSet<String>();
                for (SootMethod m : declaringClass.getMethods()) {
                    methodNames.add(m.getName());
                }
                if (MapInfo.interfaceSet.containsAll(methodNames)) {
                    NPAlgo.mapClass.add(declaringClass);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isCollectionSubClass(SootClass klass) {
        if (klass.isJavaLibraryClass()) {
            if (NPAlgo.collectionClass.contains(klass) ||
                    NPAlgo.iteratorClass.contains(klass) ||
                    NPAlgo.enumClass.contains(klass))
                return true;
            else {
                Set<String> methodNames = new HashSet<String>();
                for (SootMethod m : klass.getMethods()) {
                    methodNames.add(m.getName());
                }

                if (CollectionInfo.collectionInterfaces.containsAll(methodNames)) {
                    NPAlgo.collectionClass.add(klass);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isThisReference(PatchingChain<Unit> instructions, Value base) {
        Iterator<Unit> instIter = instructions.iterator();
        while (instIter.hasNext()) {
            Unit inst = instIter.next();
            if (inst instanceof IdentityStmt) {
                IdentityStmt identityStmt = (IdentityStmt) inst;
                Value lhs = identityStmt.getLeftOp();
                Value rhs = identityStmt.getRightOp();
                if (lhs == base && rhs instanceof ThisRef)
                    return true;
            }
        }
        return false;
    }

    public static boolean isArrayInst(Unit inst) {
        if (inst instanceof AssignStmt) {
            Value rhs = ((AssignStmt) inst).getRightOp();
            if (rhs instanceof LengthExpr)
                return true;
        }
        return false;
    }

    public static Value getInstBase(Unit u) {
        Value ret = null;
        ret = getLoadBase(u);
        if (ret != null) return ret;
        ret = getStoreBase(u);
        if (ret != null) return ret;
        ret = getInvokeOnlyBase(u);
        if (ret != null) return ret;
        ret = getInvokeAssignBase(u);
        if (ret != null) return ret;
        ret = getArrayLength(u);
        if (ret != null) return ret;
        return null;
    }

    public static Value getArrayLength(Unit u) {
        if (u instanceof AssignStmt) {
            Value rval = ((AssignStmt)u).getRightOp();
            if (rval instanceof LengthExpr) {
                return ((LengthExpr)rval).getOp();
            }
        } else if (u instanceof LengthExpr) {
            return ((LengthExpr)u).getOp();
        }
        return null;
    }

    public static Value getLoadBase(Unit u) {
        if (u instanceof AssignStmt) {
            Value rval = ((AssignStmt)u).getRightOp();
            if (rval instanceof InstanceFieldRef) {
                return ((InstanceFieldRef)rval).getBase();
            }
        }
        return null;
    }
    public static Value getStoreBase(Unit u) {
        if (u instanceof AssignStmt) {
            Value lval = ((AssignStmt)u).getLeftOp();
            if (lval instanceof InstanceFieldRef) {
                return ((InstanceFieldRef)lval).getBase();
            }
        }
        return null;
    }
    public static Value getInvokeOnlyBase(Unit u) {
        if (u instanceof InvokeStmt) {
            InvokeExpr invoke = ((InvokeStmt)u).getInvokeExpr();
            if (invoke instanceof VirtualInvokeExpr) {
                return ((VirtualInvokeExpr)invoke).getBase();
            } else if (invoke instanceof SpecialInvokeExpr) {
                return ((SpecialInvokeExpr) invoke).getBase();
            } else if (invoke instanceof InterfaceInvokeExpr) {
                return ((InterfaceInvokeExpr) invoke).getBase();
            }
        }
        return null;
    }
    public static Value getInvokeAssignBase(Unit u) {
        if (u instanceof AssignStmt) {
            Value rval = ((AssignStmt)u).getRightOp();
            if (rval instanceof VirtualInvokeExpr) {
                return ((VirtualInvokeExpr)rval).getBase();
            } else if (rval instanceof SpecialInvokeExpr) {
                return ((SpecialInvokeExpr) rval).getBase();
            } else if (rval instanceof InterfaceInvokeExpr) {
                return ((InterfaceInvokeExpr) rval).getBase();
            }
        }
        return null;
    }

    public static Set<State> removeInvalidStates(Set<State> result) {
        Set<State> toRemove = CollectionFactory.newSet();
        for (State st : result) {
            for (Predicate pred : st) {
                if (pred instanceof BinaryPredicate) {
                    BinaryPredicate bp = (BinaryPredicate) pred;
                    AccessPath lhs = bp.getLhs();
                    AccessPath rhs = bp.getRhs();
                    if (!bp.isNegated()) {
                        if ((lhs.isNullConstant() && lhs.hasSpecialFields())
                                || (rhs.isNullConstant() && rhs.hasSpecialFields())) {
                            toRemove.add(st);
                            break;
                        }
                    }
                }
            }
        }
        result.removeAll(toRemove);
        return result;
    }

    public static void updateMap(SootMethod method, Unit inst, Map<SootMethod, List<Unit>> map) {
        while (true) {
            List<Unit> instList = Collections.synchronizedList(new ArrayList<Unit>());
            List<Unit> oldList = map.putIfAbsent(method, instList);
            if (oldList != null) {
                instList = oldList;
            }
            instList.add(inst);
            if (instList == map.get(method))
                break;
        }
    }

}
