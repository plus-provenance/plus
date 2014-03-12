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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.collections.iterators.IteratorChain;
import org.mitre.provenance.Metadata;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.dag.LineageDAG;
import org.mitre.provenance.dag.TraversalSettings;
import org.mitre.provenance.dag.ViewedCollection;
import org.mitre.provenance.npe.NonProvenanceEdge;
import org.mitre.provenance.plusobject.PLUSActivity;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSFile;
import org.mitre.provenance.plusobject.PLUSFileImage;
import org.mitre.provenance.plusobject.PLUSGeneric;
import org.mitre.provenance.plusobject.PLUSInvocation;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSRelational;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.PLUSURL;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.plusobject.marking.Taint;
import org.mitre.provenance.tools.LRUCache;
import org.mitre.provenance.tools.PLUSUtils;
import org.mitre.provenance.user.OpenIDUser;
import org.mitre.provenance.user.PrivilegeClass;
import org.mitre.provenance.user.PrivilegeSet;
import org.mitre.provenance.user.User;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;

/**
 * A factory to create provenance objects from things in the Neo4J storage layer.  This class permits
 * users to load objects from the database, abstracting away the details of the database.
 * @author DMALLEN
 */
public class Neo4JPLUSObjectFactory {
	protected static final LRUCache<String,PLUSObject> cache = new LRUCache<String,PLUSObject>(500);
	
	protected static final Logger log = Logger.getLogger(Neo4JPLUSObjectFactory.class.getName());
		
	/**
	 * A reasonable setting for user views of graphs; this controls the maximum path length in a user 
	 * graph.
	 */
	public static final int USER_VIEW_PATH_LENGTH = 8;
	
	/**
	 * The maximum path length possible for discovering graphs.
	 */
	public static final int MAX_PATH_LENGTH = 20;
	
	/** The maximum number of objects that will be returned when creating a graph.
	 * Graphs are stored in memory, so this helps balance memory needs.
	 */
	public static int MAX_OBJECTS = 500;
	
	public static int DEFAULT_SEARCH_RESULTS = 30;
	
	/**
	 * @return a list of privilege classes.
	 * @see Neo4JPLUSObjectFactory#MAX_OBJECTS
	 * @throws PLUSException
	 */
	public static List<PrivilegeClass> listPrivilegeClasses() throws PLUSException {
		ArrayList<PrivilegeClass> results = new ArrayList<PrivilegeClass>();

		System.out.println("label is " + Neo4JStorage.LABEL_PRIVCLASS);
		String query = "match (n:" + Neo4JStorage.LABEL_PRIVCLASS.name() + ") " +
                "return n " +
				"order by n.created desc " + 
		        "limit " + MAX_OBJECTS;
		
		try (Transaction tx = Neo4JStorage.beginTx()) {			
			Iterator <Node> ns = Neo4JStorage.execute(query).columnAs("n");
					
			while(ns.hasNext()) results.add(newPrivilegeClass(ns.next()));
			tx.success();
		}
		
		return results;
	} // End listPrivilegeClasses
	
	public static ProvenanceCollection loadBySingleMetadataField(User user, String key, Object value) throws PLUSException { 
		Metadata m = new Metadata();
		m.put(key, value);
		return loadByMetadata(user, m, MAX_OBJECTS);
	} // End loadBySingleMetadataField
	
	public static PLUSObject load(String oid, User user) throws PLUSException {
		if(user == null) throw new PLUSException("Must specify user");
		PLUSObject obj = newObject(Neo4JStorage.oidExists(oid));
		
		return obj.getVersionSuitableFor(user);
	}
	
	public static ProvenanceCollection loadByMetadata(User user, Metadata fields, int maxReturn) throws PLUSException { 
		LineageDAG d = new LineageDAG(user);
		
		if(maxReturn <= 0 || maxReturn > MAX_OBJECTS) maxReturn = MAX_OBJECTS;
		
		StringBuffer whereClause = new StringBuffer("");
		Iterator<?> i = fields.keySet().iterator();
		
		while(i.hasNext()) {
			Object k = i.next();
			String propName = Neo4JStorage.getMetadataPropertyName(k);
			whereClause.append("n.`" + propName + "`=\"" + Neo4JStorage.formatProperty(fields.get(k)) + "\" ");
			if(i.hasNext()) whereClause.append("and ");
		}
		
		String query = "match (n:" + Neo4JStorage.LABEL_NODE.name() + ") " + 
		        "where " + whereClause +  
                "return n " + 
		        "limit " + maxReturn;
		
		// System.out.println(query);
		Iterator <Node> ns = Neo4JStorage.execute(query).columnAs("n");
		
		while(ns.hasNext()) {
			PLUSObject o = newObject(ns.next());
			PLUSObject s = o.getVersionSuitableFor(user);
			if(s != null) d.addNode(s);			
		}
		
		return d;
	} // End loadByMetadata
	
