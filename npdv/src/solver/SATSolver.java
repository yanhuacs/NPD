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
import core.State;
import predicate.BinaryPredicate;
import predicate.Predicate;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Xinwei Xie on 1/8/17.
 */
public class SATSolver {

    public boolean isSatisfied(State state) {
        Iterator<Predicate> predIter = state.iterator();
        while(predIter.hasNext()) {
            Predicate p = predIter.next();
            if (p instanceof BinaryPredicate && p.isNegated()) {
                BinaryPredicate bp = (BinaryPredicate) p;
                bp.setOp(RuleSolver.getComplementOp(bp.getOp()));
                bp.unNegate();
            }
        }
        Rule[] rules = RuleSolver.getRules();
        Set<Predicate> newPreds = CollectionFactory.newSet();
        Set<Predicate> rmPreds = CollectionFactory.newSet();
        Set<Predicate> worklist = CollectionFactory.newSet();
        worklist.addAll(state.getAllPreds());

        State oldState = null;
        State newState = state.clone();
        while (oldState == null || !oldState.equals(newState)) {
            oldState = newState.clone();
            predIter = worklist.iterator();
            while(predIter.hasNext()) {
                Predicate pred = predIter.next();
                predIter.remove();
                for (int i = 0; i < rules.length; ++i) {
                    rules[i].addPredicate(pred);
                }
            }
            for (int i = 0; i < rules.length; ++i) {
                if (!rules[i].solve(newPreds, rmPreds)) {
                    return false;
                }
            }
            worklist.addAll(newPreds);
            newState.removeAllPredicates(rmPreds);
            newState.addAllPredicates(newPreds);
        }
        state.clear();
        state.addAllPredicates(newState);
        state.setSATChecked();
        return true;
    }

}
