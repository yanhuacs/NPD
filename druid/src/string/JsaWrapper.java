/* Java and Android Analysis Framework
 * Copyright (C) 2017 Diyu Wu, Yulei Sui and Jingling Xue
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

package string;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.string.InvalidRuntimeUseException;
import dk.brics.string.StringAnalysis;
import dk.brics.string.grammar.Nonterminal;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.Type;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import string.AutomataUtil.RE;
//import string.AMUtil.RE;
import driver.Config;
import driver.DruidOptions;
import driver.SootUtils;

public class JsaWrapper {
	private static final Logger logger=LoggerFactory.getLogger(JsaWrapper.class);
	
	private List<ValueBox> hotspots;
	//private List<HotSpot> runtimehp;
	
	private static StringAnalysis sa;
	
	//need a converter from grammer to regular expressions  : gv
	private GrammarVisitor gv;
	
	//a mapping from value(associated with a hotspot) to nonterminal  :  nonterminal
	private HashMap<Value, Nonterminal> nonterminals;
	
	//Singleton for analysis
	private static JsaWrapper jsa;
	
	//map for regular expressions generated for hotspots  :  regexMap
	private Map<Value, RE> regexMap=new HashMap<Value,RE>();
	
	static Set<SootClass> srcClasses=new HashSet<>();
	
	//test for adding hotspot
	Map<SootMethod, List<InvokeExpr>> methodToInvokeExprsMap = new HashMap<SootMethod, List<InvokeExpr>>();
	
	public static JsaWrapper v(){
		if(jsa!=null){
			return jsa;
		}else{
			jsa=new JsaWrapper();
			return jsa;
		}
	}
	
	public JsaWrapper(){
		hotspots=new LinkedList<ValueBox>();
		//runtimehp=new ArrayList<HotSpot>();
		nonterminals=new HashMap<Value,Nonterminal>();
		gv=null;
		//signatureToHotspotMap = new TreeMap<String, List<HotSpot>>();
	}
	
	public void init(Config config){
		soot.options.Options.v().set_allow_phantom_refs(true);
		logger.info("loading classes...");
		setApplicationClasses(config);
		
	}
	
	private static void setApplicationClasses(Config config){
		srcClasses.addAll(Scene.v().getApplicationClasses());
		StringAnalysis.setApplicationClasses(srcClasses);
	}
	
	public static boolean isMainClasses(SootClass clz) {
        return clz.getName().startsWith(DruidOptions.MAIN_CLASS);
    }
	
	
	private RegExp getRegExp(InvokeExpr expr){
		if(expr.getArg(1) instanceof StringConstant){
			return new RegExp(((StringConstant) expr.getArg(1)).value);
		}else{
			throw new InvalidRuntimeUseException("Non-constant name");
		}
	}
	
	private void setupSpecHotspots(){

		Map<SootMethod, List<InvokeExpr>> methodToInvokeExprsMap= new HashMap<SootMethod,List<InvokeExpr>>();
			
		Iterator aci=StringAnalysis.getApplicationClasses().iterator();
					
		while(aci.hasNext()){
			SootClass ac=(SootClass)aci.next();
			Iterator mi=ac.getMethods().iterator();
			while(mi.hasNext()){
				SootMethod sm=(SootMethod)mi.next();
				if(sm.isConcrete()){
					Body body=null;
					try{
						body=sm.retrieveActiveBody();
					}catch(Exception e){
						logger.info("Exception retrieving method body {}", e);
						continue;
					}
					for(Unit unit:body.getUnits()){
						Stmt stmt=(Stmt) unit;
						if(stmt.containsInvokeExpr()){
							boolean containsHP=false;
							InvokeExpr expr=stmt.getInvokeExpr();
							SootMethod tgt=null;
							tgt=expr.getMethod();
							if(hasStringArgs(tgt)) containsHP=true;

							
							if(containsHP){
								List<InvokeExpr> exprs=methodToInvokeExprsMap.get(tgt);
								if(exprs==null){
									exprs=new ArrayList<InvokeExpr>();
									methodToInvokeExprsMap.put(tgt, exprs);
								}
								exprs.add(expr);
							}
						}
					}
				}
			}			
		}
		for(SootMethod method:methodToInvokeExprsMap.keySet()){
			String sig=method.getSignature();
			int i=0;
			for(Type t:method.getParameterTypes()){
				if(SootUtils.isStringOrSimilarType(t)){
					List<ValueBox> sigSpots=new ArrayList<ValueBox>();
					for(InvokeExpr expr:methodToInvokeExprsMap.get(method)){
						ValueBox box=expr.getArgBox(i);
						sigSpots.add(box);
					}
					addHotspot(sig,i,sigSpots);
				}
				i++;
			}
		}
	}
	
	public List<ValueBox> addHotspot(String signature, int arg, List<ValueBox> sigSpots) {
		logger.debug("For signature " + signature + " and arg " + arg  + " got " + sigSpots.size() + " hotspots.");

        if (!sigSpots.isEmpty()) {
            hotspots.addAll(sigSpots);
        }
        return sigSpots;
	}
	
	private boolean hasStringArgs(SootMethod method){
		if(method.getSignature().equals("<java.lang.Class: java.lang.Class forName(java.lang.String)>")||
				method.getSignature().equals("<java.lang.Class: java.lang.reflect.Method getMethod(java.lang.String,java.lang.Class[])>") ||
				method.getSignature().equals("<java.lang.Class: java.lang.reflect.Method getDeclaredMethod(java.lang.String,java.lang.Class[])>")){
			return true;
		}
		return false;
	}
	
	
	
	public void run(){
		
		setupSpecHotspots();
		System.out.println("### hotspot number: "+hotspots.size());
		
		
		sa=new StringAnalysis(hotspots);
		
		for(ValueBox h:hotspots){
			nonterminals.put(h.getValue(), sa.getNonterminal(h));
		}
		
		gv=new GrammarVisitor(sa.getGrammar());
		
	}
	
	
	public void log(){
		logger.info("Done with String analysis");
		//summary();
		int i=1;
		for(ValueBox e:hotspots){
			int line=sa.getLineNumber(e);
			Automaton a=sa.getAutomaton(e);
			//logger.info("  "+ i++ +": " + sa.getClassName(e) + ": line " + sa.getLineNumber(e) + " : " + e.getValue());
			//logger.info("     regular expression: \"" + JsaWrapper.v().generateRegex(e.getValue()) + "\"");
			System.out.println("  "+ i++ +": " + sa.getClassName(e) + ": line " + sa.getLineNumber(e) + " : " + e.getValue());
			System.out.println("     regular expression: \"" + JsaWrapper.v().generateRegex(e.getValue()) + "\"");
		}
	}
	
	private String generateRegex(final Value v){
		if(v instanceof StringConstant){
			return ((StringConstant)v).value;
		}
		
		try{
			string.AutomataUtil.RE regex=gv.getRE(nonterminals.get(v));
			string.AutomataUtil.RE re=regex.simplifyOps();
			regexMap.put(v, re);
			return re.getString();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
	
	public Automaton getAutomaton(ValueBox vb){
		for(ValueBox e:hotspots){
			if(e.equals(vb))
				return sa.getAutomaton(e);
		}
		return null;
	}
}




























