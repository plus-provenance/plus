/* Copyright 2014 MITRE Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mitre.provenance.db.neo4j;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.dag.ViewedCollection;
import org.mitre.provenance.npe.NonProvenanceEdge;
import org.mitre.provenance.plusobject.PLUSActivity;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.surrogate.SurrogateGeneratingFunction;
import org.mitre.provenance.tools.LRUCache;
import org.mitre.provenance.user.PrivilegeClass;
import org.mitre.provenance.user.PrivilegeSet;
import org.mitre.provenance.user.User;
import org.mitre.provenance.workflows.BulkRun;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.traversal.TraversalDescription;

/**
 * Storage layer for provenance.  Handles the storage and loading of objects from
 * Neo4J into the PLUS API
 * @see org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory
 * @author DMALLEN
 */
public class Neo4JStorage {
	protected static Logger log = Logger.getLogger(Neo4JStorage.class.getName());
	
	protected static final boolean USE_CACHING = false;
	protected static LRUCache<String,Node> cache = new LRUCache<String,Node>(1000);
	
	public static final String METADATA_PREFIX = "metadata";
	
	/** Neo4J relationship type: one object input to another */
	public static final RT INPUT_TO = new RT(PLUSEdge.EDGE_TYPE_INPUT_TO);
	
	/** Neo4J relationship type: one object contributes to another */
	public static final RT CONTRIBUTED = new RT(PLUSEdge.EDGE_TYPE_CONTRIBUTED);
	
	/** Neo4J relationship type: one object marks another */
	public static final RT MARKS = new RT(PLUSEdge.EDGE_TYPE_MARKS);
	
	/** Neo4J relationship type: one object generated another */
	public static final RT GENERATED = new RT(PLUSEdge.EDGE_TYPE_GENERATED);
	
	/** Neo4J relationship type: one object triggered another */
	public static final RT TRIGGERED = new RT(PLUSEdge.EDGE_TYPE_TRIGGERED);
	
	/** Neo4J relationship type: unspecified relationship */
	public static final RT UNSPECIFIED = new RT(PLUSEdge.EDGE_TYPE_UNSPECIFIED);
	
	/** Neo4J relationship type: this edge is an NPE */
	public static final RT NPE = new RT("NPE");
	
	/** Neo4J relationship type: head of relationship owns the tail */
	public static final RT OWNS = new RT("owns"); 
	
	/** Neo4J relationship type: head of relationship is owned by tail */
	public static final RT CONTROLLED_BY = new RT("controlledBy");
	
	/** Neo4J relationship type:  PrivilegeClass at head of relationship dominates tail */ 
	public static final RT DOMINATES = new RT("dominates");
	
	/** ID property on all provenance objects */
	public static final String PROP_PLUSOBJECT_ID = "oid";

	/** ID property on all actors */
	public static final String PROP_ACTOR_ID = "aid";

	/** ID property on all privilege classes */
	public static final String PROP_PRIVILEGE_ID = "pid";	
	
	/** ID property on all non-provenance object nodes */
	public static final String PROP_NONPROV_ID = "npid";
	
	/** Property that indicates node type */
	public static final String PROP_TYPE = "type";
	
	/** Property that indicates node subtype */
	public static final String PROP_SUBTYPE = "subtype";	
	
	/** ID property on non-provenance EDGES */
	public static final String PROP_NPEID = "npeid";
	
	/** Property that indicates create date/time (long integer, ms since epoch) */
	public static final String PROP_CREATED = "created";
	
	/** Property that indicate workflow ID */
	public static final String PROP_WORKFLOW = "workflow";
	
	/** Property that indicates name */
	public static final String PROP_NAME = "name";
	
	/** Maximum path link that will be traversed as part of cypher queries */
	public static final int MAX_PATH_LENGTH = 100;

	/** Reference to the Neo4J Graph Database service */
	protected static GraphDatabaseService db = null;	
	
	/** Label affixed to all provenance object nodes */
	protected static Label LABEL_NODE = null;
	
	/** Label affixed to all PrivilegeClass nodes */ 
	protected static Label LABEL_PRIVCLASS = null;
	
	/** Label affixed to all PLUSActor nodes */
	protected static Label LABEL_ACTOR = null;	
	
	/** Label affixed to all non provenance ID nodes */
	protected static Label LABEL_NONPROV = null;
	
	/**
	 * Class that defines relationship types in Neo4J
	 * @see org.neo4j.graphdb.RelationshipType
	 */
	public static class RT implements RelationshipType {
		public String name = null;
		public RT(String name) { this.name = name; }
		public String name() { return name; } 
	}
		
	/**
	 * This function gets executed when a new database is being established.  This pre-populates it with 
	 * various things that will be necessary.
	 * @throws Exception 
	 */
	public static void ONE_TIME_SETUP() throws Exception {
		log.info("Running a one-time setup of this new database...");
		// This simple statement causes several pieces of privilege information to be written.
		assertDominates(PrivilegeClass.ADMIN, PrivilegeClass.PUBLIC);
		
		setupConstraints();

		// Store basics that should always be there.
		store(PLUSWorkflow.DEFAULT_WORKFLOW);
		store(PLUSActivity.UNKNOWN_ACTIVITY);
		store(User.DEFAULT_USER_GOD);
		store(User.PUBLIC);	
		
		// Populate with test data.
		new BulkRun().run();  
		
		log.info("Finished running one-time setup of database.");
	} // End ONE_TIME_SETUP
	
