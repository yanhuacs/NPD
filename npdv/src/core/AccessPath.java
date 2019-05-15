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

import soot.*;
import soot.jimple.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Created by Xinwei Xie on 21/7/17.
 */
public class AccessPath {
    private Value local;
    private Vector<Ref> fields;
    private SootMethod method;
    private boolean compressed;
    private int hashCode = -1;
    private Unit consumerStmt;

    public AccessPath(Value l, SootMethod m) {
        this(l, null, m, null, false);
    }

    public AccessPath(Value l, SootMethod m, Unit inst) {
        this(l, null, m, inst, false);
    }

    public AccessPath(Value l, SootMethod m, Unit inst, boolean comp) {
        this(l, null, m, inst,  comp);
    }

    public AccessPath(Value l, Ref ref, SootMethod m) {
        this(l, ref, m, null, false);
    }

    public AccessPath(Value l, Ref ref, SootMethod m, Unit inst) {
        this(l, ref, m, inst, false);
    }

    public AccessPath(Value l, Ref ref, SootMethod m, Unit inst, boolean comp) {
        assert (ref == null || ref instanceof Ref || l == null);
        local = l;
        method = m;
        fields = new Vector<Ref>();
        if (ref != null) fields.add(ref);
        compressed = comp;
        consumerStmt = inst;
    }

    public Unit getConsumerStmt() {
        return consumerStmt;
    }

    public Value getLocal() {
        return local;
    }

    public boolean contains(AccessPath ap) {
        if (method != ap.method)
            return false;
        if (fields.size() < ap.fields.size())
            return false;

        if (!local.equals(ap.local)) {
            /*
            if (local instanceof Local && ap.local instanceof Local) {
                if (!((Local) local).getName().equals(((Local) ap.local).getName())
                    || !(local.getType() == ap.local.getType()))
                    return false;
            } else
                return false;
            */
            if (local.getClass() != ap.local.getClass())
                return false;
            else {
                if (local instanceof ParameterRef && ap.local instanceof ParameterRef) {
                    if (!((ParameterRef)local).equivTo(ap.local))
                        return false;
                } else if (local instanceof Local && ap.local instanceof Local) {
                    if (!((Local) local).getName().equals(((Local) ap.local).getName()))
                        return false;
                } else if (local instanceof ThisRef && ap.local instanceof ThisRef) {
                    FastHierarchy fh = Scene.v().getFastHierarchy();
                    if (!local.equivTo(ap.local)) {
                        if (!fh.canStoreType(local.getType(), ap.local.getType()))
                            return false;
                    }
                } else if (local instanceof ClassConstant && ap.local instanceof ClassConstant) {
                    if (!((ClassConstant) local).value.equals(((ClassConstant) ap.local).value))
                        return false;
                }
            }
            /*
            else if (local.getType() != ap.local.getType())
                return false;
            else if (local instanceof ParameterRef && ap.local instanceof ParameterRef) {
                if (!((ParameterRef)local).equivTo(ap.local))
                    return false;

            } else if (local instanceof Local && ap.local instanceof Local) {
                if (!((Local) local).getName().equals(((Local) ap.local).getName()))
                    return false;
            } else if (local instanceof ThisRef && ap.local instanceof ThisRef) {
                if (!local.equivTo(ap.local))
                    return false;
            } else if (!(local instanceof ClassConstant && ap.local instanceof ClassConstant)) {
                //if (!((ClassConstant) local).value.equals(((ClassConstant) ap.local).value))
                    //return false;
                return false;
            }
            */
        }

        for (int i = 0; i < ap.fields.size(); ++i) {
            Ref lhsRef = fields.elementAt(i);
            Ref rhsRef = ap.fields.elementAt(i);
            if (lhsRef instanceof FieldRef && rhsRef instanceof FieldRef) {
                if (!((FieldRef) lhsRef).getField().equals(((FieldRef) rhsRef).getField()))
                    return false;
            } else if (!lhsRef.equals(rhsRef))
                return false;
        }

        return true;
    }
    public String toString() {
        String str = local.toString();
        for (Ref ref : fields) {
            if (ref instanceof ArrayRef) {
                str += "[" + ref + "]";
            } else if (ref instanceof FieldRef) {
                str += "." + ((FieldRef)ref).getField().getName();
            } else {
                str += "." + ref.toString();
            }
        }
        //str += " :" + method.getName() + ":";
        return str;
    }

