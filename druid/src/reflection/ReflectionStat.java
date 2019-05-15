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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import reflection.InferenceReflectionModel.ClzNewInstCallInfo;
import reflection.InferenceReflectionModel.CtorNewInstCallInfo;
import reflection.InferenceReflectionModel.ForNameCallInfo;
import reflection.InferenceReflectionModel.InvokeCallInfo;
import soot.Value;

public class ReflectionStat {
	private static ReflectionStat stat;

	private ReflectionStat() {}
	
	public static ReflectionStat v() {
		if(stat == null)
			stat = new ReflectionStat();
		return stat;
	}
	
	/**
     * Show the statistics reflective calls
     * 
     */
	public void showInferenceReflectionStat() {
		PrintHelper helper = new PrintHelper();
		helper.add(System.out);
		try (PrintStream filePrint = new PrintStream(new FileOutputStream(new File("reflection.txt"), false))) {
			helper.add(filePrint);
			InferenceReflectionModel inferenceReflectionModel = GlobalVariable.v().getInferenceReflectionModel();
						// Class.newInstance() calls
			List<ClzNewInstCallInfo> clzNewInstCalls = new ArrayList<>();
			if(inferenceReflectionModel == null) {
				helper.println("No inference reflecion analysis results.");
				return;
			}
			
			// Class.forName calls
			List<ForNameCallInfo> forNameCalls = new ArrayList<>();
			for(Iterator<Entry<Value, Set<ForNameCallInfo>>> i = inferenceReflectionModel.getClzName2ForNameCalls().entrySet().iterator(); i.hasNext(); )
				for(ForNameCallInfo forNameCall : i.next().getValue())
					forNameCalls.add(forNameCall);
			// sort Class.forName() calls according to the target class meta objects
			forNameCalls.sort(new Comparator<ForNameCallInfo>() {
				@Override
				public int compare(ForNameCallInfo o1, ForNameCallInfo o2) {
					Integer o1Size = o1.getTargetClasses().size();
					Integer o2Size = o2.getTargetClasses().size();
					return o2Size.compareTo(o1Size);
				}
			});
			// calculate the number of reachable call sites and resolved ones
			int reachableForName = forNameCalls.size();
			long resolvedForName = forNameCalls.stream()
											   .filter(c -> c.getTargetClasses().size() != 0)
											   .count();
			long allForNameTgts  = 0;
			// output information
			helper.println("+------------------------------------------------------------------------------------------------------------------+");
			helper.println("| Resolved/Reachable Class.forName() calls: " + resolvedForName + "/" + reachableForName);
			for(ForNameCallInfo clzNewInstCall : forNameCalls) {
				helper.println("|-------------------------------------------------------------------------------------------------------------------");
				helper.println("| " + clzNewInstCall.getCaller());
				helper.println("| in method " + clzNewInstCall.getSource().getSignature());
				int targetNumber = clzNewInstCall.getTargetClasses().size();
				allForNameTgts += targetNumber;
				helper.println("| Number of targets: " + targetNumber);
				if(targetNumber != 0) {
					helper.println("| They are: ");
					clzNewInstCall.getTargetClasses()
								  .stream()
								  .map(e -> "| \t" + e.toString())
								  .sorted()
								  .forEach(helper::println);
				}
				helper.println("|-------------------------------------------------------------------------------------------------------------------");
			}
			helper.println("All Reachable Class.forName() targets: " + allForNameTgts);
			helper.println("+------------------------------------------------------------------------------------------------------------------+\n");
			
			// find the reflective call sites
			for(Iterator<Set<ClzNewInstCallInfo>> i = inferenceReflectionModel.getClz2NewInstCalls().iterator(); i.hasNext(); )
				for(ClzNewInstCallInfo clzNewInstCall : i.next())
					clzNewInstCalls.add(clzNewInstCall);
			// sort them according to the number of targets
			clzNewInstCalls.sort(new Comparator<ClzNewInstCallInfo>() {
				@Override
				public int compare(ClzNewInstCallInfo o1, ClzNewInstCallInfo o2) {
					Integer o1Size = o1.getReachingClasses().size();
					Integer o2Size = o2.getReachingClasses().size();
					return o2Size.compareTo(o1Size);
				}
			});
			// calculate the number of reachable call sites and resolved call sites
			int reachableClzNewInst = clzNewInstCalls.size();
			long resolvedClzNewInst = clzNewInstCalls.stream()
					.filter(c -> c.getReachingClasses().size() != 0)
					.count();
			// output them
			int allCtorTgts = 0;
			helper.println("+------------------------------------------------------------------------------------------------------------------+");
			helper.println("| Resolved/Reachable Class.newInstance() calls: " + resolvedClzNewInst + "/" + reachableClzNewInst);
			for(ClzNewInstCallInfo clzNewInstCall : clzNewInstCalls) {
				helper.println("|-------------------------------------------------------------------------------------------------------------------");
				helper.println("| " + clzNewInstCall.getCaller());
				helper.println("| in method " + clzNewInstCall.getSource().getSignature());
				int targetNumber = clzNewInstCall.getReachingClasses().size();
				allCtorTgts += targetNumber;
				helper.println("| Number of targets: " + targetNumber);
				if(targetNumber != 0) {
					helper.println("| They are: ");
					clzNewInstCall.getReachingClasses()
								  .stream()
								  .map(e -> "| \t" + e.toString())
								  .sorted()
								  .forEach(helper::println);
				}
				helper.println("|-------------------------------------------------------------------------------------------------------------------");
			}
			helper.println("All Reachable Class.newInstance() targets: " + allCtorTgts);
			helper.println("+------------------------------------------------------------------------------------------------------------------+\n");
			
			List<CtorNewInstCallInfo> ctorNewInstCalls = new ArrayList<>();
			// Constructor.newInstance calls
			for(Iterator<Set<CtorNewInstCallInfo>> i = inferenceReflectionModel.getClz2CtorNewInstCalls().iterator(); i.hasNext(); )
				for(CtorNewInstCallInfo ctorNewInstCall : i.next())
					if(! ctorNewInstCalls.contains(ctorNewInstCall))
						ctorNewInstCalls.add(ctorNewInstCall);
			
			ctorNewInstCalls.sort(new Comparator<CtorNewInstCallInfo>() {
				@Override
				public int compare(CtorNewInstCallInfo o1, CtorNewInstCallInfo o2) {
					Integer o1Size = o1.getCtors().size();
					Integer o2Size = o2.getCtors().size();
					return o2Size.compareTo(o1Size);
				}
			});
			helper.println("\n");
			
			int reachableCtorNewInst = ctorNewInstCalls.size();
			long resolvedCtorNewInst = ctorNewInstCalls.stream()
													   .filter(c -> c.getCtors().size() != 0)
													   .count();
			long allCtorNewInstTgts = 0;
			helper.println("+------------------------------------------------------------------------------------------------------------------+");
			helper.println("| Resolved/Reachable Constructor.newInstance() calls: " + resolvedCtorNewInst + "/" + reachableCtorNewInst);
			for(CtorNewInstCallInfo invokeCall : ctorNewInstCalls) {
				helper.println("|-------------------------------------------------------------------------------------------------------------------");
				helper.println("| " + invokeCall.getCaller());
				helper.println("| in method " + invokeCall.getSource().getSignature());
				int targetNumber = invokeCall.getCtors().size();
				allCtorNewInstTgts += targetNumber;
				helper.println("| Number of targets: " + targetNumber);
				if(targetNumber != 0) {
					helper.println("| They are: ");
					invokeCall.getCtors()
							  .stream()
							  .map(e -> "| \t" + e.toString())
							  .sorted()
							  .forEach(helper::println);
				}
				helper.println("|-------------------------------------------------------------------------------------------------------------------");
			}
			helper.println("All Reachable Constructor.newInstance() targets: " + allCtorNewInstTgts);
			helper.println("+------------------------------------------------------------------------------------------------------------------+\n");
			
			// Method.invoke() calls
			List<InvokeCallInfo> invokeCalls = new ArrayList<>();
			for(Iterator<Set<InvokeCallInfo>> i = inferenceReflectionModel.getMtd2InvokeCalls().iterator(); i.hasNext(); )
				for(InvokeCallInfo invokeCall : i.next())
					if(!invokeCalls.contains(invokeCall))
						invokeCalls.add(invokeCall);
			
			invokeCalls.sort(new Comparator<InvokeCallInfo>() {
				@Override
				public int compare(InvokeCallInfo o1, InvokeCallInfo o2) {
					Integer o1Size = o1.getInvokeExpr().size();
					Integer o2Size = o2.getInvokeExpr().size();
					return o2Size.compareTo(o1Size);
				}
			});
			helper.println("\n");

			int reachableInvoke = invokeCalls.size();
			long resolvedInvoke = invokeCalls.stream()
					.filter(c -> c.getInvokeExpr().size() != 0)
					.count();
			int allInvokeTgts = 0;
			helper.println("+------------------------------------------------------------------------------------------------------------------+");
			helper.println("| Resolved/Reachable Method.invoke() calls: " + resolvedInvoke + "/" + reachableInvoke);
			for(InvokeCallInfo invokeCall : invokeCalls) {
				helper.println("|-------------------------------------------------------------------------------------------------------------------");
				helper.println("| " + invokeCall.getCaller());
				helper.println("| in method " + invokeCall.getSource().getSignature());
				int targetNumber = invokeCall.getInvokeExpr().size();
				allInvokeTgts += targetNumber;
				helper.println("| Number of targets: " + targetNumber);
				if(targetNumber != 0) {
					helper.println("| They are: ");
					invokeCall.getInvokeExpr()
					.stream()
					.map(e -> "| \t" + e.getMethod().getSignature())
					.sorted()
					.forEach(helper::println);
				}
				helper.println("|-------------------------------------------------------------------------------------------------------------------");
			}
			helper.println("All Reachable Method.invoke() targets: " + allInvokeTgts);
			helper.println("+------------------------------------------------------------------------------------------------------------------+");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
