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

package pag.node.var;
import java.util.Map;
import pag.WholeProgPAG;
import soot.SootMethod;
import soot.Type;

/** Represents a simple variable node (Green) in the pointer assignment graph
 * that is specific to a particular method invocation.
 * @author Ondrej Lhotak
 */
public class LocalVar_Node extends Var_Node {
    public SootMethod getMethod() {
        return method;
    }
    public String toString() {
	return "LocalVarNode "+getNumber()+" "+variable+" "+method;
    }
    /* End of public methods. */

    public LocalVar_Node( WholeProgPAG pag, Object variable, Type t, SootMethod m ) {
	super( pag, variable, t );
        this.method = m;
        //if( m == null ) throw new RuntimeException( "method shouldn't be null" );
    }

    /* End of package methods. */

    protected Map<Object, ContextLocalVar_Node> cvns;
    protected SootMethod method;
}

