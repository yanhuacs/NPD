package predicate;
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

import core.AccessPath;
import soot.SootMethod;

/**
 * Created by Xinwei Xie on 24/7/17.
 */
public abstract class Predicate {
    private int hashCode = -1;
    private int stepCount = 0;
    public abstract boolean isRootPredicate();
    public abstract boolean containsAP(AccessPath ap);
    public abstract boolean isNegated();
    public abstract AccessPath[] getAllAccessPaths();
    public abstract Predicate cloneImpl();
    public abstract void replace(AccessPath oldAP, AccessPath newAP);
    public abstract void replaceAt(int index, AccessPath oldAP, AccessPath newAP);
    public abstract int generateHashCode();

    public void increaseStepCount() {
        stepCount++;
    }

    public int getStepCount() {
        return stepCount;
    }

    public Predicate clone() {
        Predicate clone = cloneImpl();
        return clone;
    }

    public boolean hasNoLocalAP(SootMethod method) {
        AccessPath[] aps = getAllAccessPaths();
        for (int i = 0; i < aps.length; ++i) {
            if (!aps[i].isConstant() && !(aps[i].getMethod() == method))
                return true;
            if (aps[i].isElemFieldContained())
                return true;
        }
        return false;
    }
    public int hashCode() {
        if (hashCode == -1)
            hashCode = generateHashCode();
        return hashCode;
    }
}
