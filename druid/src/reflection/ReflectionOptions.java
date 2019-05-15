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

public class ReflectionOptions {
	// for some issue if FlowDroid
	private boolean forFlowDroid = false;
	// android reflection analysis
	private boolean isAndroid = false;
	// use inference reflection model or default reflection model
	private boolean inferenceReflectionModel = false;
	// use lazy heap modeling
	private boolean lazyHeapModeling = false;
	// take string analysis results
	private boolean stringAnalysis = false;
	// use constant string only to resolve reflection
	private boolean constStringOnly = false;
	// modeling meta object used in reflective calls
	private boolean metaObjectModel = false;	
	// modeling Android APIs invoked by reflection or not
	private boolean libraryReturnValueModel = false;
	private boolean libraryReceiverValueModel = false;
	// debug options
	private boolean debug = false;
	
	private static ReflectionOptions options;
	
	public static ReflectionOptions v() {
		if(options == null)
			options = new ReflectionOptions();
		return options;
	}
	
	private ReflectionOptions() {}
	
	// option of Android reflection resolution or Java
	public boolean isForFlowDroid() {
		return forFlowDroid;
	}

	public void setForFlowDroid(boolean forFlowDroid) {
		this.forFlowDroid = forFlowDroid;
	}
	
	public boolean isAndroid() {
		return isAndroid;
	}
	
	public void setIsAndroid(boolean isAndroid) {
		this.isAndroid = isAndroid;
	}
	// option of inference reflection model
	public boolean isInferenceReflectionModel() {
		return inferenceReflectionModel;
	}
	
	public void setInferenceReflectionModel(boolean inferenceReflectionModel) {
		this.inferenceReflectionModel = inferenceReflectionModel;
	}
	
	// option of meta object modeling
	public boolean isMetaObjectModel() {
		return metaObjectModel;
	}

	public void setMetaObjectModel(boolean metaObjectModel) {
		this.metaObjectModel = metaObjectModel;
	}

	// option of only using constant string to resolve reflection
	public boolean isConstStringOnly() {
		return constStringOnly;
	}

	public void setConstStringOnly(boolean constStringOnly) {
		this.constStringOnly = constStringOnly;
	}

	public void setLazyHeapModeling(boolean lazyHeapModeling) {
		this.lazyHeapModeling = lazyHeapModeling;
	}
	
	public boolean isLazyHeapModeling() {
		return lazyHeapModeling;
	}
	
	public boolean isLibraryReturnValueModel() {
		return libraryReturnValueModel;
	}
	
	public boolean isStringAnalysis() {
		return stringAnalysis;
	}
	
	public void setStringAnalysis(boolean stringAnalysis) {
		this.stringAnalysis = stringAnalysis;
	}
	
	// option of library RETURN value modeling
	public void setLibraryReturnValueModel(boolean reflectionLibModel) {
		this.libraryReturnValueModel = reflectionLibModel;
	}
	
	// option of library RECEIVER value modeling
	public boolean isLibraryReceiverValueModel() {
		return libraryReceiverValueModel;
	}

	public void setLibraryReceiverValueModel(boolean libraryReceiverValueModel) {
		this.libraryReceiverValueModel = libraryReceiverValueModel;
	}
	
	// Debug options
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	
	public boolean debug() {
		return debug;
	}
}
