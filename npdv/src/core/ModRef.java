package core;

import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.Sources;

import java.util.*;

public class ModRef {

    private CallGraph callGraph;
    private PointsToAnalysis pointsToAnalysis;
    private Set<SootMethod> reachableSet;

    private Map<SootMethod, Set<SootMethod>> reachableMethods;
    public ModRef(CallGraph cg, PointsToAnalysis pta) {
        assert cg != null && pta != null;
        callGraph = cg;
        pointsToAnalysis = pta;
        reachableMethods = CollectionFactory.newMap();
        reachableSet = CollectionFactory.newSet();
    }

    public Map<SootMethod, Map<PointsToSet, SootField>> computeMod() {
        Map<SootMethod, Map<PointsToSet, SootField>> scan = scanForMod();
        return transitiveClosure(scan);
    }

    public Map<SootMethod, Map<PointsToSet, SootField>> scanForMod() {
        Map<SootMethod, Map<PointsToSet, SootField>> result = CollectionFactory.newMap();
        Iterator<Edge> iterEdge = callGraph.iterator();
        while (iterEdge.hasNext()) {
            Edge edge = iterEdge.next();
            SootMethod src = edge.src();
            SootMethod tgt = edge.tgt();
            reachableSet.add(src);
            reachableSet.add(tgt);
        }
        //Iterator<MethodOrMethodContext> it = callGraph.sourceMethods();
        Iterator<SootMethod> it = reachableSet.iterator();
        //Iterator<SootMethod> it = NPVerifier.applicationMethods.iterator();
        while (it.hasNext()) {
            SootMethod method = it.next().method();
            result.put(method, scanMethodForMod(method));
        }
        return result;
    }

    public Map<PointsToSet, SootField> scanMethodForMod(SootMethod method) {
        Map<PointsToSet, SootField> result = CollectionFactory.newMap();
        Body activeBody = null;
        if (!method.getDeclaringClass().isPhantom() && method.hasActiveBody())
            activeBody = method.retrieveActiveBody();
        if (activeBody == null)
            return result; //EmptyPointsToSet.v();

        PatchingChain<Unit> units = activeBody.getUnits();
        Iterator<Unit> unitIter = units.iterator();


        while (unitIter.hasNext()) {
            Unit inst = unitIter.next();

            if (inst instanceof AssignStmt) {
                Value lval = ((AssignStmt) inst).getLeftOp();
                Value rval = ((AssignStmt) inst).getRightOp();

                if (rval instanceof NewExpr) {
                    NewExpr newExpr = (NewExpr)rval;
                    Type type = newExpr.getType();
                    if (type instanceof ArrayType) {

                    } else if (type instanceof RefType){
                        RefType refType = (RefType) type;
                        SootClass klass = refType.getSootClass();
                        Iterator<SootField> fieldIterator = klass.getFields().iterator();
                        while (fieldIterator.hasNext()) {
                            SootField field = fieldIterator.next();
                            if (!field.isStatic()) {
                                PointsToSet pts = pointsToAnalysis.reachingObjects((Local) lval);
                                result.put(pts, field);
                            }
                        }

                    }

                } else if (lval instanceof FieldRef) {
                    if (lval instanceof StaticFieldRef) {
                        StaticFieldRef field = (StaticFieldRef) lval;
                        PointsToSet pts = pointsToAnalysis.reachingObjects(field.getField());
                        result.put(pts, field.getField());
                    } else {
                        InstanceFieldRef field = (InstanceFieldRef) lval;
                        //PointsToSet pts = pointsToAnalysis.reachingObjects((Local) field.getBase(), field.getField());
                        PointsToSet pts = pointsToAnalysis.reachingObjects((Local) field.getBase());
                        result.put(pts, field.getField());
                    }
                }
            }
        }

        //transitive closure
        return result;
    }

    private Set<SootMethod> computeBackwardReachable(SootMethod method) {
        if (reachableMethods.containsKey(method)) {
            return reachableMethods.get(method);
        }

        Set<SootMethod> reachable = CollectionFactory.newSet();
        ArrayList<SootMethod> nextRound = new ArrayList<SootMethod>();
        ArrayList<SootMethod> currRound = new ArrayList<SootMethod>();
        currRound.add(method);
        reachable.add(method);
        while (!currRound.isEmpty()) {
            nextRound.clear();
            for (SootMethod tgt : currRound) {
                //Iterator<MethodOrMethodContext> iterSource = new Sources(callGraph.edgesInto(tgt));
                Iterator<Edge> iterSource = callGraph.edgesInto(tgt);
                while (iterSource.hasNext()) {
                    SootMethod src = (SootMethod) iterSource.next().src();
                    if (!reachable.contains(src)) {
                        reachable.add(src);
                        nextRound.add(src);
                    }
                }
            }
            ArrayList<SootMethod> tmp = currRound;
            currRound = nextRound;
            nextRound = tmp;
        }
        reachableMethods.put(method, reachable);
        return reachable;
    }

