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
package org.mitre.provenance.plusobject.marking;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.PropertySet;
import org.mitre.provenance.dag.TraversalSettings;
import org.mitre.provenance.dag.ViewedCollection;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.user.User;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;

/**
 * Taints are special kinds of PLUSObjects that are asserted about other PLUSObjects.  They show
 * up in the lineage DAG as proper elements, but they are related to other nodes by non-lineage
 * edges (i.e. PLUSEdge.EDGE_TYPE_MARKING)
 * @author DMALLEN
 */
public class Taint extends HeritableMarking {
	public static final String MARK_TYPE = "taint";		
	public static final String PLUS_SUBTYPE_TAINT = MARK_TYPE;
	protected String description; 
	
	private static final String PROP_CLAIMANT = "claimant";
	private static final String PROP_DESCRIPTION = "description";
	private static final String PROP_WHEN_ASSERTED = "when_asserted";
	
	public Taint() { 
		super();
		setName("Taint");
		setObjectSubtype(MARK_TYPE);
		setDescription("");
		setWhenAsserted(new Date());
	}
	
	public Taint(User claimant, String description) {
		this();
		setClaimant(claimant);
		setDescription(description); 
	} // End Taint
		
	public void setDescription(String desc) { this.description = desc; } 
	public String getDescription() { return description; } 
		
	public Map<String,Object> getStorableProperties() {
		Map<String,Object> m = super.getStorableProperties();
		m.put(PROP_CLAIMANT, claimant.getName());
		m.put(PROP_DESCRIPTION, getDescription());
		m.put(PROP_WHEN_ASSERTED, getWhenAsserted().getTime());
		return m;
	}
	
	public PLUSObject setProperties(PropertySet props) throws PLUSException { 
		super.setProperties(props);
		setDescription(""+props.getProperty(PROP_DESCRIPTION));
				
		PLUSActor act = Neo4JPLUSObjectFactory.getActor(""+props.getProperty(PROP_CLAIMANT));
		User u = null;
				
		if(act == null) {
			log.severe("No such actor by claimant name " + claimant + " ...faking it.");
			u = new User(""+props.getProperty(PROP_CLAIMANT));
		} else if(!(act instanceof User)) {
			log.severe("Actor " + act + " isn't a user!");
			u = new User(""+props.getProperty(PROP_CLAIMANT));
		} else { 
			u = (User)act;
		}
		
		setClaimant(u);
		setWhenAsserted(new Date((Long)props.getProperty(PROP_WHEN_ASSERTED)));
		return this;
	} // End setProperties
	
	/**
	 * Mark an object as "tainted". This creates a new Taint object and links
	 * it one hop upstream of the provided object.
	 * @param tainted the object to mark as tainted.
	 * @param user the user who claims this object is tainted.
	 * @param description the user's description of the taint.
	 * @throws PLUSException
	 */
	public static Taint taint(PLUSObject tainted, User user, String description) throws PLUSException {
		Taint t = new Taint(user, description);
		Neo4JStorage.store(t);		
		PLUSEdge connector = new PLUSEdge(t, tainted, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_MARKS);
		Neo4JStorage.store(connector);
		
		return t;
	} // End taint()	
	
	/**
	 * Deletes all immediate edges to objects of type Taint, and the original taint object.
	 * @param untainted the object whose taints should be removed.
	 * @return the number of taint objects removed.
	 * @throws PLUSException
	 */
	public static int removeTaints(PLUSObject untainted) throws PLUSException {
		ProvenanceCollection col = Neo4JPLUSObjectFactory.getBLING(untainted.getId(), User.DEFAULT_USER_GOD);

		int c = 0;		
		for(PLUSEdge b : col.getEdges()) {
			//System.out.println("Looking for taint edge: " + b); 
			// We're only looking for "marks" edge types... 
			if(!PLUSEdge.EDGE_TYPE_MARKS.equals(b.getType())) continue;
			
			// Load the object...
			PLUSObject incident = Neo4JPLUSObjectFactory.newObject(Neo4JStorage.oidExists(b.getFrom().getId()));
			
			//System.out.println("Loaded " + incident + " from edge " + b); 
			
			if (incident != null && incident.isHeritable() && incident.getObjectSubtype().equals(Taint.MARK_TYPE)) {
				if(Neo4JStorage.delete(incident, true)) c++;	
			} else {
				log.warning("Failed to remove taint object " + incident + " identified via " + b.getFrom().getId()); 
			}
		} // End for
		
		return c;
	} // End removeTaints
	
