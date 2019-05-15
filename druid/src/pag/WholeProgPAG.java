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

package pag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pag.builder.GlobalPAGBuilder;
import pag.node.GNode;
import pag.node.alloc.AllocDotField_Node;
import pag.node.alloc.Alloc_Node;
import pag.node.alloc.ClassConstant_Node;
import pag.node.alloc.StringConstant_Node;
import pag.node.var.FieldRef_Node;
import pag.node.var.GlobalVar_Node;
import pag.node.var.LocalVar_Node;
import pag.node.var.Var_Node;
import pta.pts.DoublePTSet;
import pta.pts.HybridPTSet;
import pta.pts.PTSetFactory;

import static driver.DruidOptions.sparkOpts;

import soot.FastHierarchy;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.jimple.ClassConstant;
import soot.jimple.StringConstant;
import soot.jimple.spark.pag.SparkField;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.pointer.util.NativeMethodDriver;
import soot.options.SparkOptions;
import soot.tagkit.LinkTag;
import soot.tagkit.StringTag;
import soot.tagkit.Tag;
import soot.toolkits.scalar.Pair;
import soot.util.ArrayNumberer;
import soot.util.LargeNumberedMap;
import soot.util.queue.ChunkedQueue;
import soot.util.queue.QueueReader;
import util.DruidNativeHelper;
import util.StringConstantByMethod;
import util.TypeMask;

/** Pointer assignment graph.
 * @author Ondrej Lhotak
 */
public class WholeProgPAG {
	//==========================outer objects==============================
	public NativeMethodDriver nativeMethodDriver;
	protected TypeMask typeManager;
	private GlobalPAGBuilder nodeFactory;
	public PTSetFactory setFactory;

    //==========================parms==============================
    public int maxFinishNumber = 0;
	protected boolean somethingMerged = false;
	
	//==========================data=========================
	public final Map<Object, Alloc_Node> valToAllocNode = new HashMap<Object, Alloc_Node>(10000);
	protected final static GNode[] EMPTY_NODE_ARRAY = new GNode[0];
	protected final static Alloc_Node[] EMPTY_ALLOC_NODE_ARRAY = new Alloc_Node[0];
	protected final static Var_Node[] EMPTY_VAR_NODE_ARRAY = new Var_Node[0];
	protected final static FieldRef_Node[] EMPTY_FIELDREF_NODE_ARRAY = new FieldRef_Node[0];
	private final Map<Object, LocalVar_Node> valToLocalVarNode = new HashMap<Object, LocalVar_Node>(100000);
    private final Map<Object, GlobalVar_Node> valToGlobalVarNode = new HashMap<Object, GlobalVar_Node>(100000);
    private final ArrayList<Var_Node> dereferences = new ArrayList<Var_Node>();
    private final LargeNumberedMap<Local, LocalVar_Node> localToNodeMap = new LargeNumberedMap<Local, LocalVar_Node>( Scene.v().getLocalNumberer() );
    private final ArrayNumberer<Alloc_Node> allocNodeNumberer = new ArrayNumberer<Alloc_Node>();
    private final ArrayNumberer<Var_Node> varNodeNumberer = new ArrayNumberer<Var_Node>();
    private final ArrayNumberer<FieldRef_Node> fieldRefNodeNumberer = new ArrayNumberer<FieldRef_Node>();
    //temporary hack to reduce memory-inefficiency (use new field additionalVirtualCalls instead of callAssigns)
    //public HashMultiMap /* InvokeExpr -> Set[Pair] */ callAssigns = new HashMultiMap();
    protected ChunkedQueue<GNode> edgeQueue = new ChunkedQueue<GNode>();
    protected QueueReader<GNode> initialReader;
	ChunkedQueue<Alloc_Node> newAllocNodes = new ChunkedQueue<Alloc_Node>();
	private Map<GNode, Tag> nodeToTag;
	protected Map<Var_Node, Set<Var_Node>> simple = new HashMap<Var_Node, Set<Var_Node>>();
    protected Map<FieldRef_Node, Set<Var_Node>> load = new HashMap<FieldRef_Node, Set<Var_Node>>();
    protected Map<Var_Node, Set<FieldRef_Node>> store = new HashMap<Var_Node, Set<FieldRef_Node>>();
    protected Map<Alloc_Node, Set<Var_Node>> alloc = new HashMap<Alloc_Node, Set<Var_Node>>();
    protected Map<Var_Node, Set<Var_Node>> simpleInv = new HashMap<Var_Node, Set<Var_Node>>();
    protected Map<Var_Node, Set<FieldRef_Node>> loadInv = new HashMap<Var_Node, Set<FieldRef_Node>>();
    protected Map<FieldRef_Node, Set<Var_Node>> storeInv = new HashMap<FieldRef_Node, Set<Var_Node>>();
    protected Map<Var_Node, Set<Alloc_Node>> allocInv = new HashMap<Var_Node, Set<Alloc_Node>>();
	
