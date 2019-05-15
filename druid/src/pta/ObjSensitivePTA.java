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

package pta;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import driver.Config;
import driver.DruidOptions;
import pag.node.alloc.Alloc_Node;
import pag.node.alloc.ContextAlloc_Node;
import pag.node.var.LocalVar_Node;
import pag.node.var.Var_Node;
import pta.context.ContextElement;
import pta.context.CtxElements;
import pta.context.EmptyContext;
import pta.context.ParameterizedMethod;
import pta.context.StaticInitContext;
import pta.context.TypeContextElement;
import soot.Context;
import soot.FastHierarchy;
import soot.Kind;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.VirtualCalls;
import soot.util.queue.QueueReader;
import util.VirtualInvokeSite;

public class ObjSensitivePTA extends PTA {
	
	@Override
	protected CallGraphBuilder createCallGraphBuilder(){
		return new ObjSensitiveCGBuilder();
	}
	@Override
    protected Context heapSelector(Alloc_Node base, Context context) {
        int contextLength = DruidOptions.kobjsens;
        if (0 == contextLength || Config.v().limitHeapContext(base)) {
            contextLength = 1; 
        } else {
            //add more context for arrays, change to  + 2 if using the above
            if (DruidOptions.extraArrayContext && !(base.getType() instanceof RefType)) {
                contextLength = DruidOptions.kobjsens + 1;
            }
        }
        ContextElement[] array = new ContextElement[contextLength];            
        array[0] = base;
        if (contextLength > 1) {                
            if (context instanceof ContextAlloc_Node) {
                ContextAlloc_Node osan = (ContextAlloc_Node)context;
                ContextElement[] cxtAllocs = osan.getContext().getElements();
                for (int i = 1; i < contextLength; i++) {
                    if ((i-1) >= cxtAllocs.length)
                        break;
                    
                    if (DruidOptions.typesForContext && 
                    		cxtAllocs[i-1] instanceof Alloc_Node) {
                        array[i] = TypeContextElement.v(((Alloc_Node)cxtAllocs[i-1]).getType());
                    } else 
                        array[i] = cxtAllocs[i - 1];
                }
            } else if (context instanceof ContextElement) {
                if (DruidOptions.typesForContext && 
                        context instanceof Alloc_Node)
                    array[1] = TypeContextElement.v(((Alloc_Node)context).getType());
                else 
                    array[1] = (ContextElement)context;

            } else {
                throw new RuntimeException("Unsupported context on alloc node: " + context);
            }
            
            //for loop to fill context elements
            for (int i = 0; i < array.length; i++) {
                if (array[i] == null)
                    array[i] = EmptyContext.v();
            }
        }
        return new CtxElements(array);
    }
	@Override
	protected Context selector(Alloc_Node receiverNode){
		if (DruidOptions.kobjsens > 0)
			return (Context) receiverNode;
		else
			return EmptyContext.v();
	}	
    
	public class ObjSensitiveCGBuilder extends CallGraphBuilder {

	    private Map<ParameterizedMethod, Integer> apiCallDepthMap;

		public ObjSensitiveCGBuilder() {	
	        apiCallDepthMap = new HashMap<ParameterizedMethod, Integer>();
	        //initialize apiCallDepth with reachable entry point methods
	        QueueReader<ParameterizedMethod> qr = reachables.listener();
	        while (qr.hasNext()) {
	            apiCallDepthMap.put(qr.next(), 0);
	        }
		}
		
		@Override
		protected Var_Node getReceiverVarNode(Local receiver, ParameterizedMethod m){
			if (m.context() == null)
				return pag.makeLocalVarNode(receiver, receiver.getType(), m.method());//TODO
	        LocalVar_Node base = pag.makeLocalVarNode( receiver, receiver.getType(), m.method() );
	        return makeContextVarNode( base, m.context() );
		}

