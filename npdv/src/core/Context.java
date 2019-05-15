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

import soot.SootMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xinwei Xie on 25/7/17.
 */
public class Context {
    public SootMethod method;
    public State state;
    public boolean recursion;
    public List<Context> contextsInRecursion = new ArrayList<Context>();

    public Context(SootMethod m, State st) {
        method = m;
        state = st;
        recursion = false;
    }

    public boolean isRecursion() {
        return recursion;
    }

    public void addSCCContext(Context ctx) {
        contextsInRecursion.add(ctx);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Context) {
            Context ctx = (Context)obj;
            if (method == ctx.method && state.equals(ctx.state))
                return true;
        }
        return false;
    }

    // FIXME
    public boolean equalsStateSubset(Context ctx) {
        return (method.equals(ctx.method) && state != State.DummyState); // && state.isSubset(ctx.state));
    }
}
