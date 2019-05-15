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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import reflection.InferenceReflectionModel.ClzNewInstCallInfo;
import reflection.InferenceReflectionModel.CtorNewInstCallInfo;
import reflection.InferenceReflectionModel.ForNameCallInfo;
import reflection.InferenceReflectionModel.InvokeCallInfo;
import reflection.InferenceReflectionModel.StmtWithPos;
import soot.PatchingChain;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

public class JSONFormatter {
	private static JSONFormatter jsonFormatter;
	
	private JSONFormatter() {}
	
	public static JSONFormatter v() {
		if(jsonFormatter == null)
			jsonFormatter = new JSONFormatter();
		return jsonFormatter;
	}
	
	// A GSON wrapper class for conveniently configure GSON
	public class GsonWrapper {
		private GsonBuilder gsonBuilder;
		private Gson gson;
		
		public GsonWrapper() {
			gsonBuilder = new GsonBuilder();
			gsonBuilder.disableHtmlEscaping();
			gsonBuilder.setPrettyPrinting();
			gson = gsonBuilder.create();
		}
		
		public String toJson(Object src) {
			return gson.toJson(src);
		}
	}
	
	/**
	 * JavaBeans for JSON item.
	 * Each JSON item represents information of reflective calls.
	 * It may be a Class.newInstance(), Constructor.newInstance() or Method.invoke() calls.
	 * The JsonItem is initialized according to the type of passing object.
	 */
	public class JsonItem {
		private Caller caller;
		private Calling calling;
		private List<Callee> callees;
		
		public JsonItem() {}
		
		public JsonItem(StmtWithPos stmtWithPos) {
			caller = new Caller(stmtWithPos);
			calling = new Calling(stmtWithPos);
			callees = new ArrayList<>();
			
			// initialize the calles of reflective calls according to their types.
			if(stmtWithPos instanceof ForNameCallInfo) {
				for(SootClass clz : ((ForNameCallInfo) stmtWithPos).getTargetClasses())
					callees.add(new Callee(clz));
			} else if(stmtWithPos instanceof ClzNewInstCallInfo) {
				for(SootClass c : ((ClzNewInstCallInfo) stmtWithPos).getReachingClasses()) {
					callees.add(new Callee(c.getMethod("void <init>()")));
				}
			} else if(stmtWithPos instanceof CtorNewInstCallInfo) {
				for(SootMethod mtd : ((CtorNewInstCallInfo) stmtWithPos).getCtors())
					callees.add(new Callee(mtd));
			} else if(stmtWithPos instanceof InvokeCallInfo) {
				for(InvokeExpr invokeExpr : ((InvokeCallInfo) stmtWithPos).getInvokeExpr())
					callees.add(new Callee(invokeExpr.getMethod()));
			} else
				throw new RuntimeException("Unhandled reflective call information whose type is " + stmtWithPos.getClass().getName());
		}

		public Caller getCaller() {
			return caller;
		}

		public Calling getCalling() {
			return calling;
		}

		public List<Callee> getCallees() {
			return callees;
		}
	}
	
	/**
	 * JavaBean Caller
	 * This class describes containing method of reflective calls.
	 */
	public class Caller {
		private String className;
		private String methodSignagure;
		
		public Caller() {}
		
		public Caller(StmtWithPos stmtWithPos) {
			className = stmtWithPos.getSource().getDeclaringClass().getName();
			methodSignagure = stmtWithPos.getSource().getSignature();
		}

		public String getClassName() {
			return className;
		}

		public String getMethodSignagure() {
			return methodSignagure;
		}
	}
	
	/**
	 * JavaBean Calling
	 * This class describes caller of reflective calls.
	 */
	public class Calling {
		String stmt;
		long stmtSeq;
		String type;
		
		private static final String CLASS_CALL = "CLASS_CALL";
		private static final String CLASS_NEWINSTANCE ="CLASS_NEWINSTANCE";
		private static final String CONSTRUCTOR_CALL = "CONSTRUCTOR_CALL";
		private static final String METHOD_CALL = "METHOD_CALL";
		
		public Calling() {}
		
