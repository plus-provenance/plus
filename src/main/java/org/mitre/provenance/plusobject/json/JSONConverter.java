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
package org.mitre.provenance.plusobject.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.dag.FingerPrint;
import org.mitre.provenance.dag.LineageDAG;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.npe.NonProvenanceEdge;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.user.PrivilegeClass;
import org.mitre.provenance.user.PrivilegeSet;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * An object that knows how to take JSON objects and turn them into ProvenanceCollections.
 * @author DMALLEN
 */
public class JSONConverter {
	private static Logger log = Logger.getLogger(JSONConverter.class.getName());
	
	/** This inner class wraps a JsonObject in an implementation of PropertyContainer, which makes it easier to get and set object properties 
	 * from the main API 
	 */
	
	/** The set of reserved keys in a JSON dict, that should not be added to a PLUSObject's metadata table. */
	public static HashSet<String>reservedKeys = new HashSet<String>();
	
	static { 
		reservedKeys.add("plus:owner");
		reservedKeys.add("plus:type");
		reservedKeys.add("data");
		reservedKeys.add("inputs");
		reservedKeys.add("outputs");
	} // End static initialzier.
		
	/**
	 * Check for owner information in a JSONObject, and return the corresponding PLUSActor.  Wherever 
	 * possible, pre-existing actors will be loaded and reused.
	 * @param obj the JSONObject provided as a web service input.
	 * @return the PLUSActor object corresponding to the plus:owner field of the object.
	 * @throws PLUSException
	 */
	protected static PLUSActor getOwner(JsonObject obj) throws PLUSException { 
		try { 
			String name = obj.get("plus:owner").getAsString(); 
			
			// TODO is this always correct?  Should we always create if it can't be found?
			if(name != null && !"".equals(name)) 
				return Neo4JPLUSObjectFactory.getActor(name, true);
		} catch(Exception exc) { ; } 
		
		return Neo4JPLUSObjectFactory.getActor("Unknown", true);
	} // End getOwner
			
	protected static HashMap<String,Object> npidNodeToD3(String nodeLabel, NonProvenanceEdge npe) { 
		HashMap<String,Object> n = new HashMap<String,Object>();
		
		n.put("id", nodeLabel);
		n.put("label", nodeLabel);
		n.put("type", "npid");
		n.put("subtype", "npid");
		n.put("created", npe.getCreated());
				
		return n;
	} // End npidNodeToD3
	
	public static Map<String,Object> provenanceObjectToD3(PLUSObject obj) { 
		// Get the basic storable properties; sub-class specific.
		Map<String,Object> n = obj.getStorableProperties();
		
		// Put some D3-specific stuff in there.
		n.put("id", obj.getId());
		n.put("label", obj.getName()); 
		
		if(obj.getOwner() != null) n.put("ownerid", obj.getOwner().getId());
		
		PrivilegeSet ps = obj.getPrivileges();
		
		if(ps != null) { 
			ArrayList<Object> privs = new ArrayList<Object>();			
			for(PrivilegeClass pc : ps.getPrivilegeSet()) { privs.add(pc.toString()); }  
			n.put("privileges", privs);
		} // End if
				
		if(obj.getSourceHints() != null) n.put("sourceHints", obj.getSourceHints().toString());			
		
		// Metadata is handled separately/specially.
		HashMap<Object,Object> mData = new HashMap<Object,Object>();
		Metadata m = obj.getMetadata();
		for(Object k : m.keySet()) mData.put("" + k, m.get(k)); 
		
		n.put("metadata", mData);
		
		return n;
	} // End provenanceObjectToD3
	
