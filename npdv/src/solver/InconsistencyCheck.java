package solver;
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
import core.CPA;
import core.Operator;
import predicate.BinaryPredicate;
import predicate.Predicate;

import java.util.Set;

/**
 * Created by Xinwei Xie on 1/8/17.
 */

public class InconsistencyCheck extends Rule {
    @Override
    public void addPredicate(Predicate pred) {
        if (!(pred instanceof BinaryPredicate)) {
            return;
        }
        BinaryPredicate bp = (BinaryPredicate) pred;
        AccessPath lhs = bp.getLhs();
        AccessPath rhs = bp.getRhs();
        if (lhs.equals(rhs) && !bp.getOp().equals(Operator.EQ))
            consistent = false;
        // check 'this' reference equals to null
        if (lhs.isThisRef() || rhs.isThisRef()) {
            if (lhs.isNullConstant() || rhs.isNullConstant()) {
                if (bp.getOp().equals(Operator.EQ))
                    consistent = false;
            }
        }

        if (lhs.isNullConstant() && rhs.isNullConstant()) {
            if (!bp.getOp().equals(Operator.EQ))
                consistent = false;
        }
        // check (symb-obj eq null)
        if (isSymObjEqNull(lhs, rhs, bp.getOp())
            || isSymObjEqNull(rhs, lhs, bp.getOp()))
            consistent = false;

        // check (string-constant eq null)
        if (isStringConstEqNull(lhs, rhs, bp.getOp()) ||
            isStringConstEqNull(rhs, lhs, bp.getOp()))
            consistent = false;

        // check (null.f...g eq null)
        if (isAPWithNullBaseEqNull(lhs, rhs, bp.getOp()) ||
            isAPWithNullBaseEqNull(rhs, lhs, bp.getOp()))
            consistent = false;

        // check (sym-obj.f...g eq null)
        if (rhs.isNullConstant()) {
            if (lhs.isInstanceRef() && CPA.isSymObject(lhs.getLocal()) && lhs.length() > 2)
                consistent = false;
        } else if (lhs.isNullConstant()) {
            if (rhs.isInstanceRef() && CPA.isSymObject(rhs.getLocal()) && rhs.length() > 2)
                consistent = false;
        }
    }

    private boolean isSymObjEqNull(AccessPath ap1, AccessPath ap2, Operator op) {
        boolean ret = (op == Operator.EQ)
                    && (!ap1.isInstanceRef() && CPA.isSymObject(ap1.getLocal()))
                    && (ap2.isNullConstant());
        return ret;
    }

    private boolean isStringConstEqNull(AccessPath ap1, AccessPath ap2, Operator op) {
        return ap1.isStringConstant() && ap2.isNullConstant() && (op == Operator.EQ);
    }

    private boolean isAPWithNullBaseEqNull(AccessPath lhs, AccessPath rhs, Operator op) {
        /*
        if (lhs.isInstanceRef() && rhs.isNullConstant()) {
            AccessPath baseAP = lhs.getPartAP(0);
            if (baseAP.isNullConstant())
                return true;
        }
        */
        if (lhs.isNullConstant() && rhs.isNullConstant() && lhs.hasField())
            return true;
        return false;
    }

    @Override
    public boolean solve(Set<Predicate> newPreds, Set<Predicate> rmPreds) {
        return consistent;
    }

    @Override
    public String ruleName() {
        return "Inconsistency check";
    }
}
