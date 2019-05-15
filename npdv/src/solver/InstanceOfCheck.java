package solver;

import core.CollectionFactory;
import predicate.InstanceOfPredicate;
import predicate.Predicate;
import soot.*;

import java.util.Set;

public class InstanceOfCheck extends Rule {

    private Set<InstanceOfPredicate> truePreds = CollectionFactory.newSet();

    @Override
    public void addPredicate(Predicate pred) {
        if (pred instanceof InstanceOfPredicate) {
            InstanceOfPredicate iop = (InstanceOfPredicate) pred;
            PointsToAnalysis pta = driver.Main.pta;
            Value lhs = iop.getLhs().getLocal();
            if (lhs != null) {
                PointsToSet pts = pta.reachingObjects((Local)lhs);
                Set<Type> possibleTypes = pts.possibleTypes();
                if (possibleTypes.contains(iop.getRhs())) {
                    if (!iop.isNegated())
                        truePreds.add(iop);
                    else
                        consistent = false;
                } else {
                    if (iop.isNegated())
                        truePreds.add(iop);
                    else
                        consistent = false;
                }
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
        return "InstanceOfCheck";
    }
}