	public static PLUSActor newActor(Node n) throws PLUSException {
		if(n == null) throw new PLUSException("null PLUSActor node");
		
		try (Transaction tx = Neo4JStorage.beginTx()) {		
			String type = (String)n.getProperty(Neo4JStorage.PROP_TYPE);
			
			if(type == null)
				log.severe("Actor node " + n + " has null type other attrs " + 
						"aid=" +n.getProperty(Neo4JStorage.PROP_ACTOR_ID, "") + "/name=" + n.getProperty(Neo4JStorage.PROP_NAME,"") + "/created=" + n.getProperty(Neo4JStorage.PROP_CREATED, "") + "/" + 
				        "type=" +n.getProperty(Neo4JStorage.PROP_TYPE,"") + "/displayName=" + n.getProperty("displayName","") + "/email=" + n.getProperty("email",""));
			
			PLUSActor result =  null;
			
			if(OpenIDUser.OPENID_USER_TYPE.equals(type)) result = (PLUSActor)new OpenIDUser().setProperties(n);		
			else if("user".equals(type)) result = (PLUSActor)new User().setProperties(n);
			else result = (PLUSActor)new PLUSActor().setProperties(n);
			
			tx.success();
			return result;
		}
	} // End newActor
	
	public static PrivilegeClass newPrivilegeClass(Node n) throws PLUSException { 
		if(n == null) throw new PLUSException("null Privilege node");
		PrivilegeClass pc = new PrivilegeClass(1);
		pc.setProperties(n);
		return pc;
	}
	
	/**
	 * @param name the name of an actor
	 * @return the actor object, or null if it doesn't exist.
	 * @throws PLUSException
	 */
	public static PLUSActor getActor(String name) throws PLUSException {
		return getActor(name, false);
	}
	
	/**
	 * Get a PLUSActor from the database.
	 * @param name the name of the actor
	 * @param create if true, the actor will be created and returned even if it doesn't exist.  
	 * @return a PLUSActor, or null if it doesn't exist, and create=false.
	 * @throws PLUSException
	 */
	public static PLUSActor getActor(String name, boolean create) throws PLUSException {
		try(Transaction tx = Neo4JStorage.beginTx()) {
			Node n = Neo4JStorage.actorExistsByName(name);
			if(n != null) return newActor(n);
			
			PLUSActor result = null;
			
			if(create) {
				result = new PLUSActor(name); 
				Neo4JStorage.store(result);
			}
						
			tx.success();
			return result;  // This will be null if create=false.
		}		
	}
	
	/**
	 * @deprecated
	 */
	public static PLUSActor getOrCreateActor(String name) throws PLUSException {
		try(Transaction tx = Neo4JStorage.beginTx()) {
			Node n = Neo4JStorage.actorExistsByName(name);
			if(n != null) return newActor(n);
			
			PLUSActor a = new PLUSActor(name); 
			Neo4JStorage.store(a);
			
			tx.success();
			return a;
		}
	} // End getOrCreate
	
	public static PLUSEdge newEdge(Relationship r) throws PLUSException { 
		try(Transaction tx = Neo4JStorage.beginTx()) {
			//String from = ""+r.getStartNode().getProperty(Neo4JStorage.PROP_PLUSOBJECT_ID, "");
			//String to = ""+r.getEndNode().getProperty(Neo4JStorage.PROP_PLUSOBJECT_ID, "")		
			String wkflow = ""+r.getProperty(Neo4JStorage.PROP_WORKFLOW);
			String type = r.getType().name();
			
			Node workflowNode = Neo4JStorage.oidExists(wkflow);
			
			PLUSObject from = newObject(r.getStartNode());
			PLUSObject to = newObject(r.getEndNode());
			PLUSWorkflow workflow = (PLUSWorkflow)(workflowNode == null ? null : newObject(workflowNode));
			
			tx.success();
			
			return new PLUSEdge(from, to, workflow, type);
		}
	} // End newEdge
	
	public static PLUSObject newObject(String oid) throws PLUSException {
		if(cache.containsKey(oid)) return cache.get(oid);
		
		Node n = Neo4JStorage.oidExists(oid);
		if(n == null) throw new DoesNotExistException(oid);
		return newObject(n);
	}
	
	public static PLUSObject newObject(Node n) throws PLUSException { 
		if(n == null) throw new PLUSException("Cannot create PLUSObject from null");
		
		try (Transaction tx = Neo4JStorage.beginTx()) {
			if(!Neo4JStorage.isPLUSObjectNode(n)) throw new PLUSException("Node " + n.getId() + " isn't a PLUSObject node");
			
			String oid = (String)n.getProperty(Neo4JStorage.PROP_PLUSOBJECT_ID, null);
			if(cache.containsKey(oid)) return cache.get(oid);
			
			String t = ""+n.getProperty(Neo4JStorage.PROP_TYPE);
			String st = ""+n.getProperty(Neo4JStorage.PROP_SUBTYPE);
			
			PLUSObject o = null;
			
			if(PLUSInvocation.PLUS_SUBTYPE_INVOCATION.equals(st)) { 
				o = new PLUSInvocation().setProperties(n);
			} else if(PLUSWorkflow.PLUS_TYPE_WORKFLOW.equals(t)) { 
				o = new PLUSWorkflow().setProperties(n);
			} else if(st.equals(PLUSString.PLUS_SUBTYPE_STRING)) {
				o = new PLUSString().setProperties(n);
			} else if(PLUSFile.PLUS_SUBTYPE_FILE.equals(st)) { 
				o = new PLUSFile().setProperties(n);
			} else if(PLUSFileImage.PLUS_SUBTYPE_FILE_IMAGE.equals(st)) {  
				o = new PLUSFileImage().setProperties(n);
			} else if(PLUSURL.PLUS_SUBTYPE_URL.equals(st)) { 
				o = new PLUSURL().setProperties(n);
			} else if(PLUSActivity.PLUS_TYPE_ACTIVITY.equals(t)) { 
				o = new PLUSActivity().setProperties(n);	
			} else if(PLUSRelational.PLUS_SUBTYPE_RELATIONAL.equals(st)) {  
				o = new PLUSRelational().setProperties(n);		
			} else if(Taint.PLUS_SUBTYPE_TAINT.equals(st)) {
				o = new Taint().setProperties(n);
			} else {
				log.info("Couldn't find more specific type for " + t + "/" + st + " so loading as generic."); 
				o = new PLUSGeneric().setProperties(n);
			}
			
			int x=0;
			for(Relationship r : n.getRelationships(Direction.INCOMING, Neo4JStorage.OWNS)) {
				PLUSActor a = newActor(r.getStartNode());
				o.setOwner(a);
				
				if(x > 0) log.warning("Duplicate owner " + a + " on node " + o);
				
				x++;
			}
			
			PrivilegeSet ps = new PrivilegeSet();
			for(Relationship r : n.getRelationships(Direction.OUTGOING, Neo4JStorage.CONTROLLED_BY)) {
				PrivilegeClass pc = newPrivilegeClass(r.getEndNode());
				ps.addPrivilege(pc);
			}
			o.setPrivileges(ps);
		
			tx.success();

			cache.put(o.getId(), o);
			
			return o;
		}		
	} // End newObject
	