	/**
	 * This creates a JSON string suitable to represent a given provenance collection for D3.
	 * It broadly follows the format found at http://bl.ocks.org/mbostock/4062045#miserables.json
	 * @param col a provenance collection
	 * @return a JSON string
	 * @throws PLUSException
	 */
	public static String provenanceCollectionToD3Json(ProvenanceCollection col) {
		HashMap<String,Object> structure = new HashMap<String,Object>();
		ArrayList<Object> nodes = new ArrayList<Object>();
		ArrayList<Object> links = new ArrayList<Object>();
		ArrayList<Object> actors = new ArrayList<Object>();
		
		HashMap<String,Integer> indexMapping = new HashMap<String,Integer>();
		
		int idx=0;
		
		if(col == null) {
			log.severe("Cannot convert NULL collection to JSON");
			return null;
		}
		
		// Always sort by creation date; sometimes collections will be serialized for
		// feeds.
		for(PLUSObject obj : col.getNodesInOrderedList(ProvenanceCollection.SORT_BY_CREATION)) { 			
			nodes.add(provenanceObjectToD3(obj));			
			indexMapping.put(obj.getId(), idx);
			idx++;
		}
		
		for(NonProvenanceEdge npe : col.getNonProvenanceEdges()) {
			String oid = npe.getId();
			String id1 = npe.getFrom();
			String id2 = npe.getTo();
			String type = npe.getType();			
						
			Integer fromIdx = indexMapping.get(id1);
			Integer toIdx = indexMapping.get(id2);
			
			/*
			if(PLUSUtils.isPLUSOID(id1) && PLUSUtils.isPLUSOID(id2)) { 
				System.err.println("Processing NPE " + npe + " FROM " + id1 + " => " + id2 + " fromIDX=" + fromIdx + " toIDX=" + toIdx);
				System.err.println("COL contains from: " + col.containsObjectID(id1));
				System.err.println("COL contains to: " + col.containsObjectID(id2));
			}
			*/
			
			if(fromIdx == null) { 
				indexMapping.put(id1, idx);
				nodes.add(npidNodeToD3(id1, npe));
				fromIdx = idx;
				idx++;
			}
			
			if(toIdx == null) { 
				indexMapping.put(id2, idx);
				nodes.add(npidNodeToD3(id2, npe)); 
				toIdx = idx;
				idx++;
			}
			
			HashMap<String,Object> jsonEdge = new HashMap<String,Object>();

			jsonEdge.put(Neo4JStorage.PROP_NPEID, oid);
			jsonEdge.put("source", fromIdx);
			jsonEdge.put("target", toIdx);
			jsonEdge.put("from", id1);
			jsonEdge.put("to", id2);
			jsonEdge.put("label", type);
			jsonEdge.put("type", "npe");			
			jsonEdge.put("left", new Boolean(false));
			jsonEdge.put("right", new Boolean(true));
			jsonEdge.put("created", npe.getCreated());
			if(npe.getSourceHints() != null) jsonEdge.put("sourceHints", npe.getSourceHints().toString());
				
			links.add(jsonEdge);
		} // End foreach NPE
				
		for(PLUSEdge e : col.getEdges()) { 
			Integer fromIdx = indexMapping.get(e.getFrom().getId());
			Integer toIdx = indexMapping.get(e.getTo().getId());
			
			if(fromIdx == null || toIdx == null) { 
				// log.warning("Missing idx for either " + e.getFrom() + " or " + e.getTo());

				// Do not report this edge.  This usually occurs when the edge points to a 
				// valid node that is outside of this provenance collection.  E.g. if you 
				// load a 50-node graph, and the graph actually has 51 nodes, there might be 
				// an edge to that 51st node where that node isn't in the collection, but
				// the edge is valid.
				continue;
			} // End if
			
			HashMap<String,Object> jsonEdge = new HashMap<String,Object>();
			jsonEdge.put("source", fromIdx);
			jsonEdge.put("target", toIdx); 
			jsonEdge.put("from", e.getFrom().getId());
			jsonEdge.put("to", e.getTo().getId());
			jsonEdge.put("left", new Boolean(false)); 
			jsonEdge.put("right", new Boolean(true)); 
			jsonEdge.put("label", e.getType());
			jsonEdge.put("type", e.getType());
			if(e.getSourceHints() != null) jsonEdge.put("sourceHints", e.getSourceHints().toString()); 
			jsonEdge.put("workflow", e.getWorkflow().getId());
			
			links.add(jsonEdge);
		} // End foreach PLUSEdge
				
		structure.put("nodes", nodes);
		structure.put("links", links); 
		
		HashMap<String,Object> actorProps = null;
		
		for(PLUSActor a : col.getActors()) {
			actorProps = new HashMap<String,Object>();
			
			actorProps.put("id", a.getId());
			actorProps.put("name", a.getName());
			actorProps.put("created", a.getCreated());
			actorProps.put("type", a.getType());
			
			actors.add(actorProps);
		}
		
		structure.put("actors", actors);
		
		HashMap<String,Object> nodeTags = new HashMap<String,Object>();
		for(String oid : col.getTaggedNodes()) { 
			HashMap<String,String> tags = col.getTags(oid);
			nodeTags.put(oid, tags);
		}		
		
		structure.put("nodeTags", nodeTags);		
		
		if(col instanceof LineageDAG) { 
			LineageDAG dag = (LineageDAG)col;
			FingerPrint fp = dag.getFingerPrint();
			structure.put("fingerprint", fp.getStorableProperties());
		}
		
		return new GsonBuilder().setPrettyPrinting().create().toJson(structure);
	}
	
	public static String provenanceCollectionToJITJson(ProvenanceCollection col) throws PLUSException { 
		List<Object> structure = new ArrayList<Object>();

		for(PLUSObject obj : col.getNodes()) { 
			HashMap<String,Object> h = new HashMap<String,Object>();
			h.put("id", obj.getId());
			h.put("name", obj.getName());

			HashMap<Object,Object> data = new HashMap<Object,Object>();
			data.put("$type", "triangle");

			if(obj.isDataItem()) data.put("$type", "ellipse");
			else if(obj.isInvocation()) data.put("$type", "rectangle");

			Metadata m = obj.getMetadata();
			for(Object k : m.keySet()) data.put("metadata:" + k, m.get(k)); 

			Map<String,Object> sprops = obj.getStorableProperties();
			for(String k : sprops.keySet()) { 
				if(sprops.get(k) instanceof String) data.put(k, sprops.get(k));
			}

			if(obj.getOwner() != null) data.put("owner", obj.getOwner().getName());
			data.put("created", obj.getCreatedAsDate().toString());

			h.put("data", data);

			List<Object> adjs = new ArrayList<Object>();
			for(PLUSEdge e : col.getOutboundEdgesByNode(obj.getId())) {
				HashMap<String,Object> adj = new HashMap<String,Object>();
				adj.put("nodeTo", e.getTo());				
				adj.put("nodeFrom", e.getFrom());

				HashMap<String,String> edgeData = new HashMap<String,String>();
				edgeData.put("label", e.getType());
				edgeData.put("workflow", e.getWorkflow().getId());
				adj.put("data", edgeData);

				adjs.add(adj);
			}						

			h.put("adjacencies", adjs);

			structure.add(h);
		} // End for

		return new GsonBuilder().setPrettyPrinting().create().toJson(structure);		
	}	
} // End JSONConverter
