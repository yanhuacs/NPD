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

import java.util.HashMap;
import java.util.Map;

import pag.node.alloc.Constructor_Node;
import reflection.InferenceReflectionModel.StmtWithPos;

public class UnknownObjectWrapperFactory {
	private static UnknownObjectWrapperFactory factory;
	private Map<StmtWithPos, UnknownObjectWrapper> newInstCall2UnknownObject;
	
	private UnknownObjectWrapperFactory() {
		newInstCall2UnknownObject = new HashMap<>();
	}
	
	public static UnknownObjectWrapperFactory v() {
		if(factory == null)
			factory = new UnknownObjectWrapperFactory();
		return factory;
	}
	
	public UnknownObjectWrapper getUnknownObject(StmtWithPos newInstCall, Constructor_Node ctorNode) {
		UnknownObjectWrapper unknownObjectWrapper = newInstCall2UnknownObject.get(newInstCall);
		if(unknownObjectWrapper == null)
			newInstCall2UnknownObject.put(newInstCall, unknownObjectWrapper = new UnknownObjectWrapper(newInstCall, ctorNode));
		return unknownObjectWrapper;
	}
}