    public WholeProgPAG() {
		setupPTSOptions();
    	if( sparkOpts.add_tags() ) {
            nodeToTag = new HashMap<GNode, Tag>();
        }
        if (sparkOpts.simulate_natives()) {
			nativeMethodDriver = new NativeMethodDriver(new DruidNativeHelper(this));
		}
        typeManager = new TypeMask(this);
        if( !sparkOpts.ignore_types() ) {
            typeManager.setFastHierarchy( Scene.v().getOrMakeFastHierarchy() );
        }
    }
    private void setupPTSOptions(){
		switch( sparkOpts.set_impl() ) {
        case SparkOptions.set_impl_double:
            PTSetFactory oldF;
            PTSetFactory newF;
            switch( sparkOpts.double_set_old() ) {
                case SparkOptions.double_set_old_hybrid:
                    oldF = HybridPTSet.getFactory();
                    break;
                default:
                    throw new RuntimeException();
            }
            switch( sparkOpts.double_set_new() ) {
                case SparkOptions.double_set_new_hybrid:
                    newF = HybridPTSet.getFactory();
                    break;
                default:
                    throw new RuntimeException();
            }
            setFactory = DoublePTSet.getFactory( newF, oldF );
            break;
        default:
            throw new RuntimeException();
		}
	}
    public SparkOptions getOpts(){
    	return sparkOpts;
    }
   
    //=======================cleanPAG===========================
    public void cleanPAG(){
        simple.clear();
        load.clear();
        store.clear();
        alloc.clear();
        simpleInv.clear();
        loadInv.clear();
        storeInv.clear();
        allocInv.clear();
    }
    
	//========================getters and setters=========================
    public GlobalPAGBuilder GlobalNodeFactory() { return nodeFactory; }
	public void setGlobalNodeFactory(GlobalPAGBuilder nodeFactory) { this.nodeFactory = nodeFactory;}
    public ArrayNumberer<Alloc_Node> getAllocNodeNumberer() { return allocNodeNumberer; }
    public ArrayNumberer<Var_Node> getVarNodeNumberer() { return varNodeNumberer; }   
    public ArrayNumberer<FieldRef_Node> getFieldRefNodeNumberer() { return fieldRefNodeNumberer; }  
    public PTSetFactory getSetFactory() {return setFactory;}
    public TypeMask getTypeManager() {return typeManager;}
    public Map<Alloc_Node, Set<Var_Node>> getAlloc(){return alloc;}
    public Map<Var_Node, Set<Var_Node>> getSimple(){return simple;}
    public Map<FieldRef_Node, Set<Var_Node>> getLoad(){return load;}
    public Map<Var_Node, Set<FieldRef_Node>> getStore(){return store;}
    public List<Var_Node> getDereferences() {return dereferences;} /** Returns list of dereferences variables. */
   
    //===============================read data==========================
    public QueueReader<Alloc_Node> allocNodeListener() { return newAllocNodes.reader(); }
    public QueueReader<GNode> edgeReader() { return edgeQueue.reader(); }
    
    public Set<Alloc_Node> getAllocNodes() {
        Set<Alloc_Node> nodes = new HashSet<Alloc_Node>();
        nodes.addAll(valToAllocNode.values());
        return nodes;
    }
    public Set<Object> getGlobalPointers() {
        return valToGlobalVarNode.keySet();
    }
    public Set<Local> getLocalPointers() {
        Set<Local> numberedLocals = new HashSet<Local>();
        for (Iterator<Local> localsIt = localToNodeMap.keyIterator();localsIt.hasNext();)
            numberedLocals.add(localsIt.next());
        return numberedLocals;
    }
    
    public Set<Var_Node> simpleSources() { return simple.keySet(); }
    public Set<Alloc_Node> allocSources() { return alloc.keySet(); }
    public Set<Var_Node> storeSources() { return store.keySet(); }
    public Set<FieldRef_Node> loadSources() { return load.keySet(); }
    public Set<Var_Node> simpleInvSources() { return simpleInv.keySet(); }
    public Set<Var_Node> allocInvSources() { return allocInv.keySet(); }
    public Set<FieldRef_Node> storeInvSources() { return storeInv.keySet(); }
    public Set<Var_Node> loadInvSources() { return loadInv.keySet(); }
    
