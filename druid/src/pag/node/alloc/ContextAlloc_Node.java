/* Java and Android Analysis Framework
 * Copyright (C) 2017 Jingbo Lu, Yulei Sui
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

package pag.node.alloc;

import pag.WholeProgPAG;
import pta.context.ContextElement;
import pta.context.CtxElements;
import soot.Context;

/**
 * This class represents context in an object sensitive PTA.
 * 
 * It is a list of new expressions
 *
 */
public class ContextAlloc_Node extends Alloc_Node implements Context {

    /** Array for context of allocation (new exprs) */
    private CtxElements contextAllocs;
    public CtxElements getContext() {return contextAllocs;}

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("ContextAllocNode ");
        //DA: add number
        buf.append(getNumber());
        ContextElement[] arr = contextAllocs.getElements();
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != null)
                buf.append(String.format("[%s (%s)]", arr[i], 
                    (arr[i] == null ? 0 : arr[i].hashCode())));
        }

        return buf.toString();
    }

    public ContextAlloc_Node( WholeProgPAG pag, Alloc_Node base, Context contextAllocs ) {
        super( pag, base.getNewExpr(), base.getType(), base.getMethod());
        this.contextAllocs = (CtxElements)contextAllocs;
    }

    public boolean noContext() {
        return contextAllocs.numContextElements() == 1;
    }
}
