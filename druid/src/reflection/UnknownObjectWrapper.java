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
package reflection;

import soot.SootMethod;
import soot.jimple.Stmt;
import pag.node.alloc.Constructor_Node;
import reflection.InferenceReflectionModel.StmtWithPos;

/**
 * A class represents the allocation site of the unknown object
 */

public class UnknownObjectWrapper {
	private StmtWithPos newInstCall;
	private Constructor_Node constructorNode;
	
	UnknownObjectWrapper(StmtWithPos newInstCall, Constructor_Node ctorNode) {
		this.newInstCall = newInstCall;
		this.constructorNode = ctorNode;
	}
	
	public StmtWithPos getNewInstCall() {
		return newInstCall;
	}
	
	public Stmt getCaller() {
		return newInstCall.getCaller();
	}
	
	public SootMethod getSource() {
		return  newInstCall.getSource();
	}
	
	public Constructor_Node getConstructorNode() {
		return constructorNode;
	}
	
	public String toString() {
		return "Unknown Object created by " + newInstCall.getCaller() + " in method " + newInstCall.getSource().getSignature();
	}
}
