package solver;

import core.AccessPath;
import core.CollectionFactory;
import core.Operator;
import predicate.BinaryPredicate;
import predicate.Predicate;

import java.util.HashSet;
import java.util.Set;

public class StaticFieldSimplifier extends Rule {
    private Set<Predicate> dropPreds = CollectionFactory.newSet();
    @Override
    public void addPredicate(Predicate pred) {
        if (!(pred instanceof BinaryPredicate))
            return;
        BinaryPredicate bp = (BinaryPredicate) pred;
        // TODO:
    }

    private boolean isStaticFieldEqNull(AccessPath ap1, AccessPath ap2, Operator op) {
        if (ap1.isStaticRef() && ap2.isNullConstant() && op == Operator.EQ) {
            return true;
        }
        return false;
    }
    @Override
    public boolean solve(Set<Predicate> newPreds, Set<Predicate> rmPreds) {
        rmPreds.addAll(dropPreds);
        return consistent;
    }

    @Override
    public String ruleName() {
        return "StaticFieldSimplifier";
    }
}
