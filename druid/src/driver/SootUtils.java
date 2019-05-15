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
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.jimple.JasminClass;
import soot.jimple.NewExpr;
import soot.util.JasminOutputStream;
import soot.util.NumberedString;
import soot.SootClass;
import soot.ArrayType;
import soot.Body;
import soot.Printer;
import soot.RefType;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StmtBody;
import soot.tagkit.LineNumberTag;
import soot.util.Chain;

/**
 * Class to hold general utility methods that are helpful for Soot.
 *
 */
public class SootUtils {

	/** logger object */
	private static final Logger logger = LoggerFactory.getLogger(SootUtils.class);
	/**
	 * comma separated list of classes in which no matter what the length of k
	 * for object sensitivity, we want to limit the depth of the object
	 * sensitivity to one. Also add subclasses of each
	 * 
	 * Strings will be added if the precisestrings options is given
	 */
	private static String[] NO_CONTEXT = { "java.lang.Throwable" };

	public static List<SootMethod> getEntryPoints(){
        List<SootMethod> ret = new ArrayList<SootMethod>();
        final NumberedString sigMain = Scene.v().getSubSigNumberer().findOrAdd( "void main(java.lang.String[])" );
        if(DruidOptions.singleentry){
        	SootMethod sm = Scene.v().getMainClass().getMethodUnsafe(sigMain);
            if( sm != null ) 
            	ret.add( sm );
        }else{
        	ret = Scene.v().getEntryPoints();
        }
        return ret;
	}
	/**
	 * Return the source location of a method based on its first statement.
	 */
	public static int getMethodLocation(SootMethod method) {
		if (method != null && method.isConcrete()) {
			Chain<Unit> stmts = ((StmtBody) method.retrieveActiveBody()).getUnits();
			Iterator<Unit> stmtIt = stmts.snapshotIterator();

			if (stmtIt.hasNext()) {
				return getSourceLine((Stmt) stmtIt.next());
			}
		}

		return -1;
	}

	/**
	 * Return true if this reference is to a String, CharSequence, StringBuffer,
	 * or StringBuilder.
	 */
	public static boolean isStringOrSimilarType(Type type) {
		if (type instanceof RefType) {
			RefType refType = (RefType) type;

			return refType.equals(RefType.v("java.lang.String")) || refType.equals(RefType.v("java.lang.CharSequence"))
					|| refType.equals(RefType.v("java.lang.StringBuffer"))
					|| refType.equals(RefType.v("java.lang.StringBuilder"));

		}

		return false;
	}

	/**
	 * Return the source line number of a Jimple statement.
	 */
	public static int getSourceLine(Stmt stmt) {
		if (stmt != null) {
			LineNumberTag lineNumberTag = (LineNumberTag) stmt.getTag("LineNumberTag");
			if (lineNumberTag != null) {
				return lineNumberTag.getLineNumber();
			}
			logger.debug("Cannot find line number tag for {}", stmt);
		}
		return -1;
	}

	public static int getNumLines(SootMethod method) {
		if (method.isAbstract() || !method.isConcrete())
			return 0;

		try {
			int startingLine = getMethodLocation(method);

			Body body = method.retrieveActiveBody();

			Chain<Unit> units = body.getUnits();

			Unit curUnit = units.getLast();
			Unit first = units.getFirst();

			while (curUnit != first) {
				Stmt curStmt = (Stmt) curUnit;
				int sl = getSourceLine(curStmt);
				if (sl >= 0)
					return sl - startingLine;

				curUnit = units.getPredOf(curUnit);
			}

		} catch (Exception e) {
			return 0;
		}

		return 0;
	}

	/**
	 * get a list of statements that a method calls the other
	 * 
	 * @param sootMethod
	 * @param callee
	 * @return
	 */
	public static List<Stmt> getInvokeStatements(SootMethod sootMethod, SootMethod callee) {
		List<Stmt> invokeStmtList = new LinkedList<Stmt>();

		if (!sootMethod.isConcrete()) {
			return invokeStmtList;
		}

		Body body;
		try {
			body = sootMethod.retrieveActiveBody();
		} catch (Exception ex) {
			logger.warn("execption trying to get ActiveBody: {} ", ex);
			return invokeStmtList;
		}

		Chain<Unit> units = body.getUnits();

		/*
		 * Note that locals are named as follows: r => reference, i=> immediate
		 * $r, $i => true local r, i => parameter passing, and r0 is for this
		 * when it is non-static
		 */

		for (Unit unit : units) {
			Stmt statement = (Stmt) unit;

			if (statement.containsInvokeExpr()) {
				InvokeExpr expr = statement.getInvokeExpr();
				SootMethod invokedMethod = expr.getMethod();
				if (invokedMethod == callee)
					invokeStmtList.add(statement);
			}

		}
		return invokeStmtList;
	}

	public static void writeByteCode(String parentDir, SootClass clz) {

		String methodThatFailed = "";
		File packageDirectory = new File(
				parentDir + File.separator + clz.getPackageName().replace(".", File.separator));

		try {
			// make package directory
			packageDirectory.mkdirs();

			FileOutputStream fos = new FileOutputStream(
					packageDirectory.toString() + File.separator + clz.getShortName() + ".class");
			OutputStream streamOut = new JasminOutputStream(fos);
			OutputStreamWriter osw = new OutputStreamWriter(streamOut);
			PrintWriter writerOut = new PrintWriter(osw);

			for (SootMethod method : clz.getMethods()) {
				methodThatFailed = method.getName();
				if (method.isConcrete())
					method.retrieveActiveBody();
			}
			try {

				JasminClass jasminClass = new soot.jimple.JasminClass(clz);
				jasminClass.print(writerOut);
				// System.out.println("Succeeded writing class: " + clz);
			} catch (Exception e) {
				logger.warn("Error writing class to file {}", clz, e);
			}

			writerOut.flush();
			streamOut.close();
			writerOut.close();
			fos.close();
			osw.close();

		} catch (Exception e) {
			logger.error("Method that failed = " + methodThatFailed);
			logger.error("Error writing class to file {}", clz, e);
		}
	}

