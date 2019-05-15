package core;

import soot.Type;
import soot.UnitPrinter;
import soot.ValueBox;
import soot.jimple.Ref;
import soot.util.Switch;

import java.util.List;

public class ElementRef implements Ref {
    private AccessPath ap = null;
    @Override
    public List<ValueBox> getUseBoxes() {
        return null;
    }

    @Override
    public Type getType() {
        return null;
    }

    @Override
    public Object clone() {
        return null;
    }

    @Override
    public void toString(UnitPrinter unitPrinter) {

    }

    @Override
    public boolean equivTo(Object o) {
        if (o instanceof ElementRef)
            return true;
        return false;
    }

    @Override
    public int equivHashCode() {
        return 0;
    }

    @Override
    public void apply(Switch aSwitch) {

    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ElementRef)
            return true;

        return false;
    }

    public void setAliasAP(AccessPath aliasAP) {
        ap = aliasAP;
    }

    public AccessPath getAliasAP() {
        return ap;
    }

    public String toString() {
        return "$elem";
    }

    public int hashCode() {
        return toString().hashCode();
    }
}
