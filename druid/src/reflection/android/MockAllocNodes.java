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
package reflection.android;

import java.util.HashMap;
import java.util.Map;

import pag.node.alloc.Alloc_Node;
import soot.RefType;

public class MockAllocNodes {
	private static MockAllocNodes mockAllocNodes;
	private Map<RefType, Alloc_Node> typeToAllocNode;
	
	private MockAllocNodes() {
		typeToAllocNode = new HashMap<>();
	}
	
	public static MockAllocNodes v() {
		if(mockAllocNodes == null)
			mockAllocNodes = new MockAllocNodes();
		return mockAllocNodes;
	}
	
	public void put(RefType type, Alloc_Node allocNode) {
		typeToAllocNode.put(type, allocNode);
	}
	
	public Alloc_Node get(RefType type) {
		return typeToAllocNode.get(type);
	}
}
