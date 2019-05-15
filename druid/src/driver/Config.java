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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pag.node.alloc.Alloc_Node;
import soot.Scene;
import soot.SootClass;
import soot.SourceLocator;
import soot.options.Options;
import soot.util.Chain;
import soot.util.HashChain;

public class Config {
    private final static Logger logger = LoggerFactory.getLogger(Config.class);
    private final static Config config = new Config();
    
    /** list of classes for which we do not add context */
    public Set<String> appClasseNames;
    public Set<SootClass> ignoreList;
    public Set<SootClass> appClasses;
    public Set<Integer> limitHeapContext= new HashSet<Integer>();
	
	
	public boolean isAppClass(SootClass clz) {
        return appClasses.contains(clz);
    }
    
    public static Config v() {
        return config;
    }
    public void init(String[] args) {
    	DruidOptions.setPackages(args);
        DruidOptions druidOptions = new DruidOptions();
        try {
            CommandLine cmd = new GnuParser().parse(druidOptions, args);
            if (cmd.hasOption("help")) {
            	new HelpFormatter().printHelp("druid", druidOptions);
                System.exit(0);
            }
            DruidOptions.setOptions(cmd);
        } catch (Exception e) {
            logger.error("Error parsing command line options", e);
            System.exit(1);
        }
    	setSootOptions();
    	setSootClassPath();
    	appClasseNames=setAppClassesNames();
    	loadNecessaryClasses();
    	appClasses=Scene.v().getApplicationClasses().stream().collect(Collectors.toSet());
    	
    	/// Object-Sensitive configures
    	ignoreList=SootUtils.installClassListWithAncestors();
    	if (DruidOptions.limitHeapContextForStrings) {
    		StringBuffer lhcbuffer = new StringBuffer();
    		SootUtils.addStringClasses(lhcbuffer);
    		installLimitHeapContext(lhcbuffer.toString());
    	}
    }

    
    //===============================class===============================
    /**
     * Add all classes from in bin/classes to the appClasses
     */
    private Set<String> setAppClassesNames() {
    	Set<String> appClasseNames=new LinkedHashSet<String>();
    	File appPath = new File(DruidOptions.APP_PATH);
        logger.info("Setting application path to {}.", appPath.toString());
        if (!appPath.exists()) {
            logger.error("Project not configured properly. Application path {} does not exist: ", appPath);
            System.exit(1);
        }
        if (appPath.isDirectory()) {
            for (File clazz : FileUtils.listFiles(appPath, new String[]{"class"}, true)) {
                String clzName = SootUtils.fromFileToClass(clazz.toString().substring(appPath.toString().length() + 1));
                logger.info("Application class: {}", clzName);
                appClasseNames.add(clzName);
            }
        } else {
            try {
                JarFile jar = new JarFile(appPath);
                for (String clzName: SootUtils.getClassesFromJar(jar)) {
                    logger.info("Application class: {}", clzName);
                    appClasseNames.add(clzName);
                }
            } catch (Exception e) {
                logger.error("Error in processing jar file {}", appPath, e);
                System.exit(1);
            }
        }
        return appClasseNames;
    }
    private void loadNecessaryClasses() {
    	Scene scene = Scene.v();
    	if(DruidOptions.singleentry){
    		scene.loadBasicClasses();
    		SootClass theClass = scene.loadClassAndSupport(soot.options.Options.v().main_class());
			if (!theClass.isPhantom())
				theClass.setApplicationClass();
			if(DruidOptions.bunch){
				scene.setMainClass(theClass);
				for( final String path : Options.v().process_dir() ) {
					for (String cl : SourceLocator.v().getClassesUnder(path)) {
						theClass = scene.loadClassAndSupport(cl);
						if (!theClass.isPhantom())
							theClass.setApplicationClass();
					}
				}
			}
    	}else{
    		for(String cl:getAllEntryClasses()){
    			SootClass theClass = scene.loadClassAndSupport(cl);
    			if (!theClass.isPhantom())
    				theClass.setApplicationClass();
    		}
    		scene.loadBasicClasses();
	        
	    	for (String name : Options.v().classes()) {
	    		scene.loadClassAndSupport(name).setApplicationClass();
	        }

	        scene.loadDynamicClasses();

	        if(Options.v().oaat()) {
	        	if(Options.v().process_dir().isEmpty()) {
	        		throw new IllegalArgumentException("If switch -oaat is used, then also -process-dir must be given.");
	        	}
	        } else {
		        for( final String path : Options.v().process_dir() ) {
		            for (String cl : SourceLocator.v().getClassesUnder(path)) {
		            	SootClass theClass = scene.loadClassAndSupport(cl);
		            	if (!theClass.isPhantom())
		            		theClass.setApplicationClass();
		            }
		        }
	        }
    	}
    	// Remove/add all classes from packageInclusionMask as per -i option
        Chain<SootClass> processedClasses = new HashChain<SootClass>();
        while(true) {
            Chain<SootClass> unprocessedClasses = new HashChain<SootClass>(scene.getClasses());
            unprocessedClasses.removeAll(processedClasses);
            if( unprocessedClasses.isEmpty() ) break;
            processedClasses.addAll(unprocessedClasses);
            for (SootClass s : unprocessedClasses) {
                if( s.isPhantom() ) continue;
                if (Config.v().appClasseNames.contains(s.getName())) {
                    s.setApplicationClass();
                    continue;
                }
                if(s.isApplicationClass()) {
                    // make sure we have the support
                	scene.loadClassAndSupport(s.getName());
                }
            }
        }
    	scene.setDoneResolving();
	}
	private Set<String> getAllEntryClasses() {
		Set<String> ret = new HashSet<String>();
        ret.add("java.lang.System");
        ret.add("java.lang.Thread");
        ret.add("java.lang.ThreadGroup");
        ret.add("java.lang.ClassLoader");
        ret.add("java.lang.ref.Finalizer");
        ret.add("java.security.PrivilegedActionException");
        return ret;
	}
	
