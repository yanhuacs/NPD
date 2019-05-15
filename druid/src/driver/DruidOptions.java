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

package driver;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.io.output.NullOutputStream;

import reflection.ReflectionOptions;
import soot.G;
import soot.options.SparkOptions;

public class DruidOptions extends org.apache.commons.cli.Options{
	private static final long serialVersionUID = 1L;

    /** Path for the root folder for the application classes or for the application jar file. */
    public static String APP_PATH = ".";
    /** Main class for the application. */
    public static String MAIN_CLASS = null;
    /** Path for the JRE to be used for whole program analysis. */
    public static String JRE = null;
    /** Path for the root folder for the library jars. */
    public static String LIB_PATH = null;
    /** depth of obj sens when running pta for precision (with context) */
    public static int kobjsens = 0;
    /** enable string analysis*/
	public static boolean stringAnalysis = false;
    /** Path for the reflection log file for the application. */
    public static String REFLECTION_LOG = null;
    /** A lightweight mode with only one main method entry */
	public static boolean singleentry = false;
	/** A lightweight mode with only one main method entry */
	public static boolean bunch = false;
	/** use original Java names in jimples */
	public static boolean originalName = false;
    /** depth to traverse into API for call graph building, -1 is follow all edges */
    public static int apicalldepth = -1;
	/** include selected packages which are not analyzed by default */
	public static List<String> INCLUDE = null;
	/** exclude selected packages */
	public static List<String> EXCLUDE = null;
	/** include packages which are not analyzed by default */
	public static boolean INCLUDE_ALL = false;
    /** should we not add any precision for strings and clump them all together */
    public static boolean impreciseStrings = false;
    /** in spark propagate all string constants */
    public static boolean stringConstants = false;
    /** in spark limit heap context for strings if we are object sensitive */
    public static boolean limitHeapContextForStrings = false;
    /** if true, use types (instead of alloc sites) for object sensitive context elements > 1 */
    public static boolean typesForContext = false;
    /** should a context sensitive pta add context to static inits? */
    public static boolean staticinitcontext = false;
    /** should we add extra context for arrays in the pta? */
    public static boolean extraArrayContext = false;
	/** dump appclasses to jimple */
	public static boolean dumpJimple = false;
	/** if true, dump pta to a file */
    public static boolean dumppts = false;
	/** if true, dump pts of vars in library */
    public static boolean dumplibpts = false;
	/** print a CG graph */
	public static boolean dumpCallGraph = false;
	/** print a PAG graph */
	public static boolean dumppag = false;
	/** dump a PAG graph to an html */
	public static boolean dumphtml = false;

	DruidOptions() {
		addOption("help", "print this message");
		addOption("apppath", "dir or jar", "The directory containing the classes for the application or the application jar file (default: .)");
		addOption("mainclass", "class name", "Name of the main class for the application (must be specified when appmode)");
		addOption("jre", "dir", "The directory containing the version of JRE to be used for whole program analysis");
		addOption("libpath", "dir or jar", "The directory containing the library jar files for the application or the library jar file");
        addOption("kobjsens", "k", "Depth for Object Sensitivity for PTA (default value: 2");
        addOption("reflection", "Enable inference reflection (default value: false) (only jre1.6- and contextinsensitive supported!)");
        addOption("lhm", "Enable LazyHeapModeling (default value: false) (valid when reflection enabled)");
        addOption("stringanalysis", "Enable string analysis (default value: false)");
        addOption("reflectionlog", "file", "The reflection log file for the application for resolving reflective call sites");
		addOption("singleentry", "A lightweight mode with only one main method entry. (default value: false)");
		addOption("bunch", "A bunch test mode for multiple singleentries running. (default value: false)");
        addOption("originalname", "Keep original Java names. (default value: false)");
        addOption("apicalldepth", "apicalldepth", "Depth to traverse into API for call graph building, -1 is follow all edges (default value: -1");
        addOption("include", "package", "Include selected packages which are not analyzed by default");
        addOption("exclude", "package","Exclude selected packages");
        addOption("includeall", "Include packages which are not analyzed by default. (default value: false)");
        addOption("imprecisestrings", "Turn off precision for all strings, FAST and IMPRECISE (default value: false)");
        addOption("stringconstants", "Propagate all string constants (default value: true)");
        addOption("limitcontextforstrings", "Limit heap context to 1 for Strings in PTA (default value: false)");
        addOption("typesforcontext", "Use types (instead of alloc sites) for object sensitive context elements > 1 (default value: false)");
        addOption("noclinitcontext", "PTA will not add special context for static inits (default value: false)");
        addOption("extraarraycontext", "add more context for arrays (default value: false)");
        addOption("debug", "print out all G.v() information");
        addOption("dumpjimple", "Dump appclasses to jimple. (default value: false)");
        addOption("dumppts", "Dump points-to results to ./output/pta.txt (default value: false)");
        addOption("dumplibpts", "Dump points-to of lib vars results to ./output/pta.txt (default value: false)");
        addOption("dumpcallgraph", "Output .dot callgraph file (default value: false)");
        addOption("dumppag", "Print PAG to terminal. (default value: false)");
        addOption("dumphtml", "Dump PAG to html. (default value: false)");
	}
	/**add option "-option" with description*/
	private void addOption(String option, String description) {
        addOption(new Option(option, description));
	}
	/**add option "-option arg" with description*/
	@SuppressWarnings("static-access")
	private void addOption(String option, String arg, String description) {
		addOption(OptionBuilder.withArgName(arg).hasArg().withDescription(description).create(option));
	}

