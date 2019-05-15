package solver;

import core.AccessPath;
import core.ElementRef;
import core.KeyFieldRef;
import predicate.KeyNotExistPredicate;
import predicate.Predicate;

import java.util.Set;

public class KeyNotExistValidator extends Rule {
    @Override
    public void addPredicate(Predicate pred) {
       if (!(pred instanceof KeyNotExistPredicate))
           return;
       if (!consistent)
           return;
       KeyNotExistPredicate keyPred = (KeyNotExistPredicate) pred;
       AccessPath map = keyPred.getMap();
       AccessPath key = keyPred.getKey();
       if (key.equals(getMapKeyElem(map)))
           consistent = false;
    }

    @Override
    public boolean solve(Set<Predicate> newPreds, Set<Predicate> rmPreds) {
        return consistent;
    }

    @Override
    public String ruleName() {
        return "KeyNotExistValidator";
    }

    private AccessPath getMapKeyElem(AccessPath map) {
        KeyFieldRef keyFieldRef = new KeyFieldRef();
        ElementRef elementRef = new ElementRef();
        AccessPath mapAPKeyElem = new AccessPath(map.getLocal(), elementRef, map.getMethod());
        AccessPath mapKeyAP = new AccessPath(map.getLocal(), keyFieldRef, map.getMethod());
        mapAPKeyElem.replace(map, mapKeyAP);
        return mapAPKeyElem;
    }
}
