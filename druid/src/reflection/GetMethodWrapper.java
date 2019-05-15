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

public class GetMethodWrapper {
	private SootClass baseClass;
	private SootMethod targetMethod;
	private Stmt caller;
	private boolean isFromGetMethod;
	
	public GetMethodWrapper(SootClass baseClass, SootMethod targetMethod, Stmt caller) {
		this.baseClass = baseClass;
		this.targetMethod = targetMethod;
		this.caller = caller;
	}
	
	public SootClass getBaseClass() {
		return baseClass;
	}
	public SootMethod getTargetMethod() {
		return targetMethod;
	}
	public Stmt getCaller() {
		return caller;
	}
	
	public boolean isFromGetMethod() {
		return isFromGetMethod;
	}
	
	public boolean isFromGetDeclaredMethod() {
		return !isFromGetMethod;
	}
	
	public void fromGetMethod() {
		isFromGetMethod = true;
	}
	
	public void fromGetDeclaredMethod() {
		isFromGetMethod = false;
	}
	
	public String toString() {
		return "Class.getMethod: Class " + baseClass.getName() + ", method " + 
				targetMethod.getSignature() + ", caller " + caller.toString();
	}
	
	public int hashCode() {
		return toString().hashCode();
	}
	
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o instanceof GetMethodWrapper) {
			GetMethodWrapper wrapper = (GetMethodWrapper) o;
			if(baseClass.equals(wrapper.baseClass) && 
					targetMethod.equals(wrapper.targetMethod) && caller.equals(wrapper.caller))
				return true;
			else
				return false;
		} else {
			return false;
		}
	} 
}
