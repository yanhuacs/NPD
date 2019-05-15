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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import pag.WholeProgPAG;
import soot.AnySubType;
import soot.Context;
import soot.G;
import soot.RefLikeType;
import soot.Type;
import soot.jimple.spark.pag.SparkField;
import soot.toolkits.scalar.Pair;

/** Represents a simple variable node (Green) in the pointer assignment graph.
 * @author Ondrej Lhotak
 */
public abstract class Var_Node extends Val_Node implements Comparable {
    public Context context() { return null; }
    /** Returns all field ref nodes having this node as their base. */
    public Collection<FieldRef_Node> getAllFieldRefs() { 
	if( fields == null ) return Collections.emptyList();
	return fields.values(); 
    }
    /** Returns the field ref node having this node as its base,
     * and field as its field; null if nonexistent. */
    public FieldRef_Node dot( SparkField field ) 
    { return fields == null ? null : fields.get( field ); }
    public int compareTo( Object o ) {
	Var_Node other = (Var_Node) o;
        if( other.finishingNumber == finishingNumber && other != this ) {
            G.v().out.println( "This is: "+this+" with id "+getNumber()+" and number "+finishingNumber );
            G.v().out.println( "Other is: "+other+" with id "+other.getNumber()+" and number "+other.finishingNumber );
            throw new RuntimeException("Comparison error" );
        }
	return other.finishingNumber - finishingNumber;
    }
    public void setFinishingNumber( int i ) {
        finishingNumber = i;
        if( i > pag.maxFinishNumber ) pag.maxFinishNumber = i;
    }
    /** Returns the underlying variable that this node represents. */
    public Object getVariable() {
        return variable;
    }

    /** Designates this node as the potential target of a interprocedural 
     * assignment edge which may be added during on-the-fly call graph 
     * updating. */
    public void setInterProcTarget() {
        interProcTarget = true;
    }
    /** Returns true if this node is the potential target of a interprocedural 
     * assignment edge which may be added during on-the-fly call graph 
     * updating. */
    public boolean isInterProcTarget() {
        return interProcTarget;
    }

    /** Designates this node as the potential source of a interprocedural 
     * assignment edge which may be added during on-the-fly call graph 
     * updating. */
    public void setInterProcSource() {
        interProcSource = true;
    }
    /** Returns true if this node is the potential source of a interprocedural 
     * assignment edge which may be added during on-the-fly call graph 
     * updating. */
    public boolean isInterProcSource() {
        return interProcSource;
    }
    /** Returns true if this VarNode represents the THIS pointer */
	public boolean isThisPtr()
	{
		if ( variable instanceof Pair ) {
			Pair o = (Pair)variable;
			return o.isThisParameter();
		}
		
		return false;
	}
	
	/* End of public methods. */

	protected Var_Node(WholeProgPAG pag, Object variable, Type t) {
		super(pag, t);
		if (!(t instanceof RefLikeType) || t instanceof AnySubType) {
			throw new RuntimeException("Attempt to create VarNode of type " + t);
		}
		this.variable = variable;
		pag.getVarNodeNumberer().add(this);
		setFinishingNumber(++pag.maxFinishNumber);
	}
    /** Registers a frn as having this node as its base. */
    public void addField( FieldRef_Node frn, SparkField field ) {
	if( fields == null ) fields = new HashMap<SparkField, FieldRef_Node>();
	fields.put( field, frn );
    }

    /* End of package methods. */

    protected Object variable;
    protected Map<SparkField, FieldRef_Node> fields;
    protected int finishingNumber = 0;
    protected boolean interProcTarget = false;
    protected boolean interProcSource = false;
    protected int numDerefs = 0;
}

