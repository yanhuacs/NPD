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
import java.util.HashSet;
import java.util.Set;

import pta.CallGraphBuilder;
import pta.PTA;
import soot.Modifier;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.VoidType;
import soot.jimple.ClassConstant;

public class GlobalVariable {
    private String unknownName = "Unknown";
    private ClassConstant unknownClzConst;
    private SootMethod unknownMethod;
    private Set<SootMethod> unknownMtdsWithName;
    private Set<SootMethod> unknownMtdsWithClass;
	private SootClass unknownClass;
    private int containerInitCapacity = 4;
    private InferenceReflectionModel inferenceReflectionModel;
    private int switchKeyIndex = 0;
    private static GlobalVariable g;
    private GlobalVariable() {}
    
    public static GlobalVariable v() {
    	if(g == null) {
    		g = new GlobalVariable();
    	} return g;
    }
    
    public String getUnknownName() {
    	return unknownName;
    }
    
    public ClassConstant getUnknownClzConst() {
    	if(unknownClzConst == null) {
    		unknownClzConst = ClassConstant.v(unknownName);
    	}
    	return unknownClzConst;
    }
    
    public SootMethod getUnknownMethod() {
    	if(unknownMethod == null) {
    		unknownMethod = new SootMethod("Unknown", new ArrayList<Type>(), VoidType.v());
    		unknownMethod.setModifiers(Modifier.ABSTRACT);
			SootClass unknownClz = getUnknownClass();
			unknownClz.setResolvingLevel(SootClass.SIGNATURES);
			unknownMethod.setDeclaringClass(unknownClz);
			unknownMethod.setDeclared(true);
    	}
    	return unknownMethod;
    }
    
    public SootMethod getUnknownMethod(String name) {
    	if(unknownMtdsWithName == null)
    		unknownMtdsWithName = new HashSet<>();
    	for(SootMethod mtd : unknownMtdsWithName)
    		if(mtd.getName().equals(name))
    			return mtd;
    	SootMethod m = new SootMethod(name, new ArrayList<Type>(), VoidType.v());
    	SootClass unknownClz = getUnknownClass();
		unknownClz.setResolvingLevel(SootClass.SIGNATURES);
		m.setDeclaringClass(unknownClz);
		m.setDeclared(true);
		unknownMtdsWithName.add(m);
		return m;
    }
    
    public SootMethod getUnknownMethod(SootClass clz) {
    	if(unknownMtdsWithClass == null)
    		unknownMtdsWithClass = new HashSet<>();
    	for(SootMethod mtd : unknownMtdsWithClass)
    		if(mtd.getDeclaringClass().equals(clz))
    			return mtd;
    	SootMethod m = new SootMethod(unknownName, new ArrayList<Type>(), VoidType.v());
    	m.setDeclaringClass(clz);
    	m.setDeclared(true);
    	unknownMtdsWithClass.add(m);
    	return m;
    }
    
    public SootClass getUnknownClass() {
    	if(unknownClass == null) {
    		unknownClass = new SootClass("Unknown");
    		unknownClass.setResolvingLevel(SootClass.HIERARCHY);
    		unknownClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
    	}
    	return unknownClass;
    }
    
    public int getContainerInitCapacity() {
    	return containerInitCapacity;
    }
    
    public InferenceReflectionModel getInferenceReflectionModel(CallGraphBuilder cgb) {
    	if(inferenceReflectionModel == null) {
    		inferenceReflectionModel = new InferenceReflectionModel(((PTA)Scene.v().getPointsToAnalysis()).getPag(), cgb);
    	}
    	return inferenceReflectionModel;
    }
    
    public InferenceReflectionModel getInferenceReflectionModel() {
    	return inferenceReflectionModel;
    }
    
    public String getIfCondVarName() {
    	String varName = "if_cond_" + switchKeyIndex;
    	switchKeyIndex ++;
    	return varName;
    }
}