		public void connectVirtualEdge(Var_Node receiver, Type type, Alloc_Node receiverNode) {
			FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();

			for (Iterator<VirtualInvokeSite> siteIt = ((Collection<VirtualInvokeSite>) receiverToSites.get(receiver)).iterator(); siteIt.hasNext();) {
				final VirtualInvokeSite site = (VirtualInvokeSite) siteIt.next();
				if (site.kind() == Kind.THREAD && !fh.canStoreType(type, clRunnable))
					continue;

				if (site.iie() instanceof SpecialInvokeExpr && site.kind() != Kind.THREAD) {
					SootMethod target = VirtualCalls.v().resolveSpecial((SpecialInvokeExpr) site.iie(), site.subSig(),site.container().method());
					// if the call target resides in a phantom class then "target" will be null;
					// simply do not add the target in that case
					if (target != null)
						targetsQueue.add(target);
				} else
					VirtualCalls.v().resolve(type, receiver.getType(), site.subSig(), site.container().method(),targetsQueue);
				// calculate if we want to add context for this alloc node TODO insensitive
				Context tgtContext = selector(receiverNode);
				while (targets.hasNext())
					addVirtualEdge(site.container(), site.stmt(), (SootMethod) targets.next(), site.kind(), tgtContext);
			}
		}
		
	    @Override
	    public void addStaticEdge( ParameterizedMethod caller, Unit callStmt, SootMethod callee, Kind kind ) {
	    	Context typeContext = caller.context();
	       
	        if (kind.isClinit()) {
	            SootClass cl = callee.getDeclaringClass(); 
	            if (cl == null)
	                throw new RuntimeException("No declaring class for clinit when adding context!");
	            
	            if (DruidOptions.staticinitcontext)    
	                typeContext = StaticInitContext.v(callee.getDeclaringClass());
	            else
	                typeContext = EmptyContext.v();
	            
	        } 
	        if (DruidOptions.apicalldepth < 0 || checkAPICallDepth(caller, callee, typeContext)) {
	            Edge edge = new Edge( caller, callStmt, ParameterizedMethod.v(callee, typeContext), kind );
	            //System.out.println("Adding static edge: " + edge);
	            cg.addEdge( edge );
	        }
	    }
	    
	    public void addVirtualEdge(ParameterizedMethod caller, Unit callStmt, SootMethod callee, Kind kind, Context typeContext) {
	    	if (DruidOptions.apicalldepth < 0 || checkAPICallDepth(caller, callee, typeContext)) {
	            Edge edge = new Edge( caller, callStmt, ParameterizedMethod.v( callee, typeContext ), kind );
	            // System.out.println("Adding virtual edge: " + edge);
	            cg.addEdge( edge );
	        }
	    }
	    
	    private boolean isAppMethod(SootMethod method, Context context) {
	        if (context instanceof ContextAlloc_Node) {
	            //virtual call
	            ContextAlloc_Node rec = (ContextAlloc_Node)context;
	            if (rec.getType() instanceof RefType) {
	                SootClass containerClass = ((RefType)rec.getType()).getSootClass();
	                //no matter the depth if the target is an app class, then return true
	                if (Config.v().isAppClass(containerClass))
	                    return true;
	            }                     
	        } else {
	            //not a virtual call, check class of target
	            if (Config.v().isAppClass(method.getDeclaringClass())) 
	                return true;                     
	        }
	        return false;
	    }
	   
	    private boolean checkAPICallDepth(ParameterizedMethod src, SootMethod tgt, Context tgtContext) {
	        ParameterizedMethod tgtMC = ParameterizedMethod.v(tgt, tgtContext);
	        
	        //found app method
	        if (isAppMethod(tgt, tgtContext)) {
	           apiCallDepthMap.put(tgtMC, 0);
	           return true;
	        }
	        
	        if (DruidOptions.apicalldepth > 0) {
	            return checkDepth(src, tgtMC);
	        } else 
	            return false;        
	    }
	    
	    private boolean checkDepth(ParameterizedMethod src, ParameterizedMethod tgt) {
	        int prevTgtDepth = apiCallDepthMap.containsKey(tgt) ? apiCallDepthMap.get(tgt) : Integer.MAX_VALUE;
	        int fromSrcTgtDepth = apiCallDepthMap.get(src) + 1;
	        
	        if (fromSrcTgtDepth < prevTgtDepth) 
	            apiCallDepthMap.put(tgt, fromSrcTgtDepth);
	        
	        return apiCallDepthMap.get(tgt).intValue() <= DruidOptions.apicalldepth;      
	    }
	}
	
}