	/**
	 * Called as part of the one-time setup of a database; establishes new constraints on data that will be created.
	 */
	private static void setupConstraints() {
		String [] setupConstraints = new String [] {
				"CREATE CONSTRAINT ON (node:" + LABEL_NODE.name() + ") ASSERT node." + PROP_PLUSOBJECT_ID + " IS UNIQUE",
				"CREATE CONSTRAINT ON (actor:" + LABEL_ACTOR.name() + ") ASSERT actor." + PROP_ACTOR_ID + " IS UNIQUE",
				"CREATE CONSTRAINT ON (pc:" + LABEL_PRIVCLASS.name() + ") ASSERT pc." + PROP_PRIVILEGE_ID + " IS UNIQUE",
				"CREATE CONSTRAINT ON (npid:" + LABEL_NONPROV.name() + ") ASSERT npid." + PROP_NONPROV_ID + " IS UNIQUE"
		};

		try (Transaction tx = db.beginTx()) {
			for(String cypherConstraintQuery : setupConstraints) {
				log.info(cypherConstraintQuery);
				ExecutionResult r = execute(cypherConstraintQuery);				
				for(String col : r.columns()) {
					ResourceIterator<Object> it = r.columnAs(col);
					while(it.hasNext())
						System.out.println(it.next());
					it.close();
				}				
			}

			tx.success();
		}
		return;
	} // End setupConstraints
	
	/**
	 * Initializes the database, sets up auto-indexing of various properties, and calls one-time setup
	 * if necessary.
	 * @see Neo4JStorage#ONE_TIME_SETUP()
	 */
	public static synchronized void initialize() {
		if(db != null) { 
			// log.warning("Ignoring attempt to initialize DB connection when it is already present!");
			return;
		}
		
		File storageLoc = null;
		
		if(System.getenv("PROVENANCE_DB_LOCATION") != null) {
			storageLoc = new File(System.getenv("PROVENANCE_DB_LOCATION"));
		} else {		
			storageLoc = new File(System.getProperty("user.home"), "provenance.db");
		}
		
		if(storageLoc.exists())
			log.info("Opening existing Neo4J Embedded Database at " + storageLoc.getAbsolutePath());
		else
			log.info("Creating new Neo4J Embedded Database at " + storageLoc.getAbsolutePath());
		
		db = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storageLoc.getAbsolutePath()).	
			    setConfig(GraphDatabaseSettings.node_keys_indexable, "oid,npid,type,subtype,name,aid,pid" ).
			    setConfig(GraphDatabaseSettings.relationship_keys_indexable, "workflow,npeid" ).
			    setConfig(GraphDatabaseSettings.node_auto_indexing, "true").
			    setConfig(GraphDatabaseSettings.relationship_auto_indexing, "true").
			    newGraphDatabase();
				
		registerShutdownHook(); 
	
		assert(db.index().getNodeAutoIndexer().isEnabled());
		assert(db.index().getRelationshipAutoIndexer().isEnabled());
	
		initLabels();
		
