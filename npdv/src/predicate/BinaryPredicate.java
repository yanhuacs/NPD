package predicate;
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


import core.AccessPath;
import core.Operator;
import soot.SootMethod;
import soot.jimple.NullConstant;

/**
 * Created by Xinwei Xie on 24/7/17.
 */
public class BinaryPredicate extends Predicate {
    private static int hashConst = "BinaryPredicate".hashCode();
    private boolean isRoot;
    private boolean negated;
    private AccessPath lhs;
    private AccessPath rhs;

    //public static enum Operator {EQ, NE, LT, GE, GT, LE};
    Operator op;

    public Operator getOp() {
        return op;
    }

    public void setOp(Operator o) {
        op = o;
    }

    public BinaryPredicate(AccessPath l, AccessPath r, Operator o, boolean neg, boolean root) {
        lhs = l;
        rhs = r;
        op = o;
        negated = neg;
        isRoot = root;
    }

    @Override
    public boolean isRootPredicate() {
        return isRoot;
    }

    @Override
    public boolean containsAP(AccessPath ap) {
        if (lhs.contains(ap) || rhs.contains(ap))
            return true;
        return false;
    }

    @Override
    public boolean isNegated() {
        return negated;
    }

    public void unNegate() { negated = false; }

    @Override
    public AccessPath[] getAllAccessPaths() {
        return new AccessPath[] {lhs, rhs};
    }

    @Override
    public Predicate cloneImpl() {
        BinaryPredicate clone = new BinaryPredicate(lhs.clone(), rhs.clone(), op, negated, isRoot);
        return clone;
    }

    @Override
    public void replace(AccessPath oldAP, AccessPath newAP) {
        lhs.replace(oldAP, newAP);
        rhs.replace(oldAP, newAP);
    }

    @Override
    public void replaceAt(int index, AccessPath oldAP, AccessPath newAP) {
        if (index == 0)
            lhs.replace(oldAP, newAP);
        else if (index == 1)
            rhs.replace(oldAP, newAP);
        else
            throw new IllegalArgumentException("Index i out of range");
    }

    @Override
    public int generateHashCode() {
        int hash = lhs.hashCode()
                ^ rhs.hashCode()
                ^ (negated ? 1 : 0)
                ^ hashConst;
        return hash;
    }

    public String toString() {
        String str = "( " + lhs.toString() + " ";
        if (op == Operator.EQ)
            str += "==";
        else
            str += "!=";
        str += " " + rhs.toString() + " )";
        if (negated) str += "~";
        if (isRoot) str += "*";
        return str;
    }

    public static BinaryPredicate createNonNullPred(AccessPath lhs, SootMethod method) {
        AccessPath rhs = new AccessPath(NullConstant.v(), method);
        BinaryPredicate pred = new BinaryPredicate(lhs, rhs, Operator.NE, false, false);
        return pred;
    }

    public AccessPath getLhs() {
        return lhs;
    }

    public AccessPath getRhs() {
        return rhs;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BinaryPredicate) {
            BinaryPredicate bp = (BinaryPredicate)obj;
            if (lhs.equals(bp.lhs) && rhs.equals(bp.rhs) && op.equals(bp.op) && (negated == bp.negated))
                return true;
        }
        return false;

    }
}
