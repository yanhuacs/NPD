package core;
/* Java and Android Analysis Framework - NullPointer Module
 * Copyright (C) 2017 Xinwei Xie, Hua Yan, Jingbo Lu, Yulei Sui and Jingling Xue
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

import predicate.BinaryPredicate;
import predicate.Predicate;
import progressbar.ProgressBar;
import solver.SATSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;

import java.util.*;

/**
 * Created by Xinwei Xie on 24/7/17.
 */
public class NPVerifier {

    public enum FieldAccessType {GETFIELD, PUTFIELD};
    public final static Logger logger = LoggerFactory.getLogger(NPVerifier.class);

    private CallGraph callGraph;
    private PointsToAnalysis pointsToAnalysis;
    public SATSolver solver;
    //private SootMethod currentMethod;

    // concurrent access
    public static final Set<SootClass> mapClass = CollectionFactory.newSet();
    public static final Set<SootClass> collectionClass = CollectionFactory.newSet();
    public static final Set<Unit> collectionDerefs = CollectionFactory.newSet();
    public static final Set<Unit> mapDerefs = CollectionFactory.newSet();
    public Map<SootMethod, Map<State, Set<State>>> methodSummary;
    public Map<SootMethod, Map<State, Set<State>>> entryStateSummary;

    public static final Set<SootClass> iteratorClass = CollectionFactory.newSet();
    public static final Set<SootClass> enumClass = CollectionFactory.newSet();
    public static final Set<SootClass> setClass = CollectionFactory.newSet();
    public static final Set<SootClass> listClass = CollectionFactory.newSet();
    public static final Set<SootMethod> classInitMethods = CollectionFactory.newSet();

    //public static Map<SootMethod, Set<Unit>> throwMap;
    //public static Set<SootMethod> applicationMethods;

    public static final boolean boundSteps = true;
    public static final boolean globalBound = true;
    public static final int maxSteps = 10000;
    public static final int maxTargets = 100;
    public static final long maxGlobalSteps = 200000L;
    public static final boolean enableStatistic = true;
    public static final Map<Integer, Integer> virtualCallCounts = CollectionFactory.newMap();

    public static final Set<String> analyzeList = CollectionFactory.newSet();
    public static final Map<SootMethod, Set<Unit>> wrongMap = CollectionFactory.newMap();
    public static final Map<SootMethod, Set<Unit>> safeMap = CollectionFactory.newMap();
    public static final Map<SootMethod, Set<Unit>> unknownMap = CollectionFactory.newMap();
    public static final Map<SootMethod, Set<Unit>> reachBoundMap = CollectionFactory.newMap();
    public static final Map<SootMethod, List<Unit>> dereferenceMap = CollectionFactory.newMap();

    public static boolean reachBound = true;
    public static long currentGlobalSteps = 0L;
    public static boolean earlyTerminate = false;
    public static boolean analyzingClinit = false;
    public static Map<SootMethod, Map<PointsToSet, SootField>> modMap;

    public static long totalDref = 0;
    public static long currentDref = 0;
    public static long numOfWrong = 0;
    public static long numOfSafe = 0;
    public static long numOfUnknown = 0;

    public ProgressBar progressBar;
    public static final boolean enableProgressBar = false;


    public static void reset() {
		totalDref = 0;
		currentDref = 0;
		numOfWrong = 0;
		numOfSafe = 0;
		dereferenceMap.clear();
	}

    public CallGraph getCallGraph() {
        return callGraph;
    }

    public PointsToAnalysis getPointsToAnalysis() {
        return pointsToAnalysis;
    }

    public NPVerifier(CallGraph cg, PointsToAnalysis pta) { //, PTA p) {
        callGraph = cg;
        pointsToAnalysis = pta;
        //cg = callgraph;
        //ctxtStack = new Stack<Context>();
        //entryMap = new HashMap<SootMethod, Set<State>>();
        //enableRecursion = true;
        //pta = p;
    }

