package solver;

import core.CollectionFactory;
import core.NPVerifier;
import predicate.AliasPredicate;
import predicate.Predicate;

import java.util.HashSet;
import java.util.Set;

public class PredicateDropper extends Rule {
    public static int maxStateSize = 3;
    private Set<Predicate> dropPreds = CollectionFactory.newSet();
    private Set<Predicate> retainPreds = CollectionFactory.newSet();
    private Predicate oldestPred = null;

    @Override
    public void addPredicate(Predicate pred) {
        if (pred.getStepCount() > NPVerifier.maxSteps && !pred.isRootPredicate()) {
            dropPreds.add(pred);
        }

        if (pred instanceof AliasPredicate)
            return;
        if (retainPreds.size() < maxStateSize) {
            retainPreds.add(pred);
            if (!pred.isRootPredicate() &&
                    (oldestPred == null || oldestPred.getStepCount() < pred.getStepCount())) {
                oldestPred = pred;
            }
        } else if (oldestPred != null){
            if (pred.isRootPredicate() || pred.getStepCount() < oldestPred.getStepCount()) {
                retainPreds.remove(oldestPred);
                dropPreds.add(oldestPred);
                retainPreds.add(pred);
                oldestPred = null;
                int max = 0;
                for (Predicate curPred : retainPreds) {
                    if (!curPred.isRootPredicate() && curPred.getStepCount() >= max) {
                        oldestPred = curPred;
                        max = curPred.getStepCount();
                    }
                }
            }
        }
    }

    @Override
    public boolean solve(Set<Predicate> newPreds, Set<Predicate> rmPreds) {
        rmPreds.addAll(dropPreds);
        return true;
    }

    @Override
    public String ruleName() {
        return "PredicateDropper";
    }
}
