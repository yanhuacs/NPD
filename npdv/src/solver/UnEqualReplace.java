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
import core.Operator;
import predicate.BinaryPredicate;

/**
 * Created by Xinwei Xie on 14/8/17.
 */
public class UnEqualReplace extends EqualReplace {
    private boolean isLhsQualified(BinaryPredicate pred) {
        return !pred.getLhs().isConstant() &&
                !pred.getRhs().isConstant() &&
                pred.getOp().equals(Operator.NE);
    }

    private boolean isRhsQualifeid(BinaryPredicate pred) {
        return ((!pred.getLhs().isConstant() && pred.getRhs().isConstant()) ||
                (pred.getLhs().isConstant() && !pred.getRhs().isConstant())) &&
                pred.getOp().equals(Operator.EQ);
    }

    private BinaryPredicate generatePredicate(BinaryPredicate op1, BinaryPredicate op2) {
        BinaryPredicate newPred = null;
        AccessPath baseAP = null, constAP = null;
        if (!op2.getLhs().isConstant()) {
            baseAP = op2.getLhs();
            constAP = op2.getRhs();
        } else {
            baseAP = op2.getRhs();
            constAP = op2.getLhs();
        }
        if (op1.getLhs().equals(baseAP)) {
            newPred = new BinaryPredicate(op1.getRhs(), constAP, Operator.NE, false, false);
        } else {
            newPred = new BinaryPredicate(op1.getLhs(), constAP, Operator.NE, false, false);
        }
        return newPred;
    }
}