	public static ProvenanceCollection getAllTaintSources(PLUSObject obj, User user) throws PLUSException { 
		ViewedCollection col = new ViewedCollection(user);
		
		col.addAll(getIndirectTaintSources(obj, user)); 
		
		for(PLUSObject taint : getDirectTaints(obj, user)) 
			col.addNode(taint);
		
		return col;
	} // End getAllTaintSources
	
	/**
	 * Traces remote (even quite distant) sources of taint to a particular object.
	 * This way, you can find sources of taint even if it is outside of the scope of a single provenance graph.
	 * This query will NOT return *direct* taints.
	 * @param obj the object of interest
	 * @return a collection of taint objects, or an empty collection if there are none.
	 * @throws PLUSException
	 */
	public static ProvenanceCollection getIndirectTaintSources(PLUSObject obj, User user) throws PLUSException { 
		ViewedCollection pc = new ViewedCollection(user);

		String query = "start n=node:node_auto_index(oid={oid}) " + 
		               "match taintNode-[r1:marks]->intermediates-[r:contributed|`input to`|unspecified|triggered|generated*]->n " +
				       "where has(taintNode.subtype) and " +  
		               "taintNode.subtype = '" + PLUS_SUBTYPE_TAINT + "' " + 
				       "return taintNode limit 50";

		Map<String,Object> params = new HashMap<String,Object>();
		params.put("oid", obj.getId());
		
		Neo4JStorage.execute(query, params); 
		
		ExecutionResult result = Neo4JStorage.execute(query, params);
		Iterator<Node> ns = result.columnAs("taintNode");

		while(ns.hasNext()) pc.addNode(Neo4JPLUSObjectFactory.newObject(ns.next()));		
		
		return pc;
	} // End traceRemoteTaintSources
	
	/**
	 * This function provides a way of identifying all *originally* tainted nodes in a provenance collection.
	 * That is, the set of nodes that are immediately linked with a Taint object.  This function operates on 
	 * an already-assembled collection of provenance nodes, and does not consult any other database or source 
	 * of information.  As an important limitation, this means that taint objects not already in the collection
	 * will not be discovered.  For discovering per-node taint, you may want to use getDirectTaints.
	 * @param col the provenance collection to search
	 * @return a map that maps OID of an object to a list of direct taints that it has under col. 
	 */
	public static HashMap<String,ArrayList<Taint>> getTaintSources(ProvenanceCollection col) { 
		HashMap<String, ArrayList<Taint>> taints = new HashMap<String,ArrayList<Taint>>();
		
		for(PLUSObject o : col.getNodes()) { 
			List<PLUSEdge>bling = col.getInboundEdgesByNode(o.getId());
			for(PLUSEdge e : bling) { 
				if(col.contains(e.getFrom()) &&  
				   (e.getFrom() instanceof Taint)) {
					ArrayList<Taint>list = taints.get(o.getId());
					if(list == null) { list = new ArrayList<Taint>(); taints.put(o.getId(), list); } 
					list.add((Taint)e.getFrom());
				}
			}
		} // End for
				
		return taints;
	} // End getTaintSources()
	
	/**
	 * Gets the list of taints directly associated with an object, i.e. taint
	 * annotations on this object. There is also such a thing as indirect taints
	 * (inherited). This will not return inherited taints.
	 * @param obj the object to check.
	 * @return a List of Taint objects, or an empty list if there are none.
	 * @throws PLUSException
	 */
	public static Set<Taint> getDirectTaints(PLUSObject obj, User user) throws PLUSException {
		HashSet<String> oids = new HashSet<String>();
		oids.add(obj.getId()); 
		
		// Create a new traversal that goes only one hop away, backwards, and returns only nodes.
		TraversalSettings trav = new TraversalSettings().onlyBackward().setMaxDepth(1).excludeEdges().excludeNPEs().includeNodes();
		
		ProvenanceCollection col = Neo4JPLUSObjectFactory.newDAG(obj.getId(), user, trav);
		
		Set<Taint> results = new HashSet<Taint>();
		
		// Just go through the nodes that are one hop upstream.  The ones that are taint objects are the direct taints.
		for(PLUSObject upstreamNode : col.getNodes()) {
			if(upstreamNode.isHeritable() && upstreamNode.getObjectSubtype().equals(PLUS_SUBTYPE_TAINT)) {				
				results.add((Taint)upstreamNode);
			}
		}
		
		return results;
	} // End getDirectTaints
} // End Taint
