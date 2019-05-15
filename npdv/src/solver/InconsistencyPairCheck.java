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
import core.AccessPathPair;
import core.CollectionFactory;
import core.Operator;
import predicate.BinaryPredicate;
import predicate.Predicate;

import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by Xinwei Xie on 1/8/17.
 */
public class InconsistencyPairCheck extends Rule {
    private Map<AccessPathPair, Set<Operator>> pairMap = CollectionFactory.newMap();

    @Override
    public void addPredicate(Predicate pred) {
        if (!(pred instanceof BinaryPredicate)) {
            return;
        }
        BinaryPredicate bp = (BinaryPredicate) pred;
        AccessPath lhs = bp.getLhs();
        AccessPath rhs = bp.getRhs();

        //if (lhs.isElemFieldContained() || rhs.isElemFieldContained())
        //   return;
        AccessPathPair pair = new AccessPathPair(lhs, rhs);
        Set<Operator> operatorSet = pairMap.get(pair);
        if (operatorSet != null) {
            for (Operator op : operatorSet) {
                if (bp.getOp() == RuleSolver.getComplementOp(op))
                    consistent = false;
            }
        } else {
            operatorSet = CollectionFactory.newSet();
            operatorSet.add(bp.getOp());
            pairMap.put(pair, operatorSet);
        }
    }

    @Override
    public boolean solve(Set<Predicate> newPreds, Set<Predicate> rmPreds) {
        return consistent;
    }

    @Override
    public String ruleName() {
        return "InconsistencyPairCheck";
    }
}