	static void setPackages(String[] args) {
		for(int i = 0; i < args.length; i++){
    		if(args[i] == "-include"){
    			if( INCLUDE == null )
                    INCLUDE = new LinkedList<String>();
    			i++;
    			INCLUDE.add(args[i]);
    		}
    		else if(args[i] == "-exclude"){
    			if( EXCLUDE == null )
                    EXCLUDE = new LinkedList<String>();
    			i++;
    			EXCLUDE.add(args[i]);
    		}
    	}
	}
    /**
     * Set all variables from the command line arguments.
     * 
     * @param cmd
     */
    static void setOptions(CommandLine cmd) {
    	if (cmd.hasOption("apppath"))
        	APP_PATH = cmd.getOptionValue("apppath");
    	
    	if (cmd.hasOption("mainclass"))
            MAIN_CLASS = cmd.getOptionValue("mainclass");
        else if(singleentry)
        	throw new RuntimeException("Must specify MAINCLASS when appmode enabled!!!");
    	
    	if (cmd.hasOption("jre"))
            JRE = cmd.getOptionValue("jre");
    	
    	if (cmd.hasOption("libpath"))
            LIB_PATH = cmd.getOptionValue("libpath");
    	
    	if (cmd.hasOption("kobjsens")){
            kobjsens = Integer.parseInt(cmd.getOptionValue("kobjsens"));
            if(kobjsens > 0){
            	if (cmd.hasOption("limitcontextforstrings"))
                    limitHeapContextForStrings = true;
            	if (cmd.hasOption("typesforcontext"))
                    typesForContext = true;
            	if(!cmd.hasOption("noclinitcontext"))
            		staticinitcontext = true;
            	if (cmd.hasOption("extraarraycontext"))
            		extraArrayContext = true;
            }
        }
    	
    	if (cmd.hasOption("reflection")) {
	        ReflectionOptions.v().setInferenceReflectionModel(true);
	        if(cmd.hasOption("lhm"))
	        	ReflectionOptions.v().setLazyHeapModeling(true);
	        if (cmd.hasOption("stringanalysis"))
	            stringAnalysis = true;
		}
    	
    	if (cmd.hasOption("reflectionlog"))
            REFLECTION_LOG = cmd.getOptionValue("reflectionlog");
    	
    	if (cmd.hasOption("singleentry")){
            singleentry = true;
            if(cmd.hasOption("bunch"))
            	bunch =true;
    	}
    	
    	if (cmd.hasOption("originalname"))
            originalName = true;
    	
        if (cmd.hasOption("apicalldepth"))
            apicalldepth = Integer.parseInt(cmd.getOptionValue("apicalldepth"));
        
        if (cmd.hasOption("inlcudeall"))
            INCLUDE_ALL = true;

        if (cmd.hasOption("imprecisestrings"))
            impreciseStrings = true;
        
        if (cmd.hasOption("stringconstants"))
            stringConstants = true;

        if (cmd.hasOption("debug"))
    		G.v().out = System.out;
    	else
    		G.v().out = new PrintStream(NullOutputStream.NULL_OUTPUT_STREAM);
        
        if (cmd.hasOption("dumpjimple"))
            dumpJimple = true;
        
        if (cmd.hasOption("dumppts"))
        	dumppts = true;
        
        if (cmd.hasOption("dumplibpts"))
        	dumplibpts = true;
        
        if (cmd.hasOption("dumpcallgraph"))
        	dumpCallGraph = true;
		
		if (cmd.hasOption("dumppag"))
            dumppag = true;
		
		if (cmd.hasOption("dumphtml"))
            dumphtml = true;

    }
    
    public final static SparkOptions sparkOpts;
	static{
		HashMap<String, String> opt = new HashMap<String, String>();
		
		opt.put("add-tags", "false");
		opt.put("class-method-var", "true");
		opt.put("double-set-new", "hybrid");
		opt.put("double-set-old", "hybrid");
		opt.put("dump-answer", "false");
		opt.put("dump-types", "true");
		opt.put("enabled", "true");
		opt.put("field-based", "false");
		opt.put("force-gc", "false");
		opt.put("ignore-types", "false");
		opt.put("ignore-types-for-sccs", "false");
		opt.put("on-fly-cg", "true");
		opt.put("pre-jimplify", "false");
		opt.put("propagator", "worklist");
		opt.put("rta", "false");
		opt.put("set-impl", "double");
		opt.put("set-mass", "false");
		opt.put("simple-edges-bidirectional", "false");
		opt.put("simplify-offline", "false");
		opt.put("simplify-sccs", "false");
		opt.put("simulate-natives", "false");
		opt.put("string-constants", "true");
		opt.put("topo-sort", "false");
		opt.put("types-for-sites", "false");
		opt.put("verbose", "false");
		opt.put("vta", "false");

		sparkOpts = new SparkOptions(opt);
	}
}