    public Iterator<Var_Node> simpleSourcesIterator() { return simple.keySet().iterator(); }
    public Iterator<Alloc_Node> allocSourcesIterator() { return alloc.keySet().iterator(); }
    public Iterator<Var_Node> storeSourcesIterator() { return store.keySet().iterator(); }
    public Iterator<FieldRef_Node> loadSourcesIterator() { return load.keySet().iterator(); }
    public Iterator<Var_Node> simpleInvSourcesIterator() { return simpleInv.keySet().iterator(); }
    public Iterator<Var_Node> allocInvSourcesIterator() { return allocInv.keySet().iterator(); }
    public Iterator<FieldRef_Node> storeInvSourcesIterator() { return storeInv.keySet().iterator(); }
    public Iterator<Var_Node> loadInvSourcesIterator() { return loadInv.keySet().iterator(); }

    //========================write data=====================================
    /** Adds the base of a dereference to the list of dereferenced 
     * variables. */
    public void addDereference( Var_Node base ) {dereferences.add( base );}
    public void addNodeTag( GNode node, SootMethod m ) {
        if( nodeToTag != null ) {
            Tag tag;
            if( m == null ) {
                tag = new StringTag( node.toString() );
            } else {
                tag = new LinkTag( node.toString(), m, m.getDeclaringClass().getName() );
            }
            nodeToTag.put( node, tag );
        }
    }
        
    //=======================add edge===============================
    protected <K, V> boolean addToMap( Map<K, Set<V>> m, K key, V value ) {
        Set<V> valueList = m.get( key );
        if( valueList == null ) {
            m.put( key, valueList = new HashSet<V>(4) );
        }
        return valueList.add( value );
    }
    
    public boolean doAddSimpleEdge( Var_Node from, Var_Node to ) {
        return addToMap( simple, from, to ) && addToMap( simpleInv, to, from );
    }
    public boolean doAddStoreEdge( Var_Node from, FieldRef_Node to ) {
        return addToMap( store, from, to ) && addToMap( storeInv, to, from );
    }
    public boolean doAddLoadEdge( FieldRef_Node from, Var_Node to ) {
        return addToMap( load, from, to ) && addToMap( loadInv, to, from );
    }
    public boolean doAddAllocEdge( Alloc_Node from, Var_Node to ) {
        return addToMap( alloc, from, to ) && addToMap( allocInv, to, from );
    }
    
    public boolean addAllocEdge( Alloc_Node from, Var_Node to ) {
        FastHierarchy fh = typeManager.getFastHierarchy();
        if( fh == null || to.getType() == null 
                || fh.canStoreType( from.getType(), to.getType() ) ) {
            if( doAddAllocEdge( from, to ) ) {
                edgeQueue.add( from );
                edgeQueue.add( to );
                return true;
            }
        }
        return false;
    }
    public boolean addSimpleEdge( Var_Node from, Var_Node to ) {
        boolean ret = false;
        if( doAddSimpleEdge( from, to ) ) {
            edgeQueue.add( from );
            edgeQueue.add( to );
            ret = true;
        }
        if( sparkOpts.simple_edges_bidirectional() ) {
            if( doAddSimpleEdge( to, from ) ) {
                edgeQueue.add( to );
                edgeQueue.add( from );
                ret = true;
            }
        }
        return ret;
    }
    public boolean addStoreEdge( Var_Node from, FieldRef_Node to ) {
        if( !sparkOpts.rta() ) {
            if( doAddStoreEdge( from, to ) ) {
                edgeQueue.add( from );
                edgeQueue.add( to );
                return true;
            }
        }
        return false;
    }
    public boolean addLoadEdge( FieldRef_Node from, Var_Node to ) {
        if( !sparkOpts.rta() ) {
            if( doAddLoadEdge( from, to ) ) {
                edgeQueue.add( from );
                edgeQueue.add( to );
                return true;
            }
        }
        return false;
    }

    /** Adds an edge to the graph, returning false if it was already there. */
    public final boolean addEdge( GNode from, GNode to ) {
        from = from.getReplacement();
        to = to.getReplacement();
        if( from instanceof Var_Node ) {
            if( to instanceof Var_Node ) {
                return addSimpleEdge( (Var_Node) from, (Var_Node) to );
            } else {
                return addStoreEdge( (Var_Node) from, (FieldRef_Node) to );
            }
        } else if( from instanceof FieldRef_Node ) {
            return addLoadEdge( (FieldRef_Node) from, (Var_Node) to );

        } else {
            return addAllocEdge( (Alloc_Node) from, (Var_Node) to );
        }
    }
    // Must be simple edges
    public Pair<GNode, GNode> addInterproceduralAssignment(GNode from, GNode to, Edge e) 
    {
        return new Pair<GNode, GNode>(from, to);
    }
    
