/* Java and Android Analysis Framework
 * Copyright (C) 2017 Yifei Zhang, Tian Tan, Yue Li and Jingling Xue
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

import soot.RefType;
import pag.WholeProgPAG;
import reflection.UnknownObjectWrapper;

/**
 * A class represents the unknown object that is used in lazy heap modeling
 */

public class UnknownObject_Node extends Alloc_Node {
	private UnknownObjectWrapper wrapper;
	
	public UnknownObject_Node(WholeProgPAG pag, UnknownObjectWrapper wrapper) {
		super(pag, wrapper, RefType.v("java.lang.Object"), wrapper.getSource());
		this.wrapper = wrapper;
		//pag.getAllocNodeNumberer().add(this);
	}
	
	public UnknownObjectWrapper getUnknownObjectWrapper() {
		return wrapper;
	}
	
	public String toString() {
		return "AllocNode " + getNumber() + " " + wrapper.toString();
	}
}