	/**
	 * Same as getIncidentEdges(oids, user, "both", true, true)
	 * @see Neo4JPLUSObjectFactory#getIncidentEdges(Iterable, User, String, boolean)
	 */
	public static ProvenanceCollection getIncidentEdges(Iterable<String>oids, User user) throws PLUSException { 
		return getIncidentEdges(oids, user, "both", true, true); 
	}
	
	public static ProvenanceCollection getIncidentNPEs(Iterable<String>oids, User user) throws PLUSException { 
		return getIncidentEdges(oids, user, "both", false, true); 
	}
	
	/**
	 * Get edges incident to a particular set of object identifiers.
	 * @param oids the set of PLUSObject oids to search for
	 * @param user the user who wants the data
	 * @param direction which set of edges you want.  Can be "bling", "fling", or "both".  All other values throw an exception.
	 * @param includeNPEs if true, the resulting collection will include NonProvenanceEdge objects.
	 * @return a ProvenanceCollection containing the search results.
	 * @throws PLUSException
	 */
	public static ProvenanceCollection getIncidentEdges(Iterable<String>oids, User user, String direction, boolean includeProvEdges, boolean includeNPEs) 
			throws PLUSException { 
		if(!includeProvEdges && !includeNPEs) throw new PLUSException("No results possible!");
		
		LineageDAG col = new LineageDAG(user);
		
		String matchClause = "";
		
		ArrayList<String>relTypes = new ArrayList<String>();
		
		if(includeProvEdges) { 
			relTypes.add("contributed"); 
			relTypes.add("`input to`"); 
			relTypes.add("unspecified");
			relTypes.add("marks"); 
			relTypes.add("triggered"); 
			relTypes.add("generated");			
		}
		
		if(includeNPEs) relTypes.add("NPE");		
		
		StringBuffer relClause = new StringBuffer("[r:");
		
		// This code joins the arry types with a pipe, so the clause ends up:  "[r:contributed|`input to`|unspecified|NPE]" and so on.
		Iterator<String> it = relTypes.iterator();
		while(it.hasNext()) {
			relClause.append(it.next());
			if(it.hasNext()) relClause.append("|");
		}
		
		if("fling".equals(direction))
			matchClause = "n-" + relClause + "]->m";
		else if("bling".equals(direction))
			matchClause = "n<-" + relClause + "]-m";
		else if("both".equals(direction))
			matchClause = "n-" + relClause + "]-m";		
		else throw new PLUSException("Invalid direction: " + direction + " valid is fling, bling, both");
		
		for(String oid : oids) {
			Map<String,Object>params = new HashMap<String,Object>();
			params.put(Neo4JStorage.PROP_PLUSOBJECT_ID, oid);

			String query = "start n=node:node_auto_index(oid={oid}) " + 
					"match " + matchClause + " " +  
					"where has(m.oid) " + 
					"return r ";
			
			try (Transaction tx = Neo4JStorage.beginTx()) {
				ExecutionResult result = Neo4JStorage.execute(query, params);
				Iterator<Relationship> rels = result.columnAs("r");
	
				while(rels.hasNext()) { 
					Relationship r = rels.next();
	
					if(includeNPEs && Neo4JStorage.NPE.name().equals(r.getType().name())) col.addNonProvenanceEdge(newNonProvenanceEdge(r));
					else if(includeProvEdges) col.addEdge(newPLUSEdge(r));
					else throw new PLUSException("This shouldn't be possible.");
				} // End while
				
				// TODO
				// In Neo4J 2.0.1, tx.success() sometimes causes a failed transaction exception due to "unable to commit".
				// This happens in READ-ONLY CYPHER QUERIES.
				// Link to discussion thread:  https://groups.google.com/d/msg/neo4j/w1L_21z0z04/VNBN5epvgYMJ
				// Temporary work-around is to remove tx.success().
				// This is *not* the right thing to do, but it works for now until neo4j addresses the issue.			
				// tx.success();				
			} // End try
		} // End for
		
		return col;
	} // End getIncidentEdges