		public Calling(StmtWithPos stmtWithPos) {
			Stmt caller = stmtWithPos.getCaller();
			stmt = caller.toString();
			// find the stmt sequence
			PatchingChain<Unit> units = stmtWithPos.getSource().retrieveActiveBody().getUnits();
			long index = 0;
			for(Unit u : units)
				if(u.equals(caller))
					stmtSeq = index;
				else
					index++;
			// determine the type of reflective calls
			String sig = caller.getInvokeExpr().getMethod().getSignature();
			if(sig.equals("<java.lang.Class: java.lang.Class forName(java.lang.String)>"))
				type = CLASS_CALL;
			else if(sig.equals("<java.lang.Class: java.lang.Object newInstance()>"))
				type = CLASS_NEWINSTANCE;
			else if(sig.equals("<java.lang.reflect.Constructor: java.lang.Object newInstance(java.lang.Object[])>"))
				type = CONSTRUCTOR_CALL;
			else if(sig.equals("<java.lang.reflect.Method: java.lang.Object invoke(java.lang.Object,java.lang.Object[])>"))
				type = METHOD_CALL;
			else
				throw new RuntimeException("Unknown caller type. Method signature is " + sig);
		}

		public String getStmt() {
			return stmt;
		}

		public long getStmtSeq() {
			return stmtSeq;
		}

		public String getType() {
			return type;
		}
	}
	
	/**
	 * JavaBean Callee
	 * This class describes the callees of reflective calls.
	 */
	public class Callee {
		private String cls;
		private String method;
		private List<String> args;
		private String fld;
		
		public Callee() {}
		
		public Callee(SootMethod callee) {
			cls = callee.getDeclaringClass().getName();
			method = callee.getSubSignature();
			args = new ArrayList<>();
			for(Type t : callee.getParameterTypes())
				args.add(t.toString());
			fld = "";
		}
		
		public Callee(SootField fld) {
			cls = fld.getDeclaringClass().getName();
			this.fld = fld.getSubSignature();
			args = new ArrayList<>();
			method = "";
		}
		
		public Callee(SootClass clz) {
			cls = clz.getName();
			method = "";
			args = new ArrayList<>();
			fld = "";
		}
		
		public String getCls() {
			return cls;
		}
		
		public String getMethod() {
			return method;
		}
		
		public String getField() {
			return fld;
		}
		
		public List<String> getArgs() {
			return args;
		}
	}
	
	public void format() {
		InferenceReflectionModel inferModel = GlobalVariable.v().getInferenceReflectionModel();
		if(inferModel == null)
			return;
		GsonWrapper gsonWrapper = new GsonWrapper();
		PrintHelper helper = new PrintHelper();
		// helper.add(System.out);
		try (PrintStream filePrint = new PrintStream(new FileOutputStream(new File("reflection.json"), false))) {
			helper.add(filePrint);
			// Class.forName()
			List<JsonItem> forNameJsonItems = new ArrayList<>();
			for(Iterator<Entry<Value, Set<ForNameCallInfo>>> i = inferModel.getClzName2ForNameCalls().entrySet().iterator(); i.hasNext(); ) {
				for(ForNameCallInfo forNameCall : i.next().getValue())
					if(! forNameCall.getTargetClasses().isEmpty())
						forNameJsonItems.add(new JsonItem(forNameCall));
			}
			helper.println(gsonWrapper.toJson(forNameJsonItems));

			// Class.newInstance()
			List<JsonItem> clzNewInstJsonItems = new ArrayList<>();
			for(Iterator<Set<ClzNewInstCallInfo>> i = inferModel.getClz2NewInstCalls().iterator(); i.hasNext(); ) {
				for(ClzNewInstCallInfo clzNewInstCall : i.next()) {
					if(! clzNewInstCall.getReachingClasses().isEmpty())
						clzNewInstJsonItems.add(new JsonItem(clzNewInstCall));
				}
			}
			helper.println(gsonWrapper.toJson(clzNewInstJsonItems));

			// Constructor.newInstance()
			List<JsonItem> ctorNewInstJsonItems = new ArrayList<>();
			for(Iterator<Set<CtorNewInstCallInfo>> i = inferModel.getClz2CtorNewInstCalls().iterator(); i.hasNext(); ) {
				for(CtorNewInstCallInfo ctorNewInstCall : i.next())
					if(! ctorNewInstCall.getCtors().isEmpty())
						ctorNewInstJsonItems.add(new JsonItem(ctorNewInstCall));
			}
			helper.println(gsonWrapper.toJson(ctorNewInstJsonItems));

			// Method.invoke()
			List<JsonItem> invokeCallJsonItems = new ArrayList<>();
			for(Iterator<Set<InvokeCallInfo>> i = inferModel.getMtd2InvokeCalls().iterator(); i.hasNext(); ) {
				for(InvokeCallInfo invokeCall : i.next())
					if(! invokeCall.getInvokeExpr().isEmpty())
						invokeCallJsonItems.add(new JsonItem(invokeCall));
			}
			helper.println(gsonWrapper.toJson(invokeCallJsonItems));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
