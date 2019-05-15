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
import pag.node.GNode;
import soot.jimple.spark.pag.SparkField;

/** Represents a field reference node (Red) in the pointer assignment graph.
 * @author Ondrej Lhotak
 */
public class FieldRef_Node extends Val_Node {
    /** Returns the base of this field reference. */
    public Var_Node getBase() { return base; }
    public GNode getReplacement() {
        if( replacement == this ) {
            if( base.getReplacement() == base ) return this;
            GNode baseRep = base.getReplacement();
            FieldRef_Node newRep = pag.makeFieldRefNode( (Var_Node) baseRep, field );
            newRep.mergeWith( this );
            return replacement = newRep.getReplacement();
        } else {
            return replacement = replacement.getReplacement();
        }
    }
    /** Returns the field of this field reference. */
    public SparkField getField() { return field; }
    public String toString() {
	return "FieldRefNode "+getNumber()+" "+base+"."+field;
    }

    /* End of public methods. */

    public FieldRef_Node( WholeProgPAG pag, Var_Node base, SparkField field ) {
	super( pag, null );
	if( field == null ) throw new RuntimeException( "null field" );
	this.base = base;
	this.field = field;
	base.addField( this, field );
        pag.getFieldRefNodeNumberer().add( this );
    }

    /* End of package methods. */

    protected Var_Node base;
    protected SparkField field;
}

