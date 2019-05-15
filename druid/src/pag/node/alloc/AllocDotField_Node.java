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
import pag.node.var.Var_Node;
import soot.jimple.spark.pag.SparkField;

/**
 * Represents an alloc-site-dot-field node (Yellow) in the pointer assignment
 * graph.
 * 
 * @author Ondrej Lhotak
 */
public class AllocDotField_Node extends Var_Node {
	/** Returns the base AllocNode. */
	public Alloc_Node getBase() {
		return base;
	}

	/** Returns the field of this node. */
	public SparkField getField() {
		return field;
	}

	public String toString() {
		return "AllocDotField " + getNumber() + " " + base + "." + field;
	}

	/* End of public methods. */

	public AllocDotField_Node(WholeProgPAG pag, Alloc_Node base, SparkField field) {
		super(pag, field, field.getType());
		this.base = base;
		this.field = field;
		base.addField(this, field);
	}

	/* End of package methods. */

	protected Alloc_Node base;
	protected SparkField field;
}
