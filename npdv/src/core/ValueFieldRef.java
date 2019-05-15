package core;

import soot.Type;
import soot.UnitPrinter;
import soot.ValueBox;
import soot.jimple.Ref;
import soot.util.Switch;

import java.util.List;

public class ValueFieldRef implements Ref {
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
        if (o instanceof ValueFieldRef)
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

    public String toString() {
        return "$value";
    }

    public boolean equals(Object obj) {
        if (obj instanceof ValueFieldRef)
            return true;
        return false;
    }
}
