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

package pag.builder;
import soot.jimple.spark.pag.*;
import soot.jimple.*;
import static driver.DruidOptions.sparkOpts;

import driver.DruidOptions;
import pag.WholeProgPAG;
import pag.MtdPAG;
import pag.builder.MtdPAGBuilder;
import pag.node.GNode;
import pag.node.alloc.Alloc_Node;
import pag.node.var.Var_Node;
import soot.*;
import soot.toolkits.scalar.Pair;
import soot.shimple.*;

/** Class implementing builder parameters (this decides
 * what kinds of nodes should be built for each kind of Soot value).
 * @author Ondrej Lhotak
 */
public class MtdPAGBuilder extends AbstractShimpleValueSwitch {
   
    
    public MtdPAGBuilder( WholeProgPAG pag, MtdPAG mpag ) {
        this.pag = pag;
        this.mpag = mpag;
        setCurrentMethod( mpag.getMethod() );
    }

    /** Sets the method for which a graph is currently being built. */
    private void setCurrentMethod( SootMethod m ) {
        method = m;
        if( !m.isStatic() ) {
            SootClass c = m.getDeclaringClass();
            if( c == null ) {
                throw new RuntimeException( "Method "+m+" has no declaring class" );
            }
            caseThis();
        }
        for( int i = 0; i < m.getParameterCount(); i++ ) {
            if( m.getParameterType(i) instanceof RefLikeType ) {
                caseParm( i );
            }
        }
        Type retType = m.getReturnType();
        if( retType instanceof RefLikeType ) {
            caseRet();
        }
    }

    public GNode getNode( Value v ) {
        v.apply( this );
        return getNode();
    }

    /** Adds the edges required for this statement to the graph. */
	final public void handleStmt(Stmt s) {
		if (s.containsInvokeExpr()) {
			InvokeExpr ie = s.getInvokeExpr();
			// boolean virtualCall = callAssigns.containsKey(ie);
			int numArgs = ie.getArgCount();
			for (int i = 0; i < numArgs; i++) {
				Value arg = ie.getArg(i);
				if (!(arg.getType() instanceof RefLikeType))
					continue;
				if (arg instanceof NullConstant)
					continue;
				
				/// handle arguments for both static and virtual invoke calls
				mpag.nodeFactory().getNode(arg);

				/// handle receiver objects for virtual invoke calls.
				if (ie instanceof InstanceInvokeExpr) {
					InstanceInvokeExpr iie = (InstanceInvokeExpr) ie;
					mpag.nodeFactory().getNode(iie.getBase());
				}
			}
		} else {
			s.apply(new AbstractStmtSwitch() {
				final public void caseAssignStmt(AssignStmt as) {
					Value l = as.getLeftOp();
					Value r = as.getRightOp();
					if (!(l.getType() instanceof RefLikeType))
						return;
					// check for improper casts, with mal-formed code we might
					// get
					// l = (refliketype)int_type
					// if so just return
					if (r instanceof CastExpr && (!(((CastExpr) r).getOp().getType() instanceof RefLikeType))) {
						return;
					}

					if (!(r.getType() instanceof RefLikeType))
					    return;
						//throw new RuntimeException("Type mismatch in assignment (rhs not a RefLikeType) " + as + " in method " + method.getSignature());

					l.apply(MtdPAGBuilder.this);
					GNode dest = getNode();
					r.apply(MtdPAGBuilder.this);
					GNode src = getNode();
					if (l instanceof InstanceFieldRef) {
						((InstanceFieldRef) l).getBase().apply(MtdPAGBuilder.this);
						pag.addDereference((Var_Node) getNode());
					}
					if (r instanceof InstanceFieldRef) {
						((InstanceFieldRef) r).getBase().apply(MtdPAGBuilder.this);
						pag.addDereference((Var_Node) getNode());
					}
					if (r instanceof StaticFieldRef) {
						StaticFieldRef sfr = (StaticFieldRef) r;
						SootFieldRef s = sfr.getFieldRef();
						if (sparkOpts.empties_as_allocs()) {
							if (s.declaringClass().getName().equals("java.util.Collections")) {
								if (s.name().equals("EMPTY_SET")) {
									src = pag.makeAllocNode(RefType.v("java.util.HashSet"),
											RefType.v("java.util.HashSet"), method);
								} else if (s.name().equals("EMPTY_MAP")) {
									src = pag.makeAllocNode(RefType.v("java.util.HashMap"),
											RefType.v("java.util.HashMap"), method);
								} else if (s.name().equals("EMPTY_LIST")) {
									src = pag.makeAllocNode(RefType.v("java.util.LinkedList"),
											RefType.v("java.util.LinkedList"), method);
								}
							} else if (s.declaringClass().getName().equals("java.util.Hashtable")) {
								if (s.name().equals("emptyIterator")) {
									src = pag.makeAllocNode(RefType.v("java.util.Hashtable$EmptyIterator"),
											RefType.v("java.util.Hashtable$EmptyIterator"), method);
								} else if (s.name().equals("emptyEnumerator")) {
									src = pag.makeAllocNode(RefType.v("java.util.Hashtable$EmptyEnumerator"),
											RefType.v("java.util.Hashtable$EmptyEnumerator"), method);
								}
							}
						}
					}
					mpag.addInternalEdge(src, dest);
				}

				final public void caseReturnStmt(ReturnStmt rs) {
					if (!(rs.getOp().getType() instanceof RefLikeType))
						return;
					rs.getOp().apply(MtdPAGBuilder.this);
					GNode retNode = getNode();
					mpag.addInternalEdge(retNode, caseRet());
				}

				final public void caseIdentityStmt(IdentityStmt is) {
					if (!(is.getLeftOp().getType() instanceof RefLikeType))
						return;
					is.getLeftOp().apply(MtdPAGBuilder.this);
					GNode dest = getNode();
					is.getRightOp().apply(MtdPAGBuilder.this);
					GNode src = getNode();
					mpag.addInternalEdge(src, dest);
				}

				final public void caseThrowStmt(ThrowStmt ts) {
					ts.getOp().apply(MtdPAGBuilder.this);
					mpag.addInternalEdge(getNode(), pag.GlobalNodeFactory().caseThrow());
				}
			});
		}
	}
    final public GNode getNode() {
        return (GNode) getResult();
    }