	/**
	 * Create a new NonProvenanceEdge from an underlying Relationship.
	 * @param r
	 * @return
	 * @throws PLUSException
	 */
	public static NonProvenanceEdge newNonProvenanceEdge(Relationship r) throws PLUSException { 		
		try(Transaction tx = Neo4JStorage.beginTx()) {
			Node from = r.getStartNode();
			Node to = r.getEndNode();
			
			if(!Neo4JStorage.isPLUSObjectNode(from)) {
				log.warning("FROM end of NPE is not a PLUS Object node.");
			}				
			
			String id     = (String)r.getProperty(Neo4JStorage.PROP_NPEID);
			String fromID = (Neo4JStorage.isPLUSObjectNode(from) ? (String)from.getProperty(Neo4JStorage.PROP_PLUSOBJECT_ID) : (String)from.getProperty(Neo4JStorage.PROP_NONPROV_ID));
			String toID   = (Neo4JStorage.isPLUSObjectNode(to) ? (String)to.getProperty(Neo4JStorage.PROP_PLUSOBJECT_ID) : (String)to.getProperty(Neo4JStorage.PROP_NONPROV_ID));
			String type   = (String)r.getProperty(Neo4JStorage.PROP_TYPE);  
			Long created  = (Long)r.getProperty(Neo4JStorage.PROP_CREATED);

			tx.success();
			return new NonProvenanceEdge(id, fromID, toID, type, created); 
		}
	} // End newNonProvenanceEdge

	/**
	 * Create a new PLUSEdge object from a given underlying Relationship.
	 * @param r
	 * @return
	 * @throws PLUSException
	 */
	public static PLUSEdge newPLUSEdge(Relationship r) throws PLUSException { 
		try(Transaction tx = Neo4JStorage.beginTx()) {
			String wfid = (String)r.getProperty(Neo4JStorage.PROP_WORKFLOW, null);
			
			PLUSWorkflow wf = null;
			
			if(PLUSWorkflow.DEFAULT_WORKFLOW.getId().equals(wfid)) {
				wf = PLUSWorkflow.DEFAULT_WORKFLOW;
			} else if(wfid != null) {
				Node n = Neo4JStorage.oidExists(wfid);
				if(n == null) 
					log.warning("Edge workflow identified by " + wfid + " doesn't exist!");			
				else
					wf = (PLUSWorkflow)newObject(n);
			} // End if
		
			tx.success();
			return new PLUSEdge(newObject(r.getStartNode()),
					            newObject(r.getEndNode()), 
					            wf,
					            r.getType().name());
		}
	} // End newPLUSEdge
	
	/**
	 * Get a list of non provenance edges from the store.
	 * @param externalId the external identifier the edges are attached to.  If null, it will be treated as a wildcard.
	 * @param user the user looking at the data
	 * @param maxReturn the maximum number to return
	 * @return a provenanec collection.
	 * @throws PLUSException
	 */
	public static ProvenanceCollection getNonProvenanceEdges(String externalId, User user, int maxReturn) throws PLUSException {
		if(user == null) throw new PLUSException("null user");
		
		ViewedCollection col = new ViewedCollection(user);
		if(maxReturn <= 0 || maxReturn > MAX_OBJECTS) maxReturn = MAX_OBJECTS;
		
		String query = null;
		if(externalId == null) 
			query = "start r=relationship:relationship_auto_index(\"npeid:*\") " +
		            "where has(r.created) " + 
			        "return r " + 
		            "order by r.created desc " + 
				    "limit " + maxReturn;
		else 
			query = "start n=node:node_auto_index(npid=\"" + externalId + "\") " + 
		            "match n-[r:NPID]-m " + 
					"return r " + 
		            "order by r.created desc " + 
					"limit " + maxReturn;
			
		Iterator<Relationship> results = Neo4JStorage.execute(query).columnAs("r");
		while(results.hasNext()) col.addNonProvenanceEdge(newNonProvenanceEdge(results.next()));
		return col;
	} // End getNonProvenanceEdges
	
	/**
	 * Given a set of parameters, build a Cypher query which will return the appropriate data
	 * from the underlying neo4j database.
	 * @param ids a list of OIDs 
	 * @param maxNodes maximum nodes the query should return
	 * @param maxDistance maximum path distance to traverse
	 * @param followNPIDs whether or not non-provenance edges and IDs should be followed.
	 * @return
	 */
	protected static String buildQuery(Iterable<String>ids, int maxNodes, int maxDistance, boolean followNPIDs) {
		StringBuffer b = new StringBuffer("start ");
		
		/* Formula for the query we are building...
		 start n=node:node_auto_index('oid:("first identifier", "second identifier")') 
		 match n-[r:myEdge*..5]-m 
		 return m 
		*/
		
		b.append("n=node:node_auto_index('oid:(");
		Iterator<String>iter = ids.iterator();
				
		while(iter.hasNext()) {
			String oid = iter.next();
			b.append("\"" + oid + "\""); 
			if(iter.hasNext()) b.append(", "); 
		}
		b.append(")') "); // End of "start" clause.

		/* MATCH SECTION */
		String relationshipsSought = "contributed|marks|`input to`|unspecified|triggered|generated";
		if(followNPIDs) relationshipsSought = relationshipsSought + "|NPE";

		b.append("match n-[r:" + relationshipsSought + "*.." + maxDistance + "]-m ");
		b.append("return m ");		
		b.append("limit " + maxNodes);
		return b.toString();
	}
	
