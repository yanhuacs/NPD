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
import pag.WholeProgPAG;
import soot.SootClass;
import soot.SootField;
import soot.Type;

/** Represents a simple variable node (Green) in the pointer assignment graph
 * that is not associated with any particular method invocation.
 * @author Ondrej Lhotak
 */
public class GlobalVar_Node extends Var_Node {
    public GlobalVar_Node( WholeProgPAG pag, Object variable, Type t ) {
	super( pag, variable, t );
    }
    public String toString() {
	return "GlobalVarNode "+getNumber()+" "+variable;
    }
	public SootClass getDeclaringClass()
	{
		if ( variable instanceof SootField )
			return ((SootField)variable).getDeclaringClass();
		
		return null;
	}
}
