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

package pta.context;

import java.util.HashMap;
import java.util.Map;

import soot.Context;
import soot.SootClass;

public class StaticInitContext implements Context, ContextElement {
    private static Map<SootClass, StaticInitContext> universe = new HashMap<SootClass, StaticInitContext>(); 
    
    private SootClass clz;
    
    private StaticInitContext(SootClass c) {
        clz = c;
    }

    public static void reset() {
        universe = new HashMap<SootClass, StaticInitContext>(); 
    }
    
    public static StaticInitContext v(SootClass clz) {
        if (!universe.containsKey(clz))
            universe.put(clz, new StaticInitContext(clz));
        
        StaticInitContext node = universe.get(clz);
        
        return node;
    }

    public String toString() {
        return "StaticInitNode " + hashCode() + " class: " + clz;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clz == null) ? 0 : clz.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        StaticInitContext other = (StaticInitContext) obj;
        if (clz == null) {
            if (other.clz != null) return false;
        } else if (!clz.equals(other.clz)) return false;
        return true;
    }

    
}
