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

import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Stmt;

/**
 * Wrapper class that includes all the information of a getConstroctor call
 */
public class ConstructorWrapper {
	private SootClass baseClass;
	private boolean isFromGetCtor;
	private SootMethod source;
	private Stmt caller;
	
	public ConstructorWrapper(SootClass baseClass, SootMethod source, Stmt caller, boolean isFromGetCtor) {
		this.baseClass = baseClass;
		this.isFromGetCtor = isFromGetCtor;
		this.source = source;
		this.caller = caller;
	}

	public SootClass getBaseClass() {
		return baseClass;
	}

	public boolean isFromGetCtor() {
		return isFromGetCtor;
	}

	public SootMethod getSource() {
		return source;
	}

	public Stmt getCaller() {
		return caller;
	}
}