    boolean isDerefSafe(SootMethod method, Unit inst, Value base) {
        //this.currentMethod = method;
		NPVerifier.earlyTerminate = false;
		NPVerifier.currentGlobalSteps = 0L;
		NPVerifier.reachBound = false;
		NPVerifier.analyzingClinit = false;
        State initState = new State();
        AccessPath lhs = new AccessPath((Value)base, method);
        AccessPath rhs = new AccessPath((Value)NullConstant.v(), method);
        BinaryPredicate pred = new BinaryPredicate(lhs, rhs, Operator.EQ, false, true);
        BinaryPredicate cc = (BinaryPredicate) pred.clone();
        initState.addPredicate(pred);
        Stack<Context> ctxtStack;
        Map<SootMethod, Set<State>> entryMap;
        ctxtStack = CollectionFactory.newStack();
        entryMap = CollectionFactory.newMap();

        methodSummary = CollectionFactory.newMap(); //new HashMap<SootMethod, Map<State, Set<State>>>();
        entryStateSummary = CollectionFactory.newMap(); //new HashMap<SootMethod, Map<State, Set<State>>>();
        solver = new SATSolver();

        if (enableProgressBar) {
			progressBar = new ProgressBar("NPVerifier", NPVerifier.maxGlobalSteps, 10);
			progressBar.start();
		}
        Set<State> result = new CPA(this).analyzeMethod(ctxtStack, entryMap, method, inst, initState);

        if (enableProgressBar) {
			progressBar.stop();
		}

        if (NPVerifier.reachBound) {
        	Set<Unit> set = reachBoundMap.get(method);
        	if (set == null) {
        		set = CollectionFactory.newSet();
        		reachBoundMap.put(method, set);
			}
			set.add(inst);
        	NPVerifier.reachBound = false;
		}

        if (!result.isEmpty()) {
            result = removeInvalidStates(result);
        }
        if (result.isEmpty()) {
        	Set<Unit> u = safeMap.get(method);
        	if (u == null) {
				u = CollectionFactory.newSet();
			}
        	u.add(inst);

        	safeMap.put(method, u);
        	numOfSafe++;
			return true;
		} else {
        	/*
        	boolean error = NPVerifier.earlyTerminate;
        	if (NPVerifier.earlyTerminate)
				return false;
        	else
        		return true;
        		*/
        	boolean allEmpty = true;
        	for (State st : result) {
        		if (!st.getAllPreds().isEmpty())
        			allEmpty = false;
			}
			if (allEmpty && NPVerifier.earlyTerminate) {
				Set<Unit> u = NPVerifier.wrongMap.get(method);
				if (u == null) {
					u = CollectionFactory.newSet();
				}
				u.add(inst);
				NPVerifier.wrongMap.put(method, u);
				numOfWrong++;
				return false;
			} else {
				Set<Unit> u = NPVerifier.unknownMap.get(method);
				if (u == null) {
					u = CollectionFactory.newSet();
				}
				u.add(inst);
				NPVerifier.unknownMap.put(method, u);
				numOfUnknown++;
				return true;
			}
		}
    }

    private Set<State> removeInvalidStates(Set<State> result) {
        Set<State> toRemove = CollectionFactory.newSet();
        for (State st : result) {
            for (Predicate pred : st) {
                if (pred instanceof BinaryPredicate) {
                    BinaryPredicate bp = (BinaryPredicate) pred;
                    AccessPath lhs = bp.getLhs();
                    AccessPath rhs = bp.getRhs();
                    if (!bp.isNegated()) {
                        if ((lhs.isNullConstant() && lhs.hasSpecialFields())
                            || (rhs.isNullConstant() && rhs.hasSpecialFields())) {
                            toRemove.add(st);
                            break;
                        }
                    }
                }
            }
        }
        result.removeAll(toRemove);
        return result;
    }

    /*
    private Object[] createInitPair(BlockGraph cfg, Unit unit, State initState) {
        Object[] ret = new Object[4];
        if (unit != null) {
            Block instBlock = null;
            for (Block bb : cfg) {
                for (Iterator<Unit> unitIter = bb.iterator(); unitIter.hasNext(); ) {
                    if (unit == (Unit) unitIter.next()) {
                        instBlock = bb;
                        break;
                    }
                }
            }
            if (instBlock == null) {
                logger.error("Abort analysis of instruction: " + unit + ", since it is not in CFG");
                throw new IllegalStateException("Abort analysis of instruction: " + unit + ", since it is not in CFG");
            }
            ret[0] = instBlock;
            ret[1] = unit;
            ret[2] = initState;
            ret[3] = null;
        } else {
            Block lastBlock = null;
            for (Block bb : cfg) {
                if (bb.getSuccs().isEmpty()) {
                    lastBlock = bb;
                    break;
                }
            }
            if (lastBlock == null) {
                throw new IllegalArgumentException("CFG has no exit block");
            }
            ret[0] = lastBlock;
            ret[1] = null;
            ret[2] = initState;
            ret[3] = null;
        }
        return ret;
    }
    */

