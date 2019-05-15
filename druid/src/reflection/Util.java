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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.FastHierarchy;
import soot.RefType;
import soot.Scene;
import soot.SootClass;

public class Util {
	private static Util util;
	private Map<SootClass, Set<SootClass>> subClassOf;
	private FastHierarchy fastHierarchy;

	private Util() {
		subClassOf = new HashMap<>();
		fastHierarchy = Scene.v().getFastHierarchy();
	}

	public static Util v() {
		if(util == null)
			util = new Util();
		return util;
	}

	// get the all the possible subclasses of given class
	public Set<SootClass> getSubClasses(SootClass clazz) {
		Set<SootClass> subClasses = subClassOf.get(clazz);
		if(subClasses == null) {
			// if clazz is a interface, find all the implementers of clazz
			// then find subclasses of implementers
			// finally find all the subinterfaceshand
			subClasses = new HashSet<>();
			// subtyping is reflexive, add the class itself
			subClasses.add(clazz);
			if(clazz.isInterface()) {
				Set<SootClass> implementers = fastHierarchy.getAllImplementersOfInterface(clazz);
				subClasses.addAll(implementers);
				for(SootClass implementer : implementers)
					subClasses.addAll(getSubClasses(implementer));
				subClasses.addAll(fastHierarchy.getAllSubinterfaces(clazz));
			} else {
				// get directly subclasses and then recursively find all their subclasses
				Collection<SootClass> directSubClasses = fastHierarchy.getSubclassesOf(clazz);
				subClasses.addAll(directSubClasses);
				for(SootClass c : directSubClasses)
					subClasses.addAll(getSubClasses(c));
				subClassOf.put(clazz, subClasses);
			}
		}	
		return subClasses;
	}
	
	public Set<RefType> getSubTypes(RefType refType) {
		Set<SootClass> subClasses = getSubClasses(refType.getSootClass());
		Set<RefType> refTypes = new HashSet<>();
		for(SootClass clz : subClasses)
			refTypes.add(clz.getType());
		return refTypes;
	}
	
	// Output information
	public static void println(String s) {
		if(ReflectionOptions.v().debug())
			System.out.println(s);
	}
}
