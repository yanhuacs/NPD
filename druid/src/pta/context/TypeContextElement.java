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

import soot.Type;

/** 
 * Type based context element in the points to analysis.
 *
 */
public class TypeContextElement implements ContextElement {

    private Type type;
    
    private static Map<Type, TypeContextElement> universe = 
            new HashMap<Type, TypeContextElement>();
    
    public static TypeContextElement v(Type type) {
        if (!universe.containsKey(type))
            universe.put(type, new TypeContextElement(type));
        
        return universe.get(type);
    }
    
    private TypeContextElement(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        TypeContextElement other = (TypeContextElement) obj;
        if (type == null) {
            if (other.type != null) return false;
        } else if (!type.equals(other.type)) return false;
        return true;
    }

   public String toString() {
       return "TypeContext: " + type;
   }
}