    public boolean isThisRef() {
        //return local instanceof ThisRef;
        return !isRef() && (local instanceof ThisRef) && !method.isStatic();
    }

    public boolean isRef() {
        return fields.size() != 0;
    }

    public boolean isConstant() {
        // TODO:
        if (local instanceof Constant)
            return true;
        return false;
    }

    public boolean isStaticRef() {
        return isRef() && (local instanceof ClassConstant);
    }

    public boolean isNullConstant() {
        return local instanceof NullConstant;
    }

    public int length() {
        return 1 + fields.size();
    }

    public boolean isStringConstant() {
        return local instanceof StringConstant;
    }

    public boolean hasSpecialFields() {
        for (Ref ref : fields) {
            if (ref instanceof ElementRef
                    || ref instanceof IteratorFieldRef
                    || ref instanceof ValueFieldRef
                    || ref instanceof KeyFieldRef)
                return true;
        }
        return false;
    }

    public boolean isSpecialField(Ref ref) {
        if (ref instanceof ElementRef
                || ref instanceof IteratorFieldRef
                || ref instanceof ValueFieldRef
                || ref instanceof KeyFieldRef)
            return true;
        return false;
    }

    public boolean isInstanceRef() {
        //return (fields.size() != 0); // && (local instanceof InstanceFieldRef);
        //return isRef() && !(local instanceof ThisRef);
        return isRef() && (local instanceof Local);
    }

    public boolean hasField() {
        return fields.size() != 0;
    }

    public int numOfFields() {
        return fields.size();
    }

    public AccessPath clone() {
        AccessPath clone = new AccessPath(local, null, method, consumerStmt, compressed);
        clone.fields.addAll(fields);
        return clone;
    }


    private boolean isRefEqual(Ref lhs, Ref rhs) {
        if (lhs == rhs) return true;
        else if ((lhs instanceof FieldRef) && (rhs instanceof FieldRef)) {
            FieldRef lhsFieldRef = (FieldRef) lhs;
            FieldRef rhsFieldRef = (FieldRef) rhs;
            if (!lhsFieldRef.getField().equals(rhsFieldRef.getField())) {
                return false;
            }
            return true;
        } else if (lhs.equals(rhs))
            return true;
        else
            return false;
    }

    public void replace(AccessPath oldAP, AccessPath newAP) {
        if (!contains(oldAP))
            return;
        method = newAP.method;
        local = newAP.local;
        if (!compressed || oldAP.isInstanceRef())
            compressed = newAP.compressed;

        if (oldAP.fields.size() == newAP.fields.size()) {
            if (!isElemFieldContained())
                consumerStmt = newAP.consumerStmt;
        }
        for (int i = 0; i < oldAP.fields.size(); i++) {
            fields.remove(0);
        }
        fields.addAll(0, newAP.fields);

        boolean repeatedFields = false;
        int i = 0;
        do {
            repeatedFields = false;
            for (int j = fields.size() - 1; j > i; j--) {
                if (isRefEqual(fields.get(i), fields.get(j))
                    && !(isSpecialField(fields.get(i)))) {
                    repeatedFields = true;
                    compressed = true;
                    int numOfFields = j - i - 1;
                    for (int k = i + 1; numOfFields > 0; numOfFields--) {
                        fields.remove(k);
                    }

                    if (numOfFields > 0) {
                        StarFieldRef starFieldRef = new StarFieldRef();
                        fields.add(i + 1, starFieldRef);
                    }
                    break;
                }
            }
            i++;
        } while(repeatedFields);
    }

    public boolean isCompressed() { return compressed; }

    public boolean isElemFieldContained() {
        for (Ref ref : fields) {
            if (ref instanceof ElementRef)
                return true;
        }
        return false;
    }

    public boolean hasFieldAt(int fieldNum) {
        if (fieldNum < 0 || fieldNum > fields.size())
            return false;
        return true;
    }
    public AccessPath getPartAP(int fieldNum) {
        if (!hasFieldAt(fieldNum))
            throw new IllegalArgumentException("Field number should be within the limits of the field array");

        AccessPath partAP = new AccessPath(local, null, method, null, compressed);
        int fNum = 1;
        for (Ref ref : fields) {
            if (fNum > fieldNum)
                break;
            partAP.fields.add(ref);
            fNum++;
        }
        return partAP;
    }