    public boolean isThisReference(PatchingChain<Unit> instructions, Value base) {
        Iterator<Unit> instIter = instructions.iterator();
        while (instIter.hasNext()) {
            Unit inst = instIter.next();
            if (inst instanceof IdentityStmt) {
                IdentityStmt identityStmt = (IdentityStmt) inst;
                Value lhs = identityStmt.getLeftOp();
                Value rhs = identityStmt.getRightOp();
                if (lhs == base && rhs instanceof ThisRef)
                    return true;
            }
        }
        return false;
    }

    public boolean isArrayInst(Unit inst) {
    	if (inst instanceof AssignStmt) {
			Value rhs = ((AssignStmt) inst).getRightOp();
			if (rhs instanceof LengthExpr)
				return true;
		}
		return false;
	}

    public Value getInstBase(Unit u) {
        Value ret = null;
        ret = getLoadBase(u);
        if (ret != null) return ret;
        ret = getStoreBase(u);
        if (ret != null) return ret;
        ret = getInvokeOnlyBase(u);
        if (ret != null) return ret;
        ret = getInvokeAssignBase(u);
        if (ret != null) return ret;
        ret = getArrayLength(u);
        if (ret != null) return ret;
        return null;
    }

    public Value getArrayLength(Unit u) {
        if (u instanceof AssignStmt) {
            Value rval = ((AssignStmt)u).getRightOp();
            if (rval instanceof LengthExpr) {
                return ((LengthExpr)rval).getOp();
            }
        } else if (u instanceof LengthExpr) {
            return ((LengthExpr)u).getOp();
        }
        return null;
    }

    public Value getLoadBase(Unit u) {
        if (u instanceof AssignStmt) {
            Value rval = ((AssignStmt)u).getRightOp();
            if (rval instanceof InstanceFieldRef) {
                return ((InstanceFieldRef)rval).getBase();
            }
        }
        return null;
    }
    public Value getStoreBase(Unit u) {
        if (u instanceof AssignStmt) {
            Value lval = ((AssignStmt)u).getLeftOp();
            if (lval instanceof InstanceFieldRef) {
                return ((InstanceFieldRef)lval).getBase();
            }
        }
        return null;
    }
    public Value getInvokeOnlyBase(Unit u) {
        if (u instanceof InvokeStmt) {
            InvokeExpr invoke = ((InvokeStmt)u).getInvokeExpr();
            if (invoke instanceof VirtualInvokeExpr) {
                return ((VirtualInvokeExpr)invoke).getBase();
            } else if (invoke instanceof SpecialInvokeExpr) {
                return ((SpecialInvokeExpr) invoke).getBase();
            } else if (invoke instanceof InterfaceInvokeExpr) {
                return ((InterfaceInvokeExpr) invoke).getBase();
            }
        }
        return null;
    }
    public Value getInvokeAssignBase(Unit u) {
        if (u instanceof AssignStmt) {
            Value rval = ((AssignStmt)u).getRightOp();
            if (rval instanceof VirtualInvokeExpr) {
                return ((VirtualInvokeExpr)rval).getBase();
            } else if (rval instanceof SpecialInvokeExpr) {
                return ((SpecialInvokeExpr) rval).getBase();
            } else if (rval instanceof InterfaceInvokeExpr) {
                return ((InterfaceInvokeExpr) rval).getBase();
            }
        }
        return null;
    }

    public static String getMethodSignature(SootMethod method) {
        StringBuffer ret = new StringBuffer();
        ret.append(method.getDeclaringClass().toString());
        ret.append(".");
        ret.append(method.getName());
        List<Type> params = method.getParameterTypes();
        ret.append("(");
        for(int i = 0; i < params.size(); ++i) {
            ret.append(((Type)params.get(i)).getEscapedName());
            if (i < params.size() - 1) {
                ret.append(",");
            }
        }
        ret.append(")");
        ret.append(method.getReturnType().toString());
        return ret.toString().intern();
    }

