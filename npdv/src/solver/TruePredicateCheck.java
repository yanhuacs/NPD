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
import core.CollectionFactory;
import core.Operator;
import predicate.BinaryPredicate;
import predicate.Predicate;
import soot.JastAddJ.Access;
import soot.JastAddJ.Opt;
import soot.jimple.ClassConstant;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Xinwei Xie on 2/8/17.
 */

public class TruePredicateCheck extends Rule {
    Set<BinaryPredicate> result = CollectionFactory.newSet();

    private static Set<String> knownTypeNames = new HashSet<String>(
            Arrays.asList(
                    "java.lang.System",
                    "Ljava/lang/System"
            )
    );

    @Override
    public void addPredicate(Predicate pred) {
        if (!(pred instanceof BinaryPredicate)) {
            return;
        }
        BinaryPredicate bp = (BinaryPredicate) pred;
        AccessPath lhs = bp.getLhs();
        AccessPath rhs = bp.getRhs();
        // TODO: check ('this' != null)
        if (lhs.isThisRef() || rhs.isThisRef()) {
            if (lhs.isNullConstant() || rhs.isNullConstant()) {
                if (bp.getOp() == Operator.NE)
                    result.add(bp);
            }
        }

        if (lhs.equals(rhs) && bp.getOp().equals(Operator.EQ))
            result.add(bp);
        if (lhs.isNullConstant() && rhs.isNullConstant() && bp.getOp().equals(Operator.EQ)) {
            result.add(bp);
        }
        if (isSymObjNeNull(lhs, rhs, bp.getOp())
            ||isSymObjNeNull(rhs, lhs, bp.getOp())) {
            result.add(bp);
        }
        if (isStringConstantNeNull(lhs, rhs, bp.getOp())
            ||isStringConstantNeNull(rhs, lhs, bp.getOp())) {
            result.add(bp);
        }
        if (isStaticBaseNeNull(lhs, rhs, bp.getOp())
            ||isStaticBaseNeNull(rhs, lhs, bp.getOp())) {
            result.add(bp);
        }

        if (isSystemNeNull(lhs, rhs, bp.getOp())
            ||isSystemNeNull(rhs, lhs, bp.getOp())) {
            result.add(bp);
        }
    }

    private boolean isSystemNeNull(AccessPath ap1, AccessPath ap2, Operator op) {
        boolean ret = (op == Operator.NE) &&
                        ap1.isStaticRef() && ap2.isNullConstant() &&
                        knownTypeNames.contains(ap1.getStaticDeclaringClass().getName());
        return ret;
    }

    private boolean isStaticBaseNeNull(AccessPath ap1, AccessPath ap2, Operator op) {
        boolean ret = (op == Operator.NE) &&
                        !ap1.isRef() && (ap1.getLocal() instanceof ClassConstant) &&
                        ap2.isNullConstant();
        return ret;
    }

    private boolean isSymObjNeNull(AccessPath ap1, AccessPath ap2, Operator op) {
        boolean ret = (op == Operator.NE)
                && (!ap1.isInstanceRef() && CPA.isSymObject(ap1.getLocal()))
                && (ap2.isNullConstant());
        return ret;
    }

    private boolean isStringConstantNeNull(AccessPath ap1, AccessPath ap2, Operator op) {
        return ap1.isStringConstant() && ap2.isNullConstant() && op == Operator.NE;
    }

    @Override
    public boolean solve(Set<Predicate> newPreds, Set<Predicate> rmPreds) {
        rmPreds.addAll(result);
        return true;
    }

    @Override
    public String ruleName() {
        return "True predicate check";
    }
}