	//===============================soot====================================
	/**
	 * Set command line options for soot.
	 */
	private static void setSootOptions() {
		List<String> dirs = new ArrayList<String>();
		dirs.add(DruidOptions.APP_PATH);
		//dirs.addAll(jreClassPathEntries());
		//System.out.println("soot process dirs: " + Arrays.toString(dirs.toArray()));
		soot.options.Options.v().set_process_dir(dirs);
		
		if (DruidOptions.MAIN_CLASS != null)
			soot.options.Options.v().set_main_class(DruidOptions.MAIN_CLASS);
		
		if (DruidOptions.INCLUDE_ALL)
			soot.options.Options.v().set_include_all(true);
		
		if (DruidOptions.INCLUDE != null)
			soot.options.Options.v().set_include(DruidOptions.INCLUDE);
		
		if (DruidOptions.EXCLUDE != null)
			soot.options.Options.v().set_include(DruidOptions.EXCLUDE);
		
		if(DruidOptions.originalName)
			soot.options.Options.v().setPhaseOption("jb", "use-original-names:true");
		
		if (DruidOptions.REFLECTION_LOG != null)
			soot.options.Options.v().setPhaseOption("cg", "reflection-log:" + DruidOptions.REFLECTION_LOG);
		
		soot.options.Options.v().set_keep_line_number(true);
		soot.options.Options.v().set_whole_program(true);
		//soot.options.Options.v().set_src_prec(Options.src_prec_jimple);
		soot.options.Options.v().setPhaseOption("cg", "verbose:false");
		soot.options.Options.v().setPhaseOption("cg", "trim-clinit:true");
		// soot.options.Options.v().setPhaseOption("jb.tr","ignore-wrong-staticness:true");
		soot.options.Options.v().setPhaseOption("wjop", "enabled:false");// don't optimize the program
		soot.options.Options.v().set_allow_phantom_refs(true);// allow for the absence of some classes
		soot.options.Options.v().set_validate(true);
	}
	/**
	 * Set the soot class path to point to the default class path appended with
	 * the app path (the classes dir or the application jar) and jar files in
	 * the library dir of the application.
	 */
	private static void setSootClassPath() {
		StringBuffer cp = new StringBuffer();
		// String defaultClassPath = Scene.v().defaultClassPath();
		// cp.append(defaultClassPath);
		
		cp.append(DruidOptions.APP_PATH);
		cp.append(jreClassPath());
		for (File f : getLibJars(DruidOptions.LIB_PATH))
			cp.append(File.pathSeparator + f.toString());
		if (DruidOptions.REFLECTION_LOG != null)
			cp.append(File.pathSeparator + new File(DruidOptions.REFLECTION_LOG).getParent());

		final String classpath = cp.toString();
		logger.info("Setting Soot ClassPath: {}", classpath);
		System.setProperty("soot.class.path", classpath);
		Scene.v().setSootClassPath(classpath);
	}
	private static String jreClassPath() {
		StringBuffer buf = new StringBuffer();
		for (String entry : jreClassPathEntries()) {
			buf.append(File.pathSeparator+entry );
		}
		return buf.toString();
	}
	private static List<String> jreClassPathEntries() {
		if (DruidOptions.JRE == null)
			return Collections.emptyList();
		List<String> entries = new LinkedList<String>();
		// String jreClassesDir = Config.v().JRE + File.separator + ".." +File.separator + "Classes" + File.separator;
		String jreLibDir = DruidOptions.JRE + File.separator + "lib" + File.separator;
		entries.add(jreLibDir + "rt.jar");
		entries.add(jreLibDir + "jce.jar");
		return entries;
	}
	/**
     * Returns a collection of files, one for each of the jar files in the app's lib folder
     */
    private static Collection<File> getLibJars(String LIB_PATH) {
    	if(LIB_PATH==null)
    		return Collections.emptyList();
    	File libPath = new File(LIB_PATH);
        if (libPath.exists()) {
            if (libPath.isDirectory()) {
            	//DA: whether this is aafe/android-lib
            	if(LIB_PATH.endsWith("android-lib")){
            		Collection<File> libJars = new ArrayList<File>();
            		File jar = new File(LIB_PATH + File.separator + "droidsafe-api-model.jar");
            		libJars.add(jar);
            		jar = new File(LIB_PATH + File.separator + "droidsafe-libs.jar");
            		libJars.add(jar);
                    return libJars;
            	}
            	else
            		return FileUtils.listFiles(libPath, new String[]{"jar"}, true);
            } else if (libPath.isFile()) {
                if (libPath.getName().endsWith(".jar")) {
                    Collection<File> libJars = new ArrayList<File>();
                    libJars.add(libPath);
                    return libJars;
                }
                logger.error("Project not configured properly. Application library path {} is not a jar file.", libPath);
                System.exit(1);
            }
        }
        logger.error("Project not configured properly. Application library path {} is not correct.", libPath);
        System.exit(1);
		return null;
    }
	
	//=================================Context===================================
	private void installLimitHeapContext(String lhc) {
    	if (lhc == null || lhc.isEmpty())
    		return;
    	String[] hashcodes = lhc.split(",");
    	for (String str : hashcodes) {
    		str = str.trim();
    		try {
    			int hashCode = Integer.parseInt(str);
    			limitHeapContext.add(hashCode);
    		} catch (NumberFormatException e) {
    			System.out.println("Invalid hashCode integer in limit heap context string: " + str);
    		}    		    		
    	}
    }
	/**Limit heap context to 1 for these AllocNodes.*/
    public boolean limitHeapContext(Alloc_Node base) {
        return limitHeapContext.contains(base.getNewExpr().hashCode());
    }
    public boolean addHeapContext(Alloc_Node probe) {
        //shortcircuit below computation
        if (!ignoreList.isEmpty()){
        	//check if the type that is allocated should never has context because it is on the ignore List
        	SootClass allocated = SootUtils.getSootClass(probe.getType());
        	//first check if on the no context list, that trumps all
        	if (allocated != null&&ignoreList.contains(allocated))
        		return false;
        }
        return true;
    }
    
    
}