    public boolean equals(Object obj) {
        if (obj instanceof AccessPath) {
            AccessPath ap = (AccessPath) obj;
            if (isNullConstant() && ap.isNullConstant()) {
                return true;
            }
            //else if (this.method == ap.method && this.local.equivTo(ap.local)) { //&& this.fields.equals(ap.fields)) {
            else if (this.method == ap.method && this.local.equivHashCode() == ap.local.equivHashCode()) { //&& this.fields.equals(ap.fields)) {
                if (fields.size() == ap.fields.size()) {
                    for (int i = 0; i < fields.size(); ++i) {
                        Ref lhsRef = fields.elementAt(i);
                        Ref rhsRef = ap.fields.elementAt(i);
                        /*
                        if (!lhsRef.equivTo(rhsRef))
                            return false;
                        */
                        if (lhsRef instanceof FieldRef && rhsRef instanceof FieldRef) {
                            if (!((FieldRef) lhsRef).getField().equals(((FieldRef) rhsRef).getField()))
                                return false;
                        } else if (!lhsRef.equals(rhsRef))
                            return false;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public int hashCode() {
        if (hashCode != -1) return hashCode;
        if (isConstant()) {
            if (isNullConstant())
                hashCode = 1;
            else
                hashCode = local.equivHashCode();
        }
        else {
            if (method == null || local == null || fields == null) {
                throw new IllegalArgumentException("AccessPath has null members");
            }
            hashCode = method.hashCode()
                    ^ (local.equivHashCode() << 4)
                    ^ fields.size();
        }
        return hashCode;
    }

    public SootMethod getMethod() {
        return method;
    }

    public AccessPath getBase() {
        if (!isInstanceRef())
            throw new IllegalArgumentException("The access path: " + this + " has no base");
        AccessPath base = this.clone();
        int lastIndex = base.fields.size() - 1;
        base.fields.remove(lastIndex);
        return base;
    }

    public Ref getLastField() {
        //if (!isInstanceRef())
        if (!isRef())
            return null;
        return fields.lastElement();
    }

    public Ref getFieldAt(int fieldNum) {
        if (!hasFieldAt(fieldNum))
            throw new IllegalArgumentException("Field number should be within the limits of the field array");
        return fields.elementAt(fieldNum - 1);
    }

    public Map<Integer, PointsToSet> getPointsToSet(PointsToAnalysis pta) {
        Map<Integer, PointsToSet> pts = CollectionFactory.newMap();
        //PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
        if (local instanceof ClassConstant) {
            if (!isRef())
                return pts;
        } else if ((local instanceof Local)) {
            pts.put(0, pta.reachingObjects((Local) local));
        }

        for (int i = 0; i < fields.size(); ++i) {
            Ref ref = fields.elementAt(i);
            if (ref instanceof InstanceFieldRef) {
                InstanceFieldRef fieldRef = (InstanceFieldRef) ref;
                pts.put((i + 1), pta.reachingObjects((Local) fieldRef.getBase(), fieldRef.getField()));
            } else if (ref instanceof StaticFieldRef) {
                StaticFieldRef staticFieldRef = (StaticFieldRef) ref;
                pts.put(i, pta.reachingObjects(staticFieldRef.getField()));
            }
        }
        return pts;
    }

    public AccessPath getAliasedAP() {
        AccessPath cloneAP = this.clone();
        int lastIndex = fields.lastIndexOf(new ElementRef());
        if (lastIndex == -1 || ((ElementRef)fields.get(lastIndex)).getAliasAP() == null) {
            return cloneAP;
        }
        AccessPath oldAP = this.getPartAP(lastIndex + 1);
        AccessPath newAP = ((ElementRef)fields.get(lastIndex)).getAliasAP();
        cloneAP.replace(oldAP, newAP);
        return cloneAP;
    }

    public SootClass getStaticDeclaringClass() {
        if (!isStaticRef())
            throw new IllegalArgumentException("This AP doesn't start with a static field");
        Ref ref0 = fields.get(0);
        //if (!(ref0 instanceof StarFieldRef))
            //throw new IllegalArgumentException("");
        FieldRef ref = (FieldRef) fields.get(0);
        return ref.getFieldRef().declaringClass();
    }

    public int numOfElemField() {
        int count = 0;
        for (Ref ref : fields) {
            if (ref instanceof ElementRef)
                count++;
        }
        return count;
    }

    public int numOfMapField() {
        int count = 0;
        for (Ref ref : fields) {
            if (ref instanceof ValueFieldRef || ref instanceof KeyFieldRef)
                count++;
        }
        return count;
    }

    public void setMethod(SootMethod m) {
        method = m;
    }
}
