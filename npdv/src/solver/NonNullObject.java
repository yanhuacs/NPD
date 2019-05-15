package solver;

import core.AccessPath;
import core.Operator;
import predicate.BinaryPredicate;
import predicate.Predicate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class NonNullObject extends Rule {

    private static Set<String> knownTypeNames = new HashSet<String>(
        Arrays.asList(
            "java.lang.System",
            "Ljava/lang/System"
        )
    );

    @Override
    public void addPredicate(Predicate pred) {
        if (!(pred instanceof BinaryPredicate))
            return;
        BinaryPredicate bp = (BinaryPredicate) pred;
        if (isKnownObjectEqNull(bp.getLhs(), bp.getRhs(), bp.getOp()) ||
            isKnownObjectEqNull(bp.getRhs(), bp.getLhs(), bp.getOp())) {
            consistent = false;
        }
    }

    /*
    private boolean isKnownFieldEqNull(AccessPath ap1, AccessPath ap2, Operator op) {
        if (ap1.isInstanceRef() && ap2.isNullConstant()) {
            Ref ref = ap1.getLastField();
            if (ref instanceof ThisRef) {
                return true;
            }
        }
        return false;
    }
    */

    private boolean isKnownObjectEqNull(AccessPath ap1, AccessPath ap2, Operator op) {
        if (ap1.isStaticRef() && ap2.isNullConstant() && op == Operator.EQ) {
            if (knownTypeNames.contains(ap1.getStaticDeclaringClass().getName()))
                return true;
        }
        return false;
    }

    @Override
    public boolean solve(Set<Predicate> newPreds, Set<Predicate> rmPreds) {
        return consistent;
    }

    @Override
    public String ruleName() {
        return "KnownNullObject";
    }
}
