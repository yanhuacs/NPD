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

import core.CollectionFactory;
import predicate.BinaryPredicate;
import predicate.Predicate;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Xinwei Xie on 1/8/17.
 */

/**
 * This class converts the predicate <x eq false> to <x ne true>.
 * This is stateless and will not invalidate any state.
 * This will remove some preds and add new preds (i.e replace).
 */
public class BooleanSimplifier extends Rule {

    private Set<BinaryPredicate> oldPreds = CollectionFactory.newSet();
    private Set<BinaryPredicate> newPreds = CollectionFactory.newSet();
    @Override
    public void addPredicate(Predicate pred) {
        if (!(pred instanceof BinaryPredicate)) {
            return;
        }
        BinaryPredicate bpred = ((BinaryPredicate) pred);
        //if (!(bpred.getLhs().isConstant() && bpred.getRhs().isConstant()) && RuleSolver.hasBoolean(bpred) && !RuleSolver.hasBooleanTrue(bpred)) {

        //}
    }

    @Override
    public boolean solve(Set<Predicate> newPreds, Set<Predicate> rmPreds) {
        return consistent;
    }

    @Override
    public String ruleName() {
        return "BooleanSimplifer";
    }
}