    //======================lookups===========================
    protected <K, V> V[] lookup( Map<K, Set<V>> m, K key, V[] emptyArray) {
        Set<V> valueList = m.get( key );
        if( valueList == null )
            return emptyArray;
        return valueList.toArray(emptyArray);
    }
    public Var_Node[] simpleLookup( Var_Node key ) { return lookup( simple, key, EMPTY_VAR_NODE_ARRAY); }
    public Var_Node[] simpleInvLookup( Var_Node key ) { return lookup( simpleInv, key, EMPTY_VAR_NODE_ARRAY ); }
    public Var_Node[] loadLookup( FieldRef_Node key ) { return lookup( load, key, EMPTY_VAR_NODE_ARRAY ); }
    public FieldRef_Node[] loadInvLookup( Var_Node key ) { return lookup( loadInv, key, EMPTY_FIELDREF_NODE_ARRAY ); }
    public FieldRef_Node[] storeLookup( Var_Node key ) { return lookup( store, key , EMPTY_FIELDREF_NODE_ARRAY); }
    public Var_Node[] storeInvLookup( FieldRef_Node key ) { return lookup( storeInv, key, EMPTY_VAR_NODE_ARRAY ); }
    public Var_Node[] allocLookup( Alloc_Node key ) { return lookup( alloc, key , EMPTY_VAR_NODE_ARRAY); }
    public Alloc_Node[] allocInvLookup( Var_Node key ) { return lookup( allocInv, key , EMPTY_ALLOC_NODE_ARRAY); }

    //===================find nodes==============================
    /** Finds the GlobalVarNode for the variable value, or returns null. */
    public GlobalVar_Node findGlobalVarNode( Object value ) {
        if( sparkOpts.rta() ) {
            value = null;
        }
        return valToGlobalVarNode.get( value );
    }
    /** Finds the LocalVarNode for the variable value, or returns null. */
    public LocalVar_Node findLocalVarNode( Object value ) {
        if( sparkOpts.rta() ) {
            value = null;
        } else if( value instanceof Local ) {
            return localToNodeMap.get( (Local) value );
        }
        return valToLocalVarNode.get( value );
    }
    /** Finds the FieldRefNode for base variable value and field
     * field, or returns null. */
    public FieldRef_Node findLocalFieldRefNode( Object baseValue, SparkField field ) {
        Var_Node base = findLocalVarNode( baseValue );
        if( base == null ) return null;
        return base.dot( field );
    }
    /** Finds the FieldRefNode for base variable value and field
     * field, or returns null. */
    public FieldRef_Node findGlobalFieldRefNode( Object baseValue, SparkField field ) {
        Var_Node base = findGlobalVarNode( baseValue );
        if( base == null ) return null;
        return base.dot( field );
    }
    /** Finds the AllocDotField for base AllocNode an and field
     * field, or returns null. */
    public AllocDotField_Node findAllocDotField( Alloc_Node an, SparkField field ) {
        return an.dot( field );
    }
    