    public static boolean byPass(Unit inst) {
    	boolean pass = false;
		if (inst instanceof AssignStmt) {
			AssignStmt assign = (AssignStmt) inst;
			Value rhs = assign.getRightOp();
			if (rhs instanceof LengthExpr)
			    pass = true;
			if (rhs instanceof InvokeExpr) {
				InvokeExpr expr = (InvokeExpr) rhs;
				SootClass klass = expr.getMethod().getDeclaringClass();
				if (klass.getName().startsWith("java")
						|| klass.getName().startsWith("sun")
						|| klass.getName().startsWith("javax"))
					pass = true;
			}
		}
		if (inst instanceof InvokeStmt) {
			InvokeExpr expr = (InvokeExpr) ((InvokeStmt) inst).getInvokeExpr();
			SootClass klass = expr.getMethod().getDeclaringClass();
			if (klass.getName().startsWith("java")
					|| klass.getName().startsWith("sun")
					|| klass.getName().startsWith("javax"))
				pass = true;
		}
		return pass;
	}

	public static final Map<String, String> notNullList = new HashMap<String, String>(){{
		put("android.content.Context", "getSystemService");
		put("android.app.Activity", "getSystemService");
		put("android.view.ContextThemeWrapper", "getSystemService");
		put("java.lang.String", "substring");
		put("java.lang.StringBuilder", "toString");
	}};

