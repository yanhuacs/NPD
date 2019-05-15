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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static driver.DruidOptions.sparkOpts;
import driver.Config;
import pag.WholeProgPAG;
import pag.node.GNode;
import pag.node.alloc.Alloc_Node;
import pag.node.alloc.ClassConstant_Node;
import pag.node.alloc.Constructor_Node;
import pag.node.alloc.MethodMetaObject_Node;
import pag.node.alloc.StringConstant_Node;
import pag.node.alloc.UnknownObject_Node;
import pag.node.var.FieldRef_Node;
import pag.node.var.LocalVar_Node;
import pag.node.var.Var_Node;
import pta.CallGraphBuilder;
import pta.context.ParameterizedMethod;
import pta.pts.PTSetInternal;
import pta.pts.PTSetVisitor;
import reflection.ConstructorWrapper;
import reflection.GetMethodWrapper;
import reflection.android.HandleTgtMtdInvocation;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.Context;
import soot.DoubleType;
import soot.EntryPoints;
import soot.FastHierarchy;
import soot.FloatType;
import soot.G;
import soot.IntType;
import soot.Kind;
import soot.Local;
import soot.LongType;
import soot.NullType;
import soot.PhaseOptions;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.ClassConstant;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.NewArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JNewArrayExpr;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.spark.pag.ArrayElement;
import soot.options.CGOptions;
import soot.util.SmallNumberedMap;

public class InferenceReflectionModel extends ReflectionModel {
	// The super class of all the classes used to represent reflective call sites
	public class StmtWithPos {
		protected SootMethod source;
		protected Stmt caller;
		protected Context ctx;
		
		public SootMethod getSource() {
			return source;
		}

		public Stmt getCaller() {
			return caller;
		}
		
		public Context getContext() {
			return ctx;
		}
		
		public StmtWithPos(SootMethod source, Stmt caller, Context ctx) {
			this.source = source;
			this.caller = caller;
			this.ctx = ctx;
		}
		
