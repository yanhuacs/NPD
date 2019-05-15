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
import core.CPA;
import core.CollectionFactory;
import predicate.AliasPredicate;
import predicate.Predicate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Xinwei Xie on 14/8/17.
 */
public class AliasPredicateValidator extends Rule {

    private Set<AliasPredicate> truePreds = CollectionFactory.newSet();
    Map<AccessPathPair, Boolean> negMap = CollectionFactory.newMap();

    @Override
    public void addPredicate(Predicate pred) {
        if (!(pred instanceof AliasPredicate)) {
            return;
        }
        if (!consistent)
            return;

        AliasPredicate aliasPred = (AliasPredicate) pred;
        AccessPath lhs = aliasPred.getLhs();
        AccessPath rhs = aliasPred.getRhs();

        if (lhs.isConstant() || rhs.isConstant()) {
            if (aliasPred.isNegated())
                truePreds.add(aliasPred);
            else
                consistent = false;
        }

        //if (!lhs.isCompressed() && !rhs.isCompressed()) {
            if (lhs.equals(rhs)) {
                if (aliasPred.isNegated())
                    consistent = false;
                else
                    truePreds.add(aliasPred);
            }
            AccessPathPair pair = new AccessPathPair(lhs, rhs);
            if (negMap.keySet().contains(pair)) {
                if (aliasPred.isNegated() != negMap.get(pair))
                    consistent = false;
            }
            else
                negMap.put(pair, aliasPred.isNegated());
        //}

        //symbolic objects correspond to allocation sites. Hence, if two symbolic objects are not equal then their corresponding
        //runtime objects are not equal for sure. (the converse does not hold.)
        if (!lhs.isInstanceRef() && CPA.isSymObject(lhs.getLocal())) {
            if (!rhs.equals(lhs)) {
                if (aliasPred.isNegated())
                    truePreds.add(aliasPred);
                else
                    consistent = false;
            }
        }
        if (!rhs.isInstanceRef() && CPA.isSymObject(rhs.getLocal())) {
            if (!lhs.equals(rhs)) {
                if (aliasPred.isNegated())
                    truePreds.add(aliasPred);
                else
                    consistent = false;
            }
        }
    }

    @Override
    public boolean solve(Set<Predicate> newPreds, Set<Predicate> rmPreds) {
        rmPreds.addAll(truePreds);
        return consistent;
    }

    @Override
    public String ruleName() {
        return "AliasPredicateValidator";
    }
}
