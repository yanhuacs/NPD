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
import reflection.ConstructorWrapper;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;

public class Constructor_Node extends Alloc_Node {
	private ConstructorWrapper ctorWrapper;	
	public Constructor_Node(WholeProgPAG pag, ConstructorWrapper ctorWrapper) {
		super(pag, ctorWrapper, RefType.v("java.lang.reflect.Constructor"), ctorWrapper.getSource());
		this.ctorWrapper = ctorWrapper;
		//pag.getAllocNodeNumberer().add(this);
	}
	
	public SootClass getBaseClass() {
		return ctorWrapper.getBaseClass();
	}
	
	public SootMethod getSource() {
		return ctorWrapper.getSource();
	}
	
	public boolean isFromGetCtor() {
		return ctorWrapper.isFromGetCtor();
	}
	
	public boolean isFromGetDeclaredCtor() {
		return ! ctorWrapper.isFromGetCtor();
	}
	
	public String toString() {
		return "Constructor meta-object " + getNumber() + " of class " + ctorWrapper.getBaseClass().getName() + ", created by " + 
				(isFromGetCtor() ? "Class.getConstructor call." : "Class.getDeclaredConstructor call");		
	}
}
