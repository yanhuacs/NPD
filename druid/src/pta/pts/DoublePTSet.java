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

package pta.pts;
import java.util.*;

import pag.WholeProgPAG;
import pag.node.GNode;
import soot.*;

/** Implementation of points-to set that holds two sets: one for new
 * elements that have not yet been propagated, and the other for elements
 * that have already been propagated.
 * @author Ondrej Lhotak
 */
public class DoublePTSet extends PTSetInternal {
	static PTSetFactory newSetFactory;
	static PTSetFactory oldSetFactory;	
    public DoublePTSet( Type type, WholeProgPAG pag ) {
        super( type );
        newSet = newSetFactory.newSet( type, pag );
        oldSet = oldSetFactory.newSet( type, pag );
        this.pag = pag;
    }
    /** Returns true if this set contains no run-time objects. */
    public boolean isEmpty() {
        return oldSet.isEmpty() && newSet.isEmpty();
    }
    /** Returns true if this set shares some objects with other. */
    public boolean hasNonEmptyIntersection( PointsToSet other ) {
        return oldSet.hasNonEmptyIntersection(other)
            || newSet.hasNonEmptyIntersection(other);
    }
    /** Set of all possible run-time types of objects in the set. */
    public Set<Type> possibleTypes() {
        Set<Type> ret = new HashSet<Type>();
        ret.addAll(oldSet.possibleTypes());
        ret.addAll(newSet.possibleTypes());
        return ret;
    }
    /** Adds contents of other into this set, returns true if this set 
     * changed. */
    public boolean addAll( PTSetInternal other,
            PTSetInternal exclude ) {
        if( exclude != null ) {
            throw new RuntimeException( "NYI" );
        }
        return newSet.addAll( other, oldSet );
    }
    /** Calls v's visit method on all nodes in this set. */
    public boolean forall( PTSetVisitor v ) {
        oldSet.forall( v );
        newSet.forall( v );
        return v.getReturnValue();
    }
    /** Adds n to this set, returns true if n was not already in this set. */
    public boolean add( GNode n ) {
        if( oldSet.contains( n ) ) return false;
        return newSet.add( n );
    }
    /** Returns set of nodes already present before last call to flushNew. */
    public PTSetInternal getOldSet() { return oldSet; }
    /** Returns set of newly-added nodes since last call to flushNew. */
    public PTSetInternal getNewSet() { return newSet; }
    /** Sets all newly-added nodes to old nodes. */
    public void flushNew() {
        oldSet.addAll( newSet, null );
        newSet = newSetFactory.newSet( type, pag );
    }
    /** Sets all nodes to newly-added nodes. */
    public void unFlushNew() {
        newSet.addAll( oldSet, null );
        oldSet = oldSetFactory.newSet( type, pag );
    }
    /** Merges other into this set. */
    public void mergeWith( PTSetInternal other ) {
        if( !( other instanceof DoublePTSet ) ) {
            throw new RuntimeException( "NYI" );
        }
        final DoublePTSet o = (DoublePTSet) other;
        if( other.type != null && !( other.type.equals( type ) ) ) {
            throw new RuntimeException( "different types "+type+" and "+other.type );
        }
        if( other.type == null && type != null ) {
            throw new RuntimeException( "different types "+type+" and "+other.type );
        }
        final PTSetInternal newNewSet = newSetFactory.newSet( type, pag );
        final PTSetInternal newOldSet = oldSetFactory.newSet( type, pag );
        oldSet.forall( new PTSetVisitor() {
        public final void visit( GNode n ) {
            if( o.oldSet.contains( n ) ) newOldSet.add( n );
        }} );
        newNewSet.addAll( this, newOldSet );
        newNewSet.addAll( o, newOldSet );
        newSet = newNewSet;
        oldSet = newOldSet;
    }
    /** Returns true iff the set contains n. */
    public boolean contains( GNode n ) {
        return oldSet.contains( n ) || newSet.contains( n );
    }
    
    private static PTSetFactory defaultP2SetFactory = new PTSetFactory() {
        public PTSetInternal newSet( Type type, WholeProgPAG pag ) {
            return new DoublePTSet( type, pag );
        }
    };

    public static PTSetFactory getFactory( PTSetFactory newFactory,
            PTSetFactory oldFactory ) {
        newSetFactory = newFactory;
        oldSetFactory = oldFactory;
        return defaultP2SetFactory;
    }

    /* End of public methods. */
    /* End of package methods. */

    private WholeProgPAG pag;
    protected PTSetInternal newSet;
    protected PTSetInternal oldSet;
}

