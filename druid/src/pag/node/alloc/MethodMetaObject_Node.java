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

import pag.WholeProgPAG;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;

public class MethodMetaObject_Node extends Alloc_Node {
	
	public String toString() {
		return "Method meta-object " + getNumber() + " " + targetMtd.getSignature() + ", created by " + 
				(isFromGetMethod ? "Class.getMethod call." : "Class.getDeclaredMethod call.");
	}
	
	public SootClass getBaseClass() {
		return baseClass;
	}
	
	public SootMethod getTargetMethod() {
		return targetMtd;
	}
	
	public boolean isFromGetMethod() {
		return isFromGetMethod;
	}
	
	public boolean isFromGetDeclaredMethod() {
		return !isFromGetMethod;
	}
	
	public MethodMetaObject_Node(WholeProgPAG pag, SootClass baseClass, SootMethod targetMtd, SootMethod source, boolean isFromGetMethod) {
		super(pag, targetMtd, RefType.v("java.lang.reflect.Method"), source);
		this.baseClass = baseClass;
		this.targetMtd = targetMtd;
		this.isFromGetMethod = isFromGetMethod;
		//pag.getAllocNodeNumberer().add(this);
	}
	
	private SootClass baseClass;
	private SootMethod targetMtd;
	private boolean isFromGetMethod;
}
