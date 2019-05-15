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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pag.WholeProgPAG;
import pag.node.GNode;
import pag.node.var.ContextLocalVar_Node;
import pta.context.ContextElement;
import soot.PhaseOptions;
import soot.RefType;
import soot.SootMethod;
import soot.Type;
import soot.jimple.spark.pag.SparkField;
import soot.options.CGOptions;

/**
 * Represents an allocation site node (Blue) in the pointer assignment graph.
 * 
 * @author Ondrej Lhotak
 */
public class Alloc_Node extends GNode implements ContextElement  {
	/** Returns the new expression of this allocation site. */
	public Object getNewExpr() {
		return newExpr;
	}

	/** Returns all field ref nodes having this node as their base. */
	public Collection<AllocDotField_Node> getAllFieldRefs() {
		if (fields == null)
			return Collections.emptySet();
		return fields.values();
	}

	/**
	 * Returns the field ref node having this node as its base, and field as its
	 * field; null if nonexistent.
	 */
	public AllocDotField_Node dot(SparkField field) {
		return fields == null ? null : fields.get(field);
	}

	public String toString() {
		return "AllocNode " + getNumber() + " " + newExpr + " in method " + method;
	}

	/* End of public methods. */

	public Alloc_Node(WholeProgPAG pag, Object newExpr, Type t, SootMethod m) {
		super(pag, t);
		this.method = m;
		if (t instanceof RefType) {
			RefType rt = (RefType) t;
			if (rt.getSootClass().isAbstract()) {
				boolean usesReflectionLog = new CGOptions(PhaseOptions.v().getPhaseOptions("cg"))
						.reflection_log() != null;
				if (!usesReflectionLog) {
					throw new RuntimeException("Attempt to create allocnode with abstract type " + t);
				}
			}
		}
		this.newExpr = newExpr;
		if (newExpr instanceof ContextLocalVar_Node)
			throw new RuntimeException();
		pag.getAllocNodeNumberer().add(this);
	}

	/** Registers a AllocDotField as having this node as its base. */
	public void addField(AllocDotField_Node adf, SparkField field) {
		if (fields == null)
			fields = new HashMap<SparkField, AllocDotField_Node>();
		fields.put(field, adf);
	}

	public Set<AllocDotField_Node> getFields() {
		if (fields == null)
			return Collections.emptySet();
		return new HashSet<AllocDotField_Node>(fields.values());
	}

	/* End of package methods. */

	protected Object newExpr;
	protected Map<SparkField, AllocDotField_Node> fields;

	private SootMethod method;

	public SootMethod getMethod() {
		return method;
	}
}
