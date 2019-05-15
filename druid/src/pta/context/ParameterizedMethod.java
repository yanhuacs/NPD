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
import java.util.*;

import soot.Context;
import soot.MethodOrMethodContext;
import soot.SootClass;
import soot.SootMethod;

/** Represents a pair of a method and a context.
 * @author Ondrej Lhotak
 */
public final class ParameterizedMethod implements MethodOrMethodContext
{ 
    public static Map<ParameterizedMethod, ParameterizedMethod> MethodContext_map = new HashMap<ParameterizedMethod, ParameterizedMethod>();
    public static void reset() {
    	MethodContext_map = new HashMap<ParameterizedMethod, ParameterizedMethod>();
	}
    
    private SootMethod method;
    public SootMethod method() { return method; }
    public String getMtdName() { return method.getName(); }
    public SootClass getClz() { return method.getDeclaringClass(); }
    public String getClzName() { return method.getDeclaringClass().getName(); }
    private Context context;
    public Context context() { return context; }
    private ParameterizedMethod( SootMethod method, Context context ) {
        this.method = method;
        this.context = context;
    }
  
    public static ParameterizedMethod v( SootMethod method, Context context ) {
        ParameterizedMethod probe = new ParameterizedMethod( method, context );
        ParameterizedMethod ret = MethodContext_map.get( probe );
        if( ret == null ) {
        	MethodContext_map.put( probe, probe );
            return probe;
        }
        return ret;
    }
    
    public static ParameterizedMethod mtdWithNoCxt( SootMethod method) {
        return v(method,EmptyContext.v());
    }
    public String toString() {
        return "Method "+method+" in context "+context;
    }
    
    //obj modified Feb 6 2014
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((context == null) ? 0 : context.hashCode());
        result = prime * result + ((method == null) ? 0 : method.hashCode());
        return result;
    }
    
    //obj modified Feb 6 2014
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ParameterizedMethod other = (ParameterizedMethod) obj;
        if (context == null) {
            if (other.context != null) return false;
        } else if (!context.equals(other.context)) return false;
        if (method == null) {
            if (other.method != null) return false;
        } else if (!method.equals(other.method)) return false;
        return true;
    }
	
    
    
}
