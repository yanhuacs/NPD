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

/**
 * Created by Xinwei Xie on 9/8/17.
 */
public class AliasPredicate extends Predicate {
    private static int hashConst = "AliasPredicate".hashCode();

    private AccessPath lhs;
    private AccessPath rhs;
    boolean negated;

    public AliasPredicate(AccessPath ap1, AccessPath ap2, boolean neg) {
        lhs = ap1;
        rhs = ap2;
        negated = neg;
    }
    @Override
    public boolean isRootPredicate() {
        return false;
    }

    @Override
    public boolean containsAP(AccessPath ap) {
        return lhs.contains(ap) || rhs.contains(ap);
    }

    @Override
    public boolean isNegated() {
        return negated;
    }

    @Override
    public AccessPath[] getAllAccessPaths() {
        return new AccessPath[] { lhs, rhs };
    }

    @Override
    public Predicate cloneImpl() {
        return new AliasPredicate(lhs.clone(), rhs.clone(), negated);
    }

    @Override
    public void replace(AccessPath oldAP, AccessPath newAP) {
        lhs.replace(oldAP, newAP);
        rhs.replace(oldAP, newAP);
    }

    @Override
    public void replaceAt(int index, AccessPath oldAP, AccessPath newAP) {
        if (index == 0)
            lhs.replace(oldAP, newAP);
        else if (index == 1)
            rhs.replace(oldAP, newAP);
        else
            throw new IllegalArgumentException("Index i out of range");
    }

    @Override
    public int generateHashCode() {
        int hash = lhs.hashCode()
                ^ rhs.hashCode()
                ^ (negated ? 1 : 0)
                ^ hashConst;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AliasPredicate) {
            AliasPredicate aliasPred = (AliasPredicate) obj;
            if (lhs.equals(aliasPred.lhs) && rhs.equals(aliasPred.rhs) && negated == aliasPred.negated)
                return true;
        }
        return false;
    }

    @Override
    public String toString() {
        String str = "";
        if (isNegated())
            str += "~";
        str += "alias(" + lhs + "," + rhs + ")";
        return str;
    }

    public AccessPath getRhs() {
        return rhs;
    }

    public AccessPath getLhs() {
        return lhs;
    }
}