	/**
	 * Write the jimple file for clz. ParentDir is the absolute path of parent
	 * directory.
	 */
	public static void writeJimple(String parentDir, SootClass clz) {

		File packageDirectory = new File(
				parentDir + File.separator + clz.getPackageName().replace(".", File.separator));

		try {
			packageDirectory.mkdirs();

			OutputStream streamOut = new FileOutputStream(
					packageDirectory.toString() + File.separator + clz.getShortName() + ".jimple");
			PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
			Printer.v().printTo(clz, writerOut);
			writerOut.flush();
			writerOut.close();
			streamOut.close();

		} catch (Exception e) {
			logger.error("Error writing jimple to file {}", clz, e);
		}
	}

	public static void dumpJimple(String outputDir) {
		for (SootClass clz : Config.v().appClasses) {
			writeJimple(outputDir, clz);
		}
	}

	/**
	 * Given a file name with separators, convert them in to . so it is a legal
	 * class name. modified: Ammonia: handle .* not only .class
	 */
	public static String fromFileToClass(String name) {
		return name.substring(0, name.lastIndexOf('.')).replace(File.separatorChar, '.');
	}

	/**
	 * Given a jarFile, return a list of the classes contained in the jarfile
	 * with . replacing /.
	 */
	public static List<String> getClassesFromJar(JarFile jarFile) {
		LinkedList<String> classes = new LinkedList<String>();
		Enumeration<JarEntry> allEntries = jarFile.entries();

		while (allEntries.hasMoreElements()) {
			JarEntry entry = (JarEntry) allEntries.nextElement();
			String name = entry.getName();
			if (!name.endsWith(".class")) {
				continue;
			}

			String clsName = name.substring(0, name.length() - 6).replace('/', '.');
			classes.add(clsName);
		}
		return classes;
	}

	/**
	 * Return the terminal classname from a fully specified classname
	 */
	public static String extractClassname(String fullname) {
		return fullname.replaceFirst("^.*[.]", "");
	}


	public static String buildNoContextList() {
		StringBuffer buf = new StringBuffer();
		int i = 0;
		for (String str : NO_CONTEXT) {
			SootClass clz = Scene.v().getSootClass(str);
			for (SootClass child : Scene.v().getActiveHierarchy().getSubclassesOfIncluding(clz)) {
				i++;
				buf.append(child + ",");
				logger.info("Adding class to ignore context list of spark: {}", child);
			}
		}
		String ret = buf.substring(0, buf.length() - 1);
		System.out.println("No context: " + i);
		return ret;
	}

	public static void addStringClasses(StringBuffer buf) {
		if (buf.length() > 0 && ',' != buf.charAt(buf.length() - 1))
			buf.append(',');

		for (SootClass clz : Scene.v().getClasses()) {
			if (clz.isInterface() || clz.isPhantom())
				continue;

			for (SootMethod method : clz.getMethods()) {
				if (!method.isConcrete())
					continue;
				Body body = null;
				try {
					body = method.retrieveActiveBody();
				} catch (Exception e) {
					body = null;
				}

				if (body == null)
					continue;

				for (Unit unit : body.getUnits()) {
					Stmt stmt = (Stmt) unit;
					if (stmt instanceof AssignStmt && ((AssignStmt) stmt).getRightOp() instanceof NewExpr) {
						NewExpr newExpr = (NewExpr) ((AssignStmt) stmt).getRightOp();
						if (newExpr.getType() instanceof RefType
								&& SootUtils.isStringOrSimilarType(newExpr.getType())) {
							buf.append(newExpr.hashCode() + ",");
							logger.info("Adding string allocation to limit heap context list of spark: {} {}", stmt,
									newExpr.hashCode());
						}
					}
				}
			}

		}
	}
	
    public static SootClass getSootClass(Type type) {
        SootClass allocated = null;
        if (type instanceof RefType) {
            allocated = ((RefType)type).getSootClass();
        } else if (type instanceof ArrayType && ((ArrayType)type).getArrayElementType() instanceof RefType) {
            allocated = ((RefType)((ArrayType)type).getArrayElementType()).getSootClass();
        }

        return allocated;
    }
    
    private static SootClass tryToGetClass(String cls) {
        try {
            SootClass clz = Scene.v().getSootClass(cls);
            return clz;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Install no context list for classes given plus all subclasses.
     */
    public static Set<SootClass> installClassListWithAncestors() {
    	Set<SootClass> set= new HashSet<SootClass>();
    	String csl = buildNoContextList();
        if (csl == null || csl.isEmpty())
            return set;
        Scene.v().getActiveHierarchy();
        String[] classes = csl.split(",");
        for (String str : classes) {
            str = str.trim();
            //System.out.println("Adding class plus subclasses to ignore list: " + str);
            SootClass clz = tryToGetClass(str);
            if (clz != null)
                set.add(clz);
        }
        return set;
    }
    
    
}
