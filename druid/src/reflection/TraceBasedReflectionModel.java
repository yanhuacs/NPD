/* Soot - a J*va Optimization Framework
 * Copyright (C) 2003 Ondrej Lhotak
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pag.WholeProgPAG;
import pta.CallGraphBuilder;
import pta.context.ParameterizedMethod;
import soot.Body;
import soot.EntryPoints;
import soot.G;
import soot.Kind;
import soot.Local;
import soot.PackManager;
import soot.PhaseOptions;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Transform;
import soot.Type;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.NewExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.Edge;

public class TraceBasedReflectionModel extends ReflectionModel {
	private CallGraphBuilder callGraphBuilder;
	
    class Guard {
        public Guard(SootMethod container, Stmt stmt, String message) {
            this.container = container;
            this.stmt = stmt;
            this.message = message;
        }
        final SootMethod container;
        final Stmt stmt;
        final String message;
    }

    protected Set<Guard> guards;

    protected ReflectionTrace reflectionInfo;

    private boolean registeredTransformation = false;

    public TraceBasedReflectionModel(CallGraphBuilder callGraphBuilder) {
        guards = new HashSet<Guard>();
        this.callGraphBuilder=callGraphBuilder;
        String logFile = callGraphBuilder.options.reflection_log();
        if(logFile==null) {
            throw new InternalError("Trace based refection model enabled but no trace file given!?");
        } else {
            reflectionInfo = new ReflectionTrace(logFile);
        }
    }

    /**
     * Adds an edge to all class initializers of all possible [s
     * of Class.forName() calls within source.
     */
    public void classForName(ParameterizedMethod container, Stmt forNameInvokeStmt) {
        Set<String> classNames = reflectionInfo.classForNameClassNames(container.method());
        if(classNames==null || classNames.isEmpty()) {
            registerGuard(container, forNameInvokeStmt, "Class.forName() call site; Soot did not expect this site to be reached");
        } else {
            for (String clsName : classNames) {
                constantForName( clsName, container, forNameInvokeStmt);
            }
        }
    }

    private void constantForName(String cls, ParameterizedMethod src, Stmt srcUnit) {
		if (cls.length() > 0 && cls.charAt(0) == '[') {
			if (cls.length() > 1 && cls.charAt(1) == 'L' && cls.charAt(cls.length() - 1) == ';') {
				cls = cls.substring(2, cls.length() - 1);
				constantForName(cls, src, srcUnit);
			}
		} else {
			if (!Scene.v().containsClass(cls)) {
				if (callGraphBuilder.options.verbose()) {
					G.v().out.println("Warning: Class " + cls + " is" + " a dynamic class, and you did not specify"
							+ " it as such; graph will be incomplete!");
				}
			} else {
				SootClass sootcls = Scene.v().getSootClass(cls);
				if (!sootcls.isPhantomClass()) {
					if (!sootcls.isApplicationClass()) {
						sootcls.setLibraryClass();
					}
					for (SootMethod clinit : EntryPoints.v().clinitsOf(sootcls)) {
						callGraphBuilder.addStaticEdge(src, srcUnit, clinit, Kind.CLINIT);
						System.out.println("Adding clinit for reflective for/getName(): " + sootcls);
					}
				}

			}
		}
	}
    /**
     * Adds an edge to the constructor of the target class from this call to
     * {@link Class#newInstance()}.
     */
    public void classNewInstance(ParameterizedMethod container, Stmt newInstanceInvokeStmt) {
        Set<String> classNames = reflectionInfo.classNewInstanceClassNames(container.method());
        if(classNames==null || classNames.isEmpty()) {
            registerGuard(container, newInstanceInvokeStmt, "Class.newInstance() call site; Soot did not expect this site to be reached");
        } else {
            for (String clsName : classNames) {
                SootClass cls = Scene.v().getSootClass(clsName);
                if( cls.declaresMethod(sigInit) ) {
                    SootMethod constructor = cls.getMethod(sigInit);
                    callGraphBuilder.addStaticEdge( container, newInstanceInvokeStmt, constructor, Kind.REFL_CLASS_NEWINSTANCE );
                }
            }
        }
    }

    /** 
     * Adds a special edge of kind {@link Kind#REFL_CONSTR_NEWINSTANCE} to all possible target constructors
     * of this call to {@link Constructor#newInstance(Object...)}.
     * Those kinds of edges are treated specially in terms of how parameters are assigned,
     * as parameters to the reflective call are passed into the argument array of
     * {@link Constructor#newInstance(Object...)}.
     * @see WholeProgPAG#addCallTarget(Edge) 
     */
    public void contructorNewInstance(ParameterizedMethod container, Stmt newInstanceInvokeStmt) {
        Set<String> constructorSignatures = reflectionInfo.constructorNewInstanceSignatures(container.method());
        if(constructorSignatures==null || constructorSignatures.isEmpty()) {
            registerGuard(container, newInstanceInvokeStmt, "Constructor.newInstance(..) call site; Soot did not expect this site to be reached");
        } else {
            for (String constructorSignature : constructorSignatures) {
                SootMethod constructor = Scene.v().getMethod(constructorSignature);
                callGraphBuilder.addStaticEdge( container, newInstanceInvokeStmt, constructor, Kind.REFL_CONSTR_NEWINSTANCE );
            }
        }
    }

    /** 
     * Adds a special edge of kind {@link Kind#REFL_INVOKE} to all possible target methods
     * of this call to {@link Method#invoke(Object, Object...)}.
     * Those kinds of edges are treated specially in terms of how parameters are assigned,
     * as parameters to the reflective call are passed into the argument array of
     * {@link Method#invoke(Object, Object...)}.
     * @see WholeProgPAG#addCallTarget(Edge) 
     */
    public void methodInvoke(ParameterizedMethod container, Stmt invokeStmt) {
        Set<String> methodSignatures = reflectionInfo.methodInvokeSignatures(container.method());
        if (methodSignatures == null || methodSignatures.isEmpty()) {
            registerGuard(container, invokeStmt, "Method.invoke(..) call site; Soot did not expect this site to be reached");
        } else {
            for (String methodSignature : methodSignatures) {
                SootMethod method = Scene.v().getMethod(methodSignature);
                callGraphBuilder.addStaticEdge( container, invokeStmt, method, Kind.REFL_INVOKE );
            }
        }
    }

    private void registerGuard(ParameterizedMethod container, Stmt stmt, String string) {
        guards.add(new Guard(container.method(),stmt,string));

        if(callGraphBuilder.options.verbose()) {
            G.v().out.println("Incomplete trace file: Class.forName() is called in method '" +
                    container+"' but trace contains no information about the receiver class of this call.");
            if(callGraphBuilder.options.guards().equals("ignore")) {
                G.v().out.println("Guarding strategy is set to 'ignore'. Will ignore this problem.");
            } else if(callGraphBuilder.options.guards().equals("print")) {
                G.v().out.println("Guarding strategy is set to 'print'. " +
                        "Program will print a stack trace if this location is reached during execution.");
            } else if(callGraphBuilder.options.guards().equals("throw")) {
                G.v().out.println("Guarding strategy is set to 'throw'. Program will throw an " +
                        "Error if this location is reached during execution.");
            } else {
                throw new RuntimeException("Invalid value for phase option (guarding): "+callGraphBuilder.options.guards());
            }
        }

        if(!registeredTransformation) {
            registeredTransformation=true;
            PackManager.v().getPack("wjap").add(new Transform("wjap.guards",new SceneTransformer() {

                @Override
                protected void internalTransform(String phaseName, Map<String, String> options) {
                    for (Guard g : guards) {
                        insertGuard(g);
                    }
                }
            }));
            PhaseOptions.v().setPhaseOption("wjap.guards", "enabled");
        }
    }

    private void insertGuard(Guard guard) {
        if(callGraphBuilder.options.guards().equals("ignore")) return;

        SootMethod container = guard.container;
        Stmt insertionPoint = guard.stmt;
        if(!container.hasActiveBody()) {
            G.v().out.println("WARNING: Tried to insert guard into "+container+" but couldn't because method has no body.");
        } else {
            Body body = container.getActiveBody();

            //exc = new Error
            RefType runtimeExceptionType = RefType.v("java.lang.Error");
            NewExpr newExpr = Jimple.v().newNewExpr(runtimeExceptionType);
            LocalGenerator lg = new LocalGenerator(body);
            Local exceptionLocal = lg.generateLocal(runtimeExceptionType);
            AssignStmt assignStmt = Jimple.v().newAssignStmt(exceptionLocal, newExpr);
            body.getUnits().insertBefore(assignStmt, insertionPoint);

            //exc.<init>(message)
            SootMethodRef cref = runtimeExceptionType.getSootClass().getMethod("<init>", Collections.<Type>singletonList(RefType.v("java.lang.String"))).makeRef();
            SpecialInvokeExpr constructorInvokeExpr = Jimple.v().newSpecialInvokeExpr(exceptionLocal, cref, StringConstant.v(guard.message));
            InvokeStmt initStmt = Jimple.v().newInvokeStmt(constructorInvokeExpr);
            body.getUnits().insertAfter(initStmt, assignStmt);

            if(callGraphBuilder.options.guards().equals("print")) {
                //exc.printStackTrace();
                VirtualInvokeExpr printStackTraceExpr = Jimple.v().newVirtualInvokeExpr(exceptionLocal, Scene.v().getSootClass("java.lang.Throwable").getMethod("printStackTrace", Collections.<Type>emptyList()).makeRef());
                InvokeStmt printStackTraceStmt = Jimple.v().newInvokeStmt(printStackTraceExpr);
                body.getUnits().insertAfter(printStackTraceStmt, initStmt);
            } else if(callGraphBuilder.options.guards().equals("throw")) {
                body.getUnits().insertAfter(Jimple.v().newThrowStmt(exceptionLocal), initStmt);
            } else {
                throw new RuntimeException("Invalid value for phase option (guarding): "+callGraphBuilder.options.guards());
            }
        }
    }

}