	protected static Set<String> findStandinOIDsForNPID(String npid) { 
		HashSet<String> oids = new HashSet<String>();
		
		Node n = Neo4JStorage.getNPID(npid, false);
		Iterable<Relationship> rels = n.getRelationships(Neo4JStorage.NPE);
		int x=0;
		
		for(Relationship r : rels) {
			if(Neo4JStorage.isPLUSObjectNode(r.getStartNode())) { 
				x++;
				oids.add((String)r.getStartNode().getProperty(Neo4JStorage.PROP_PLUSOBJECT_ID));
			}
			
			if(Neo4JStorage.isPLUSObjectNode(r.getEndNode())) {
				x++;
				oids.add((String)r.getEndNode().getProperty(Neo4JStorage.PROP_PLUSOBJECT_ID));
			}
						
			if(x >= 5) break;
		}
		
		return oids;
	} // End findStandinOIDsforNPID
	
	/**
	 * Gets a list of provenance objects (nodes only) that are immediately incident to a non-provenance identifier.
	 * @param npid
	 * @return a D3 formatted collection of objects and non-provenance edges incident to the given NPID
	 * @throws PLUSException 
	 */
	public static ProvenanceCollection getIncidentProvenance(String npid, int maxResults) throws PLUSException {
		if(maxResults <= 0 || maxResults > MAX_OBJECTS) maxResults = 50;
		
		ProvenanceCollection col = new ProvenanceCollection();
		Node n = Neo4JStorage.getNPID(npid, false);
		if(n == null) return col;
		
		Iterable<Relationship> rels = n.getRelationships(Neo4JStorage.NPE);
		
		int x=0;
		for(Relationship r : rels) { 
			Node plusNode = (Neo4JStorage.isPLUSObjectNode(r.getStartNode()) ? r.getStartNode() : r.getEndNode());
			
			col.addNode(newObject(plusNode));
			col.addNonProvenanceEdge(newNonProvenanceEdge(r));
			
			x++;
			if(x >= maxResults) break;
		}
		
		return col;
	} // End getIncidentProvenance

	/**
	 * Builds a traversal description object from a set of TraversalSettings
	 * @param settings the settings to use
	 * @return a TraversalDescription object suitable for traversing provenance graphs from some point.
	 */
	public static TraversalDescription buildTraversalDescription(TraversalSettings settings) { 
		TraversalDescription desc = Neo4JStorage.traversalDescription();
		
		// Set basic traversal mode.
		if(settings.breadthFirst) desc = desc.breadthFirst();
		else desc = desc.depthFirst();
		
		// Set the max depth, if applicable.
		if(settings.maxDepth > 0)
			desc = desc.evaluator(Evaluators.toDepth(settings.maxDepth));
		
		// When looking at relationships, set the direction to look in to control
		// whether we're discovering upgraph, downgraph, or both.
		Direction dirToTraverse = Direction.BOTH;
		if(settings.forward && !settings.backward) dirToTraverse = Direction.OUTGOING;
		else if(!settings.forward && settings.backward) dirToTraverse = Direction.INCOMING;
		
		// System.err.println("With forward=" + settings.forward + " and backward=" + settings.backward + " direction=" + dirToTraverse);
		
		// Visit each node only once.
		desc = desc.uniqueness(Uniqueness.NODE_GLOBAL);		
		
		// Make sure to traverse all of the provenance relationship edges.
		desc = desc.relationships(Neo4JStorage.CONTRIBUTED, dirToTraverse);
		desc = desc.relationships(Neo4JStorage.GENERATED, dirToTraverse);
		desc = desc.relationships(Neo4JStorage.INPUT_TO, dirToTraverse);
		desc = desc.relationships(Neo4JStorage.MARKS, dirToTraverse);
		desc = desc.relationships(Neo4JStorage.TRIGGERED, dirToTraverse);
		desc = desc.relationships(Neo4JStorage.UNSPECIFIED, dirToTraverse);
		
		// Optionally, traverse the NPE edges if necessary.
		// Note that even if we're not including NPEs, we have to follow the links to get the NPIDs.
		// TODO should we prohibit includeNPEs=false && followNPIDs=true?
		if(settings.includeNPEs || settings.followNPIDs)
			desc = desc.relationships(Neo4JStorage.NPE, dirToTraverse);

		return desc;
	}
	
