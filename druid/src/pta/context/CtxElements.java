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

import java.util.Arrays;

import soot.Context;

public class CtxElements implements Context{
	
	private ContextElement[] array;

	public  ContextElement[] getElements(){ return array; }
	
	public  int getLength(){ return array.length; }
	
    public CtxElements(ContextElement[] array) {
		this.array=array;
	}

	@Override
    public int hashCode() {
        return Arrays.hashCode(array);
    }

    /** 
     * Return the number of non-null or non-No-Context elements, assuming that in the array
     * once we see a no-context element, we don't see a context element.
     * 
     * @return
     */
    public int numContextElements() {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == null || array[i].equals(EmptyContext.v()))
                return i;
        }

        return array.length;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (getClass() != obj.getClass()) return false;
        CtxElements other = (CtxElements) obj;
        
        //if (!Arrays.equals(array, other.array)) return false;
        
        //custom array equals for context
        //allows for checking of different sized arrays, but represent same context-sensitive heap object
        if (this.array == null || other.array == null)
            return false;
       
        if (this.numContextElements() != other.numContextElements())
            return false;
        
        for (int i = 0; i < numContextElements(); i++) {
            Object o1 = this.array[i];
            Object o2 = other.array[i];
            if (!(o1==null ? o2==null : o1.equals(o2)))
                return false;                
        }
        
        return true;
    }
}