    //==========================create nodes==================================
    public Alloc_Node makeAllocNode( Object newExpr, Type type, SootMethod m) {
        if( sparkOpts.types_for_sites() || sparkOpts.vta() ) newExpr = type;

        Alloc_Node ret = valToAllocNode.get( newExpr );

        if( ret == null ) {
            valToAllocNode.put( newExpr, ret = new Alloc_Node( this, newExpr, type, m) );
            addNodeTag( ret, m );
        } else if( !( ret.getType().equals( type ) ) ) {
            throw new RuntimeException( "NewExpr "+newExpr+" of type "+type+
                " previously had type "+ret.getType() );
        }
        return ret;
    }
    public Alloc_Node makeStringConstantNode( StringConstant s, SootMethod method ) {
        if( sparkOpts.types_for_sites() || sparkOpts.vta() )
            return makeAllocNode( RefType.v( "java.lang.String" ),
                RefType.v( "java.lang.String" ), null );
        
        //we are using a wrapper class on jimple string constant that includes
        //the method that declared the string constant, this allows
        //us to use a local var node in the pag to point to the string constant
        //and thus parameterize the local var node for more precision

        //rely on equals of StringConstantByMethod to create a probe on 
        //the map
        StringConstantByMethod probe = new StringConstantByMethod(s, method);
        
        if (valToAllocNode.containsKey(probe)) {
            return valToAllocNode.get(probe);
        } else {
            StringConstant_Node ret = new StringConstant_Node(this, probe);
            valToAllocNode.put(probe, ret);
            addNodeTag(ret, null);
            return ret;
        }
    }
    public Alloc_Node makeClassConstantNode( ClassConstant cc ) {
        if( sparkOpts.types_for_sites() || sparkOpts.vta() )
            return makeAllocNode( RefType.v( "java.lang.Class" ),
                RefType.v( "java.lang.Class" ), null );

        ClassConstant_Node ret = (ClassConstant_Node) valToAllocNode.get(cc);
        if( ret == null ) {
            valToAllocNode.put(cc, ret = new ClassConstant_Node(this, cc));
            addNodeTag( ret, null );
        }
        return ret;
    }
    /** Finds or creates the GlobalVarNode for the variable value, of type type. */
    public GlobalVar_Node makeGlobalVarNode( Object value, Type type ) {
        if( sparkOpts.rta() ) {
            value = null;
            type = RefType.v("java.lang.Object");
        }
        GlobalVar_Node ret = valToGlobalVarNode.get( value );
        if( ret == null ) {
            valToGlobalVarNode.put( value, 
                ret = new GlobalVar_Node( this, value, type ) );
            addNodeTag( ret, null );
        } else if( !( ret.getType().equals( type ) ) ) {
            throw new RuntimeException( "Value "+value+" of type "+type+
                " previously had type "+ret.getType() );
        }
        return ret;
    }
    /** Finds or creates the LocalVarNode for the variable value, of type type. */
    public LocalVar_Node makeLocalVarNode( Object value, Type type, SootMethod method ) {
        if( sparkOpts.rta() ) {
            value = null;
            type = RefType.v("java.lang.Object");
            method = null;
        } else if( value instanceof Local ) {
            Local val = (Local) value;
            if( val.getNumber() == 0 ) Scene.v().getLocalNumberer().add(val);
            LocalVar_Node ret = localToNodeMap.get( val );
            if( ret == null ) {
                localToNodeMap.put( (Local) value,
                    ret = new LocalVar_Node( this, value, type, method ) );
                addNodeTag( ret, method );
            } else if( !( ret.getType().equals( type ) ) ) {
                throw new RuntimeException( "Value "+value+" of type "+type+
                    " previously had type "+ret.getType() );
            }
            return ret;
        }
        LocalVar_Node ret = valToLocalVarNode.get( value );
        if( ret == null ) {
            valToLocalVarNode.put( value, 
                ret = new LocalVar_Node( this, value, type, method ) );
            addNodeTag( ret, method );
        } else if( !( ret.getType().equals( type ) ) ) {
            throw new RuntimeException( "Value "+value+" of type "+type+
                " previously had type "+ret.getType() );
        }
        return ret;
    }
    /** Finds or creates the FieldRefNode for base variable baseValue and field
     * field, of type type. */
    public FieldRef_Node makeLocalFieldRefNode( Object baseValue, Type baseType,
                                               SparkField field, SootMethod method ) {
        Var_Node base = makeLocalVarNode( baseValue, baseType, method );
        return makeFieldRefNode( base, field );
    }
    /** Finds or creates the FieldRefNode for base variable baseValue and field
     * field, of type type. */
    public FieldRef_Node makeGlobalFieldRefNode( Object baseValue, Type baseType,
                                                SparkField field ) {
        Var_Node base = makeGlobalVarNode( baseValue, baseType );
        return makeFieldRefNode( base, field );
    }
    /** Finds or creates the FieldRefNode for base variable base and field
     * field, of type type. */
    public FieldRef_Node makeFieldRefNode( Var_Node base, SparkField field ) {
        FieldRef_Node ret = base.dot( field );
        if( ret == null ) {
            ret = new FieldRef_Node( this, base, field );
            if( base instanceof LocalVar_Node ) {
                addNodeTag( ret, ((LocalVar_Node) base).getMethod() );
            } else {
                addNodeTag( ret, null );
            }
        }
        return ret;
    }
    /** Finds or creates the AllocDotField for base variable baseValue and field
     * field, of type t. */
    public AllocDotField_Node makeAllocDotField( Alloc_Node an, SparkField field ) {
        AllocDotField_Node ret = an.dot( field );
        if( ret == null ) {
            ret = new AllocDotField_Node( this, an, field );
        }
        return ret;
    }

    //=====================merges================
    /** Node uses this to notify PAG that n2 has been merged into n1. */
    public void mergedWith( GNode n1, GNode n2 ) {}

	public void setInitialReader() {
		initialReader=edgeReader();
	}
}