	/**
	 * <p>Creates a new DAG, with configurable properties, and a full DAG fingerprint.
	 * This method will call the surrogate algorithm and compute what the specified user is 
	 * permitted to see of the underlying data.
	 * <p>Given that the DAG can start from multiple points, there is no connectedness guarantee
	 * about the result that will be returned.
	 * @param id the starting point to spider out from
	 * @param user the user viewing the DAG, which will be used as part of the surrogate algorithm to compute what they are
	 * permitted to see
	 * @param settings the settings used to control how the graph is discovered.
	 * @return a LineageDAG object.
	 * @throws PLUSException
	 */	
	public static LineageDAG newDAG(String id, User user, TraversalSettings settings) throws PLUSException {
		Node startingPoint = Neo4JStorage.oidExists(id);
		
		if(startingPoint == null) throw new PLUSException("No such node " + id);
		
		LineageDAG dag = new LineageDAG(user);
		
		dag.getFingerPrint().startTimer("DiscoverCollection"); 
		TraversalDescription desc = buildTraversalDescription(settings);

		dag.getFingerPrint().startTimer("Build");
		
		dag.getFingerPrint().startTimer("TraverseIterator");
		// log.info("Traversing from " + id);
		
		try (Transaction tx = Neo4JStorage.beginTx()) { 			
			for(Node n : desc.traverse(startingPoint).nodes()) {
				// log.info("Traversing through " + n);
				dag.getFingerPrint().stopTimer("TraverseIterator");
	
				// Throttle at this many nodes maximum.
				// If n is negative, then there's no limit.
				if(settings.n > 0 && (settings.n <= dag.countNodes())) 				
					break;			
							
				if(settings.includeNodes && n.hasProperty(Neo4JStorage.PROP_PLUSOBJECT_ID)) {
					PLUSObject o = Neo4JPLUSObjectFactory.newObject(n).getVersionSuitableFor(user);
					if(o != null) { 
						// log.info("Added node " + o.getId());
						dag.addNode(o);
					}
				} else 
					log.warning("That doesn't seem right...Node " + n + " lacks OID property, type=" + n.getProperty(Neo4JStorage.PROP_TYPE, "(missing)") + " name=" + n.getProperty(Neo4JStorage.PROP_NAME, "(missing)"));			
	
				if(settings.includeEdges) { 
					Iterable<Relationship> rels = n.getRelationships(							
							Neo4JStorage.CONTRIBUTED, Neo4JStorage.MARKS, 
							Neo4JStorage.UNSPECIFIED, Neo4JStorage.INPUT_TO, 
							Neo4JStorage.GENERATED, Neo4JStorage.TRIGGERED);
					
					for(Relationship r : rels)  {
						PLUSEdge e = newPLUSEdge(r);
						// log.info("Added edge " + e);
						dag.addEdge(e);
					}
				} // End if
	
				if(settings.includeNPEs) { 
					Iterable<Relationship> rels = n.getRelationships(Neo4JStorage.NPE);
					for(Relationship r : rels) {
						NonProvenanceEdge np = newNonProvenanceEdge(r);
						// log.info("Added NPE " + np);
						dag.addNonProvenanceEdge(np);
						
						String oid = np.getIncidentOID();
						if(!dag.containsObjectID(oid)) { 
							// This should never occur.  Since we're going through all
							// provenance nodes, 
							log.warning("When adding NPE " + np + " found we were missing incident OID " + oid);
						}			
					}
				} // End if
			
				dag.getFingerPrint().startTimer("TraverseIterator");
			} // End for
			
			// TODO
			// In Neo4J 2.0.1, tx.success() sometimes causes a failed transaction exception due to "unable to commit".
			// This happens in READ-ONLY CYPHER QUERIES.
			// Link to discussion thread:  https://groups.google.com/d/msg/neo4j/w1L_21z0z04/VNBN5epvgYMJ
			// Temporary work-around is to remove tx.success().
			// This is *not* the right thing to do, but it works for now until neo4j addresses the issue.			
			// tx.success();
		} catch(TransactionFailureException exc) { 
			log.severe("Transaction failed: " + exc.getMessage());
		}
		
		dag.getFingerPrint().stopTimer("TraverseIterator");
		
		// Add actors that are relevant to the graph.
		for(PLUSObject o : dag.getNodes()) {
			if(o.getOwner() != null) dag.addActor(o.getOwner());
		}
		
		PLUSObject focus = LineageDAG.chooseFocus(dag, id);
		if(focus != null) dag.setFocus(focus);		
		dag.getFingerPrint().stopTimer("Build"); 		
		
		// Because of max size constraints, we will frequently load less
		// of the graph than is actually in the database.  This method loops
		// through the edges, and tags nodes as having "more" information, whenever
		// there's an edge where the other end isnt in the dag.
		// This information lets GUI displays of the graph signify that a node isn't
		// actually a dead-end in the provenance graph.
		for(PLUSEdge e : dag.getEdges()) {
			if(dag.contains(e.getFrom()) && !dag.contains(e.getTo()))
				dag.tagNode(e.getFrom(), LineageDAG.TAG_MORE_AVAILABLE, "true");
			else if(!dag.contains(e.getFrom()) && dag.contains(e.getTo()))
				dag.tagNode(e.getTo(), LineageDAG.TAG_MORE_AVAILABLE, "true"); 
		} // End for
		
		dag = LineageDAG.computeEdgeVoting(dag);   // Edge voting for surrogates
		dag = LineageDAG.traceTaintSources(dag);   // Trace indirect taints from direct taints
		dag = LineageDAG.drawInferrableEdges(dag); // Draw inferred edges based on surrogate alg.
		dag = LineageDAG.tagHeadAndFeet(dag);
				
		List<PLUSEdge> danglers = LineageDAG.detectDanglers(dag);
		
		if(danglers.size() > 0) 
			log.warning("Collection " + dag + " contains " + danglers.size() + " dangling edges."); 
				
		dag.getFingerPrint().startTimer("GraphFunctions");
		dag.getFingerPrint().finished(dag);
		dag.getFingerPrint().stopTimer("GraphFunctions");		
		
		dag.getFingerPrint().stopTimer("DiscoverCollection"); 
		dag.getFingerPrint().finished(dag);
		return dag;
	} // End newDAG
	
