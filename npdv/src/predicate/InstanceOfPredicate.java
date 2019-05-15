package predicate;

import core.AccessPath;
import soot.Type;

public class InstanceOfPredicate extends Predicate{
    private static int hashConst = "InstanceOfPredicate".hashCode();
    boolean negated;
    AccessPath lhs;
    Type rhs;

    public InstanceOfPredicate(AccessPath lhs, Type rhs, boolean negated) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.negated = negated;
    }

    public AccessPath getLhs() {
        return lhs;
    }

    public Type getRhs() {
        return rhs;
    }

    @Override
    public boolean isRootPredicate() {
        return false;
    }

    @Override
    public boolean containsAP(AccessPath ap) {
        return false;
    }

    @Override
    public boolean isNegated() {
        return negated;
    }

    @Override
    public AccessPath[] getAllAccessPaths() {
        return new AccessPath[0];
    }

    @Override
    public Predicate cloneImpl() {
        return null;
    }

    @Override
    public void replace(AccessPath oldAP, AccessPath newAP) {

    }

    @Override
    public void replaceAt(int index, AccessPath oldAP, AccessPath newAP) {

    }

    @Override
    public int generateHashCode() {
        int hash = lhs.hashCode()
                ^ rhs.hashCode()
                ^ (negated ? 1 : 0)
                ^ hashConst;
        return hash;
    }

    public String toString() {
        String str = "( " + lhs.toString() + " ";
        str += " iof ";
        str += " " + rhs.toString() + " )";
        if (negated) str += "~";
        return str;
    }
}
