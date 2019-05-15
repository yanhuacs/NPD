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

package util;
import soot.jimple.spark.pag.*;
import soot.jimple.toolkits.pointer.representations.*;
import soot.jimple.toolkits.pointer.util.*;
import soot.toolkits.scalar.Pair;
import pag.WholeProgPAG;
import pag.node.GNode;
import pag.node.alloc.Alloc_Node;
import pag.node.var.FieldRef_Node;
import pag.node.var.Var_Node;
import soot.*;

public class DruidNativeHelper extends NativeHelper {
    protected WholeProgPAG pag;

    public DruidNativeHelper( WholeProgPAG pag ) {
	this.pag = pag;
    }
//    protected void assignImpl(ReferenceVariable lhs, ReferenceVariable rhs) {
//        pag.addEdge( (GNode) rhs, (GNode) lhs );
//    }
//    protected void assignObjectToImpl(ReferenceVariable lhs, AbstractObject obj) {
//	Alloc_Node objNode = pag.makeAllocNode( 
//		new Pair( "AbstractObject", obj.getType() ),
//		 obj.getType(), null );
//
//        Var_Node var;
//        if( lhs instanceof FieldRef_Node ) {
//	    var = pag.makeGlobalVarNode( objNode, objNode.getType() );
//            pag.addEdge( (GNode) lhs, var );
//        } else {
//            var = (Var_Node) lhs;
//        }
//        pag.addEdge( objNode, var );
//    }
//    protected void throwExceptionImpl(AbstractObject obj) {
//	Alloc_Node objNode = pag.makeAllocNode( 
//		new Pair( "AbstractObject", obj.getType() ),
//		 obj.getType(), null );
//        pag.addEdge( objNode, pag.GlobalNodeFactory().caseThrow() );
//    }
//    protected ReferenceVariable arrayElementOfImpl(ReferenceVariable base) {
//        Var_Node l;
//	if( base instanceof Var_Node ) {
//	    l = (Var_Node) base;
//	} else {
//	    FieldRef_Node b = (FieldRef_Node) base;
//	    l = pag.makeGlobalVarNode( b, b.getType() );
//	    pag.addEdge( b, l );
//	}
//        return pag.makeFieldRefNode( l, ArrayElement.v() );
//    }
//    protected ReferenceVariable cloneObjectImpl(ReferenceVariable source) {
//	return source;
//    }
//    protected ReferenceVariable newInstanceOfImpl(ReferenceVariable cls) {
//        return pag.GlobalNodeFactory().caseNewInstance( (Var_Node) cls );
//    }
//    protected ReferenceVariable staticFieldImpl(String className, String fieldName ) {
//	SootClass c = RefType.v( className ).getSootClass();
//	SootField f = c.getFieldByName( fieldName );
//	return pag.makeGlobalVarNode( f, f.getType() );
//    }
//    protected ReferenceVariable tempFieldImpl(String fieldsig) {
//	return pag.makeGlobalVarNode( new Pair( "tempField", fieldsig ),
//            RefType.v( "java.lang.Object" ) );
//    }
//    protected ReferenceVariable tempVariableImpl() {
//	return pag.makeGlobalVarNode( new Pair( "TempVar", new Integer( ++G.v().SparkNativeHelper_tempVar ) ),
//		RefType.v( "java.lang.Object" ) );
//    }
//    protected ReferenceVariable tempLocalVariableImpl(SootMethod method) {
//        return pag.makeLocalVarNode( new Pair( "TempVar", new Integer( ++G.v().SparkNativeHelper_tempVar ) ),
//                                     RefType.v( "java.lang.Object" ) , method);
//    }
//    

	@Override
	protected void assignImpl(ReferenceVariable lhs, ReferenceVariable rhs) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void assignObjectToImpl(ReferenceVariable lhs, AbstractObject obj) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void throwExceptionImpl(AbstractObject obj) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected ReferenceVariable arrayElementOfImpl(ReferenceVariable base) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ReferenceVariable cloneObjectImpl(ReferenceVariable source) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ReferenceVariable newInstanceOfImpl(ReferenceVariable cls) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ReferenceVariable staticFieldImpl(String className, String fieldName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ReferenceVariable tempFieldImpl(String fieldsig) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ReferenceVariable tempVariableImpl() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ReferenceVariable tempLocalVariableImpl(SootMethod method) {
		// TODO Auto-generated method stub
		return null;
	}
}