	/**
	 * <p>Creates a new DAG, with configurable properties, and a full DAG fingerprint.
	 * This method will call the surrogate algorithm and compute what the specified user is 
	 * permitted to see of the underlying data.
	 * <p>Given that the DAG can start from multiple points, there is no connectedness guarantee
	 * about the result that will be returned.
	 * @deprecated use TraversalSettings instead
	 * @param ids an iterable set of object IDs that should be included in the DAG
	 * @param user the user viewing the DAG, which will be used as part of the surrogate algorithm to compute what they are
	 * permitted to see
	 * @param maxNodes the maximum number of nodes to include in the DAG.
	 * @param maxHops the maximum number of hops away to fetch graph items.
	 * @param includeNodes if true, the DAG will include nodes.  If false, it will not.
	 * @param includeEdges if true, the DAG will include provenance edges.  If false, it will not.
	 * @param includeNonProvenanceEdges if true, the DAG will include NPEs.  If false, it will not.
	 * @param followNPIDs if true, the "spider" that discovers the graph will follow non-provenance IDs. If false, it will not.  
	 * For example, if a node (A) is linked to an MD5 sum as an external identifier, and that MD5 sum is linked to some other provenance
	 * object (B), then that object B may be included in the resulting DAG (subject to maxNodes) even if there is no provenance
	 * relationship between A and B.
	 * @return a LineageDAG object.
	 * @throws PLUSException
	 */
	public static LineageDAG newDAG(Iterable<String>ids, User user, int maxNodes, 
			int maxHops, 
			boolean includeNodes, boolean includeEdges,
			boolean includeNonProvenanceEdges, 
			boolean followNPIDs) throws PLUSException {
		ArrayList<String>oids = new ArrayList<String>();
		
		// Some IDs are provenance, and we just add those as starting points.
		// Others may refer to other systems; we first need to look up appropriate
		// provenance IDs
		for(String o : ids) {
			if(PLUSUtils.isPLUSOID(o)) oids.add(o);
			else oids.addAll(findStandinOIDsForNPID(o));
		} // End for
		
		LineageDAG col = new LineageDAG(user);

		if(oids.size() <= 0) 
			throw new PLUSException("Cannot create a DAG from no starting points!");
		
		col.getFingerPrint().startTimer("DiscoverCollection"); 
		
		if(maxNodes <= 0 || maxNodes > MAX_OBJECTS) { 
			log.warning("Setting maxNodes to 100 on bad setting of " + maxNodes);
			maxNodes = 100;
		}
				
		if(maxHops <= 0 || maxHops > MAX_PATH_LENGTH) { 
			log.warning("Setting maxHops to " + USER_VIEW_PATH_LENGTH + " on bad setting of " + maxHops);
			maxHops = USER_VIEW_PATH_LENGTH;
		}
		
		col.getFingerPrint().startTimer("Build");
		String lastOID = null;
				
		col.getFingerPrint().startTimer("sumAccessTime");
		
		// Build a Cypher query which will get all of the relevant nodes.
		String query = buildQuery(oids, maxNodes, maxHops, true);
		ExecutionResult result = Neo4JStorage.execute(query);

		// The result of the query...
		Iterator<Node> nodes = result.columnAs("m");
		
		ArrayList<Node> startingPoints = new ArrayList<Node>();
		for(String id : oids) { 
			Node sp = Neo4JStorage.oidExists(id);
			if(sp != null) {
				startingPoints.add(sp);
				lastOID = id;   // The last starting point that got picked will be the graph focus.
			} 
		}
					
		col.getFingerPrint().stopTimer("sumAccessTime");
			
		// This iterator chain holds all of the nodes we want to 
		// be in the result.  The starting points have to be added
		// because the query returns only nodes the starting points are
		// connected to, not the starting points themselves.
		IteratorChain chain = new IteratorChain();
		chain.addIterator(startingPoints.iterator());
		chain.addIterator(nodes);		
		
		while(chain.hasNext()) {
			Node n = (Node)chain.next();
			
			if(includeNodes && n.hasProperty(Neo4JStorage.PROP_PLUSOBJECT_ID)) {
				PLUSObject o = Neo4JPLUSObjectFactory.newObject(n).getVersionSuitableFor(user);
				if(o != null) { 
					// log.info("Added node " + o.getId());
					col.addNode(o);
				}
			}

			if(includeEdges) { 
				Iterable<Relationship> rels = n.getRelationships(
						Neo4JStorage.CONTRIBUTED, Neo4JStorage.MARKS, 
						Neo4JStorage.UNSPECIFIED, Neo4JStorage.INPUT_TO, 
						Neo4JStorage.GENERATED, Neo4JStorage.TRIGGERED);
				
				for(Relationship r : rels)  {
					PLUSEdge e = newPLUSEdge(r);
					//log.info("Added edge " + e);
					col.addEdge(e);
				}
			} // End if

			if(includeNonProvenanceEdges) { 
				Iterable<Relationship> rels = n.getRelationships(Neo4JStorage.NPE);
				for(Relationship r : rels) {
					NonProvenanceEdge np = newNonProvenanceEdge(r);
					//log.info("Added NPE " + np);
					col.addNonProvenanceEdge(np);
					
					String oid = np.getIncidentOID();
					if(!col.containsObjectID(oid)) { 
						// TODO
						// Add this back to the collection, because we came from the NPID
					}
					
				}
			} // End if
		} // End while

		PLUSObject focus = LineageDAG.chooseFocus(col, lastOID);
		if(focus != null) col.setFocus(focus);		
		col.getFingerPrint().stopTimer("Build"); 		
		
		// Because of max size constraints, we will frequently load less
		// of the graph than is actually in the database.  This method loops
		// through the edges, and tags nodes as having "more" information, whenever
		// there's an edge where the other end isnt in the dag.
		// This information lets GUI displays of the graph signify that a node isn't
		// actually a dead-end in the provenance graph.
		for(PLUSEdge e : col.getEdges()) {
			if(col.contains(e.getFrom()) && !col.contains(e.getTo()))
				col.tagNode(e.getFrom(), LineageDAG.TAG_MORE_AVAILABLE, "true");
			else if(!col.contains(e.getFrom()) && col.contains(e.getTo()))
				col.tagNode(e.getTo(), LineageDAG.TAG_MORE_AVAILABLE, "true"); 
		} // End for
		
		col = LineageDAG.computeEdgeVoting(col);   // Edge voting for surrogates
		col = LineageDAG.traceTaintSources(col);   // Trace indirect taints from direct taints
		col = LineageDAG.drawInferrableEdges(col); // Draw inferred edges based on surrogate alg.
		col = LineageDAG.tagHeadAndFeet(col);
				
		List<PLUSEdge> danglers = LineageDAG.detectDanglers(col);
		
		if(danglers.size() > 0) 
			log.warning("Collection " + col + " contains " + danglers.size() + " dangling edges."); 
		
		col.getFingerPrint().stopTimer("DiscoverCollection");
		
		col.getFingerPrint().startTimer("GraphFunctions");
		col.getFingerPrint().finished(col);
		col.getFingerPrint().stopTimer("GraphFunctions");		
		
		return col;		
	} // End newDAG
		