		try { 
			// Check to see if anything is in the database.  The default workflow should
			// always be there.
			Node n = Neo4JStorage.oidExists(PLUSWorkflow.DEFAULT_WORKFLOW.getId());
			
			// If it's not, do one-time setup.
			if(n == null) ONE_TIME_SETUP();
		} catch(Exception exc) { exc.printStackTrace(); }
	} // End doSetup
	
	/** Initializes labels used for storage
	 * @see #Neo4JStorage{@link #LABEL_NODE}
	 */
	private static void initLabels() {
		try (Transaction tx = db.beginTx()) {
			LABEL_NODE = DynamicLabel.label("Provenance");
			LABEL_ACTOR = DynamicLabel.label("Actor");
			LABEL_PRIVCLASS = DynamicLabel.label("PrivilegeClass");
			LABEL_NONPROV = DynamicLabel.label("NonProvenance");
			tx.success();
		}		
	}
	
	/**
	 * Shuts down the database; use of Neo4JStorage after this call results in undefined results.
	 */
	public static void shutdown() {
		try { 
			if(db != null) {
				db.shutdown();
				db = null;
			} else {
				log.severe("Shutdown failed: db was not initiatlized."); 
			}
		} catch(Exception exc) { 
			exc.printStackTrace(); 
		}
	}
	
	/**
	 * Adds a hook to the execution environment so that the Neo4J database is shut down automatically
	 * when the VM exits.
	 */
	private static void registerShutdownHook() {
	    // Registers a shutdown hook for the Neo4j instance so that it
	    // shuts down nicely when the VM exits (even if you "Ctrl-C" the
	    // running example before it's completed)
	    Runtime.getRuntime().addShutdownHook( new Thread() {	        
	        public void run() {	        	
	            Neo4JStorage.shutdown();         
	        }
	    } );
	} // End registerShutdownHook
	
	/**
	 * Get or create a node that refers to a non-provenance identifier.   Must be called from within a transaction.
	 * @param npid the non-provenance identifier for the node
	 * @param create if true, and the NPID doesn't exist, it will be created.  If false, will return null if the NPID doesn't exist.
	 * @return the Node in the store corresponding to what was already present, or created.
	 */
	public static Node getNPID(String npid, boolean create) {
		if(db == null) initialize();
		
		try(Transaction tx = db.beginTx()) {
			Node n = db.index().getNodeAutoIndexer().getAutoIndex().get(PROP_NONPROV_ID, npid).getSingle();
			
			if(n != null) { 
				tx.success(); 
				return n; 
			}
			
			if(create) { 
				n = db.createNode();
				n.setProperty(PROP_NONPROV_ID, npid);
				n.addLabel(LABEL_NONPROV);
				tx.success();
				return n;
			}  
			
			tx.success();
			return null;
		}
	} // End getNPID
	
	/** @deprecated */
	protected static String getNodeStorageType(Node n) { 
		if(n.hasProperty(PROP_PLUSOBJECT_ID) && n.hasProperty(PROP_TYPE)) return "PLUSObject";
		if(n.hasProperty(PROP_ACTOR_ID)) return "PLUSActor";
		if(n.hasProperty(PROP_PRIVILEGE_ID)) return "PrivilegeClass";
		return "other";
	}
	
	/**
	 * Get a list of PLUSObjects that this PLUSActor owns.
	 * @param actor the actor whose objects you are interested in
	 * @param user the user requesting the data
	 * @param maxSetSize the maximum number of items to return
	 * @return a list of the most recently registered PLUSObjects that this actor owns.
	 * @throws PLUSException
	 */
	public static ProvenanceCollection getOwnedObjects(PLUSActor actor, User user, int maxSetSize) throws PLUSException {
		if(actor == null || actor.getId() == null) throw new PLUSException("Invalid actor"); 
						
		if(db == null) initialize();
		ProvenanceCollection col = new ProvenanceCollection();
		
		Node n = exists(actor);
		
		for(Relationship r : n.getRelationships(Direction.OUTGOING, OWNS)) {
			if(isPLUSObjectNode(r.getEndNode()))
				col.addNode(Neo4JPLUSObjectFactory.newObject(r.getEndNode()));
		}
		
		log.info(col.countNodes() + " nodes owned by " + actor);
		return col;		
	} // End getOwnedObjects
	
	/**
	 * Get a Neo4J transaction object.  
	 * @return a Transaction object.
	 */
	public static Transaction beginTx() { 
		if(db == null) initialize();
		return db.beginTx(); 
	}
	
	public static TraversalDescription traversalDescription() { 
		if(db == null) initialize();
		return db.traversalDescription(); 
	} 
	
	/**
	 * Determines whether or not a particular node is a PLUSObject.
	 * @param n
	 * @return
	 */
	public static boolean isPLUSObjectNode(Node n) { 
		if(db == null) initialize();
		
		try (Transaction tx = db.beginTx()) {
			// TODO: this next line is how this whole method should be implemented.
			// n.hasLabel(LABEL_NODE);
			
			boolean result = n != null && n.hasProperty(PROP_PLUSOBJECT_ID) && n.hasProperty(PROP_TYPE) && n.hasProperty(PROP_SUBTYPE);
			tx.success();
			return result;
		}
	}
	
	/**
	 * Execute a cypher query, and return a provenance collection according to the results.
	 * @param query the query to execute
	 * @param params any bound parameters (if necessary)
	 * @param user the user who is viewing the collection.
	 * @param includeNodes if true, resulting collection will include any nodes, if false, they will be omitted.
	 * @param includeEdges if true, resulting collection will include any edges, if false, they will be omitted.
	 * @param includeNPEs if true, resulting collection will include any non-provenance edges, if false, they will be omitted.
	 * @return a provenance collection
	 * @deprecated
	 * @throws PLUSException
	 * TODO
	 */
	public static ProvenanceCollection fetchProvenance(
			String query, Map<String,Object>params, User user,
			boolean includeNodes, boolean includeEdges, boolean includeNPEs) throws PLUSException { 
		if(db == null) initialize();
		
		ViewedCollection vc = new ViewedCollection(user);
		
		ExecutionResult er = execute(query, params);
		for(String columnName : er.columns()) { 
			Iterator<?> it = er.columnAs(columnName);
			
			while(it.hasNext()) { 
				Object o = it.next();
				
				if(o instanceof Node) { 
					Node n = (Node)o;
					String nt = getNodeStorageType(n);
					
					if("PLUSObject".equals(nt)) { 
						PLUSObject po = Neo4JPLUSObjectFactory.newObject(n);
						po = po.getVersionSuitableFor(user);
						if(po != null) { if(includeNodes) vc.addNode(po); } 
						else log.warning("No suitable version of found object for " + user);
					} else { 
						log.warning("Currently, this method does not support result columns of type " + 
					             "Node => " + nt);
						break;
					}
				} else if(o instanceof Relationship) { 
					Relationship r = (Relationship)o;
					if(r.hasProperty(PROP_NPEID)) { 
						NonProvenanceEdge npe = Neo4JPLUSObjectFactory.newNonProvenanceEdge(r);
						if(npe != null && includeNPEs) vc.addNonProvenanceEdge(npe);
					} else { 
						PLUSEdge pe = Neo4JPLUSObjectFactory.newEdge(r);
						if(pe != null && includeEdges) vc.addEdge(pe);
					}
				} else { 
					log.warning("Currently, this method does not support result columns of instance type " + 
							 o.getClass().getCanonicalName());
					break;
				}				
			} // End while
		} // End for
		
		return vc;
	} // End fetchProvenance
	
	public static ProvenanceCollection getActors(int maxNumber) throws PLUSException { 
		if(db == null) initialize();
		
		String query = "match (n:" + Neo4JStorage.LABEL_ACTOR.name() + ") " + 
		        "where has(n.aid) " + 
                "return n " + 
		        "order by n.name desc " + 
		        "limit " + maxNumber;

		ProvenanceCollection col = new ProvenanceCollection();
		
		try (Transaction tx = db.beginTx()) { 
			ExecutionResult result = Neo4JStorage.execute(query);
			ResourceIterator<Node> ns = result.columnAs("n");
						
			while(ns.hasNext()) {
				Node an = ns.next();				
				if(USE_CACHING) cache.put(""+an.getProperty(PROP_ACTOR_ID), an);		
				col.addActor(Neo4JPLUSObjectFactory.newActor(an));
			}
			
			ns.close();
			tx.success();
		}
		
		return col;
	} // End getActors
		
	/**
	 * @param npe a non-provenance edge
	 * @return true if it is in the store, false otherwise.
	 */
	public static boolean exists(NonProvenanceEdge npe) {
		if(db == null) initialize();
		
		try (Transaction tx = db.beginTx()) {
			boolean r = db.index().getRelationshipAutoIndexer().getAutoIndex().get(PROP_NPEID, npe.getId()).getSingle() != null;
			tx.success();
			return r;
		}
	}

	/**
	 * @param edge a PLUSEdge
	 * @return true if it is in the store, false otherwise.
	 */
	public static boolean exists(PLUSEdge edge) {		
		if(edge == null || edge.getType() == null) { 
			log.warning("Can't check existence of an edge that is null or has a null type: " + edge);
			return false;
		}
		
		if(db == null) initialize();
		
		try (Transaction tx = db.beginTx()) { 
			Node f = db.index().getNodeAutoIndexer().getAutoIndex().get(PROP_PLUSOBJECT_ID, edge.getFrom()).getSingle();
			Node t = db.index().getNodeAutoIndexer().getAutoIndex().get(PROP_PLUSOBJECT_ID, edge.getTo()).getSingle();
			
			if(f == null) return false;
			if(t == null) return false; 
			
			Iterable<Relationship> rels = f.getRelationships(Direction.OUTGOING, new RT(edge.getType()));
			
			for(Relationship r : rels) { 
				if(r.getEndNode().equals(t)) { tx.success(); return true; } 
			}
			
			tx.success();
		}
			
		return false;
	}
	
	/**
	 * @param wf a PLUSWorkflow
	 * @param user the user who is looking at this data
	 * @param maximum the maximum number of nodes to return, up to Neo4JPLUSObjectFactory.MAX_OBJECTS
	 * @return a ProvenanceCollection consisting of the most recent objects participating in the workflow.
	 * @throws PLUSException
	 */
	public static ProvenanceCollection getMembers(PLUSWorkflow wf, User user, int maximum) { 
		if(db == null) initialize();
		
		if(maximum <= 0 || maximum > Neo4JPLUSObjectFactory.MAX_OBJECTS)
			maximum = 100;

		ViewedCollection d = new ViewedCollection(user);

		Map<String,Object>params = new HashMap<String,Object>();
		params.put("wf", wf.getId());
		String query = "start r=relationship:relationship_auto_index(workflow={wf}) " +                 
			    "return r " +
				"limit " + maximum;
		
		try (Transaction tx = db.beginTx()) { 		
			ResourceIterator<Relationship> rs = Neo4JStorage.execute(query, params).columnAs("r");		
			
			try { 
				while(rs.hasNext()) { 
					Relationship r = rs.next();
								
					d.addNode(Neo4JPLUSObjectFactory.newObject(r.getStartNode()));
					d.addNode(Neo4JPLUSObjectFactory.newObject(r.getEndNode()));
					d.addEdge(Neo4JPLUSObjectFactory.newEdge(r));			
				}
			} catch(PLUSException exc) {
				exc.printStackTrace();
			}

			rs.close();
			
			// TODO
			// In Neo4J 2.0.1, tx.success() sometimes causes a failed transaction exception due to "unable to commit".
			// This happens in READ-ONLY CYPHER QUERIES.
			// Link to discussion thread:  https://groups.google.com/d/msg/neo4j/w1L_21z0z04/VNBN5epvgYMJ
			// Temporary work-around is to remove tx.success().
			// This is *not* the right thing to do, but it works for now until neo4j addresses the issue.
			// tx.success();
		} catch(TransactionFailureException exc) { 
			log.severe("Failed transaction: " + exc.getMessage());
			exc.printStackTrace();
		}
		
		// System.out.println("Returning collection with " + d.countNodes() + " nodes.");
		return d;
	} // End getMembers	
	
	/**
	 * Write a domination relationship between a and b, meaning that any privilege which b has, a also has.
	 * @param a a PrivilegeClass
	 * @param b a PrivilegeClass
	 * @return true if successsful, false otherwise.
	 * @throws PLUSException
	 */
	public static boolean assertDominates(PrivilegeClass a, PrivilegeClass b) throws PLUSException {
		if(db == null) initialize();
		
		Node n1 = Neo4JStorage.getOrCreate(a);
		Node n2 = Neo4JStorage.getOrCreate(b);		
		
		try (Transaction tx = db.beginTx()) {
			Iterable<Relationship>rs = n1.getRelationships(DOMINATES);
			for(Relationship r : rs) {
				if(r.getEndNode().equals(n2)) {
					tx.success();
					return true;
				}
			}
		}
		
		try (Transaction tx = db.beginTx()) {
			boolean r = n1.createRelationshipTo(n2, DOMINATES) != null;
			tx.success();
			return r;
		} 
	} // End assertDominates
	
	/**
	 * Get or create a privilege class node in the graph database.
	 * @param pc the privilege class
	 * @return the node in the store corresponding to this privilege class.
	 * @throws PLUSException
	 */
	public static Node getOrCreate(PrivilegeClass pc) throws PLUSException {
		if(db == null) initialize();
		
		Node n = privilegeClassExistsById(pc.getId());		
		if(n == null) n = store(pc);
		return n;
	}
	
	/**
	 * Determine whether the DAG contains a path from one object to another.
	 * @param one a PLUSObject in the DAG
	 * @param two a PLUSObject in the DAG
	 * @return false if one or both of the objects isn't in the DAG.  True if and only if there is a path
	 * from one object to the other.  If both inputs are the same, returns true.
	 * @throws PLUSException
	 */
	public boolean pathExists(PLUSObject one, PLUSObject two) throws PLUSException {
		if(db == null) initialize();
		
		return pathExistsViaOperation(one, two, "bling") || pathExistsViaOperation(one, two, "fling");
	} // End pathExists
	
	/**
	 * Do a DFS from one node to another to determine whether a path exists.  The DFS only goes in one
	 * direction, either "bling" or "fling" specified by the operation.
	 * @param one a PLUSObject in the DAG
	 * @param two a PLUSObject in the DAG
	 * @param operation either "bling" or "fling"
	 * @return true if there is a path from one to two via that operation, false otherwise. 
	 * @throws PLUSException
	 */
	public static boolean pathExistsViaOperation(PLUSObject one, PLUSObject two, String operation) throws PLUSException { 
		if(!"bling".equals(operation) && !"fling".equals(operation)) throw new PLUSException("Invalid operation " +operation + ": valid is bling, fling");
		
		if(db == null) initialize();
		
		String relTypes = "[r:contributed|`input to`|marks|unspecified|triggered|generated*.." + MAX_PATH_LENGTH +"]";
		
		Map<String,Object> params = new HashMap<String,Object>();
		params.put("one", one.getId());
		params.put("two", two.getId());
		
		String query = "start n=node:node_auto_index(oid={one}), " +
		               "      m=node:node_auto_index(oid={two}) " + 
				       "match n" +
		               ("fling".equals(operation) ?     // Match a path either to or from, depending on "direction"
		            		"-" + relTypes + "->" : 
		            	    "<-" + relTypes + "-") + 
		               "m " +
		               "return r";
		
		Iterator<Object> result = execute(query, params).columnAs("r");
		if(result.hasNext()) return true;               
		return false;
	} // End pathExistsViaOperation
	
	/** 
	 * Check to see if a privilege class exists.
	 * @param id the ID of the privilege class
	 * @return a Node corresponding to its storage.
	 * @throws PLUSException
	 */
	public static Node privilegeClassExistsById(String id) throws PLUSException {
		if(db == null) initialize();
		if(id == null || "".equals(id)) throw new PLUSException("id cannot be empty or null!");
		
		if(cache.containsKey(id)) return cache.get(id);
		
		assert(db != null); 
		
		Node result = null;
		try (Transaction tx = db.beginTx()) {
			IndexManager mgr = db.index();
			IndexHits<Node> hits = mgr.getNodeAutoIndexer().getAutoIndex().get(PROP_PRIVILEGE_ID, id);
		
			result = hits.getSingle();		
			if(USE_CACHING) if(result != null) cache.put(id, result);
			tx.success();
		}
		
		return result;		
	} // End privilegeClassExistsById
	
	public static Node privilegeExistsByName(String name) throws PLUSException { 
		if(db == null) initialize();
		
		if(name == null || "".equals(name)) throw new PLUSException("Name cannot be empty or null");
		
		Node result = db.index().getNodeAutoIndexer().getAutoIndex().get("name", name).getSingle();	
		
		if(USE_CACHING) if(result != null) cache.put(""+result.getProperty(PROP_PRIVILEGE_ID), result);
		return result;
	} // End privilegeExistsByName
	
	public static Node actorExistsByName(String name) throws PLUSException {
		if(db == null) initialize();
		
		if(name == null || "".equals(name)) throw new PLUSException("Name cannot be null or empty!"); 
		
		if(cache.containsKey("ACTOR::" + name)) return cache.get("ACTOR::" + name);
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put( "name", name );
		String query = "start n=node:node_auto_index(name={name}) " + 
                "where has(n.aid) " + 
			    "return n ";
		
		ExecutionResult result = execute(query, params );
				
		Iterator<Node> ns = result.columnAs("n");
		if(!ns.hasNext()) return null;
		
		Node n = ns.next();
		if(USE_CACHING) { 
			cache.put(""+n.getProperty(PROP_ACTOR_ID), n);
			cache.put("ACTOR::" + name, n);
		}
	
		return n;
	} // End actorExistsByName
	
	public static Node actorExists(String aid) {
		if(db == null) initialize();
		
		if(aid == null || "".equals(aid)) return null;
		
		if(cache.containsKey(aid)) return cache.get(aid);
		
		Map<String,Object> params = new HashMap<String,Object>();
		params.put(PROP_ACTOR_ID, aid);
		String query = "start n=node:node_auto_index(aid={aid}) " + 
                "where has(n.aid) " + 
			    "return n ";
		
		Iterator<Node> ns = Neo4JStorage.execute(query, params).columnAs("n");
		if(!ns.hasNext()) {
			if(USE_CACHING) cache.put(aid, null);
			return null;
		}
		
		Node n = ns.next();
		if(USE_CACHING) cache.put(aid, n);
		return n;
	} // End actorExists
	
	public static Node exists(PLUSActor actor) { return actorExists(actor.getId()); } 
	public static Node exists(PrivilegeClass pc) { return pidExists(pc.getId()); }
	public static Node exists(PLUSObject obj) { return oidExists(obj.getId()); } 
	
	public static Node pidExists(String pid) {
		if(db == null) initialize();
		return db.index().getNodeAutoIndexer().getAutoIndex().get(PROP_PRIVILEGE_ID, pid).getSingle();
	}
	
	public static Node oidExists(String oid) {
		if(db == null) initialize(); 		
		
		try (Transaction tx = db.beginTx()) {			
			Node n = db.index().getNodeAutoIndexer().getAutoIndex().get(PROP_PLUSOBJECT_ID, oid).getSingle();
			tx.success();
			return n;
		}
	}
	
	public static boolean store(PLUSEdge edge) throws PLUSException { 
		return store(Arrays.asList(new PLUSEdge [] { edge }));
	}
	
	public static boolean store(Iterable<PLUSEdge>edges) throws PLUSException {
		if(db == null) initialize(); 
		
		try (Transaction tx = db.beginTx()) {
			for(PLUSEdge e : edges) { 
				Node from = oidExists(e.getFrom().getId());
				Node to = oidExists(e.getTo().getId());
				
				if(from == null) throw new PLUSException("Cannot store edge " + e + " where from OID is not in the store!");
				if(to == null) throw new PLUSException("Cannot store edge " + e + " where to OID is not in the store!"); 
							
				Relationship rel = from.createRelationshipTo(to, new RT(e.getType()));
				rel.setProperty("workflow", (e.getWorkflow() != null ? e.getWorkflow().getId() : null));
				
				// log.info("Stored edge of type " + e.getType() + " (" + e.getFrom() + " => " + e.getTo() + ")");
			} // End for
			
			tx.success();
		} 
		
		return true;
	} // End store
	
	public static boolean store(NonProvenanceEdge npe) throws PLUSException { 
		if(db == null) initialize(); 
		
		try (Transaction tx = db.beginTx()) {
			Node a = oidExists(npe.getIncidentOID());
			if(a == null) throw new PLUSException("Cannot store NPE where OID is not in the store!");
			
			String npid = npe.getIncidentForeignID();
			Node np = getNPID(npid, true);
			
			Relationship rel = a.createRelationshipTo(np, NPE);
			
			rel.setProperty(PROP_TYPE, npe.getType());
			rel.setProperty(PROP_NPEID, npe.getId());
			rel.setProperty(PROP_CREATED, npe.getCreated()); 
			
			// log.info("Stored NPE to identifier " + npe.getIncidentForeignID());
			
			tx.success();
		} 
		
		return true;
	}
	
	/**
	 * Re-formats a raw object for property storage in Neo4J.  See PropertyContainer in the neo4j docs to find out which are valid options.
	 * 
	 */
	public static Object formatProperty(Object raw) { 
		if(raw == null) return "";
		else if(raw instanceof Iterable) { 
			ArrayList<String> al = new ArrayList<String>();	
			for(Object o : (Iterable<?>)raw) al.add(""+formatProperty(o));
			return al.toArray(new String[]{});
		} else if(raw instanceof PrivilegeSet) { 
			ArrayList<String> al = new ArrayList<String>();
			
			for(PrivilegeClass p : ((PrivilegeSet)raw).getPrivilegeSet())
				al.add(p.getName());
				
			return al.toArray(new String[]{});
		} else if(raw instanceof Class) { 
			return ((Class<?>)raw).getName();
		} else if(raw instanceof PLUSActor) { 
			return ((PLUSActor)raw).getId();
		} else if(raw instanceof SurrogateGeneratingFunction) {
			return raw.getClass().getName();
		}
		
		return raw;
	}
	
	public static boolean update(PLUSObject o, String propName, Object propValue, String newPropName, Object newPropValue) throws PLUSException {
		if(newPropName == null || "".equals(newPropName)) throw new PLUSException("Invalid new property name");
		
		if(db == null) initialize();
		
		Node n = exists(o);
		if(n == null) throw new PLUSException("Can't update non-existant node " + o.getId());
		
		if(!n.hasProperty(propName)) throw new PLUSException("Can't update property " + propName + " because that node doesn't have that property.");
		
		if(!newPropName.equals(propName))
			n.removeProperty(propName);
		
		if(propValue != null && !formatProperty(propValue).equals(n.getProperty(propName)))
			throw new PLUSException("Cannot update property '" + propName + "' on " + o.getId() + " because you said its current value was " + 
					formatProperty(propValue) + " and that doesn't match what I have (" + n.getProperty(propName));
		
		n.setProperty(newPropName, formatProperty(newPropValue));
		return true;
	} // End update
	
	public static Node store(PLUSObject o) throws PLUSException {
		if(db == null) initialize(); 
				
		// log.info("STORE: " + o); 
		Node n = oidExists(o.getId());
		if(n != null) {
			log.warning("Skipping storage of " + o + " under OID " + o.getId() + " because that OID already exists.");
			return n;		
		}

		try (Transaction tx = db.beginTx()) {						
			Node provObj = store((Neo4JCapable)o);

			provObj.addLabel(LABEL_NODE);
			
			Metadata m = o.getMetadata();

			for(Object k : m.keySet()) { 
				try { provObj.setProperty(getMetadataPropertyName(k), formatProperty(m.get(k))); } 
				catch(Exception exc) { 
					String err = "Failed to log metadata property '" + k + "' => " + m.get(k) + " of type " + m.get(k).getClass().getName();
					throw new PLUSException(err, exc); 
				}
			} 
			
			String aid = (o.getOwner() != null ? o.getOwner().getId() : null);			
			if(aid != null && !"".equals(aid.trim())) {
				Node actor = actorExists(aid);					
				if(actor == null) {
					log.warning("Cannot store owner of " + o + " because AID " + aid + " doesn't exist!  Actors must be pre-saved.");
				} else { 					
					actor.createRelationshipTo(provObj, OWNS);
					//	provObj.createRelationshipTo(actor, OWNS);
				} // End else
			} // End if

			PrivilegeSet ps = o.getPrivileges();
			for(PrivilegeClass pc : ps.getPrivilegeSet()) {
				Node pcn = getOrCreate(pc);
				provObj.createRelationshipTo(pcn, CONTROLLED_BY);
			}
			
			tx.success();
			return provObj;
		} 
	} // End store
	
	public static String getMetadataPropertyName(Object keyName) { 
		return METADATA_PREFIX + ":" + keyName; 
	}
	
	public static int store(ProvenanceCollection col) throws PLUSException {
		if(db == null) initialize(); 
		int x = 0;
				
		// log.info("Storing provenance collection " + col);
		try (Transaction tx = db.beginTx()) {
			// Actors need to be stored first because some other things may depend on their
			// existence.
			for(PLUSActor a : col.getActors()) {
				if(Neo4JStorage.store(a) != null) x++;				
			}
			
			for(PLUSObject o : col.getNodes()) {
				if(Neo4JStorage.store(o) != null) x++;				
			}
				
			for(PLUSEdge e : col.getEdges()) {
				if(Neo4JStorage.store(e)) x++;				
			}
			
			for(NonProvenanceEdge npe : col.getNonProvenanceEdges()) {
				if(Neo4JStorage.store(npe)) x++;				
			}
			
			tx.success();
		} // End try
		
		return x;
	}
	
	public static Node store(Neo4JCapable n4jc) throws PLUSException {
		if(db == null) initialize(); 
		
		// log.info("STORE: " + n4jc.getClass().getSimpleName() + " => " + n4jc);
		Node n = null;
				
		try (Transaction tx = db.beginTx()) {			
			n = db.createNode();
			
			if(n4jc instanceof PLUSActor) 
				n.addLabel(LABEL_ACTOR);
			else if(n4jc instanceof PrivilegeClass)
				n.addLabel(LABEL_PRIVCLASS);
			else if(n4jc instanceof PLUSObject)
				n.addLabel(LABEL_NODE);
			
			Map<String,Object> map = n4jc.getStorableProperties();
			
			for(String k : map.keySet()) {
				Object v = map.get(k);				
				try { 					
					n.setProperty(k, v == null ? "" : formatProperty(v));
				} catch(Exception exc) { 
					String err = "Failed to log property '" + k + "' => " + v + " of type " + v.getClass().getName(); 
					log.severe(err);
					throw new PLUSException(err, exc);
				}
			}
			
			tx.success();
		} 
		
		return n;
	} // End store
							
	/**
	 * Same as delete(o, true)
	 */
	public static boolean delete(PLUSObject o) { return delete(o, true); } 

	/**
	 * Delete a PLUSObject from Neo4J.
	 * @param o the object to delete
	 * @param deleteIncidentDanglingEdges if true, any remaining incident edges will also be deleted.  If false, 
	 * incident edges will not be deleted.  NOTE:  if the parameter is false, and incident edges still exist, 
	 * this delete will fail and likely will throw an exception.
	 * @return
	 */
	public static boolean delete(PLUSObject o, boolean deleteIncidentDanglingEdges) {		
		Node n = Neo4JStorage.oidExists(o.getId());
		
		log.info("DELETE NODE " + o + " neo4j node " + (n != null ? n.getId() : "N/A")); 
		if(n == null) return false;
		
		try (Transaction tx = db.beginTx()) {
			if(deleteIncidentDanglingEdges) { 
				for(Relationship r : n.getRelationships()) {
					log.info("Deleting incident edge " + r.getId());
					r.delete();
				}
			}
			
			n.delete();
			
			tx.success();
			
			if(Neo4JStorage.oidExists(o.getId()) != null) {
				log.severe("OMGWTFBBQ!!!  Node " + o + " (" + o.getId() + ") still exists.  DELETE FAIL");
			}
			
			return true;
		} catch(Exception exc) {
			exc.printStackTrace();
			return false;
		} 
		
		/*
		 * CYPHER EQUIVALENT CODE FOR DELETING NODES AND EDGES
		String q = "start n=node:node_auto_index(oid=\"" + o.getId() + "\") ";
		
		if(deleteIncidentDanglingEdges) {
			q = q + "MATCH n-[r]-() DELETE n, r";
		} else {
			q = q + "DELETE n";
		}

		log.info("DELETING " + o + " query " + q);
		
		//Transaction tx = db.beginTx();
		
		try { 
			boolean r = Neo4JStorage.execute(q) != null;
			//tx.success();
			if(!r) log.severe("Delete query " + q + " failed");
			return r;
		} finally { 
			//tx.finish();
		} // End finally
		*/
	} // End delete
	
	public static boolean delete(PLUSEdge e) throws PLUSException {
		System.out.println("DELETING EDGE " + e);

		if(e.getFrom() == null) throw new PLUSException("Missing FROM object");
		if(e.getTo() == null) throw new PLUSException("Missing TO object"); 
		
		System.out.println("To exists");
		Node from = Neo4JStorage.oidExists(e.getFrom().getId());
		System.out.println("from exists");
		Node to = Neo4JStorage.oidExists(e.getTo().getId());
		
		System.out.println("Err conditions?");
		if(from == null) { 
			log.severe("Cannot delete edge " + e + " because from node doesn't exist.");
			return false;
		} else if(to == null) { 
			log.severe("Cannot delete edge " + e + " because to node doesn't exist.");
			return false;
		}
		
		System.out.println("Getting rels.");
		Iterable<Relationship> rels = from.getRelationships(Direction.OUTGOING, new RT(e.getType()));
		
		String wfid = (e.getWorkflow() != null ? e.getWorkflow().getId() : null); 
		System.out.println("Iterating rels");
		for(Relationship r : rels) { 
			if(!to.equals(r.getEndNode())) continue;
			
			String otherID = (String)r.getProperty("workflow", null);
			
			if((wfid == null && otherID == null) || (wfid != null && wfid.equals(otherID))) {
				System.out.println("Begin tx");
				try (Transaction tx = db.beginTx()) {
					System.out.println("Delete");
					r.delete();
					System.out.println("Success");
					tx.success();
				} 
				
				System.out.println("Succeed.");
				return true;
			} else {
				System.err.println("Workflows did not match; not deleting.");
			}
		}
		
		System.out.println("Fail");
		log.severe("Cannot delete edge " + e + " because no matching edge was found."); 
		return false;
		
		/*
		String q = "start h=node:node_auto_index(oid=\"" + e.getFrom() + "\"), t=node:node_auto_index(oid=\"" + e.getTo() + "\") " + 
	               "match h-[r:" + e.getType() + "]->t " + 
				   "where r.workflow=\"" + e.getWorkflowId() + "\" " + 
	               "delete r";
		
		//Transaction tx = db.beginTx();
		try { 
			boolean r = Neo4JStorage.execute(q) != null;
			//tx.success();
			if(!r) log.severe("Delete query " + q + " failed");
			return r;
		} finally { 
			//tx.finish();
		}
		*/
	} // End delete
	
	public static Float parseFloat(Object o) {
		if(o == null) return null;
		String s = ""+o;
		if("".equals(s) || "null".equals(s)) return null;
		return Float.parseFloat(s);				
	}
	
	public static List<PLUSWorkflow> listWorkflows(User user, int maxReturn) throws PLUSException {
		if(maxReturn <= 0 || maxReturn > 1000) maxReturn = 100;
		
		String query = "start n=node:node_auto_index(type=\"" + PLUSWorkflow.PLUS_TYPE_WORKFLOW + "\") " + 
	                   "where has(n.oid) " + 
				       "return n " + 
	                   "order by n.created desc, n.name " + 
	                   "limit " + maxReturn;
				
		ArrayList<PLUSWorkflow> wfs = new ArrayList<PLUSWorkflow>();
		
		try (Transaction tx = db.beginTx()) {	
			ResourceIterator<Node> ns = Neo4JStorage.execute(query).columnAs("n");

			while(ns.hasNext()) { 
				PLUSObject o = Neo4JPLUSObjectFactory.newObject(ns.next());
				if(o.isWorkflow()) wfs.add((PLUSWorkflow)o);
				else log.warning("Returned non-workflow " + o + " from workflow query!");				
			} // End while
				
			// TODO Neo4J throws an exception on a read-only query here.  For now,
			// this fixes it, but it's not the right thing to do.
			// tx.success();
		}

		return wfs;
	} // End listWorkflows
	
	public static ProvenanceCollection list(User user, Map<String,Object>searchTerms, int maxReturn) throws PLUSException {
		ProvenanceCollection col = new ProvenanceCollection();
		if(maxReturn <= 0 || maxReturn > 1000) maxReturn = 100;
		
		StringBuffer luceneQuery = new StringBuffer("");
		ArrayList<String>kz = new ArrayList<String>(searchTerms.keySet());
		
		for(int x=0; x<kz.size(); x++) { 
			luceneQuery.append(kz.get(x) + ":\\\"" + searchTerms.get(kz.get(x)) + "\\\"");
			if(x < (kz.size() - 1)) luceneQuery.append(" AND ");
		}
		
		String query = "start n=node:node_auto_index(\"" + luceneQuery.toString() + "\") " + 				
                "where has(n.oid) " +				
			    "return n " + 
			    "limit " + maxReturn;		
				
		Iterator<Node> ns = Neo4JStorage.execute(query).columnAs("n");
		
		try (Transaction tx = db.beginTx()) {
			while(ns.hasNext()) { 
				col.addNode(Neo4JPLUSObjectFactory.newObject(ns.next()));
			}
			
			tx.success();
		}
			
		return col;
	} // End list
	
	public static ExecutionResult execute(String cypherQuery, Map<String,Object>params) {
		if(db == null) initialize();
		
		ExecutionEngine engine = new ExecutionEngine(db);
				
		assert(db.index().getNodeAutoIndexer().isEnabled());
				
		StringBuffer sb = new StringBuffer("");
		for(String k : params.keySet()) sb.append(" " + k + "=" + params.get(k));
		
		//log.info("EXECUTING: " + cypherQuery + " /" +sb);
		return engine.execute(cypherQuery + " ", params);		
	}
	
	public static ExecutionResult execute(String cypherQuery) { 
		if(db == null) initialize();
		ExecutionEngine engine = new ExecutionEngine(db);
		// log.info("EXECUTING: " + cypherQuery);
		return engine.execute(cypherQuery + " ");
	}
			
	public static void main(String [] args) throws Exception { 
		System.out.println(System.getenv("PROVENANCE_DB_LOCATION"));
	}
	
	public static void __main(String [] args) throws Exception { 
		initialize();
		
		String oid = "ABC";
		
		try (Transaction tx = db.beginTx()) { 
			Node n = db.createNode();
			n.setProperty(PROP_PLUSOBJECT_ID, oid);
			tx.success();
		}  
		
		Node l = db.index().getNodeAutoIndexer().getAutoIndex().get(PROP_PLUSOBJECT_ID, oid).getSingle();
		System.out.println("Found node " + l.getId());
				
		try (Transaction tx = db.beginTx()) { 
			l.delete();
			tx.success();
		} 
		
		System.out.println("Deleted node");
		
		System.out.println("Trying to load again:");
		l = db.index().getNodeAutoIndexer().getAutoIndex().get(PROP_PLUSOBJECT_ID, oid).getSingle();
		
		System.out.println("Loaded:  " + l); 
	}
} // End Neo4JStorage