package core;
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

import predicate.Predicate;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Xinwei Xie on 24/7/17.
 */
public class State implements Iterable<Predicate> {
    private Set<Predicate> preds = CollectionFactory.newSet();
    private Set<Predicate> vppreds = CollectionFactory.newSet();
    private Predicate rootPredicate = null;
    private boolean isSATChecked = false;
    private int hashCode = -1;
    private int stepCount = 0;
    public static State DummyState = new State();

    public int getStepCount() {
        return stepCount;
    }

    public void increaseStepCount() {
        stepCount++;
        for (Predicate pred : preds) {
            pred.increaseStepCount();
        }
        for (Predicate pred : vppreds) {
            pred.increaseStepCount();
        }
    }

    public void resetState(State inState) {
        stepCount = inState.stepCount;
    }

    public void setRootPredicate(Predicate pred) {
        if (rootPredicate != null) return;
        rootPredicate = pred;
    }

    public void addPredicate(Predicate pred) {
        if (!preds.contains(pred)) {
            preds.add(pred);
        /*
        } else {
            if (rootPredicate != null) {
                preds.remove(pred);
                preds.add(rootPredicate);
            }
        */
        }
        if (pred.isRootPredicate()) {
            if (rootPredicate != null)
                return; //throw new IllegalStateException("Cannot add new root when a root exists");
            else
                rootPredicate = pred;
        }
    }

    public void removePredicate(Predicate pred) {
        if (preds.contains(pred)) {
            preds.remove(pred);
            if (hasRootPredicate() && rootPredicate.equals(pred))
                rootPredicate = null;
        }
        vppreds.remove(pred);
    }

    public Set<Predicate> getPreds() { return preds; }
    public boolean hasRootPredicate() { return rootPredicate != null; }
    public Predicate getRootPredicate() { return rootPredicate; }
    @Override
    public Iterator<Predicate> iterator()
    {
        return preds.iterator();
    }

    public Set<Predicate> getAllPreds() {
        Set<Predicate> allPreds = CollectionFactory.newSet();
        allPreds.addAll(preds);
        allPreds.addAll(vppreds);
        return allPreds;
    }

    public boolean isSubset(State other) {
        Set<Predicate> thisPreds = this.getAllPreds();
        Set<Predicate> otherPreds = other.getAllPreds();
        if (otherPreds.containsAll(thisPreds))
            return true;
        return false;
    }

    public static boolean hasSubset(Set<State> stateSet, State state) {
        for (State s : stateSet) {
            if (s != State.DummyState && s.isSubset(state))
                return true;
        }
        return false;
    }

    public State clone() {
        State clone = new State();
        for (Predicate pred : preds) {
            clone.addPredicate(pred);
        }
        for (Predicate pred : vppreds) {
            clone.addPredicate(pred);
        }
        clone.stepCount = stepCount;
        clone.rootPredicate = rootPredicate;
        return clone;
    }

    public void removeAllPredicates(Set<Predicate> remove) {
        for (Predicate pred : remove) {
            removePredicate(pred);
        }
    }
    public void addAllPredicates(Set<Predicate> add) {
        for (Predicate pred : add) {
            addPredicate(pred);
        }
    }
    public void addAllPredicates(State st) {
        this.addAllPredicates(st.preds);
    }

    public void clear() {
        preds.clear();
        rootPredicate = null;
        isSATChecked = false;
    }
    public void setSATChecked() {
        isSATChecked = true;
    }

    public boolean equals(Object obj) {
        if (obj instanceof State) {
            State st = (State)obj;
            boolean ret = preds.equals(st.preds); // && vppreds.equals(st.vppreds);
            return ret;
        }
        return false;
    }

    @Override
    public String toString() {
        String str = "< ";
        for (Predicate pred : preds) {
            str += pred.toString() + " , ";
        }
        for (Predicate pred : vppreds) {
            str += pred.toString() + " , ";
        }
        str += " > (" + stepCount + ") " + NPVerifier.currentGlobalSteps;
        return str;
    }

    public int hashCode() {
        if (hashCode == -1) {
            for (Predicate pred : preds) {
                hashCode ^= pred.hashCode();
            }
            hashCode <<= 7;
            hashCode ^= preds.size();
        }
        return hashCode;
    }
}