	/**
	 * Search for provenance objects by name.
	 * @param regex a regular expression to use to match names
	 * @param user the user permitted to see the data
	 * @return a provenance collection containing the results
	 */
	public static ProvenanceCollection searchFor(String regex, User user) { return searchFor(regex, user, DEFAULT_SEARCH_RESULTS); }
	
	public static ProvenanceCollection searchFor(String regex, User user, int max) {
		String query = "start n=node:node_auto_index({searchCriteria}) "+
                "where has(n.oid) and has(n.name) and has(n.created) " +
			    "return n " + 
                "order by n.created desc " + 
                "limit " + max;
        	
		log.info("regex=" + regex);
		
		Map<String,Object>params = new HashMap<String,Object>();
		params.put("searchCriteria", "name:*" + regex + "*");
				
		ProvenanceCollection col = new ProvenanceCollection();
		
		try (Transaction tx = Neo4JStorage.beginTx()) {
			ExecutionResult result = Neo4JStorage.execute(query, params);
			Iterator<Node> nodes = result.columnAs("n");
		
			while(nodes.hasNext()) { 
				Node n = nodes.next();
	
				try {
					col.addNode(Neo4JPLUSObjectFactory.newObject(n));
				} catch (PLUSException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return null;
				}
			}
			
			tx.success();
		} catch(TransactionFailureException exc) { 
			log.fine("Ignoring transaction failed exception.");
		}
			
		return col;
	} // End searchFor
	
	public static ProvenanceCollection getBLING(Iterable<String> oids, User user) {
		try {
			return Neo4JPLUSObjectFactory.getIncidentEdges(oids, user, "bling", true, false);
		} catch (PLUSException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static ProvenanceCollection getFLING(String id, User user) {
		return getFLING(Arrays.asList(new String[] { id }), user);
	}
	
	public static ProvenanceCollection getBLING(String id, User user) {
		return getBLING(Arrays.asList(new String[] { id }), user);
	}
	
	public static ProvenanceCollection getFLING(Iterable<String> oids, User user) {
		try { 
			return Neo4JPLUSObjectFactory.getIncidentEdges(oids, user, "fling", true, false);
		} catch(PLUSException e) { 
			e.printStackTrace();
		}
				
		return null;
	} // End getFLING
	
	/**
	 * Get the most recently created provenance objects.
	 * @param user the user permitted to see the data
	 * @param max the maximum number of results to return
	 * @return a provenance collection containing the most recently created items.
	 */
	public static ProvenanceCollection getRecentlyCreated(User user, int max) {
		if(max <= 0) max = 20;
		if(max > MAX_OBJECTS) {
			log.warning("Maximum objects that can be returned is " + MAX_OBJECTS + " not " + max); 
			max = MAX_OBJECTS;
		}
		
		String query = "match (n:" + Neo4JStorage.LABEL_NODE.name() + ") " +
				       "return n " + 
	                   "order by n.created desc " + 
	                   "limit " + max;
				
		ViewedCollection col = new ViewedCollection(user);		

		try (Transaction tx = Neo4JStorage.beginTx()) {
			ExecutionResult result = Neo4JStorage.execute(query);
			ResourceIterator<Node> nodes = result.columnAs("n");
			
			while(nodes.hasNext()) { 
				Node n = nodes.next();
				try {
					col.addNode(newObject(n));
				} catch (PLUSException e) {
					log.severe("Failed to add node: " + e.getMessage());
					e.printStackTrace();
					
					nodes.close();					
					return null;
				} // End catch
			} // End while
		
			nodes.close();
			
			// TODO
			// In Neo4J 2.0.1, tx.success() sometimes causes a failed transaction exception due to "unable to commit".
			// This happens in READ-ONLY CYPHER QUERIES.
			// Link to discussion thread:  https://groups.google.com/d/msg/neo4j/w1L_21z0z04/VNBN5epvgYMJ
			// Temporary work-around is to remove tx.success().
			// This is *not* the right thing to do, but it works for now until neo4j addresses the issue.			
			// tx.success();
			
			return col;
		} catch(TransactionFailureException exc) { 
			log.severe("Failed transaction on get most recent: " + exc.getMessage()); 
			return col; 
		}  
	}	
} // End Neo4JPLUSObjectFactory