    public static final Set<String> exclusionList = new HashSet<String>(
        Arrays.asList(
            "java.lang.Object.equals(java.lang.Object)boolean",
            "java.lang.Object.hashCode()int",
			"java.lang.Object.<clinit>()void",
			"java.lang.Object.<init>()void",
			"java.lang.Object.registerNatives()void"
        )
    );
    public static final Set<String> skipMethodList = new HashSet<String>(
        Arrays.asList(
			"java.beans.XMLDecoder.getHandler()com.sun.beans.ObjectHandler",
			"java.beans.XMLDecoder.readObject()java.lang.Object",
			"java.beans.XMLEncoder.close()void",
			"java.io.ByteArrayOutputStream.toString()java.lang.String",
			"java.io.DataInputStream.readByte()byte",
			"java.io.DataInputStream.readFully(byte[])void",
			"java.io.DataInputStream.readInt()int",
			"java.io.File.canRead()boolean",
			"java.io.File.isFile()boolean",
			"java.io.File.lastModified()long",
			"java.io.File.mkdirs()boolean",
			"java.io.FileSystem.resolve(java.lang.String,java.lang.String)java.lang.String",
			"java.io.ObjectStreamClass.toString()java.lang.String",
			"java.io.ObjectStreamField.toString()java.lang.String",
			"java.io.PrintStream.print(char)void",
			"java.io.PrintStream.print(int)void",
			"java.io.PrintStream.print(java.lang.String)void ",
			"java.io.PrintStream.println(java.lang.Object)void",
			"java.io.PrintStream.println(java.lang.String)void ",
			"java.io.PrintWriter.print(char)void",
			"java.io.PrintWriter.print(java.lang.String)void",
			"java.io.PrintWriter.println(int)void",
			"java.io.PrintWriter.println(java.lang.String)void ",
			"java.io.PrintWriter.println()void",
			"java.lang.Byte.toString()java.lang.String",
			"java.lang.Character.toString()java.lang.String",
			"java.lang.Class.enumConstantDirectory()java.util.Map",
			"java.lang.Class.getClassLoader()java.lang.ClassLoader",
			"java.lang.Class.getField0(java.lang.String)java.lang.reflect.Field",
			"java.lang.Class.toString()java.lang.String",
			"java.lang.Double.toString()java.lang.String",
			"java.lang.Enum.toString()java.lang.String",
			"java.lang.Enum.valueOf(java.lang.Class,java.lang.String)java.lang.Enum",
			"java.lang.Float.toString()java.lang.String",
			"java.lang.Object.clone()java.lang.Object",
			"java.lang.Object.toString()java.lang.String",
			"java.lang.reflect.Method.toString()java.lang.String",
			"java.lang.Short.toString()java.lang.String",
			"java.lang.StringCoding.encode(char[],int,int)byte[]",
			"java.lang.String.equals(java.lang.Object)boolean",
			"java.lang.String.matches(java.lang.String)boolean",
			"java.lang.String.substring(int)java.lang.String",
			"java.lang.System.getProperties()java.util.Properties",
			"java.lang.Thread.getContextClassLoader()java.lang.ClassLoader",
			"java.lang.ThreadLocal.get()java.lang.Object",
			"java.lang.Thread.toString()java.lang.String",
			"java.lang.Throwable.printStackTrace(java.io.PrintWriter)void ",
			"java.lang.Throwable.printStackTrace()void",
			"java.math.BigInteger.toString()java.lang.String",
			"java.net.DatagramSocket.getImpl()java.net.DatagramSocketImpl",
			"java.net.InetAddress.getAllByName(java.lang.String)java.net.InetAddress[]",
			"java.net.InetAddress.getByName(java.lang.String)java.net.InetAddress",
			"java.net.MulticastSocket.getInterface()java.net.InetAddress",
			"java.net.Socket.getLocalAddress()java.net.InetAddress",
			"java.net.Socket.toString()java.lang.String",
			"java.net.URI.defineSchemeSpecificPart()void",
			"java.net.URI.getPath()java.lang.String",
			"java.net.URI.toURL()java.net.URL",
			"java.net.URLConnection.getContentType()java.lang.String",
			"java.net.URLEncoder.encode(java.lang.String,java.lang.String)java.lang.String",
			"java.nio.charset.Charset.name()java.lang.String",
			"java.sql.Connection.prepareStatement(java.lang.String)java.sql.PreparedStatement",
			"java.sql.PreparedStatement.executeQuery()java.sql.ResultSet",
			"java.sql.ResultSet.getString(java.lang.String)java.lang.String",
			"java.sql.ResultSetMetaData.getColumnCount()int",
			"java.sql.ResultSetMetaData.getColumnName(int)java.lang.String",
			"java.text.AttributedCharacterIterator$Attribute.toString()java.lang.String",
			"java.text.AttributeEntry.toString()java.lang.String",
			"java.text.DateFormat.format(java.util.Date)java.lang.String",
			"java.text.FieldPosition.toString()java.lang.String",
			"java.util.AbstractCollection.toString()java.lang.String",
			"java.util.Calendar.get(int)int",
			"java.util.Collection.iterator()java.util.Iterator",
			"java.util.Currency.toString()java.lang.String",
			"java.util.Date.toString()java.lang.String",
			"java.util.Formatter$FixedString.toString()java.lang.String",
			"java.util.Formatter$FormatSpecifier.toString()java.lang.String",
			"java.util.HashSet.contains(java.lang.Object)boolean",
			"java.util.Iterator.next()java.lang.Object",
			"java.util.jar.Attributes$Name.toString()java.lang.String",
			"java.util.LinkedHashMap.get(java.lang.Object)java.lang.Object",
			"java.util.LinkedList.clear()void",
			"java.util.LinkedList.clone()java.lang.Object",
			"java.util.List.get(int)java.lang.Object",
			"java.util.Locale.getDisplayName()java.lang.String",
			"java.util.Locale.getDisplayName(java.util.Locale)java.lang.String",
			"java.util.Locale.getInstance(java.lang.String,java.lang.String,java.lang.String)java.util.Locale",
			"java.util.logging.Level.toString()java.lang.String",
			"java.util.Map.get(java.lang.Object)java.lang.Object",
			"java.util.Map.keySet()java.util.Set",
			"java.util.Map.values()java.util.Collection",
			"java.util.Scanner.hasNextLine()boolean",
			"java.util.Vector.listIterator()java.util.ListIterator",
			"java.util.Vector.toString()java.lang.String",
			"java.util.zip.ZipInputStream.getNextEntry()java.util.zip.ZipEntry",
			"javax.xml.parsers.DocumentBuilderFactory.newInstance()javax.xml.parsers.DocumentBuilderFactory",
			"javax.xml.stream.XMLInputFactory.newInstance()javax.xml.stream.XMLInputFactory",
			"javax.xml.stream.XMLOutputFactory.newInstance()javax.xml.stream.XMLOutputFactory",
			"javax.xml.transform.TransformerFactory.newInstance()javax.xml.transform.TransformerFactory",
			"javolution.util.FastMap.get(java.lang.Object)java.lang.Object",
			"org.w3c.dom.Document.getFirstChild()org.w3c.dom.Node",
			"org.w3c.dom.NamedNodeMap.getNamedItem(java.lang.String)org.w3c.dom.Node",
			"org.w3c.dom.Node.getAttributes()org.w3c.dom.NamedNodeMap",
			"org.w3c.dom.Node.getNodeValue()java.lang.String",
			"java.io.File.isDirectory()boolean",
			"java.io.File.listFiles()java.io.File[]",
			"java.io.File.toString()java.lang.String",
			"java.io.File.toURI()java.net.URI",
			"java.io.ObjectInputStream.readClassDesc(boolean)java.io.ObjectStreamClass",
			"java.io.PrintWriter.println(java.lang.String)void",
			"java.lang.Boolean.toString()java.lang.String",
			"java.lang.Character$Subset.toString()java.lang.String",
			"java.lang.Class.getDeclaredField(java.lang.String)java.lang.reflect.Field",
			"java.lang.Class.getField(java.lang.String)java.lang.reflect.Field",
			"java.lang.Integer.toString()java.lang.String",
			"java.lang.Long.toString()java.lang.String",
			"java.lang.StackTraceElement.toString()java.lang.String",
			"java.lang.String.toString()java.lang.String",
			"java.lang.Throwable.printStackTrace(java.io.PrintStream)void",
			"java.lang.Throwable.printStackTrace(java.io.PrintWriter)void",
			"java.lang.Throwable.toString()java.lang.String",
			"java.net.Socket.getImpl()java.net.SocketImpl",
			"java.security.PrivilegedActionException.toString()java.lang.String",
			"java.util.AbstractList.listIterator()java.util.ListIterator",
			"java.util.AbstractMap.toString()java.lang.String",
			"java.util.HashMap.get(java.lang.Object)java.lang.Object",
			"java.util.Locale.toString()java.lang.String",
			"java.io.File.exists()boolean",
			"java.io.PrintStream.println()void",
			"java.lang.String.getBytes()byte[]",
			"java.lang.String.valueOf(java.lang.Object)java.lang.String",
			"java.util.Locale.getDefault()java.util.Locale",
			"java.io.PrintStream.println(java.lang.String)void",
			"java.util.AbstractSequentialList.iterator()java.util.Iterator",
			"java.io.PrintStream.print(java.lang.String)void"
        )
    );
    public static final Set<String> analyzeLibCallList = new HashSet<String>(
        Arrays.asList(
			"java.beans.XMLDecoder.close()void",
			"java.io.BufferedInputStream.read()int",
			"java.io.BufferedWriter.<init>(java.io.Writer)void",
			"java.io.ByteArrayInputStream.read()int",
			"java.io.DataInputStream.<init>(java.io.InputStream)void",
			"java.io.File.<init>(java.io.Filejava.lang.String)void",
			"java.io.FileInputStream.<init>(java.io.File)void",
			"java.io.FileOutputStream.<init>(java.io.File)void",
			"java.io.FileReader.<init>(java.io.File)void",
			"java.io.ObjectInputStream$BlockDataInputStream.read()int",
			"java.io.PrintStream.<init>(java.io.OutputStream)void",
			"java.io.PrintWriter.<init>(java.io.OutputStream)void",
			"java.io.RandomAccessFile.<init>(java.io.Filejava.lang.String)void",
			"java.io.StreamTokenizer.nextToken()int",
			"java.lang.ConditionalSpecialCasing.toLowerCaseCharArray(java.lang.String,int,java.util.Locale)char[]",
			"java.lang.ConditionalSpecialCasing.toLowerCaseEx(java.lang.String,int,java.util.Locale)int",
			"java.lang.StringBuffer.append(java.lang.Object)java.lang.StringBuffer",
			"java.lang.StringBuilder.append(java.lang.Object)java.lang.StringBuilder",
			"java.lang.String.<init>(byte[],int,int,java.lang.String)void",
			"java.lang.String.<init>(byte[],int,int)void",
			"java.lang.String.<init>(byte[],java.lang.String)void",
			"java.lang.String.<init>(byte[])void",
			"#java.lang.String.matches(java.lang.String)boolean",
			"java.lang.String.toLowerCase()java.lang.String",
			"java.lang.String.toLowerCase(java.util.Locale)java.lang.String",
			"java.lang.System.exit(int)void",
			"java.lang.System.setProperties(java.util.Properties)void",
			"java.lang.ThreadLocal.setInitialValue()java.lang.Object",
			"java.lang.ThreadLocal$ThreadLocalMap.access$000(java.lang.ThreadLocal$ThreadLocalMapjava.lang.ThreadLocal)java.lang.ThreadLocal$ThreadLocalMap$Entry",
			"java.lang.Thread.start()void",
			"java.net.Authenticator.setDefault(java.net.Authenticator)void",
			"java.net.DatagramSocket.receive(java.net.DatagramPacket)void",
			"java.net.DatagramSocket.send(java.net.DatagramPacket)void",
			"java.net.URL.<init>(java.lang.String)void",
			"java.text.SimpleDateFormat.<init>(java.lang.Stringjava.util.Locale)void",
			"java.util.Arrays.sort(java.lang.Object[],int,int)void",
			"java.util.concurrent.locks.ReentrantLock.lock()void",
			"java.util.concurrent.ScheduledThreadPoolExecutor.delayedExecute(java.util.concurrent.RunnableScheduledFuture)void",
			"java.util.concurrent.ScheduledThreadPoolExecutor.scheduleAtFixedRate(java.lang.RunnableJJjava.util.concurrent.TimeUnit)java.util.concurrent.ScheduledFuture",
			"java.util.concurrent.ScheduledThreadPoolExecutor.schedule(java.lang.RunnableJjava.util.concurrent.TimeUnit)java.util.concurrent.ScheduledFuture",
			"java.util.concurrent.ThreadPoolExecutor.execute(java.lang.Runnable)void",
			"#java.util.HashMap.get(java.lang.Object)java.lang.Object",
			"java.util.jar.JarOutputStream.<init>(java.io.OutputStreamjava.util.jar.Manifest)void",
			"java.util.jar.JarOutputStream.putNextEntry(java.util.zip.ZipEntry)void",
			"#java.util.LinkedList.add(int,java.lang.Object)void",
			"java.util.LinkedList.addLast(java.lang.Object)void",
			"java.util.LinkedList.add(java.lang.Object)boolean",
			"java.util.LinkedList.<init>()void",
			"java.util.LinkedList.listIterator(int)java.util.ListIterator",
			"java.util.LinkedList$ListItr.add(java.lang.Object)void",
			"java.util.LinkedList$ListItr.next()java.lang.Object",
			"java.util.LinkedList$ListItr.previous()java.lang.Object",
			"java.util.LinkedList.removeFirst()java.lang.Object",
			"java.util.LinkedList.remove(java.lang.Object)boolean",
			"java.util.logging.Logger.fine(java.lang.String)void",
			"java.util.logging.Logger.info(java.lang.String)void",
			"java.util.logging.Logger.warning(java.lang.String)void",
			"java.util.logging.LogManager.readPrimordialConfiguration()void",
			"java.util.Vector$Itr.next()java.lang.Object",
			"java.util.zip.DeflaterOutputStream.close()void",
			"java.util.zip.ZipEntry.<init>(java.lang.String)void",
			"java.util.zip.ZipFile$2.nextElement()java.lang.Object",
			"java.util.zip.ZipOutputStream.putNextEntry(java.util.zip.ZipEntry)void",
			"java.lang.StringCoding.decode(byte[],int,int)char[]",
			"java.lang.Thread.run()void",
			"java.lang.System.setProperty(java.lang.Stringjava.lang.String)java.lang.String",
			"java.lang.StringCoding.decode(java.lang.String,byte[],int,int)char[]",
			"java.lang.Runtime.exit(int)void",
			"java.lang.Shutdown.exit(int)void",
			"java.nio.charset.Charset.defaultCharset()java.nio.charset.Charset",
			"java.lang.StringCoding.lookupCharset(java.lang.String)java.nio.charset.Charset",
			"java.lang.Shutdown.sequence()void",
			"java.nio.charset.Charset.forName(java.lang.String)java.nio.charset.Charset",
			"java.security.AccessController.doPrivileged(java.security.PrivilegedAction)java.lang.Object",
			"java.lang.SecurityManager.checkExit(int)void",
			"java.nio.charset.Charset.lookup(java.lang.String)java.nio.charset.Charset",
			"java.nio.charset.Charset.isSupported(java.lang.String)boolean",
			"java.lang.SecurityManager.checkPermission(java.security.Permission)void",
			"java.lang.Shutdown.runHooks()void",
			"javax.xml.transform.SecuritySupport$1.run()java.lang.Object",
			"javax.xml.transform.SecuritySupport$2.run()java.lang.Object",
			"javax.xml.transform.SecuritySupport$3.run()java.lang.Object",
			"javax.xml.transform.SecuritySupport$4.run()java.lang.Object",
			"javax.xml.transform.SecuritySupport$5.run()java.lang.Object",
			"java.util.List.add(java.lang.Object)boolean"
        )
    );
}