    final public GNode caseThis() {
        Var_Node ret = pag.makeLocalVarNode(
            new Pair( method, PointsToAnalysis.THIS_NODE ),
            method.getDeclaringClass().getType(), method );
        ret.setInterProcTarget();
        return ret;
    }

    final public GNode caseParm( int index ) {
        Var_Node ret = pag.makeLocalVarNode(
            new Pair( method, new Integer( index ) ),
            method.getParameterType( index ), method );
        ret.setInterProcTarget();
        return ret;
    }

    final public void casePhiExpr(PhiExpr e) {
        Pair phiPair = new Pair( e, PointsToAnalysis.PHI_NODE );
        GNode phiNode = pag.makeLocalVarNode( phiPair, e.getType(), method );
        for (Value op : e.getValues()) {
            op.apply( MtdPAGBuilder.this );
            GNode opNode = getNode();
            mpag.addInternalEdge( opNode, phiNode );
        }
        setResult( phiNode );
    }

    final public GNode caseRet() {
        Var_Node ret = pag.makeLocalVarNode(
            Parm.v( method, PointsToAnalysis.RETURN_NODE ),
            method.getReturnType(), method );
        ret.setInterProcSource();
        return ret;
    }
    final public GNode caseArray( Var_Node base ) {
        return pag.makeFieldRefNode( base, ArrayElement.v() );
    }
    /* End of public methods. */
    /* End of package methods. */

    // OK, these ones are public, but they really shouldn't be; it's just
    // that Java requires them to be, because they override those other
    // public methods.
    @Override
    final public void caseArrayRef( ArrayRef ar ) {
        caseLocal( (Local) ar.getBase() );
        setResult( caseArray( (Var_Node) getNode() ) );
    }
    final public void caseCastExpr( CastExpr ce ) {
        Pair castPair = new Pair( ce, PointsToAnalysis.CAST_NODE );
        ce.getOp().apply( this );
        GNode opNode = getNode();
        GNode castNode = pag.makeLocalVarNode( castPair, ce.getCastType(), method );
        mpag.addInternalEdge( opNode, castNode );
        setResult( castNode );
    }

    @Override
    final public void caseCaughtExceptionRef( CaughtExceptionRef cer ) {
        setResult( pag.GlobalNodeFactory().caseThrow() );
    }

    @Override
    final public void caseInstanceFieldRef( InstanceFieldRef ifr ) {
        if( sparkOpts.field_based() || sparkOpts.vta() ) {
            setResult( pag.makeGlobalVarNode( 
                ifr.getField(), 
                ifr.getField().getType() ) );
        } else {
            setResult( pag.makeLocalFieldRefNode( 
                ifr.getBase(), 
                ifr.getBase().getType(),
                ifr.getField(),
                method ) );
        }
    }

    @Override
    final public void caseLocal( Local l ) {
        setResult( pag.makeLocalVarNode( l,  l.getType(), method ) );
    }

