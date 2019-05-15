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

import pag.node.var.Var_Node;
import pta.context.ParameterizedMethod;
import pta.pts.PTSetInternal;
import soot.Scene;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.util.NumberedString;
/**
 * DA: Adapt for new CallGraphBuilder
 * @author Jingbo Lu
 *
 */
public abstract class ReflectionModel {
	protected final NumberedString sigInit = Scene.v().getSubSigNumberer().
            findOrAdd( "void <init>()" );
	protected final NumberedString sigForName = Scene.v().getSubSigNumberer().
            findOrAdd( "java.lang.Class forName(java.lang.String)" );

	abstract void methodInvoke(ParameterizedMethod container, Stmt invokeStmt);

	abstract void classNewInstance(ParameterizedMethod source, Stmt s);

	abstract void contructorNewInstance(ParameterizedMethod source, Stmt s);

	abstract void classForName(ParameterizedMethod source, Stmt s);

	public void handleInvokeExpr(InvokeExpr ie, ParameterizedMethod source, Stmt s){
		final String methRefSig = ie.getMethodRef().getSignature();
		if( methRefSig.equals( "<java.lang.reflect.Method: java.lang.Object invoke(java.lang.Object,java.lang.Object[])>" ) )
			methodInvoke(source,s);
		else if( methRefSig.equals( "<java.lang.Class: java.lang.Object newInstance()>" ) )
			classNewInstance(source,s);
		else if( methRefSig.equals( "<java.lang.reflect.Constructor: java.lang.Object newInstance(java.lang.Object[])>" ) )
			contructorNewInstance(source, s);
		else if( ie.getMethodRef().getSubSignature() == sigForName )
			classForName(source,s);
	}
	public void updateNode(final Var_Node vn, PTSetInternal p2set) {
	}
}