    public Map<SootMethod, Map<PointsToSet, SootField>> transitiveClosure(Map<SootMethod, Map<PointsToSet, SootField>> scan) {
        for (SootMethod method : scan.keySet()) {
            reachableMethods.put(method, computeBackwardReachable(method));
        }

        Map<SootMethod, Map<PointsToSet, SootField>> scan_prime = CollectionFactory.newMap();

        for (SootMethod method : reachableMethods.keySet()) {
            Set<SootMethod> reachable = reachableMethods.get(method);
            Map<PointsToSet, SootField> pts = scan.get(method);
            for (SootMethod reach : reachable) {
                Map<PointsToSet, SootField> reachPTS = scan_prime.get(reach);
                if (reachPTS == null) {
                    reachPTS = CollectionFactory.newMap();
                    scan_prime.put(reach, reachPTS);
                } else
                    reachPTS.putAll(pts);
            }
        }

        for (SootMethod method: scan.keySet()) {
            //scan.get(method).putAll(scan_prime.get(method));
            Map<PointsToSet, SootField> prime = scan_prime.get(method);
            if (prime != null) {
                scan.get(method).putAll(prime);
            }
        }

        /*
        for (SootMethod method : scan.keySet()) {
            Queue<SootMethod> worklist = new PriorityQueue<>();
            Iterator<Edge> iterSource = callGraph.edgesInto(method);
            worklist.add(method);
            while (!worklist.isEmpty()) {

                boolean modified = false;
                SootMethod src = iterSource.next().src();

                if (modified) {
                    worklist.add(src);
                }
            }
        }
        */
        return scan;
    }

    public Map<SootMethod, Map<PointsToSet, SootField>> computeRef() {
        Map<SootMethod, Map<PointsToSet, SootField>> scan = scanForRef();
        return transitiveClosure(scan);
    }

    public Map<SootMethod, Map<PointsToSet, SootField>> scanForRef() {
        Map<SootMethod, Map<PointsToSet, SootField>> result = CollectionFactory.newMap();
        Iterator<MethodOrMethodContext> it = callGraph.sourceMethods();
        //Iterator<SootMethod> it = NPVerifier.applicationMethods.iterator();
        while (it.hasNext()) {
            SootMethod method = it.next().method();
            result.put(method, scanMethodForRef(method));
        }
        return result;
    }

    public Map<PointsToSet, SootField> scanMethodForRef(SootMethod method) {
        Map<PointsToSet, SootField> result = CollectionFactory.newMap();
        Body activeBody = method.retrieveActiveBody();
        if (activeBody == null)
            return result; //EmptyPointsToSet.v();

        PatchingChain<Unit> units = activeBody.getUnits();
        Iterator<Unit> unitIter = units.iterator();


        while (unitIter.hasNext()) {
            Unit inst = unitIter.next();

            if (inst instanceof AssignStmt) {
                Value lval = ((AssignStmt) inst).getLeftOp();
                Value rval = ((AssignStmt) inst).getRightOp();

                if (rval instanceof NewExpr) {
                    NewExpr newExpr = (NewExpr)rval;
                    Type type = newExpr.getType();
                    if (type instanceof ArrayType) {

                    } else if (type instanceof RefType){
                        RefType refType = (RefType) type;
                        SootClass klass = refType.getSootClass();
                        Iterator<SootField> fieldIterator = klass.getFields().iterator();
                        while (fieldIterator.hasNext()) {
                            SootField field = fieldIterator.next();
                            if (!field.isStatic()) {
                                PointsToSet pts = pointsToAnalysis.reachingObjects((Local) lval);
                                result.put(pts, field);
                            }
                        }

                    }

                } else if (lval instanceof FieldRef) {
                    if (lval instanceof StaticFieldRef) {
                        StaticFieldRef field = (StaticFieldRef) lval;
                        PointsToSet pts = pointsToAnalysis.reachingObjects(field.getField());
                        result.put(pts, field.getField());
                    } else {
                        InstanceFieldRef field = (InstanceFieldRef) lval;
                        PointsToSet pts = pointsToAnalysis.reachingObjects((Local) field.getBase(), field.getField());
                        result.put(pts, field.getField());
                    }
                }
            }
        }
        return result;
    }

}
