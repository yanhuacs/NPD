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

import core.*;
import predicate.BinaryPredicate;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Xinwei Xie on 1/8/17.
 */
public class RuleSolver {
    private static Rule[] latestRules;
    private static Map<Operator, Operator> complementMap = null; //new HashMap<Operator, Operator>();
    private static Map<Operator, Operator> reverseMap = null; //new HashMap<Operator, Operator>();

    public static Rule[] getRules() {
        Rule[] rules = {
            new EqualReplace(),
            new UnEqualReplace(),
            new InfeasiblePathInvalidator(),
            new InconsistencyCheck(),
            new InconsistencyPairCheck(),
            new TruePredicateCheck(),
            new NonNullObject(),
            new KeyNotExistValidator(),
            new AliasPredicateValidator(),
            new CompressedAPSimplifier(),
            new PredicateDropper(),
            new InstanceOfCheck()
        };
        latestRules = rules;
        return rules;
    }

    public static Operator getComplementOp(Operator op) {
        if (complementMap == null) {
            complementMap = CollectionFactory.newMap();
            complementMap.put(Operator.EQ, Operator.NE);
            complementMap.put(Operator.GE, Operator.LT);
            complementMap.put(Operator.GT, Operator.LE);
            complementMap.put(Operator.LE, Operator.GT);
            complementMap.put(Operator.LT, Operator.GE);
            complementMap.put(Operator.NE, Operator.EQ);
        }
        Operator complement = complementMap.get(op);
        assert(complement != null);
        return complement;
    }

    public static Operator getReverseOp(Operator op) {
        if (reverseMap == null) {
            reverseMap = CollectionFactory.newMap();
            reverseMap.put(Operator.EQ, Operator.EQ);
            reverseMap.put(Operator.GE, Operator.LE);
            reverseMap.put(Operator.GT, Operator.LT);
            reverseMap.put(Operator.LE, Operator.GE);
            reverseMap.put(Operator.LT, Operator.GT);
            reverseMap.put(Operator.NE, Operator.NE);
        }
        Operator reverse = reverseMap.get(op);
        assert(reverse != null);
        return reverse;
    }

    public static boolean hasBoolean(BinaryPredicate pred) {
        return isBoolean(pred.getLhs()) || isBoolean(pred.getRhs());
    }
    public static boolean isBoolean(AccessPath ap) {
        return ap.isConstant();
    }
}