		public boolean equals(Object o) {
			if(o == this)
				return true;
			else if(o == null)
				return false;
			else if (o instanceof StmtWithPos) {
				StmtWithPos stmt = (StmtWithPos) o;
				if(ctx == null) {
					if(stmt.ctx == null) {
						return source.equals(stmt.source) && caller.equals(stmt.caller);
					} else {
						return false;
					}
				} else
					return source.equals(stmt.source) && caller.equals(stmt.caller) && ctx.equals(stmt.ctx); 
			} else
				return false;
		}
		
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + source.hashCode();
			result = prime * result + caller.hashCode();
			result = prime * result + (ctx == null ? 0 : ctx.hashCode());
			return result;
		}
	}
	
	public class ForNameCallInfo extends StmtWithPos {
		private Set<SootClass> targetClasses;
		
		public ForNameCallInfo(SootMethod source, Stmt caller, Context ctx) {
			super(source, caller, ctx);
			targetClasses = new HashSet<>();
		}
		
		public void add(SootClass clz) {
			targetClasses.add(clz);
		}
		
		public boolean contains(SootClass clz) {
			return targetClasses.contains(clz);
		}
		
		public Set<SootClass> getTargetClasses() {
			return targetClasses;
		}
	}

	public class ClzNewInstCallInfo extends StmtWithPos {
		private Type postDomCast;
		private Set<SootClass> reachingClasses;

		public ClzNewInstCallInfo(SootMethod source, Stmt caller, Type postDomCast, Context ctx) {
			super(source, caller, ctx);
			this.postDomCast = postDomCast;
			reachingClasses = new HashSet<>();
		}
		
		public Set<SootClass> getReachingClasses() {
			return reachingClasses;
		}
	}
	
	public abstract class ConstructorCall extends StmtWithPos {
		protected Set<SootClass> metaClzs;
		protected boolean isFromGetCtor;

		public ConstructorCall(SootMethod source, Stmt caller, Context ctx, boolean isFromGetCtor) {
			super(source, caller, ctx);
			this.isFromGetCtor = isFromGetCtor;
			metaClzs = new HashSet<>();
		}

		public Set<SootClass> getMetaClzs() {
			return metaClzs;
		}

		public void add(SootClass clz) {
			metaClzs.add(clz);
		}

		public boolean contains(SootClass clz) {
			return metaClzs.contains(clz);
		}
		
		public boolean isFromGetCtor() {
			return isFromGetCtor;
		}
	}
	
	/**
	 * An abstract class that describes the Class.get*Constructor() information.
	 * It is inherited by GetCtorCallInfo and GetDeclaredCtorCallInfo.
	 * 
	 */
	public abstract class CtorCallInfo extends ConstructorCall {
		public CtorCallInfo(SootMethod source, Stmt caller, Context ctx, boolean isFromGetCtor) {
			super(source, caller, ctx, isFromGetCtor);
		}
	}
	
	public class GetCtorCallInfo extends CtorCallInfo {
		public GetCtorCallInfo(SootMethod source, Stmt caller, Context ctx) {
			super(source, caller, ctx, true);
		}
	}
	
	public class GetDeclaredCtorCallInfo extends CtorCallInfo {
		public GetDeclaredCtorCallInfo(SootMethod source, Stmt caller, Context ctx) {
			super(source, caller, ctx, false);
		}
	}
	
	/**
	 * An abstract class that describes Class.get*Constructors() information
	 * It is the exactly same as CtorCallInfo.
	 * It is extended by GetCtorsCallInfo and GetDeclaredCtorsCallInfo.
	 * 
	 */
	public abstract class CtorsCallInfo extends ConstructorCall {
		public CtorsCallInfo(SootMethod source, Stmt caller, Context ctx, boolean isFromGetCtor) {
			super(source, caller, ctx, isFromGetCtor);
		}
	}	
	
	public class GetCtorsCallInfo extends CtorsCallInfo {
		public GetCtorsCallInfo(SootMethod source, Stmt caller, Context ctx) {
			super(source, caller, ctx, true);
		}
	}
	
	public class GetDeclaredCtorsCallInfo extends CtorsCallInfo {
		public GetDeclaredCtorsCallInfo(SootMethod source, Stmt caller, Context ctx) {
			super(source, caller, ctx, false);
		}
	}
	
	public class CtorNewInstCallInfo extends StmtWithPos {
		private Set<SootClass> metaClzs;
		private Type postDomCast;
		private boolean isClassUnknown;
		private Set<SootMethod> ctors;
		
		public CtorNewInstCallInfo(SootMethod source, Stmt caller, Type postDomCast, Context ctx) {
			super(source, caller, ctx);
			this.postDomCast = postDomCast;
			isClassUnknown = false;
			metaClzs = new HashSet<>();
			ctors = new HashSet<>();
		}
		
		public void add(SootClass clz) {
			metaClzs.add(clz);
		}
		
		public void add(SootMethod ctor) {
			ctors.add(ctor);
		}
		
		public boolean contains(SootClass clz) {
			return metaClzs.contains(clz);
		}
		
		public boolean contains(SootMethod ctor) {
			return ctors.contains(ctor);
		}
		
		public Type getPostDomCast() {
			return postDomCast;
		}
		
		public boolean isClassUnknown() {
			return isClassUnknown;
		}
		
		public Set<SootMethod> getCtors() {
			return ctors;
		}
	}
	
	public class MtdCallInfo extends StmtWithPos {
		public Set<SootClass> getMetaClzs() {
			return metaClzs;
		}

		protected Set<SootClass> metaClzs;
		protected Set<String> mtdNames;
		protected Set<SootMethod> targetMtds;
		protected boolean isMtdUnkown;

		public MtdCallInfo(SootMethod source, Stmt caller, Context ctx) {
			super(source, caller, ctx);
			this.source = source;
			this.caller = caller;
			int initCapacity = GlobalVariable.v().getContainerInitCapacity();
			metaClzs = new HashSet<>(initCapacity);
			mtdNames = new HashSet<>(initCapacity);
			targetMtds = new HashSet<>(initCapacity);
			isMtdUnkown = false;
		}

		public void add(SootClass clz) {
			metaClzs.add(clz);
		}

		public void add(String mtdName) {
			mtdNames.add(mtdName);
		}
		
		public boolean isMtdUnkown() {
			return isMtdUnkown;
		}
	}
	
	public class GetMtdCallInfo extends MtdCallInfo {		
		public GetMtdCallInfo(SootMethod source, Stmt caller, Context ctx) {
			super(source, caller, ctx);
		}
	}
	
	public class GetDeclaredMtdCallInfo extends MtdCallInfo {
		public GetDeclaredMtdCallInfo(SootMethod source, Stmt caller, Context ctx) {
			super(source, caller, ctx);
		}
	}
	
	public class MethodsCallInfo extends StmtWithPos {
		protected Set<SootClass> metaClzs;
		protected Set<SootMethod> targetMtds;
		
		public MethodsCallInfo(SootMethod source, Stmt caller, Context ctx) {
			super(source, caller, ctx);
			metaClzs = new HashSet<>();
			targetMtds = new HashSet<>();
		}
		
		public void add(SootClass clz) {
			metaClzs.add(clz);
		}
		
		public void add(SootMethod mtd) {
			targetMtds.add(mtd);
		}
	}
	
	public class GetMethodsCallInfo extends MethodsCallInfo {
		public GetMethodsCallInfo(SootMethod source, Stmt caller, Context ctx) {
			super(source, caller, ctx);
		}
	}
	
	public class GetDeclaredMethodsCallInfo extends MethodsCallInfo {
		public GetDeclaredMethodsCallInfo(SootMethod source, Stmt caller, Context ctx) {
			super(source, caller, ctx);
		}
	}
	
	public class InvokeCallInfo extends StmtWithPos {		
		public Set<SootMethod> getTargetMtds() {
			return targetMtds;
		}
		
		public Set<InvokeExpr> getInvokeExpr() {
			return invokeExprs;
		}
		
		public List<ArgList> getArgLists() {
			return argLists;
		}
		
		private Type postDomCast;
		private Set<MethodMetaObject_Node> mtdMetaObjectNodes;
		private Set<SootMethod> targetMtds;
		private Set<SootClass> baseClasses;
		private Set<Alloc_Node> receiverAllocNodes;
		private Value receiver;
		private List<ArgList> argLists;
		private Set<InvokeExpr> invokeExprs;
		// for Android
		private Local ifCond;
		private Unit lastPos;
		private int caseValue;

		public InvokeCallInfo(SootMethod source, Stmt caller, Value receiver, List<ArgList> argLists, Type postDomCast, Context ctx) {
			super(source, caller, ctx);
			this.argLists = argLists;
			this.postDomCast = postDomCast;
			this.receiver = receiver;
			int initCapacity = GlobalVariable.v().getContainerInitCapacity();
			mtdMetaObjectNodes = new HashSet<>(initCapacity);
			targetMtds = new HashSet<>(initCapacity);
			baseClasses = new HashSet<>(initCapacity);
			receiverAllocNodes = new HashSet<>(initCapacity);
			invokeExprs = new HashSet<>(initCapacity);
		}
		
		/** For Android **/
		public Local getIfCond() {
			return ifCond;
		}

		public void setIfCond(Local ifCond) {
			this.ifCond = ifCond;
		}
		
		public Unit getLastPos() {
			return lastPos;
		}

		public void setLastPos(Unit lastPos) {
			this.lastPos = lastPos;
		}
		
		public void add(SootMethod targetMtd) {
			targetMtds.add(targetMtd);
		}
		
		public int getCaseValue() {
			return caseValue++;
		}
		/** End for Android **/
		
		public void add(SootClass baseClass) {
			baseClasses.add(baseClass);
		}
		
		public void add(MethodMetaObject_Node node) {
			mtdMetaObjectNodes.add(node);
		}
		
		public void add(Alloc_Node receiverAllocNode) {
			receiverAllocNodes.add(receiverAllocNode);
		}
		
		public boolean containsMethodFromGetMtd() {
			for(MethodMetaObject_Node node : mtdMetaObjectNodes)
				if(node.isFromGetMethod())
					return true;
			return false;
		}
		
		public boolean containsMethodFromGetDeclaredMtd() {
			for(MethodMetaObject_Node node : mtdMetaObjectNodes)
				if(node.isFromGetDeclaredMethod())
					return true;
			return false;
		}
	}

	/**
	 * ArgList is used to store all the arguments invoked in the Method.invoke() calls.
	 * ArgList is stored into an array, where the i-th element corresponds to the i-th argument of method invocation.
	 * Because the i-th argument may have multiple values, the i-th element of ArgList is also a list.
	 */
	public class ArgList {
		private int length;
		private boolean processed;
		
		private List<List<Arg>> args;

		public ArgList(AssignStmt allocStmt, int length) {
			this.length = length;
			processed = false;
			args = new ArrayList<>(length);
			if(length == 0)
				args.add(new ArrayList<>(1));
			for(int i = 0; i < length; i++) {
				args.add(new ArrayList<>());
			}
		}

		// adding argument into argument list if it has not been added
		public void add(int index, Arg newArg) {
			List<Arg> args = get(index);
			for(Arg arg : args) {
				if(arg.equals(newArg))
					return;
			}
			args.add(newArg);
		}
		
		public List<Arg> get(int index) {
			return args.get(index);
		}
		
		public String toString() {
			int upperBound = 0;
			if(processed)
				upperBound = args.size();
			else
				upperBound = length;
			StringBuffer buffer = new StringBuffer();
			buffer.append("[");
			for(int i = 0; i < upperBound; i++) {
				buffer.append("[ ");
				List<Arg> arg = get(i);
				for(int j = 0; j < arg.size(); j++) {
					buffer.append(arg.get(j).toString());
					buffer.append(", ");
				}
				buffer.append("], ");
			}
			buffer.append("]");
			return buffer.toString();
		}
	}

	class Arg {
		private Type declaringType;
		private Value var;
		
		public Arg(Type declaringType, Value var) {
			this.declaringType = declaringType;
			this.var = var;
		}
		
		public Value getVar() {
			return var;
		}
		
		public boolean equals(Object o) {
			if(o instanceof Arg) {
				Arg arg = (Arg) o;
				if(declaringType.equals(arg.declaringType) && var.equals(arg.var))
					return true;
				else
					return false;
			}
			return false;
		}
		
		public String toString() {
			return "Local " + var + ", Type: " + declaringType;
		}
	}

	private WholeProgPAG pag;
	private CGOptions options = new CGOptions( PhaseOptions.v().getPhaseOptions("cg"));
	// one container that is used to resolve Class.forName
	private Map<Value, Set<ForNameCallInfo>> clzName2ForNameCalls;
	// one container that is used to resolve Class.newInstance
	private SmallNumberedMap<Set<ClzNewInstCallInfo>> clz2NewInstCalls;
	// one container that is used to resolve Class.getConstructor
	private SmallNumberedMap<Set<GetCtorCallInfo>> clz2GetCtorCalls;
	// one container that is used to resolve Class.getDeclaredConstructor
	private SmallNumberedMap<Set<GetDeclaredCtorCallInfo>> clz2GetDeclaredCtorCalls;
	// one container that is used to resolve Class.getConstructors
	private SmallNumberedMap<Set<GetCtorsCallInfo>> clz2GetCtorsCalls;
	// one container that is used to resolve Class.getDeclaredConstructors
	private SmallNumberedMap<Set<GetDeclaredCtorsCallInfo>> clz2GetDeclaredCtorsCalls;
	// one container that is used to resolve Constructor.newInstance
	private SmallNumberedMap<Set<CtorNewInstCallInfo>> clz2CtorNewInstCalls;
	// two containers that used to resolve Class.getMethod
	private SmallNumberedMap<Set<GetMtdCallInfo>> clz2GetMtdCalls;
	private SmallNumberedMap<Set<GetMtdCallInfo>> mtdName2GetMtdCalls;
	// two containers that used to resolve Class.getDeclaredMethod
	private SmallNumberedMap<Set<GetDeclaredMtdCallInfo>> clz2GetDeclaredMtdCalls;
	private SmallNumberedMap<Set<GetDeclaredMtdCallInfo>> mtdName2GetDeclaredMtdCalls;
	// Class.getMethods()
	private SmallNumberedMap<Set<GetMethodsCallInfo>> clz2GetMethodsCalls;
	// Class.getDeclaredMethods()
	private SmallNumberedMap<Set<GetDeclaredMethodsCallInfo>> clz2GetDeclaredMethodsCalls;
	// container that used to resolve Method.invoke
	private SmallNumberedMap<Set<InvokeCallInfo>> mtd2InvokeCalls;
	private SmallNumberedMap<Set<InvokeCallInfo>> receiverObj2IncokeCalls;
	private CallGraphBuilder cgb;
	private FastHierarchy fastHierarchy;
	private static Map<Type, List<Type>> wideningPrimTypes;
	
	static {		
		wideningPrimTypes = new HashMap<>();
		wideningPrimTypes.put(DoubleType.v().boxedType(), Arrays.asList(new Type[] {ByteType.v().boxedType(), ShortType.v().boxedType(),
				CharType.v().boxedType(), IntType.v().boxedType(), LongType.v().boxedType(), FloatType.v().boxedType()}));
		wideningPrimTypes.put(FloatType.v().boxedType(), Arrays.asList(new Type[]{ByteType.v().boxedType(), ShortType.v().boxedType(),
				CharType.v().boxedType(), IntType.v().boxedType(), LongType.v().boxedType()}));
		wideningPrimTypes.put(LongType.v().boxedType(), Arrays.asList(new Type[] {ByteType.v().boxedType(), ShortType.v().boxedType(),
				CharType.v().boxedType(), IntType.v().boxedType()}));
		wideningPrimTypes.put(IntType.v().boxedType(), Arrays.asList(new Type[] {ByteType.v().boxedType(), ShortType.v().boxedType(),
				CharType.v().boxedType()}));
		wideningPrimTypes.put(CharType.v().boxedType(), new ArrayList<Type>(1));
		wideningPrimTypes.put(ShortType.v().boxedType(), Arrays.asList(new Type[] {ByteType.v().boxedType()}));
		wideningPrimTypes.put(ByteType.v().boxedType(), new ArrayList<Type>(1));
		wideningPrimTypes.put(BooleanType.v().boxedType(), new ArrayList<Type>(1));
	}
	
	public InferenceReflectionModel(WholeProgPAG pag, CallGraphBuilder cgb) {
		this.pag = pag;
		clzName2ForNameCalls = new HashMap<>();
		clz2NewInstCalls = new SmallNumberedMap<>();
		// Class.getConstroctor
		clz2GetCtorCalls = new SmallNumberedMap<>();
		// Class.getDeclaredConstructor
		clz2GetDeclaredCtorCalls = new SmallNumberedMap<>();
		// Class.getConstructors
		clz2GetCtorsCalls = new SmallNumberedMap<>();
		// Class.getDeclaredConstructors
		clz2GetDeclaredCtorsCalls = new SmallNumberedMap<>();
		// Constructor.newInstance
		clz2CtorNewInstCalls = new SmallNumberedMap<>();
		// Class.getMethod
		clz2GetMtdCalls = new SmallNumberedMap<>();
		mtdName2GetMtdCalls = new SmallNumberedMap<>();
		// Class.getDeclaredMethod
		clz2GetDeclaredMtdCalls = new SmallNumberedMap<>();
		mtdName2GetDeclaredMtdCalls = new SmallNumberedMap<>();
		// Class.getMethods
		clz2GetMethodsCalls = new SmallNumberedMap<>();
		//Class.getDeclaredMethods
		clz2GetDeclaredMethodsCalls = new SmallNumberedMap<>();
		// Method.invoke
		mtd2InvokeCalls = new SmallNumberedMap<>();
		receiverObj2IncokeCalls = new SmallNumberedMap<>();
		this.cgb = cgb;
		fastHierarchy = Scene.v().getFastHierarchy();
	}
	
	public Map<Value, Set<ForNameCallInfo>> getClzName2ForNameCalls() {
		return clzName2ForNameCalls;
	}

	public SmallNumberedMap<Set<ClzNewInstCallInfo>> getClz2NewInstCalls() {
		return clz2NewInstCalls;
	}
	
	public SmallNumberedMap<Set<CtorNewInstCallInfo>> getClz2CtorNewInstCalls() {
		return clz2CtorNewInstCalls;
	}

	public SmallNumberedMap<Set<GetMtdCallInfo>> getClz2GetMtdCalls() {
		return clz2GetMtdCalls;
	}

	public SmallNumberedMap<Set<GetMtdCallInfo>> getMtdName2GetMtdCalls() {
		return mtdName2GetMtdCalls;
	}

	public SmallNumberedMap<Set<GetDeclaredMtdCallInfo>> getClz2GetDeclaredMtdCalls() {
		return clz2GetDeclaredMtdCalls;
	}

	public SmallNumberedMap<Set<GetDeclaredMtdCallInfo>> getMtdName2GetDeclaredMtdCalls() {
		return mtdName2GetDeclaredMtdCalls;
	}

	public SmallNumberedMap<Set<InvokeCallInfo>> getMtd2InvokeCalls() {
		return mtd2InvokeCalls;
	}

	public SmallNumberedMap<Set<InvokeCallInfo>> getReceiverObj2IncokeCalls() {
		return receiverObj2IncokeCalls;
	}

	public void getMethod(ParameterizedMethod source, Stmt caller) {
		// Collect info of Class.getMethod call
		// get base object of this call
		// Calls that are not assignment statements are ignored 
		// because they do not have any side effects
		if(!(caller instanceof AssignStmt &&
				((AssignStmt) caller).getRightOp() instanceof InstanceInvokeExpr)) {
			return;
		}
		InstanceInvokeExpr invokeExpr = null;
		invokeExpr = (InstanceInvokeExpr) ((AssignStmt) caller).getRightOp();
		Local baseObj = (Local) invokeExpr.getBase();
		Value mtdName = invokeExpr.getArg(0);
		GetMtdCallInfo getMtdCall = new GetMtdCallInfo(source.method(), caller, null);
		// get target method name
		if(mtdName instanceof StringConstant) {
			getMtdCall.add(((StringConstant) mtdName).value);
		} else {
			// update map, method local variable -> Class.getMethod() call
			Local mtdNameLocal = (Local) mtdName;
			Set<GetMtdCallInfo> getMtdCalls = mtdName2GetMtdCalls.get(mtdNameLocal);
			if(getMtdCalls == null) {
				mtdName2GetMtdCalls.put(mtdNameLocal, getMtdCalls = new HashSet<>());
			}
			getMtdCalls.add(getMtdCall);
		}
		// update map, class meta object -> Class.getMethod() call
		Set<GetMtdCallInfo> getMtdCalls = clz2GetMtdCalls.get(baseObj);
		if(getMtdCalls == null) {
			clz2GetMtdCalls.put(baseObj, getMtdCalls = new HashSet<>());
		}
		getMtdCalls.add(getMtdCall);
	}
	
	public void getDeclaredMethod(ParameterizedMethod source, Stmt caller) {
		if(!(caller instanceof AssignStmt &&
				((AssignStmt) caller).getRightOp() instanceof InstanceInvokeExpr)) {
			return;
		}
		InstanceInvokeExpr invokeExpr = null;
		invokeExpr = (InstanceInvokeExpr) ((AssignStmt) caller).getRightOp();
		Local baseObj = (Local) invokeExpr.getBase();
		Value mtdName = invokeExpr.getArg(0);
		GetDeclaredMtdCallInfo getDeclaredMtdMtdCall = new GetDeclaredMtdCallInfo(source.method(), caller, null);
		// get target method name
		if(mtdName instanceof StringConstant) {
			getDeclaredMtdMtdCall.add(((StringConstant) mtdName).value);
		} else {
			// update map, method local variable -> Class.getMethod() call
			Local mtdNameLocal = (Local) mtdName;
			Set<GetDeclaredMtdCallInfo> getMtdCalls = mtdName2GetDeclaredMtdCalls.get(mtdNameLocal);
			if(getMtdCalls == null) {
				mtdName2GetDeclaredMtdCalls.put(mtdNameLocal, getMtdCalls = new HashSet<>());
			}
			getMtdCalls.add(getDeclaredMtdMtdCall);
		}
		// update map, class meta object -> Class.getMethod() call
		Set<GetDeclaredMtdCallInfo> getMtdCalls = clz2GetDeclaredMtdCalls.get(baseObj);
		if(getMtdCalls == null) {
			clz2GetDeclaredMtdCalls.put(baseObj, getMtdCalls = new HashSet<>());
		}
		getMtdCalls.add(getDeclaredMtdMtdCall);
	}
	
	// If getMethods call is not an assignment statement, then it is ignored
	// Use class metaobjects to index all the reachable getMethods calls
	public void getMethods(ParameterizedMethod source, Stmt caller) {
		if(!(caller instanceof AssignStmt))
			return;
		GetMethodsCallInfo getMethodsCallInfo = new GetMethodsCallInfo(source.method(), caller, null);
		InstanceInvokeExpr invokeExpr = (InstanceInvokeExpr) ((AssignStmt) caller).getRightOp();
		Local baseObj = (Local) invokeExpr.getBase();
		Set<GetMethodsCallInfo> getMethodsCalls = clz2GetMethodsCalls.get(baseObj);
		if(getMethodsCalls == null) {
			clz2GetMethodsCalls.put(baseObj, getMethodsCalls = new HashSet<>());
		}
		getMethodsCalls.add(getMethodsCallInfo);
	}
	
	// Insert an unknown method into the array
	public void handleGetMethodsSideEffect(Local l, Context ctx, GNode n) {
		Set<GetMethodsCallInfo> updateGetMethodsCalls = new HashSet<>();
		if(clz2GetMethodsCalls.get(l) != null) {
			if(! (n instanceof ClassConstant_Node))
				return;
			ClassConstant_Node clzConstNode = (ClassConstant_Node) n;
			if(clzConstNode.getClassConstant().equals(GlobalVariable.v().getUnknownClzConst())) {
				for(GetMethodsCallInfo c : clz2GetMethodsCalls.get(l)) {
					c.add(GlobalVariable.v().getUnknownClass());
					updateGetMethodsCalls.add(c);
				}
			} else {
				String clzName = clzConstNode.getClassConstant().value.replace('/', '.');
				if(!Scene.v().containsClass(clzName)) {
					if(options.verbose())
						G.v().out.println( "Warning: Class " + clzName + " is"+
							" a dynamic class, and you did not specify"+
							" it as such; graph will be incomplete!" );
				} else {
					SootClass clz = Scene.v().getSootClass(clzName);
					clz2GetMethodsCalls.get(l)
								   .stream()
								   .forEach(c -> {
									   c.add(clz);
									   updateGetMethodsCalls.add(c);
								   });
				}
			}
		}
		
		for(GetMethodsCallInfo c : updateGetMethodsCalls) {
			Stmt caller = c.caller;
			Value arrayBase = ((AssignStmt) caller).getLeftOp();
			SootMethod source = c.source;
			SootClass methodMetaClass = Scene.v().getSootClass("java.lang.reflect.Method");
			for(SootClass clz : c.metaClzs) {
				SootMethod mtd = GlobalVariable.v().getUnknownMethod(clz);
				if(! c.targetMtds.contains(mtd)) {
					// allocate method metaobject allocation <=> Method m = Unknown();
					GetMethodWrapper wrapper = new GetMethodWrapper(clz, mtd, caller);
					wrapper.fromGetMethod();
					Alloc_Node methodMetaObj = makeMethodMetaObjectNode(wrapper, source);
					Var_Node methodMetaVarNode = pag.makeLocalVarNode(
							Jimple.v().newLocal("$Unknown", methodMetaClass.getType()), methodMetaClass.getType(), source);
					pag.addAllocEdge(methodMetaObj, methodMetaVarNode);
					// allocate array <=> Method[] mtds = new Method[1];
					NewArrayExpr newArrayExpr = new JNewArrayExpr(methodMetaClass.getType(), IntConstant.v(1));
					Alloc_Node arrayAllocNode = pag.makeAllocNode(newArrayExpr, newArrayExpr.getType(), source);
					Var_Node arrayNode = pag.findLocalVarNode(arrayBase);
					if(arrayNode == null)
						arrayNode = pag.makeLocalVarNode(arrayBase, arrayBase.getType(), source);
					pag.addAllocEdge(arrayAllocNode, arrayNode);
					// Array element store mtds[0] = m;
					FieldRef_Node arrayFieldRefNode = pag.makeFieldRefNode(arrayNode, ArrayElement.v());
					pag.addEdge(methodMetaVarNode, arrayFieldRefNode);
				}
			}
		}
	}
	
	public void getDeclaredMethods(ParameterizedMethod source, Stmt caller) {
		if(! (caller instanceof AssignStmt)) {
			return;
		}
		GetDeclaredMethodsCallInfo getDeclaredMethodsCallInfo = new GetDeclaredMethodsCallInfo(source.method(), caller, null);
		InstanceInvokeExpr invokeExpr = (InstanceInvokeExpr) ((AssignStmt) caller).getRightOp();
		Local baseObj = (Local) invokeExpr.getBase();
		Set<GetDeclaredMethodsCallInfo> getDeclaredMethodsCalls = clz2GetDeclaredMethodsCalls.get(baseObj);
		if(getDeclaredMethodsCalls == null) {
			clz2GetDeclaredMethodsCalls.put(baseObj, getDeclaredMethodsCalls = new HashSet<>());
		}
		getDeclaredMethodsCalls.add(getDeclaredMethodsCallInfo);
	}
	
	public void handleGetDeclaredMethodsSideEffect(Local l, Context ctx, GNode n) {
		Set<GetDeclaredMethodsCallInfo> updateGetDeclaredMethodsCalls = new HashSet<>();
		if(clz2GetDeclaredMethodsCalls.get(l) != null) {
			if(! (n instanceof ClassConstant_Node))
				return;
			ClassConstant_Node clzConstNode = (ClassConstant_Node) n;
			if(clzConstNode.getClassConstant().equals(GlobalVariable.v().getUnknownClzConst())) {
				for(GetDeclaredMethodsCallInfo c : clz2GetDeclaredMethodsCalls.get(l)) {
					c.add(GlobalVariable.v().getUnknownClass());
					updateGetDeclaredMethodsCalls.add(c);
				}
			} else {
				String clzName = clzConstNode.getClassConstant().value.replace('/', '.');
				if(!Scene.v().containsClass(clzName)) {
					if(options.verbose())
						G.v().out.println( "Warning: Class " + clzName + " is"+
							" a dynamic class, and you did not specify"+
							" it as such; graph will be incomplete!" );
				} else {
					SootClass clz = Scene.v().getSootClass(clzName);
					clz2GetDeclaredMethodsCalls.get(l)
								   .stream()
								   .forEach(c -> {
									   c.add(clz);
									   updateGetDeclaredMethodsCalls.add(c);
								   });
				}
			}
		}
		
		/**
		 * TODO
		 * The logic is similar to getMethods(). Abstract and simplify the code
		 * yifei
		 */
		for(GetDeclaredMethodsCallInfo c : updateGetDeclaredMethodsCalls) {
			Stmt caller = c.caller;
			Value arrayBase = ((AssignStmt) caller).getLeftOp();
			SootMethod source = c.source;
			SootClass methodMetaClass = Scene.v().getSootClass("java.lang.reflect.Method");
			for(SootClass clz : c.metaClzs) {
				SootMethod mtd = GlobalVariable.v().getUnknownMethod(clz);
				if(! c.targetMtds.contains(mtd)) {
					// allocate method metaobject allocation <=> Method m = Unknown();
					GetMethodWrapper wrapper = new GetMethodWrapper(clz, mtd, caller);
					wrapper.fromGetDeclaredMethod();
					Alloc_Node methodMetaObj = makeMethodMetaObjectNode(wrapper, source);
					Var_Node methodMetaVarNode = pag.makeLocalVarNode(
							Jimple.v().newLocal("$Unknown", methodMetaClass.getType()), methodMetaClass.getType(), source);
					pag.addAllocEdge(methodMetaObj, methodMetaVarNode);
					// allocate array <=> Method[] mtds = new Method[1];
					NewArrayExpr newArrayExpr = new JNewArrayExpr(methodMetaClass.getType(), IntConstant.v(1));
					Alloc_Node arrayAllocNode = pag.makeAllocNode(newArrayExpr, newArrayExpr.getType(), source);
					Var_Node arrayNode = pag.findLocalVarNode(arrayBase);
					if(arrayNode == null)
						arrayNode = pag.makeLocalVarNode(arrayBase, arrayBase.getType(), source);
					pag.addAllocEdge(arrayAllocNode, arrayNode);
					// Array element store mtds[0] = m;
					FieldRef_Node arrayFieldRefNode = pag.makeFieldRefNode(arrayNode, ArrayElement.v());
					pag.addEdge(methodMetaVarNode, arrayFieldRefNode);
				}
			}
		}
	}
	
	@Override
	public void methodInvoke(ParameterizedMethod source, Stmt caller) {
		InstanceInvokeExpr invokeExpr = null;
		Type postDomCast = null;
		if(caller instanceof AssignStmt) {
			invokeExpr = (InstanceInvokeExpr) ((AssignStmt) caller) .getInvokeExpr();
			// postDomCast is ignored if only constant strings are considered
			if(!ReflectionOptions.v().isConstStringOnly())
				postDomCast = PostDominantFinder.findPostDominator(source.method().retrieveActiveBody(), (AssignStmt) caller);
		} else if(caller instanceof InvokeStmt) {
			invokeExpr = (InstanceInvokeExpr) ((InvokeStmt) caller).getInvokeExpr();
		}
		Local mtdMetaObj = (Local) invokeExpr.getBase();
		Value receiver = invokeExpr.getArg(0);
		Value argListValue = invokeExpr.getArg(1);
		List<ArgList> argLists = null;
		// if argument list is an local variable, then we collect its elements
		// if not, we create a empty argument list
		if(argListValue instanceof Local) {
			argLists = processArgList(source.method(), (Local) argListValue);
		} else {
			argLists = new ArrayList<>();
			argLists.add(new ArgList(null, 0));
		}
//		System.out.println("======================================");
//		argLists.stream()
//				.forEach(System.out::println);
//		System.out.println("======================================");
		InvokeCallInfo invokeCall = new InvokeCallInfo(source.method(), caller, receiver, argLists, postDomCast, null);
		// update method meta object to Class.invoke call
		Set<InvokeCallInfo> invokeCalls = mtd2InvokeCalls.get(mtdMetaObj);
		if(invokeCalls == null) {
			mtd2InvokeCalls.put(mtdMetaObj, invokeCalls = new HashSet<>());
		}
		invokeCalls.add(invokeCall);
		// update receiver object to Class.invoke call
		if(receiver instanceof Local) {
			Local reveiverLocal = (Local) receiver;
			invokeCalls = receiverObj2IncokeCalls.get(reveiverLocal);
			if(invokeCalls == null) {
				receiverObj2IncokeCalls.put(reveiverLocal, invokeCalls = new HashSet<>());
			}
		}
		invokeCalls.add(invokeCall);
	}

	@Override
	public void classNewInstance(ParameterizedMethod source, Stmt s) {
		if(s instanceof AssignStmt) {
			Value newInstInvoke = ((AssignStmt) s).getRightOp();
			assert newInstInvoke instanceof InstanceInvokeExpr;
			InstanceInvokeExpr vInvokeExpr = (InstanceInvokeExpr) newInstInvoke;
			Local baseObj = (Local) vInvokeExpr.getBase();
			// add newInstance to virtual call sites 
			// find post dominant cast
			// if only constant string are considered, post ddominant cast is always null
			// yifei
			Type postDomCast = null;
			if(!ReflectionOptions.v().isConstStringOnly())
				postDomCast = PostDominantFinder.findPostDominator(source.method().retrieveActiveBody(), (AssignStmt)s);
			/*if(postDomCast != null && postDomCast.equals(Scene.v().getSootClass("android.content.BroadcastReceiver").getType())) {
				System.out.println("#### post-dominant cast " + postDomCast + " is ignored.");
				postDomCast = null;
			}*/				
			ClzNewInstCallInfo clzNewInstCallInfo = new ClzNewInstCallInfo(source.method(), s, postDomCast, null);
			Set<ClzNewInstCallInfo> clzNewInstCalls = clz2NewInstCalls.get(baseObj);
			if(clzNewInstCalls == null) {
				clz2NewInstCalls.put(baseObj, clzNewInstCalls = new HashSet<>());
			}
			clzNewInstCalls.add(clzNewInstCallInfo);
		}
	}
	
	public void getCtor(ParameterizedMethod source, Stmt s) {
		if(s instanceof AssignStmt) {
			Value ctorCall = ((AssignStmt) s).getRightOp();
			assert ctorCall instanceof InstanceInvokeExpr;
			Local baseObj = (Local) ((InstanceInvokeExpr) ctorCall).getBase();
			GetCtorCallInfo getCtor = new GetCtorCallInfo(source.method(), s, null);
			Set<GetCtorCallInfo> getCtorCalls = clz2GetCtorCalls.get(baseObj);
			if(getCtorCalls == null)
				clz2GetCtorCalls.put(baseObj, getCtorCalls = new HashSet<>());
			getCtorCalls.add(getCtor);
		}
	}
	
	public void getDeclaredCtor(ParameterizedMethod source, Stmt s) {
		if(s instanceof AssignStmt) {
			Value ctorCall = ((AssignStmt) s).getRightOp();
			assert ctorCall instanceof InstanceInvokeExpr;
			Local baseObj = (Local) ((InstanceInvokeExpr) ctorCall).getBase();
			GetDeclaredCtorCallInfo getDeclaredCtor = new GetDeclaredCtorCallInfo(source.method(), s, null);
			Set<GetDeclaredCtorCallInfo> getDeclaredCtorCalls = clz2GetDeclaredCtorCalls.get(baseObj);
			if(getDeclaredCtorCalls == null)
				clz2GetDeclaredCtorCalls.put(baseObj, getDeclaredCtorCalls = new HashSet<>());
			getDeclaredCtorCalls.add(getDeclaredCtor);
		}
	}
	
	public void getCtors(ParameterizedMethod source, Stmt s) {
		if(s instanceof AssignStmt) {
			Value ctorCall = ((AssignStmt) s).getRightOp();
			assert ctorCall instanceof InstanceInvokeExpr;
			Local baseObj = (Local) ((InstanceInvokeExpr) ctorCall).getBase();
			GetCtorsCallInfo getCtors = new GetCtorsCallInfo(source.method(), s, null);
			Set<GetCtorsCallInfo> getCtorsCalls = clz2GetCtorsCalls.get(baseObj);
			if(getCtorsCalls == null)
				clz2GetCtorsCalls.put(baseObj, getCtorsCalls = new HashSet<>());
			getCtorsCalls.add(getCtors);
		}
	}
	
	public void getDeclaredCtors(ParameterizedMethod source, Stmt s) {
		if(s instanceof AssignStmt) {
			Value ctorCall = ((AssignStmt) s).getRightOp();
			assert ctorCall instanceof InstanceInvokeExpr;
			Local baseObj = (Local) ((InstanceInvokeExpr) ctorCall).getBase();
			GetDeclaredCtorsCallInfo getCtors = new GetDeclaredCtorsCallInfo(source.method(), s, null);
			Set<GetDeclaredCtorsCallInfo> getCtorsCalls = clz2GetDeclaredCtorsCalls.get(baseObj);
			if(getCtorsCalls == null)
				clz2GetDeclaredCtorsCalls.put(baseObj, getCtorsCalls = new HashSet<>());
			getCtorsCalls.add(getCtors);
		}
	}
	
	@Override
	public void contructorNewInstance(ParameterizedMethod source, Stmt s) {
		if(! (s instanceof AssignStmt))
			return;
		InvokeExpr invokeExpr = s.getInvokeExpr();
		assert invokeExpr instanceof InstanceInvokeExpr;
		Local base = (Local) ((InstanceInvokeExpr) invokeExpr).getBase();
		Type postDomCast = null;
		if(!ReflectionOptions.v().isConstStringOnly())
			postDomCast = PostDominantFinder.findPostDominator(source.method().retrieveActiveBody(), (AssignStmt) s);
		CtorNewInstCallInfo ctorNewInstCall = new CtorNewInstCallInfo(source.method(), s, postDomCast, null);
		Set<CtorNewInstCallInfo> ctorNewInstCalls = clz2CtorNewInstCalls.get(base);
		if(ctorNewInstCalls == null)
			clz2CtorNewInstCalls.put(base, ctorNewInstCalls = new HashSet<>());
		ctorNewInstCalls.add(ctorNewInstCall);
	}

	@Override
	public void classForName(ParameterizedMethod source, Stmt s) {	
		if(! (s instanceof AssignStmt))
			return;
		Value clzName = s.getInvokeExpr().getArg(0);
		ForNameCallInfo forNameCall = new ForNameCallInfo(source.method(), s, null);
		Set<ForNameCallInfo> forNameCalls = clzName2ForNameCalls.get(clzName);
		if(forNameCalls == null) {
			clzName2ForNameCalls.put(clzName, forNameCalls = new HashSet<>());
			forNameCalls.add(forNameCall);
		}
		if(clzName instanceof StringConstant)
			constantForName(((StringConstant) clzName).value, forNameCall);
	}

	private void constantForName(String targetClzName, ForNameCallInfo forNameCall) {
		if(targetClzName.length() > 0 && targetClzName.charAt(0) == '[') {
			if(targetClzName.length() > 1 && targetClzName.charAt(1) == 'L' && targetClzName.charAt(targetClzName.length() - 1) == ';') {
				targetClzName = targetClzName.substring(2, targetClzName.length() - 1);
				constantForName(targetClzName, forNameCall);
			}
		} else {
			if(!Scene.v().containsClass(targetClzName)) {
				if( options.verbose() ) {
					G.v().out.println( "Warning: Class " + targetClzName + " is"+
							" a dynamic class, and you did not specify"+
							" it as such; graph will be incomplete!" );
				}
			} else {
				SootClass clz = Scene.v().getSootClass(targetClzName);
				if(! forNameCall.contains(clz)) {
					forNameCall.add(clz);
					SootMethod source = forNameCall.getSource();
					Stmt caller = forNameCall.getCaller();
					updateClinit(targetClzName, source, caller);
					updateClassMetaObjPTS(source, caller, targetClzName);
					Util.println("[InferenceReflectionModel] Class " + targetClzName + " is loaded via reflection.");
				}
			}
		}
	}
	
	private boolean isPossibleClassName(String clzName) {
		Pattern p = Pattern.compile("([a-zA-Z_$][a-zA-Z\\\\d_$]*\\\\.)*[a-zA-Z_$][a-zA-Z\\\\d_$]*");
		Matcher m = p.matcher(clzName);
		return m.matches();
	}

	private void updateClinit(String clzName, SootMethod source, Stmt caller) {
		SootClass targetClz = Scene.v().getSootClass(clzName);
		if(!targetClz.isPhantomClass()) {
			if(!targetClz.isApplicationClass()) {
				targetClz.setLibraryClass();
			}
			for(SootMethod clinit : EntryPoints.v().clinitsOf(targetClz)) {
				cgb.addStaticEdge(ParameterizedMethod.mtdWithNoCxt(source), caller, clinit, Kind.CLINIT);
			}
		}
	}

	private void updateClassMetaObjPTS(SootMethod source, Stmt caller, String targetClassName) {
		ClassConstant cc = null;
		if(targetClassName.equals(GlobalVariable.v().getUnknownName()))
			cc = GlobalVariable.v().getUnknownClzConst();
		else
			cc = ClassConstant.v(targetClassName.replace('.', '/'));
		if(caller instanceof AssignStmt) {
			Alloc_Node classConstant = pag.makeClassConstantNode(cc);
			AssignStmt assign = (AssignStmt) caller;
			Local metaClass = (Local) assign.getLeftOp();
			LocalVar_Node metaClassNode = pag.findLocalVarNode(metaClass);
			if(metaClassNode == null) {
				metaClassNode = pag.makeLocalVarNode(metaClass, metaClass.getType(), source);
			}
			pag.addAllocEdge(classConstant, metaClassNode);
		}
	}

	public void handleForNameSideEffect(Local arg, Context ctx, GNode clzNameNode) {
		// If node is a string constant node, get the string constant and
		// create corresponding class constant node
		// if not, create a Unknown class type which informs Class.newInstance
		// to use post dominant cast to decide what type of objects are created
		// yifei
		if(clzNameNode instanceof StringConstant_Node) {
			String clzName = ((StringConstant_Node) clzNameNode).getString();
			for(ForNameCallInfo forName : clzName2ForNameCalls.get(arg)) {
				constantForName(clzName, forName);
			}
		} else {
			for(ForNameCallInfo forName : clzName2ForNameCalls.get(arg)) {
				// String analysis
				if(ReflectionOptions.v().isStringAnalysis() && forName.source.method().getDeclaringClass().isApplicationClass()) {
					Set<String> possibleClassNames = StringAnalysisAdapter.v().getPossibleClassNames(forName);
					possibleClassNames.forEach(c -> constantForName(c, forName));
					// If string analysis is useful, which means it can determine the class name, then inference is unnecessary.
					// If it is useless, then we add unknown class name to enable inference.
					if(possibleClassNames.isEmpty()) {
						updateClassMetaObjPTS(forName.source, forName.caller, GlobalVariable.v().getUnknownName());
						Util.println("[InferenceReflectionModel] Class.forName call " + forName.caller + 
									" in method " + forName.source.getSignature() + " is unresolved.");
					}
				} else {
					updateClassMetaObjPTS(forName.source, forName.caller, GlobalVariable.v().getUnknownName());
					Util.println("[InferenceReflectionModel] Class.forName call " + forName.caller + 
								" in method " + forName.source.getSignature() + " is unresolved.");

				}
			}
		}

	}

	public boolean isInForNameCallSite(Local arg) {
		return clzName2ForNameCalls.get(arg) != null;
	}

	public boolean isInClzNewInstCallSite(Local receiver) {
		return clz2NewInstCalls.get(receiver) != null;
	}
	
	public boolean isInGetCtorCallSite(Local receiver) {
		return clz2GetCtorCalls.get(receiver) != null;
	}
	
	public boolean isInGetDeclaredCtorCallSite(Local receiver) {
		return clz2GetDeclaredCtorCalls.get(receiver) != null;
	}
	
	public boolean isInGetCtorsCallSite(Local receiver) {
		return clz2GetCtorsCalls.get(receiver) != null;
	}
	
	public boolean isInGetDeclaredCtorsCallSite(Local receiver) {
		return clz2GetDeclaredCtorsCalls.get(receiver) != null;
	}
	
	public boolean isInCtorNewInstCallSite(Local receiver) {
		return clz2CtorNewInstCalls.get(receiver) != null;
	}
	
	public boolean isInGetMtdCallSite(Local l) {
		return clz2GetMtdCalls.get(l) != null ||
				mtdName2GetMtdCalls.get(l) != null;
	}
	
	public boolean isInGetDeclaredMtdCallSite(Local l) {
		return clz2GetDeclaredMtdCalls.get(l) != null ||
				mtdName2GetDeclaredMtdCalls.get(l) != null;
	}

	public boolean isInGetMethodsCallSite(Local l) {
		return clz2GetMethodsCalls.get(l) != null;
	}
	
	public boolean isInGetDeclaredMethodsCallSite(Local l) {
		return clz2GetDeclaredMethodsCalls.get(l) != null;
	}
	
	public boolean isInInvokeCallSite(Local l) {
		return mtd2InvokeCalls.get(l) != null || 
				receiverObj2IncokeCalls.get(l) != null;
	}
	
	public void handleClzNewInstSideEffect(Local receiver, Context srcContext, Type type, GNode n) {
		// 1. find target classes
		// 2. find <init>
		// 3. create an object
		// 4. invoke corresponding <init> method
		//    yifei
		if(!(n instanceof ClassConstant_Node)) {
			return;
		}
		for(ClzNewInstCallInfo cNewInst : clz2NewInstCalls.get(receiver)) {
			// 1. find target classes
			Set<SootClass> targetClzs = new HashSet<>();
			// If class is unknown, resolve Class.newInstance with post dominant cast
			// If post dominant cast does not present, then this call site cannot be resolved
			// If class in known, resolve Class.newInstance with class meta object
			if(((ClassConstant_Node) n).getClassConstant().equals(GlobalVariable.v().getUnknownClzConst())) {
				if(cNewInst.postDomCast != null) {
					SootClass castClass = ((RefType) cNewInst.postDomCast).getSootClass();
					if(castClass == null) {
						if(options.verbose()) 
							G.v().out.println( "Warning: Class " + castClass + " is"+ 
								" a dynamic class, and you did not specify"+ 
				                " it as such; graph will be incomplete!" ); 
							Util.println("[InferenceReflectionModel] Class " +  
				                  cNewInst.postDomCast.toString() + " in Class.newInstance call "  
				                  + cNewInst.caller + " is not found."); 
					} else {
						// find subclasses of a given class in class hierarchy 
						// add itself and remove abstract (which cannot be initialized)
						targetClzs = Util.v()
										 .getSubClasses(castClass).stream()
										 .filter(SootClass::isConcrete)
										 .collect(Collectors.toSet());
						// output results
						for(ClzNewInstCallInfo newInstCall : clz2NewInstCalls.get(receiver)) {
							Util.println("[InferenceReflectionModel] Class.newInstance call " + newInstCall.caller + 
									   " is resolved by post-dominant cast. The following calsses are resolved here.");
							targetClzs.stream()
									  .map(SootClass::getName)
									  .sorted()
									  .forEach(Util::println);
						}
					}
				} else {
					if(ReflectionOptions.v().isLazyHeapModeling()) {
						handleUnknownObject(cNewInst, null);
					}
				}
			} else {
				String targetClzName = ((ClassConstant_Node) n).getClassConstant().value.replace('/', '.');
				targetClzs.add(Scene.v().getSootClass(targetClzName));
				// output necessary information
				for(ClzNewInstCallInfo newInstCall : clz2NewInstCalls.get(receiver)) {
					Util.println("[InferenceReflectionModel] Class.newInstance call " + 
								newInstCall.caller + " is resolved by Class " + targetClzName);
				}
			}
			
			// Target classes found. Do the following work
			handleClzNewInstSideEffect(cNewInst, targetClzs);
		}
	}
	
	void handleClzNewInstSideEffect(ClzNewInstCallInfo call, Set<SootClass> targetClasses) {
		SootMethod source = call.source;
		Stmt caller = call.caller;
		Set<SootClass> reachingClasses = call.reachingClasses;
		for(SootClass targetClz : targetClasses) {
			// 2. get <init>
			List<SootMethod> inits = 
					targetClz.getMethods()
					.stream()
					.filter(m -> m.getSubSignature().equals("void <init>()"))
					.collect(Collectors.toList());
			SootMethod init = null;
			if(inits.size() == 1) {
				init = inits.get(0);
			} else {
				Util.println("[InferenceReflectionModel] No <init>() of class " +
						targetClz.getName() + " in Class.newInstance call " + call.caller);
				continue;
			}

			// objects of the same type are created only once
			// in Class.newInstance() call site
			// TODO context sensitivity
			// yifei
			if(! reachingClasses.contains(targetClz)) {
				reachingClasses.add(targetClz);
				cgb.addVirtualEdge(ParameterizedMethod.mtdWithNoCxt(source), caller, init, Kind.REFL_CLASS_NEWINSTANCE, null);
			}
		}
	}
	
	private void handleCtorSideEffect(SmallNumberedMap<? extends Set<? extends CtorCallInfo>> calls, Local l, Context ctx, GNode n) {
		/**
		 * Handle the side effect of Class.getConstroctor()
		 * 1. find the getConstroctor calls that need to be updated
		 * 2. update points-to sets
		 */
		if(! (n instanceof ClassConstant_Node))
			return;
		ClassConstant_Node clzConstantNode = (ClassConstant_Node) n;
		SootClass baseClass = null;
		if(clzConstantNode.getClassConstant().equals(GlobalVariable.v().getUnknownClzConst())) {
			baseClass = GlobalVariable.v().getUnknownClass();
		} else {
			String clzName = clzConstantNode.getClassConstant().value.replace('/', '.');
			if(! Scene.v().containsClass(clzName)) {
				if(options.verbose())
					G.v().out.println( "Warning: Class " + clzName + " is"+
							" a dynamic class, and you did not specify"+
							" it as such; graph will be incomplete!" );
				return;
			}
			else
				baseClass = Scene.v().getSootClass(clzName);
		}
		// target class is found
		for(CtorCallInfo getCtorCallInfo : calls.get(l)) {
			if(! getCtorCallInfo.contains(baseClass)) {
				getCtorCallInfo.add(baseClass);
				SootMethod source = getCtorCallInfo.getSource();
				Stmt caller = getCtorCallInfo.getCaller();
				if(! (caller instanceof AssignStmt))
					return;
				Local ctorLocal = (Local) ((AssignStmt) caller).getLeftOp();
				ConstructorWrapper ctorWrapper = new ConstructorWrapper(baseClass, source, caller, getCtorCallInfo.isFromGetCtor);
				Alloc_Node ctorMetaObj = makeCtorMetaObjectNode(ctorWrapper);
				LocalVar_Node ctorMetaObjNode = pag.findLocalVarNode(ctorLocal);
				if(ctorMetaObjNode == null)
					ctorMetaObjNode = pag.makeLocalVarNode(ctorLocal, ctorLocal.getType(), source);
				pag.addAllocEdge(ctorMetaObj, ctorMetaObjNode);
			}
		}
	}

	public void handleGetCtorSideEffect(Local l, Context ctx, GNode n) {
		handleCtorSideEffect(clz2GetCtorCalls, l, ctx, n);
	}
	
	public void handleGetDeclaredCtorSideEffect(Local l, Context ctx, GNode n) {
		handleCtorSideEffect(clz2GetDeclaredCtorCalls, l, ctx, n);
	}
	
	/**
	 * Handle Class.getConstructors() side effect
	 * 1. Allocate the constructor meta-object
	 * 2. Allocate an array
	 * 3. Store the constructor meta-object into the array
	 */
	private void handleCtorsSideEffect(SmallNumberedMap<? extends Set<? extends CtorsCallInfo>> calls, Local l, Context ctx, GNode n) {
		if (! (n instanceof ClassConstant_Node))
			return;
		ClassConstant_Node clzConstantNode = (ClassConstant_Node) n;
		SootClass baseClass = null;
		// find target class
		if(clzConstantNode.getClassConstant().equals(GlobalVariable.v().getUnknownClzConst()))
			baseClass = GlobalVariable.v().getUnknownClass();
		else {
			String clzName = clzConstantNode.getClassConstant().value.replace('/', '.');
			if(! Scene.v().containsClass(clzName)) {
				if(options.verbose())
					G.v().out.println( "Warning: Class " + clzName + " is"+
							" a dynamic class, and you did not specify"+
							" it as such; graph will be incomplete!" );
				return;
			}
				else 
					baseClass = Scene.v().getSootClass(clzName);
		}
		// target class found. Allocate constructor node.
		SootClass ctorMetaClass = Scene.v().getSootClass("java.lang.reflect.Constructor");
		for(CtorsCallInfo getCtorCallInfo : calls.get(l)) {
			if(! getCtorCallInfo.contains(baseClass)) {
				getCtorCallInfo.add(baseClass);
				SootMethod source = getCtorCallInfo.getSource();
				Stmt caller = getCtorCallInfo.getCaller();
				if(! (caller instanceof AssignStmt))
					return;
				// array variable
				Value arrayBase = ((AssignStmt) caller).getLeftOp();
				// 1. allocate Constructor meta object
				ConstructorWrapper ctorWrapper = new ConstructorWrapper(baseClass, source, caller, getCtorCallInfo.isFromGetCtor);
				Alloc_Node ctorMetaObj = makeCtorMetaObjectNode(ctorWrapper);
				// TODO
				// Is there a better way to update points to set of an array
				Var_Node ctorMetaVarNode = pag.makeLocalVarNode(
						Jimple.v().newLocal("$ctor", ctorMetaClass.getType()), ctorMetaClass.getType(), source);
				pag.addAllocEdge(ctorMetaObj, ctorMetaVarNode);
				// 2. Allocate an array
				NewArrayExpr newArrayExpr = new JNewArrayExpr(ctorMetaClass.getType(), IntConstant.v(1));
				Alloc_Node arrayAllocNode = pag.makeAllocNode(newArrayExpr, newArrayExpr.getType(), source);
				Var_Node arrayNode = pag.findLocalVarNode(arrayBase);
				if(arrayNode == null)
					arrayNode = pag.makeLocalVarNode(arrayBase, arrayBase.getType(), source);
				pag.addAllocEdge(arrayAllocNode, arrayNode);
				// 3. Store array element
				FieldRef_Node arrayFieldRefNode = pag.makeFieldRefNode(arrayNode, ArrayElement.v());
				pag.addEdge(ctorMetaVarNode, arrayFieldRefNode);
			}
		}
	}
	
	public void handleGetCtorsSideEffect(Local l, Context ctx, GNode n) {
		handleCtorsSideEffect(clz2GetCtorsCalls, l, ctx, n);
	}
	
	public void handleGetDeclaredCtorsSideEffect(Local l, Context ctx, GNode n) {
		handleCtorsSideEffect(clz2GetDeclaredCtorsCalls, l, ctx, n);
	}
	
	public void handleCtorNewInstSideEffect(Local l, Context ctx, GNode n) {
		if(! (n instanceof Constructor_Node))
			return;
		Constructor_Node ctorNode = (Constructor_Node) n;
		SootClass baseClass = ctorNode.getBaseClass();
		for(CtorNewInstCallInfo c : clz2CtorNewInstCalls.get(l)) {
			// find target class
			Set<SootClass> targetClzs = new HashSet<>();
			if(baseClass.equals(GlobalVariable.v().getUnknownClass())) {
				// unknown is passed here
				if(c.postDomCast != null) {
					// post dominant cast exists
					SootClass castClass = ((RefType) c.postDomCast).getSootClass();
					if(castClass == null) {
						if(options.verbose())
							G.v().out.println( "Warning: Class " + castClass + " is"+
									" a dynamic class, and you did not specify"+
									" it as such; graph will be incomplete!" );
						Util.println("[InferenceReflectionModel] Class " + 
									c.postDomCast.toString() + " in Constructor.newInstance call " 
									+ c.caller + " is not found.");
					} else {
						// infer the targets by post dominant cast
						targetClzs = Util.v()
										 .getSubClasses(castClass)
										 .stream()
										 .filter(SootClass::isConcrete)
										 .collect(Collectors.toSet());
					}
				} else {
					// no post dominant cast
					if(ReflectionOptions.v().isLazyHeapModeling()) {
						handleUnknownObject(c, ctorNode);
					}
					continue;
				}
			} else {
				// class is known
				targetClzs.add(baseClass);
			}
			
			handleCtorNewInstSideEffect(c, ctorNode, targetClzs);
		}
	}
	
	void handleCtorNewInstSideEffect(CtorNewInstCallInfo call, Constructor_Node ctorNode, Set<SootClass> targetClasses) {
		SootMethod source = call.getSource();
		Stmt caller = call.getCaller();
		InstanceInvokeExpr invokeExpr = (InstanceInvokeExpr) caller.getInvokeExpr();
		/**
		 * if argument list is an local variable, then we collect its elements
		 * if not, we create a empty argument list
		 */
		Value argListValue = invokeExpr.getArg(0);
		List<ArgList> argLists = null;
		if(argListValue instanceof Local) {
			argLists = processArgList(source, (Local) argListValue);
		} else {
			argLists = new ArrayList<>();
			argLists.add(new ArgList(null, 0));
		}
		/**
		 * Infer the target constructor from the argument lists
		 */
		Set<SootMethod> targets = new HashSet<>();
		for(SootClass clz : targetClasses)
			for(ArgList argList : argLists)
				for(List<Arg> args : argList.args)
					if(ctorNode.isFromGetCtor())
						targets.addAll(
						findMethodsInClass(clz.getType(), null, args, new Predicate<SootMethod>() {
							@Override
							public boolean test(SootMethod t) {
								return t.isConstructor() && t.isPublic();
							}
						}));
					else
						targets.addAll(
						findMethodsInClass(clz.getType(), null, args, new Predicate<SootMethod>() {
							@Override
							public boolean test(SootMethod t) {
								return t.isConstructor();
							}
						}));
		/**
		 * Target constructor inference completed.
		 * Handling side effect.
		 */
		for(SootMethod init : targets)
			if(! call.contains(init)) {
				call.add(init);
				cgb.addVirtualEdge(ParameterizedMethod.mtdWithNoCxt(source), caller, init, Kind.REFL_CONSTR_NEWINSTANCE, null);
			}
	}
	
	/**
	 * Lazy heap modeling when the class meta-object is unknown and the post-dominant casting does not present.
	 */
	private void handleUnknownObject(StmtWithPos newInstCall, Constructor_Node ctorNode) {
		Stmt caller = newInstCall.getCaller();
		SootMethod source = newInstCall.getSource();
		if(! (caller instanceof AssignStmt))
			return;
		Local newObject = (Local) ((AssignStmt) caller).getLeftOp();
		UnknownObjectWrapper unknownObjectAllocSite = UnknownObjectWrapperFactory.v().getUnknownObject(newInstCall, ctorNode);
		Var_Node objectVarNode = pag.findLocalVarNode(newObject);
		if(objectVarNode == null)
			return;
		Alloc_Node unknownObjectAllocNode = makeUnknownObjectNode(source, unknownObjectAllocSite);
		pag.addAllocEdge(unknownObjectAllocNode, objectVarNode);
		Util.println("[InferenceReflectionModel] Lazy heap modeling at call site " + caller 
				+ " in method " + source + ".");
	}
	
	public void handleGetMtdSideEffect(Local l, Context ctx, GNode n) {
		// 1. find what variable is updated, class meta object,
		//		target method name or parameter list
		// 2. find target method
		// 3. allocate a method meta object
		// 4. update points to set

		// 1. find what variable is updated and update corresponding info
		Set<GetMtdCallInfo> updateGetMtdCalls = new HashSet<>();
		if(clz2GetMtdCalls.get(l) != null) {
			if(!(n instanceof ClassConstant_Node)) {
				return;
			}
			ClassConstant_Node clzConstNode = (ClassConstant_Node) n;
			// handle unknown class
			// insert a unknown class and a unknown method
			// because class is unknown, for instance method, we expect to infer target method
			// at Method.invoke call site by the dynamic type of receiver object
			// but for static method, we cannot infer it now
			if(clzConstNode.getClassConstant().equals(GlobalVariable.v().getUnknownClzConst())) {
				for(GetMtdCallInfo c : clz2GetMtdCalls.get(l)) {
					c.add(GlobalVariable.v().getUnknownClass());
					updateGetMtdCalls.add(c);
				}
			} else {
				String clzName = clzConstNode.getClassConstant().value.replace('/', '.');
				if(!Scene.v().containsClass(clzName)) {
					if(options.verbose())
						G.v().out.println( "Warning: Class " + clzName + " is"+
							" a dynamic class, and you did not specify"+
							" it as such; graph will be incomplete!" );
				} else {
					SootClass clz = Scene.v().getSootClass(clzName);
					clz2GetMtdCalls.get(l)
								   .stream()
								   .forEach(c -> {
									   c.add(clz);
									   updateGetMtdCalls.add(c);
								   });
				}
			}
		} else if(mtdName2GetMtdCalls.get(l) != null) {
			if(n instanceof StringConstant_Node) {
				// add constant string method name
				String mtdName = ((StringConstant_Node) n).getString();
				mtdName2GetMtdCalls.get(l)
								   .stream()
								   .forEach(c-> {
									   c.add(mtdName);
									   updateGetMtdCalls.add(c);
								   });
			} else if (n.getType().equals(RefType.v(Scene.v().getSootClass("java.lang.String")))) {
				// add Unknown method
				mtdName2GetMtdCalls.get(l)
								   .stream()
								   .filter(c -> !c.isMtdUnkown)
								   .forEach(c -> {
									   c.isMtdUnkown = true;
									   updateGetMtdCalls.add(c);
								   });
			}
		}
		
		// 2. find target method
		for(GetMtdCallInfo c : updateGetMtdCalls) {
			SootMethod source = c.source;
			Stmt caller = c.caller;
			// if method is unknown, put class and method meta object into Wrapper
			if(c.isMtdUnkown) {
				for(SootClass clz : c.metaClzs) {
					// String analysis
					if(ReflectionOptions.v().isStringAnalysis() && c.getSource().getDeclaringClass().isApplicationClass()) {
						Set<String> possibleMethodNames = StringAnalysisAdapter.v().getPossibleMethodNames(c, clz);
						// If string analysis is able to determine the method name, then inference is unnecessary.
						// If not, the unknown method meta-object is created to enable inference.
						// If class name is unknown, we always performs inference.
						if(possibleMethodNames.isEmpty()) {
							SootMethod unknownMtd = GlobalVariable.v().getUnknownMethod(clz);
							GetMethodWrapper wrapper = new GetMethodWrapper(clz, unknownMtd, caller);
							wrapper.fromGetMethod();
							updateMtdMetaObjPTS(source, caller, wrapper);
							c.targetMtds.add(unknownMtd);
							Util.println("[InferenceReflectionModel] Method name of Class.getMethod call " + 
									c.caller + " is unknown.");
						} else {
							for(String methodName : possibleMethodNames)
								for(SootMethod targetMethod : findMethodsInClassHierarchy(clz, methodName))
									if(! c.targetMtds.contains(targetMethod)) {
										// 3. update PTS of method meta object
										GetMethodWrapper wrapper = new GetMethodWrapper(clz, targetMethod, caller);
										wrapper.fromGetMethod();
										updateMtdMetaObjPTS(source, caller, wrapper);
										c.targetMtds.add(targetMethod);
										c.mtdNames.add(methodName);
									}
						}
					} else {
						SootMethod unknownMtd = GlobalVariable.v().getUnknownMethod(clz);
						GetMethodWrapper wrapper = new GetMethodWrapper(clz, unknownMtd, caller);
						wrapper.fromGetMethod();
						updateMtdMetaObjPTS(source, caller, wrapper);
						c.targetMtds.add(unknownMtd);
						Util.println("[InferenceReflectionModel] Method name of Class.getMethod call " + 
									c.caller + " is unknown.");
					}
				}
			} else {
				for(SootClass clz : c.metaClzs) {
					for(String name : c.mtdNames)
						if(clz.equals(GlobalVariable.v().getUnknownClass())) {
							SootMethod unknownMtdWithName = GlobalVariable.v().getUnknownMethod(name);
							if(! c.targetMtds.contains(unknownMtdWithName)) {
								// 3. passing the method name
								GetMethodWrapper wrapper = new GetMethodWrapper(clz, unknownMtdWithName, caller);
								wrapper.fromGetMethod();
								updateMtdMetaObjPTS(source, caller, wrapper);
								c.targetMtds.add(unknownMtdWithName);
							}
						} else {
							for(SootMethod targetMtd :findMethodsInClassHierarchy(clz, name))
								if(! c.targetMtds.contains(targetMtd)) {
									// 3. update PTS of method meta object
									GetMethodWrapper wrapper = new GetMethodWrapper(clz, targetMtd, caller);
									wrapper.fromGetMethod();
									updateMtdMetaObjPTS(source, caller, wrapper);
									c.targetMtds.add(targetMtd);
								}
						}
				}
			}
		}
	}

	public void handleGetDeclaredMtdSideEffect(Local l, Context ctx, GNode n) {
		Set<GetDeclaredMtdCallInfo> updateGetDeclaredMtdCalls = new HashSet<>();
		if(clz2GetDeclaredMtdCalls.get(l) != null) {
			if(!(n instanceof ClassConstant_Node)) {
				return;
			}
			ClassConstant_Node clzConstNode = (ClassConstant_Node) n;
			// handle unknown class
			if(clzConstNode.getClassConstant().equals(GlobalVariable.v().getUnknownClzConst())) {
				for(GetDeclaredMtdCallInfo c : clz2GetDeclaredMtdCalls.get(l)) {
					c.add(GlobalVariable.v().getUnknownClass());
					updateGetDeclaredMtdCalls.add(c);
				}
			} else {
				String clzName = clzConstNode.getClassConstant().value.replace('/', '.');
				if(!Scene.v().containsClass(clzName)) {
					if(options.verbose())
						G.v().out.println( "Warning: Class " + clzName + " is"+
							" a dynamic class, and you did not specify"+
							" it as such; graph will be incomplete!" );
				} else {
					SootClass clz = Scene.v().getSootClass(clzName);
					clz2GetDeclaredMtdCalls.get(l)
										   .stream()
										   .forEach(c -> {
											   c.add(clz);
											   updateGetDeclaredMtdCalls.add(c);
										   });
				}
			}
		} else if(mtdName2GetDeclaredMtdCalls.get(l) != null) {
			if(n instanceof StringConstant_Node) {
				// add constant string method name
				String mtdName = ((StringConstant_Node) n).getString();
				mtdName2GetDeclaredMtdCalls.get(l)
										   .stream()
										   .forEach(c-> {
											   c.add(mtdName);
											   updateGetDeclaredMtdCalls.add(c);
										   });
			} else if (n.getType().equals(RefType.v(Scene.v().getSootClass("java.lang.String")))) {
				// add Unknown method
				mtdName2GetDeclaredMtdCalls.get(l)
										   .stream()
										   .filter(c -> !c.isMtdUnkown)
										   .forEach(c -> {
											   c.isMtdUnkown = true;
											   updateGetDeclaredMtdCalls.add(c);
										   });
			}
		}
		
		// 2. find target method
		for(GetDeclaredMtdCallInfo c : updateGetDeclaredMtdCalls) {
			SootMethod source = c.source;
			Stmt caller = c.caller;
			// if method is unknown, put class and method meta object into Wrapper
			if(c.isMtdUnkown) {
				for(SootClass clz : c.metaClzs) {
					// String analysis
					if(ReflectionOptions.v().isStringAnalysis() && c.getSource().getDeclaringClass().isApplicationClass()) {
						Set<String> possibleMethodNames = StringAnalysisAdapter.v().getPossibleMethodNames(c, clz);
						c.mtdNames.addAll(possibleMethodNames);
						// If string analysis is able to determine the method name, then inference is unnecessary.
						// If not, the unknown method meta-object is created to enable inference.
						// If class name is unknown, we always performs inference.
						if(possibleMethodNames.isEmpty()) {
							SootMethod unknownMtd = GlobalVariable.v().getUnknownMethod();
							GetMethodWrapper wrapper = new GetMethodWrapper(clz, unknownMtd, caller);
							wrapper.fromGetDeclaredMethod();
							updateMtdMetaObjPTS(source, caller, wrapper);
							c.targetMtds.add(unknownMtd);
							Util.println("[InferenceReflectionModel] Method name of Class.getDeclaredMethod call " + 
										c.caller + " is unknown.");
						} else {
							for(String methodName : possibleMethodNames)
								for(SootMethod targetMethod : findMethodsInClass(clz, methodName))
									if(! c.targetMtds.contains(targetMethod)) {
										// 3. update PTS of method meta object
										GetMethodWrapper wrapper = new GetMethodWrapper(clz, targetMethod, caller);
										updateMtdMetaObjPTS(source, caller, wrapper);
										c.targetMtds.add(targetMethod);
										c.mtdNames.add(methodName);
									}
						}
					} else {
						SootMethod unknownMtd = GlobalVariable.v().getUnknownMethod();
						GetMethodWrapper wrapper = new GetMethodWrapper(clz, unknownMtd, caller);
						wrapper.fromGetDeclaredMethod();
						updateMtdMetaObjPTS(source, caller, wrapper);
						c.targetMtds.add(unknownMtd);
						Util.println("[InferenceReflectionModel] Method name of Class.getDeclaredMethod call " + 
									c.caller + " is unknown.");
					}
				}
			} else {
				for(SootClass clz : c.metaClzs) {
					for(String name : c.mtdNames)
						if(clz.equals(GlobalVariable.v().getUnknownClass())) {
							SootMethod unknownMtdWithName = GlobalVariable.v().getUnknownMethod(name);
							if(! c.targetMtds.contains(unknownMtdWithName)) {
								// 3. pass the method name
								GetMethodWrapper wrapper = new GetMethodWrapper(clz, unknownMtdWithName, caller);
								wrapper.fromGetDeclaredMethod();
								updateMtdMetaObjPTS(source, caller, wrapper);
								c.targetMtds.add(unknownMtdWithName);
							}
						} else
							for(SootMethod targetMtd :findMethodsInClass(clz, name))
								if(!c.targetMtds.contains(targetMtd)) {
									// 3. update PTS of method meta object
									GetMethodWrapper wrapper = new GetMethodWrapper(clz, targetMtd, caller);
									wrapper.fromGetDeclaredMethod();
									updateMtdMetaObjPTS(source, caller, wrapper);
									c.targetMtds.add(targetMtd);
								}
				}
			}
		}
	}
	
	public void handleMethodInvokeSideEffect(Local l, Context ctx, GNode n) {
		// 1. find what variable is updated, method meta object or receiver object
		// dispatching method
		Set<InvokeCallInfo> updateInvokeCalls = new HashSet<>();
		// add newly passed method meta object
		if(mtd2InvokeCalls.get(l) != null) {
			if(!(n instanceof MethodMetaObject_Node)) {
				return;
			}
			MethodMetaObject_Node mtdMetaNode = (MethodMetaObject_Node) n;
			for(InvokeCallInfo c : mtd2InvokeCalls.get(l)) {
				c.add(mtdMetaNode.getTargetMethod());
				c.add(mtdMetaNode.getBaseClass());
				c.add(mtdMetaNode);
				updateInvokeCalls.add(c);
			}
		}
		// add newly passed receiver object
		if(receiverObj2IncokeCalls.get(l) != null) {
			if(!(n instanceof Alloc_Node)) {
				return;
			}
			receiverObj2IncokeCalls.get(l)
								   .stream()
								   .forEach(c -> {
									   c.add((Alloc_Node) n);
									   updateInvokeCalls.add(c);
								   });
		}
		
		// Lazy heap modeling
		// Infer the receiver type by the method class type
		for(InvokeCallInfo call : updateInvokeCalls) {
			for(Alloc_Node receiverAllocNode : call.receiverAllocNodes)
				if(receiverAllocNode instanceof UnknownObject_Node) {
					Set<SootClass> methodClassTypes = call.baseClasses
							.stream()
							.filter(c -> { return ! c.equals(GlobalVariable.v().getUnknownClass()); })
							.collect(Collectors.toSet());
					LazyHeapHander.handleMethodInvoke((UnknownObject_Node) receiverAllocNode, methodClassTypes);
				}
		}
		
		for(InvokeCallInfo c : updateInvokeCalls) {
			Set<SootMethod> targetMtds = null;
			Set<SootMethod> reachableMtds = c.targetMtds;
			if(isContainUnknownMtd(reachableMtds)) {
				// if only constant string is considered, no inference, and unknown method
				// need to be filter out
				if(ReflectionOptions.v().isConstStringOnly()) {
					targetMtds = reachableMtds.stream()
											  .filter(m -> 
												  ! (m.getDeclaringClass().equals(GlobalVariable.v().getUnknownClass()) ||
												  m.getName().equals(GlobalVariable.v().getUnknownName())))
											  .collect(Collectors.toSet());
				} else {
					targetMtds = infer(c);
					// if the names of all the reachable methods are known, the inference results
					// are filtered out with the reachable names
					boolean reachableMtdNameAllKnown = true;
					for(SootMethod m : reachableMtds) {
						if(m.getName().equals(GlobalVariable.v().getUnknownName()))
							reachableMtdNameAllKnown = false;
					}
					if(reachableMtdNameAllKnown) {
						Set<String> mtdNames = 
								reachableMtds.stream()
											 .map(SootMethod::getName)
											 .collect(Collectors.toSet());
						targetMtds.removeIf(m -> ! mtdNames.contains(m.getName()));
					}
					Util.println("## inferred methods ");
					targetMtds.stream()
							  .map(SootMethod::getSignature)
							  .forEach(Util::println);
				}
			} else {
				targetMtds = reachableMtds;
			}
			for(SootMethod targetMtd : targetMtds) {
				for(Alloc_Node receiverAllocNode : c.receiverAllocNodes) {
					Type dynamicType = receiverAllocNode.getType();
					for(ArgList argList : c.argLists) {						
						SootMethod concrete = null;
						// if method is static, no dispatching is needed 
						// if not, virtual method need to be dispatching 
						// w.r.t. type of receiver object
						if(!targetMtd.isStatic()) {
							// TODO bugs here
							// Method defined in subclass cannot be invoked on
							// object of superclass
							concrete = dispatching(dynamicType, targetMtd);
							if(concrete == null) {
								Util.println("dispatching failed");
								Util.println("dispatched method" + targetMtd.getSignature());
								Util.println("dynamic type " + dynamicType);
								Util.println("In method " + c.source.getSignature());
								continue;
							}
							Util.println("=================================");
							Util.println("Test dispatching");
							Util.println("Target method " + targetMtd.getSignature());
							Util.println("Dispathed method" + concrete.getSignature());
							Util.println("=================================");
							addCallEdge(c, dynamicType, concrete, argList);
						}
					}
				}
				if(targetMtd.isStatic()) {
					for(ArgList argList : c.argLists) {
						addCallEdge(c, null, targetMtd, argList);
					}
				}
			}
		}
	}
	
	private boolean isContainUnknownMtd(Set<SootMethod> mtds) {
		for(SootMethod m : mtds) {
			if(m.getDeclaringClass().equals(GlobalVariable.v().getUnknownClass()) ||
					m.getName().equals(GlobalVariable.v().getUnknownName()))
				return true;
		}
		return false;
	}
	
	// static methods do not have receiver object, then add static edge directly
	// parameter and argument list matching is performed here
	private void addCallEdge(InvokeCallInfo c, Type dynamicType, SootMethod target, ArgList argList) {
		if(target.isStatic()) {
			if(target.getParameterCount() == 0) {
				addStaticEdge(c, target, new ArrayList<>());
			} else {
				List<List<Arg>> args = matching(target, argList);
				for(List<Arg> actualArgs : args) {
					List<Value> actualArg = processArgs(target.getParameterTypes(), actualArgs);
					addStaticEdge(c, target, actualArg);
				}
			}
		} else {
			if(target.getParameterCount() == 0) {
				addVirtualEdge(c, dynamicType, target, new ArrayList<>());
			} else {
				List<List<Arg>> args = matching(target, argList);
				for(List<Arg> actualArgs : args) {
					List<Value> actualArg = processArgs(target.getParameterTypes(), actualArgs);
					addVirtualEdge(c, dynamicType, target, actualArg);
				}
			}
		}
	}
	
	// parameters may be primitive type whereas arguments always are reference type
	// if parameters are primitive type, we add a null
	private List<Value> processArgs(List<Type> params, List<Arg> args) {
		List<Value> actualArgs = new ArrayList<>(params.size());
		for(int i = 0; i < params.size(); i++) {
			if(params.get(i) instanceof PrimType) {
				actualArgs.add(NullConstant.v());
			} else {
				actualArgs.add(args.get(i).getVar());
			}
		}
		return actualArgs;
	}
	
	// add static call edges
	private void addStaticEdge(InvokeCallInfo c, SootMethod target, List<Value> args) {
		SootMethod source = c.source;
		Stmt reflCaller = c.caller;
		InvokeExpr invokeExpr = new JStaticInvokeExpr(target.makeRef(), args);
		for(InvokeExpr e : c.invokeExprs)
			if(e.equivTo(invokeExpr))
				return;
		c.invokeExprs.add(invokeExpr);
		Unit caller = getReflectiveCallCaller(c, invokeExpr);
		
		/**
		 * @author yifei
		 * Because FlowDroid filter out call graph edges with REFL_INVOKE kind,
		 * in order to avoid modifying core code of FlowDroid, currently,
		 * the invoking statement of target methods are inserted into the body of 
		 * containing method.
		 * 
		 * This issue does not exist in Java application.
		 */
		if(ReflectionOptions.v().isForFlowDroid()) {
			// Invoke statement is inserted into body of the containing method
			// source.retrieveActiveBody().getUnits().insertAfter(caller, reflCaller);
			HandleTgtMtdInvocation.handleTgtMtdInvocation(c, caller);
			cgb.addStaticEdge(ParameterizedMethod.mtdWithNoCxt(source), caller, target, Kind.STATIC);
		} else {
			cgb.addStaticEdge(ParameterizedMethod.mtdWithNoCxt(source), reflCaller, target, Kind.REFL_INVOKE);
		}
		// add <clinit> calls that might be invoked here
		for(SootMethod clinit : EntryPoints.v().clinitsOf(target.getDeclaringClass()))
			cgb.addStaticEdge(ParameterizedMethod.mtdWithNoCxt(source), reflCaller, clinit, Kind.CLINIT);
		Util.println("[InferenceReflectionModel] Add static call to " + target.getSignature() + 
				" in method " + source.toString() + ", reflective caller " + c.caller + 
				"real caller " + caller);
	}
	
	// add virtual edge
	// caller is constructed with DYNAMIC type of receiver object and target method
	private void addVirtualEdge(InvokeCallInfo c, Type dynamicType, SootMethod target, List<Value> args) {
		SootMethod source = c.source;
		Stmt reflCaller = c.caller;
		Value receiver = c.receiver;
		SootMethodRef mtdRef = Scene.v().makeMethodRef(target.getDeclaringClass(), 
				target.getName(), target.getParameterTypes(), target.getReturnType(), false);
		InvokeExpr invokeExpr = new JVirtualInvokeExpr(receiver, mtdRef, args);
		// no same calls in one call site
		for(InvokeExpr e : c.invokeExprs)
			if(e.getMethod().equals(target) && e.getMethodRef().declaringClass().equals(target.getDeclaringClass()))
				return;
		c.invokeExprs.add(invokeExpr);
		Unit caller = getReflectiveCallCaller(c, invokeExpr);
		
		// see comment in addStaticEdge()
		if(ReflectionOptions.v().isForFlowDroid()) {
			// source.retrieveActiveBody().getUnits().insertAfter(caller, reflCaller);
			HandleTgtMtdInvocation.handleTgtMtdInvocation(c, caller);
			cgb.addVirtualEdge(ParameterizedMethod.mtdWithNoCxt(source), caller, target, Kind.VIRTUAL, null);
		} else {
			cgb.addVirtualEdge(ParameterizedMethod.mtdWithNoCxt(source), reflCaller, target, Kind.REFL_INVOKE, null);
		}
		Util.println("[InferenceReflectionModel] Add virtual call to " + target.getSignature() + 
				" in method " + source.toString() + ", reflective caller " + c.caller + 
				", real caller " + caller);
	}
	
	// generate method invocation statement. It may be assignment statement or invoke statement
	public Unit getReflectiveCallCaller(InvokeCallInfo c, InvokeExpr invokeExpr) {
		Unit caller = null;
		Type retType = invokeExpr.getMethod().getReturnType();
		if(c.getCaller() instanceof AssignStmt && ! retType.equals(VoidType.v()) && 
				! (retType instanceof PrimType)) {
			Value left = ((AssignStmt) c.getCaller()).getLeftOp();
			caller = new JAssignStmt(left, invokeExpr);
		} else {
			caller =  new JInvokeStmt(invokeExpr);
		}
		return caller;
	}
	
	private Set<SootMethod> infer(InvokeCallInfo c) {
		Set<SootMethod> targets = new HashSet<>();
		boolean isMtdFromGetMethod = false;
		boolean isMtdFromGetDeclaredMethod = false;
		// find the origins of Method meta object
		if(c.containsMethodFromGetMtd())
			isMtdFromGetMethod = true;
		if(c.containsMethodFromGetDeclaredMtd())
			isMtdFromGetDeclaredMethod = true;
		Type postDomCast = c.postDomCast;
		// infer instance methods from the dynamic type of receiver object
		for(Alloc_Node receiverAllocNode : c.receiverAllocNodes) {
			Type dynamicType = receiverAllocNode.getType();
			for(ArgList argList : c.argLists) {
				for(List<Arg> args : argList.args) {
					if(isMtdFromGetMethod)
						targets.addAll(resolveGetInstanceMethod(dynamicType, postDomCast, args));
					if(isMtdFromGetDeclaredMethod)
						targets.addAll(resolveGetDeclaredInstanceMethod(dynamicType, postDomCast, args));
					if(!isMtdFromGetMethod && ! isMtdFromGetDeclaredMethod) {
						System.out.println("# Error.");
						System.exit(0);
					}
				}
			}
		}
		// infer static methods from the base classes
		for(SootClass baseClass : c.baseClasses) {
			for(ArgList argList : c.argLists) {
				for(List<Arg> args : argList.args) {
					if(!baseClass.equals(GlobalVariable.v().getUnknownClass())) {
						if(isMtdFromGetMethod)
							targets.addAll(resolveGetStaticMethod(baseClass, postDomCast, args));
						if(isMtdFromGetDeclaredMethod)
							targets.addAll(resolveGetDeclaredStaticMethod(baseClass, postDomCast, args));
						if(!isMtdFromGetMethod && ! isMtdFromGetDeclaredMethod) {
							System.out.println("# Error.");
							System.exit(0);
						}
					} else {
						Util.println("[InferenceReflectionModel] Meta class is unkonwn, " +
									"static method cannot be inferred at call " + c.caller);
					}
				}
			}
		}
		return targets;
	}
	
	// target methods are public, concrete, non-static, and they are searched along class hierarchy
	private Set<SootMethod> resolveGetInstanceMethod(Type dynamicType, Type postDomCast, List<Arg> args) {
		return findMethodsInClassHierarchy(dynamicType, postDomCast, args, 
				new Predicate<SootMethod>() {
					@Override
					public boolean test(SootMethod m) {
						return m.isConcrete() && m.isPublic() && 
						!m.isStatic() && !m.getName().contains("<init>");
					}
			});
	}
	
	// target methods are concrete, non-static, and they are searched in given class
	private Set<SootMethod> resolveGetDeclaredInstanceMethod(Type dynamicType, Type postDomCast, List<Arg> args) {
		return findMethodsInClass(dynamicType, postDomCast, args, 
				new Predicate<SootMethod>() {
					@Override
					public boolean test(SootMethod m) {
						return m.isConcrete() && m.getDeclaringClass().getType().equals(dynamicType) && 
						!m.isStatic() && !m.getName().contains("<init>");
					}
			});
	}
	
	// target methods are public, static, and they are searched along class hierarchy
	private Set<SootMethod> resolveGetStaticMethod(SootClass baseClass, Type postDomCast, List<Arg> args) {
		return findMethodsInClassHierarchy(baseClass.getType(), postDomCast, args, 
				new Predicate<SootMethod>() {
					@Override
					public boolean test(SootMethod m) {
						return m.isPublic() && m.isStatic() && !m.getName().contains("<clinit>");
					}
		});
	}
	
	// target methods are static, and they are searched in given class
	private Set<SootMethod> resolveGetDeclaredStaticMethod(SootClass baseClass, Type postDomCast, List<Arg> args) {
		return findMethodsInClass(baseClass.getType(), postDomCast, args, 
				new Predicate<SootMethod>() {
					@Override
					public boolean test(SootMethod m) {
						return m.isStatic() && !m.getName().contains("<clinit>");
					}
		});
	}
	
	// find method with given return type and parameter type from the dynamic type of receiver object
	private Set<SootMethod> findMethodsInClassHierarchy(Type dynamicType, Type retPostDomCast, List<Arg> args, Predicate<SootMethod> pred) {
		Set<SootMethod> mtds = new HashSet<>();
		SootClass clazz = ((RefType) dynamicType).getSootClass();
		for(SootMethod mtd : clazz.getMethods()) {
			boolean paraMatch = false;
			boolean retMatch = false;
			if(pred.test(mtd) && mtd.getParameterCount() == args.size()) {
				List<Type> paraTypes = mtd.getParameterTypes();
				if(matching(paraTypes, args))
					paraMatch = true;
			}
			if(retPostDomCast == null)
				retMatch = true;
			else if(castRetTypeMatching(retPostDomCast, mtd.getReturnType()))
				retMatch = true;
			if(paraMatch && retMatch) {
				mtds.add(mtd);
			}
		}
		// if current class is not Object and it has super class, then continue search
		// in super class. remove found methods which are overwritten in subclass 
		if(!clazz.equals(Scene.v().getSootClass("java.lang.Object")) &&
				clazz.hasSuperclass()) {
			Set<SootMethod> mtdFromSuperClz = findMethodsInClassHierarchy(clazz.getSuperclass().getType(), retPostDomCast, args, pred);
			for(Iterator<SootMethod> superIter = mtdFromSuperClz.iterator(); superIter.hasNext(); ) {
				SootMethod superMtd = superIter.next();
				for(SootMethod m : mtds)
					if(m.getSubSignature().equals(superMtd.getSubSignature()))
						superIter.remove();
			}
			// add the searching results from super class to final results
			mtds.addAll(mtdFromSuperClz);
		}
		return mtds;
	}
	
	private Set<SootMethod> findMethodsInClass(Type dynamicType, Type retPostDomCast, List<Arg> args, Predicate<SootMethod> pred) {
		Set<SootMethod> mtds = new HashSet<>();
		SootClass clazz = ((RefType) dynamicType).getSootClass();
		for(SootMethod mtd : clazz.getMethods()) {
			boolean paraMatch = false;
			boolean retMatch = false;
			if(pred.test(mtd) && mtd.getParameterCount() == args.size()) {
				List<Type> paraTypes = mtd.getParameterTypes();
				if(matching(paraTypes, args))
					paraMatch = true;
			}
			if(retPostDomCast == null)
				retMatch = true;
			else if(castRetTypeMatching(retPostDomCast, mtd.getReturnType()))
				retMatch = true;
			if(paraMatch && retMatch) {
				mtds.add(mtd);
			}
		}
		return mtds;
	}
	
	// match casting type and return type
	private boolean castRetTypeMatching(Type castType, Type retType) {
		// if casting type or return type are primitive type, they need to be the same
		if(castType instanceof PrimType)
			castType = ((PrimType) castType).boxedType();
		if(retType instanceof PrimType)
			retType = ((PrimType) retType).boxedType();
		// cast type can be the super type of return type
		return fastHierarchy.canStoreType(retType, castType);
	}
	
	// dispatching virtual method
	private SootMethod dispatching(Type dynamicType, SootMethod m) {
		if(dynamicType instanceof RefType) {
			SootMethod mtd = null;
			try {
				mtd = fastHierarchy.resolveConcreteDispatch(((RefType) dynamicType).getSootClass(), m);
			} catch (RuntimeException e) {
				Util.println("Dispatching failed.");
			}
			return mtd;
		}
		return null;
	}
	
	// matching parameters and presenting all the possible argument lists
	public List<List<Arg>> matching(SootMethod target, ArgList argList) {
		List<List<Arg>> matchingArgLists = new ArrayList<>(1);
		List<Type> paraList = target.getParameterTypes();
		if(paraList.size() != argList.length) {
			return matchingArgLists;
		} else {
			for(List<Arg> args : argList.args) {
				if(matching(paraList, args)) {
					matchingArgLists.add(args);
				}
			}
		}
//		System.out.println("==============================");
//		System.out.println("Matching args");
//		System.out.println(matchingArgLists.toString());
//		System.out.println("==============================");
		return matchingArgLists;
	}
	
	// matching one parameter list and one argument list
	private boolean matching(List<Type> paraList, List<Arg> argList) {
		assert paraList.size() == argList.size();
		for(int i = 0; i < paraList.size(); i++) {
			if(!matching(paraList.get(i), argList.get(i).declaringType)) {
				return false;
			}
		}
		return true;
	}
	
	// matching one parameter and one argument
	private boolean matching(Type paraType, Type argType) {
		// Parameter type can be primitive type, but argument type
		// always is reference type
		// If parameter is primitive type, assigning between it and 
		// its corresponding wrapper type is feasible
		if(paraType instanceof PrimType) {
			Type boxedPrimType = ((PrimType) paraType).boxedType();
			// If they are the same
			if(boxedPrimType.equals(argType))
				return true;
			// Widening primitive type conversion 
			else if(wideningPrimTypes.get(boxedPrimType).contains(argType))
				return true;
			else if(argType.equals(RefType.v("java.lang.Object")))
				return true;
			else
				return false;
		} else {
			if(fastHierarchy.canStoreType(paraType, argType) ||
					fastHierarchy.canStoreType(argType, paraType))
				return true;
			else
				return false;
		}
	}
	
	private void updateMtdMetaObjPTS(SootMethod source, Stmt caller, GetMethodWrapper wrapper) {
		Local mtdMetaLocal = (Local) ((AssignStmt) caller).getLeftOp();
		Alloc_Node mtdMetaObj = makeMethodMetaObjectNode(wrapper, source);
		LocalVar_Node metaMtdNode = pag.findLocalVarNode(mtdMetaLocal);
		if(metaMtdNode == null) {
			metaMtdNode = pag.makeLocalVarNode(mtdMetaLocal, mtdMetaLocal.getType(), source);
		}
		pag.addAllocEdge(mtdMetaObj, metaMtdNode);
//			MethodPAG.v(pag, source).nodeFactory().setResult(metaMtdNode);
	} 
	
	// Find method with name in class hierarchy
	// Note that we only consider method name when handling Class.getMethod
	private Set<SootMethod> findMethodsInClassHierarchy(SootClass base, String mtdName) {
		// find method with given name from all the PUBLIC methods in current class
		// <clinit> and <init> are illegal method names 
		if(mtdName.equals("<clinit>") || mtdName.equals("<init>"))
			return new HashSet<>(1);
		if(base == null)
			return new HashSet<>(1);
		// all the possible public methods
		Set<SootMethod> mtds = base.getMethods()
								   .stream()
								   .filter(SootMethod::isPublic)
								   .filter(m -> m.getName().equals(mtdName))
								   .collect(Collectors.toSet());
		
		// find target method in super class
		if(!base.equals(Scene.v().getSootClass("java.lang.Object")))
			mtds.addAll(findMethodsInClassHierarchy(base.getSuperclass(), mtdName));
		for(SootClass i : base.getInterfaces())
			mtds.addAll(findMethodsInClassHierarchy(i, mtdName));
		return mtds;
	}

	// get methods declaring in given class with name
	private Set<SootMethod> findMethodsInClass(SootClass target, String mtdName) {
		if(mtdName.equals("<clinit>") || mtdName.equals("<init>"))
			return new HashSet<>(1);
		Set<SootMethod> mtds = new HashSet<>();
		for(SootMethod mtd : target.getMethods()) {
			if(mtdName.equals(mtd.getName()) && mtd.getDeclaringClass().equals(target)) {
				mtds.add(mtd);
			}
		}
		return mtds;
	}
	
	// process intra-procedural argument list
	private List<ArgList> processArgList(SootMethod source, Local argListLocal) {		
		List<ArgList> argLists = new ArrayList<>();
		// find all the assign statements in given method
		List<AssignStmt> assignStmts = 
				source.retrieveActiveBody()
					  .getUnits()
					  .stream()
					  .filter(u -> u instanceof AssignStmt)
					  .map(u -> (AssignStmt) u)
					  .collect(Collectors.toList());		
		// find all the definition of an array of an argument list
		// add construct an object of ArgList and put it into a list
		for(AssignStmt assign : assignStmts) {
			if(assign.getLeftOp().equals(argListLocal) && 
					assign.getRightOp() instanceof NewArrayExpr) {
				NewArrayExpr newArgList = (NewArrayExpr) assign.getRightOp();
				// we only handle array with constant length
				if(newArgList.getSize() instanceof IntConstant) {
					int length = ((IntConstant) newArgList.getSize()).value;
					argLists.add(new ArgList(assign, length));
				}
			}
		}
		
		// find all the references of argument lists in argLists
		// TODO some elements of argument list may not be stored
		for(ArgList argList : argLists) {
			for(AssignStmt assignStmt : assignStmts) {
				if(assignStmt.getLeftOp() instanceof ArrayRef && 
						((ArrayRef) assignStmt.getLeftOp()).getBase().equals(argListLocal)) {
					ArrayRef arrayRef = (ArrayRef) assignStmt.getLeftOp();
					if(arrayRef.getIndex() instanceof IntConstant) {
						int index = ((IntConstant) arrayRef.getIndex()).value;
						// because an array reference may be initialized by different array with 
						// varied length, we over-approximate the element stored into an array can 
						// reach every definition pointed by current array reference, as long as 
						// the index is bound
						if(argList.length > index) {
							Value rightVar = assignStmt.getRightOp();
							Type rightType = rightVar.getType();
							argList.add(index, new Arg(rightType, rightVar));
						}
					}
				}
			}
		}
		
		// if some argument is not assigned, we assign it with a null
		for(ArgList argList : argLists)
			for(int i = 0; i < argList.length; i++)
				if(argList.get(i).isEmpty())
					argList.add(i, new Arg(NullType.v(), new JimpleLocal("null", NullType.v())));
		
		// calculate the all the possible combination of argument lists
		for(ArgList argList : argLists) {
			if(argList.length != 0)
				argList.args = cartesianProduct(argList.args);
			argList.processed = true;
		}
		return argLists;
	}
	
	// calculate Cartesian product
	private static <T> List<List<T>> cartesianProduct(List<List<T>> lists) {
		if(lists.size() < 1)
			return lists;
		else
			return _cartesianProduct(0, lists);
	}

	private static <T> List<List<T>> _cartesianProduct(int index, List<List<T>> lists) {
		List<List<T>> result = new ArrayList<List<T>>();
		if(index == lists.size())
			result.add(new ArrayList<T>());
		else 
			for(T o : lists.get(index))
				for(List<T> list : _cartesianProduct(index + 1, lists)) {
					list.add(0, o);
					result.add(list);
				}
		return result;
	}
	//DA: Adapt for new CallGraphBuilder
	@Override
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
		else if(methRefSig.equalsIgnoreCase("<java.lang.Class: java.lang.reflect.Constructor getConstructor(java.lang.Class[])>"))
			getCtor(source, s);
		else if(methRefSig.equalsIgnoreCase("<java.lang.Class: java.lang.reflect.Constructor getDeclaredConstructor(java.lang.Class[])>"))
			getDeclaredCtor(source, s);
		else if(methRefSig.equalsIgnoreCase("<java.lang.Class: java.lang.reflect.Constructor[] getConstructors()>"))
			getCtors(source, s);
		else if(methRefSig.equalsIgnoreCase("<java.lang.Class: java.lang.reflect.Constructor[] getDeclaredConstructors()>"))
			getDeclaredCtors(source, s);
		else if(methRefSig.equals("<java.lang.Class: java.lang.reflect.Method getMethod(java.lang.String,java.lang.Class[])>"))
			getMethod(source, s);
		else if(methRefSig.equals("<java.lang.Class: java.lang.reflect.Method getDeclaredMethod(java.lang.String,java.lang.Class[])>"))
			getDeclaredMethod(source, s);
		else if(methRefSig.equals("<java.lang.Class: java.lang.reflect.Method[] getMethods()>"))
			getMethods(source, s);
		else if(methRefSig.equals("<java.lang.Class: java.lang.reflect.Method[] getDeclaredMethods()>"))
			getDeclaredMethods(source, s);
	}
    
    @Override
	public void updateNode(final Var_Node vn, PTSetInternal p2set){
		final Local receiver = (Local) vn.getVariable();
		final Context context = vn.context();
    	// resolve class name
        if(isInForNameCallSite(receiver)) {
            p2set.forall(new PTSetVisitor() {
                @Override
                public void visit(GNode n) {
                    handleForNameSideEffect(receiver, context, n);
                }
            });
        }
        // resolve Class.newInstance()
        if(isInClzNewInstCallSite(receiver)) {
            p2set.forall(new PTSetVisitor() {
                @Override
                public void visit(GNode n) {
                    handleClzNewInstSideEffect(receiver, context, n.getType(), (Alloc_Node) n);
                }
            });
        }
        // resolve Class.getConstructor()
        if(isInGetCtorCallSite(receiver)) {
            p2set.forall(new PTSetVisitor() {
                @Override
                public void visit(GNode n) {
                    handleGetCtorSideEffect(receiver, context, n);
                }
            });
        }
        // resolve Class.getDeclaredConstroctor()
        if(isInGetDeclaredCtorCallSite(receiver)) {
            p2set.forall(new PTSetVisitor() {
                @Override
                public void visit(GNode n) {
                    handleGetDeclaredCtorSideEffect(receiver,context, n);
                }
            });
        }
        // resolve Class.getConstructors()
        if(isInGetCtorsCallSite(receiver)) {
            p2set.forall(new PTSetVisitor() {
                @Override
                public void visit(GNode n) {
                    handleGetCtorsSideEffect(receiver, context, n);
                }
            });
        }
        if(isInGetDeclaredCtorsCallSite(receiver)) {
            p2set.forall(new PTSetVisitor() {
                @Override
                public void visit(GNode n) {
                    handleGetDeclaredCtorsSideEffect(receiver, context, n);
                }
            });
        }
        // resolve Ctor.newInstance()
        if(isInCtorNewInstCallSite(receiver)) {
            p2set.forall(new PTSetVisitor() {
                @Override
                public void visit(GNode n) {
                    handleCtorNewInstSideEffect(receiver, context, n);
                }
            });
        }
        // resolve Class.getMethod
        if(isInGetMtdCallSite(receiver)) {
            p2set.forall(new PTSetVisitor() {
                @Override
                public void visit(GNode n) {
                    handleGetMtdSideEffect(receiver, context, n);
                }
            });
        }
        // resolve Class.getDeclaredMethod
        if(isInGetDeclaredMtdCallSite(receiver)) {
            p2set.forall(new PTSetVisitor() {
                @Override
                public void visit(GNode n) {
                    handleGetDeclaredMtdSideEffect(receiver, context, n);
                }
            });
        }
        // resolve Class.getMethods
        if(isInGetMethodsCallSite(receiver)) {
            p2set.forall(new PTSetVisitor() {
                @Override
                public void visit(GNode n) {
                    handleGetMethodsSideEffect(receiver, context, n);
                }
            });
        }
        // resolve Class.getDeclaredMethods
        if(isInGetDeclaredMethodsCallSite(receiver)) {
            p2set.forall(new PTSetVisitor() {
                @Override
                public void visit(GNode n) {
                    handleGetDeclaredMethodsSideEffect(receiver, context, n);
                }
            });
        }
        // resolve Method.invoke
        if(isInInvokeCallSite(receiver)) {
            p2set.forall(new PTSetVisitor() {
                @Override
                public void visit(GNode n) {
                    handleMethodInvokeSideEffect(receiver, context, n);
                }
            });
        }
    }
    public Alloc_Node makeMethodMetaObjectNode(GetMethodWrapper wrapper, SootMethod source) {
        if( sparkOpts.types_for_sites() || sparkOpts.vta() )
            return pag.makeAllocNode( RefType.v( "java.lang.reflect.Method" ),
                    RefType.v( "java.lang.reflect.Method" ), null );
        MethodMetaObject_Node ret = (MethodMetaObject_Node) pag.valToAllocNode.get(wrapper);
        if( ret == null ) {
            pag.valToAllocNode.put(wrapper, ret = new MethodMetaObject_Node(pag, wrapper.getBaseClass(), 
                    wrapper.getTargetMethod(), source, wrapper.isFromGetMethod()));
            //this removal valid for double&&hybrid pts
            //pag.newAllocNodes.add( ret );
            pag.addNodeTag( ret, null );
        }
        return ret;
    }
    public Alloc_Node makeCtorMetaObjectNode(ConstructorWrapper wrapper) {
        if( sparkOpts.types_for_sites() || sparkOpts.vta() )
            return pag.makeAllocNode( RefType.v( "java.lang.reflect.Constructor" ),
                    RefType.v( "java.lang.reflect.Constructor" ), null );
        Constructor_Node ret = (Constructor_Node) pag.valToAllocNode.get(wrapper);
        if(ret == null) {
            pag.valToAllocNode.put(wrapper, ret = new Constructor_Node(pag, wrapper));
            //this removal valid for double&&hybrid pts
            //pag.newAllocNodes.add( ret );
            pag.addNodeTag(ret, null);
        }
        return ret;
    }
    public Alloc_Node makeUnknownObjectNode(SootMethod source, UnknownObjectWrapper unknownObjectAllocSite) {
    	if( sparkOpts.types_for_sites() || sparkOpts.vta() )
    		return pag.makeAllocNode( RefType.v( "java.lang.Object" ),
    				RefType.v( "java.lang.Object" ), null );
    	UnknownObject_Node node = (UnknownObject_Node) pag.valToAllocNode.get(unknownObjectAllocSite);
    	if(node == null) {
    		pag.valToAllocNode.put(unknownObjectAllocSite, node = new UnknownObject_Node(pag, unknownObjectAllocSite));
    		//this removal valid for double&&hybrid pts
            //pag.newAllocNodes.add( ret );
    		pag.addNodeTag(node, null);
    	}
    	return node;
    }
}