    @Override
    final public void caseNewArrayExpr( NewArrayExpr nae ) {
        setResult( pag.makeAllocNode( nae, nae.getType(), method ) );
    }

    private boolean isStringBuffer(Type t) {
        if(!(t instanceof RefType)) return false;
        RefType rt = (RefType) t;
        String s = rt.toString();
        if(s.equals("java.lang.StringBuffer")) return true;
        if(s.equals("java.lang.StringBuilder")) return true;
        return false;
    }

    @Override
    final public void caseNewExpr( NewExpr ne ) {
        if( (DruidOptions.impreciseStrings 
                && isStringBuffer(ne.getType()))   ) {
            setResult( pag.makeAllocNode( ne.getType(), ne.getType(), null ) );
        } else {
            setResult( pag.makeAllocNode( ne, ne.getType(), method ) );
        }
    }

    @Override
    final public void caseNewMultiArrayExpr( NewMultiArrayExpr nmae ) {
        ArrayType type = (ArrayType) nmae.getType();
        Alloc_Node prevAn = pag.makeAllocNode(
            new Pair( nmae, new Integer( type.numDimensions ) ), type, method );
        Var_Node prevVn = pag.makeLocalVarNode( prevAn, prevAn.getType(), method );
        mpag.addInternalEdge( prevAn, prevVn );
        setResult( prevAn );
        while( true ) {
            Type t = type.getElementType();
            if( !( t instanceof ArrayType ) ) break;
            type = (ArrayType) t;
            Alloc_Node an = pag.makeAllocNode(
                new Pair( nmae, new Integer( type.numDimensions ) ), type, method );
            Var_Node vn = pag.makeLocalVarNode( an, an.getType(), method );
            mpag.addInternalEdge( an, vn );
            mpag.addInternalEdge( vn, pag.makeFieldRefNode( prevVn, ArrayElement.v() ) );
            prevAn = an;
            prevVn = vn;
        }
    }

    @Override
    final public void caseParameterRef( ParameterRef pr ) {
        setResult( caseParm( pr.getIndex() ) );
    }

    @Override
    final public void caseStaticFieldRef( StaticFieldRef sfr ) {
        setResult( pag.makeGlobalVarNode( 
            sfr.getField(), 
            sfr.getField().getType() ) );
    }

    @Override
    final public void caseStringConstant( StringConstant sc ) {
        Alloc_Node stringConstant;
        if( (sparkOpts.string_constants()
                || Scene.v().containsClass(sc.value) 
                || ( sc.value.length() > 0 && sc.value.charAt(0) == '[' ))) {
            stringConstant = pag.makeStringConstantNode( sc, method );
        } else {
            stringConstant = pag.makeAllocNode(
                PointsToAnalysis.STRING_NODE,
                RefType.v( "java.lang.String" ), null );
        }
        
        /*  old code
        VarNode stringConstantLocal = pag.makeGlobalVarNode(
            stringConstant,
            RefType.v( "java.lang.String" ) );
        //TODO: What??       
        pag.addEdge( stringConstant, stringConstantLocal );
        mpag.addInternalEdge( stringConstant, stringConstantLocal );
        setResult( stringConstantLocal );
        */
        
        
        //we are using a wrapper class on jimple string constant that includes
        //the method that declared the string constant, this allows
        //us to use a local var node in the pag to point to the string constant
        //and thus parameterize the local var node for more precision

        
        //changed this to use a local variable node that can be parameterized
        Var_Node stringConstantLocal = pag.makeLocalVarNode(
            sc,
            RefType.v( "java.lang.String" ), method );         
        mpag.addInternalEdge( stringConstant, stringConstantLocal );
        setResult( stringConstantLocal );
    }

    @Override
    final public void caseThisRef( ThisRef tr ) {
        setResult( caseThis() );
    }

    @Override
    final public void caseNullConstant( NullConstant nr ) {
        setResult( null );
    }

    @Override
    final public void caseClassConstant( ClassConstant cc ) {
        Alloc_Node classConstant = pag.makeClassConstantNode(cc);
        Var_Node classConstantLocal = pag.makeGlobalVarNode(cc,RefType.v( "java.lang.Class" ) );
        //TODO: What??
        //DA: currently just ignore since other edges related to classConstantLocal would be parameterized later.
        //pag.addEdge(classConstant, classConstantLocal);
        mpag.addInternalEdge(classConstant, classConstantLocal);
        setResult(classConstantLocal);
    }

    @Override
    final public void defaultCase( Object v ) {
        throw new RuntimeException( "failed to handle "+v );
    }

    protected WholeProgPAG pag;
    protected MtdPAG mpag;
    protected SootMethod method;
}

