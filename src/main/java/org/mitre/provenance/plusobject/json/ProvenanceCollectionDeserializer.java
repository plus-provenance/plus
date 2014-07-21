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

import java.lang.reflect.Type;
import java.util.Map;
import java.util.logging.Logger;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.PLUSException;
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
import org.mitre.provenance.tools.PLUSUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * A utility class that plugs into Google GSON that turns a set of JsonElements into a ProvenanceCollection.
 * 
 * <p>TODO many design decisions may need to be revisited here.  While you're deserializing a collection, because it's a graph many
 * objects make reference to one another.  Edges have workflows, objects have owners, and so on.  When converting an object, we have to do 
 * it in a special order (e.g. owners first) so that when we go to referring objects (like nodes) we have already converted the requisite owner.
 * This raises a design decision - should it be OK for a json instance to refer to an owner that isn't already in that same JSON instance?
 * If yes, then that requires that the deserializer do database lookups, which couples the deserializer to the DB code.  If no, that means that
 * every instance that wants to talk about a PLUSActor has to drag the actor around with it.  Which isn't ideal.
 * 
 * <p>A second design decision here is completeness.  Let's say you specify an owner by reference (ownerid=blahblah) that isn't in the collection. 
 * OK, well we can't set that object to be owned by that user, because at present the deserializer doesn't look things up in the database to
 * prevent that coupling.  Now should that failure to set the owner be a fatal parsing error, or should it be recoverable?
 * 
 * <p>Right now, such errors are recoverable. If you give the deserializer bad data, it tries hard to get a coherent collection out of it.  This
 * may be the wrong choice though for developers who are new to logging provenance and don't know what mistakes they're making.
 * 
 * @author moxious
 */
public class ProvenanceCollectionDeserializer implements JsonDeserializer<ProvenanceCollection> {
	private static Logger log = Logger.getLogger(ProvenanceCollectionDeserializer.class.getName());
	
	public ProvenanceCollection deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {		
		if(!json.isJsonObject()) {
			log.info("Cannot deserialize this: " + json);
			throw new JsonParseException("Can only deserialize objects");
		}
		JsonObject obj = json.getAsJsonObject();
		
		ProvenanceCollection col = new ProvenanceCollection();
		
		JsonElement nodes = obj.get("nodes");
		JsonElement links = obj.get("links");
		JsonElement actors = obj.get("actors");
		
		if(!nodes.isJsonArray()) throw new JsonParseException("Missing top-level nodes array");
		if(!links.isJsonArray()) throw new JsonParseException("Missing top-level links array");
		if(!actors.isJsonArray()) throw new JsonParseException("Missing top-level actors array");
		
		JsonArray n = (JsonArray)nodes;
		JsonArray l = (JsonArray)links;
		JsonArray a = (JsonArray)actors;
		
		for(JsonElement actor : a) { 
			if(!actor.isJsonObject()) throw new JsonParseException("Actors list contains non-object " + actor.toString());
			
			PLUSActor convertedActor = convertActor((JsonObject)actor);
			log.info("Adding converted actor " +convertedActor);
			col.addActor(convertedActor);
		}
		
		for(JsonElement e : n) { 
			if(!e.isJsonObject()) throw new JsonParseException("Node list contains non-object " + e.toString());
			JsonObject o = (JsonObject)e;
			
			// NPID nodes are dummy stand-ins, and not provenance objects to be added.
			if("npid".equals(o.get("type").getAsString())) continue;
			
			PLUSObject pobj = convertObject(o, col);
			col.addNode(pobj);
		}
		
		for(JsonElement linkItem : l) { 
			if(!linkItem.isJsonObject()) throw new JsonParseException("Link list contains non-object " + linkItem.toString());
			JsonObject link = (JsonObject)linkItem;
			
			if(link == null || link.isJsonNull()) { 
				log.warning("Null link; skipping");
				continue;
			} else if(link.get("label") == null) { 
				log.warning("Link " + link + " MISSING type");
				continue;
			}
			
			if(link.get("label") == null || link.get("label").isJsonNull()) throw new JsonParseException("Missing attribute label on link/edge " + link);
			if(link.get("type") == null || link.get("type").isJsonNull()) throw new JsonParseException("Missing attribute type on link/edge " + link);
			
			String label = link.get("label").getAsString();
			String type = link.get("type").getAsString();
			
			if(PLUSEdge.isProvenanceEdgeType(type) && !"npe".equals(label)) { 
				PLUSEdge e = convertEdge(link, col);
				if(e != null) col.addEdge(e);				
			} else { 			
				NonProvenanceEdge npe = convertNPE(link);
				if(npe != null) col.addNonProvenanceEdge(npe);
			} 
		} // End for
		
		return col;
	}
		
	public static PLUSActor convertActor(JsonObject act) throws JsonParseException {
		String id = act.get("id").getAsString();
		String name = act.get("name").getAsString();
		long created = act.get("created").getAsLong();
		String type = act.get("type").getAsString();
		
		if(id == null || "".equals(id)) throw new JsonParseException("Invalid empty or missing 'id' on actor " + act);
		if(name == null || "".equals(name)) throw new JsonParseException("Invalid empty or missing 'name' on actor " + act);
		if(created <= 0) throw new JsonParseException("Invalid created " + created + " on actor " + act);
		if(type == null || "".equals(type)) throw new JsonParseException("Invalid empty or missing 'type' on actor " + act);
		
		if(!"actor".equals(type)) {
			log.warning("At this time, only type='actor' PLUSActors can be converted, but provided JSON presents " + type);
		}
		
		return new PLUSActor(id, name, created, type);
	}
	
	protected static PLUSObject convertObject(JsonObject obj, ProvenanceCollection contextCollection) throws JsonParseException {
		String t = obj.get("type").getAsString();
		String st = obj.get("subtype").getAsString();
		String name = obj.get("name").getAsString();
		
		if(name == null || "null".equals(name)) throw new JsonParseException("Missing name on object " + obj);
		if(t == null || "null".equals(t)) throw new JsonParseException("Missing type on object " + obj);
		if(st == null || "null".equals(st)) throw new JsonParseException("Missing subtype on object " + obj);
		
		JsonObjectPropertyWrapper n = new JsonObjectPropertyWrapper(obj);
		
		try {
			PLUSObject o = null;
			
			if(PLUSInvocation.PLUS_SUBTYPE_INVOCATION.equals(st)) { 
				o = new PLUSInvocation().setProperties(n, contextCollection);
			} else if(PLUSWorkflow.PLUS_TYPE_WORKFLOW.equals(t)) { 
				o = new PLUSWorkflow().setProperties(n, contextCollection);
			} else if(st.equals(PLUSString.PLUS_SUBTYPE_STRING)) {
				o = new PLUSString().setProperties(n, contextCollection);
			} else if(PLUSFile.PLUS_SUBTYPE_FILE.equals(st)) { 
				o = new PLUSFile().setProperties(n, contextCollection);
			} else if(PLUSFileImage.PLUS_SUBTYPE_FILE_IMAGE.equals(st)) {  
				o = new PLUSFileImage().setProperties(n, contextCollection);
			} else if(PLUSURL.PLUS_SUBTYPE_URL.equals(st)) { 
				o = new PLUSURL().setProperties(n, contextCollection);
			} else if(PLUSActivity.PLUS_TYPE_ACTIVITY.equals(t)) { 
				o = new PLUSActivity().setProperties(n, contextCollection);	
			} else if(PLUSRelational.PLUS_SUBTYPE_RELATIONAL.equals(st)) {  
				o = new PLUSRelational().setProperties(n, contextCollection);		
			} else if(Taint.PLUS_SUBTYPE_TAINT.equals(st)) {
				o = new Taint().setProperties(n, contextCollection);
			} else {
				log.info("Couldn't find more specific type for " + t + "/" + st + " so loading as generic."); 
				o = new PLUSGeneric().setProperties(n, contextCollection);
			}
			
			// Check metadata
			if(obj.has("metadata") && obj.get("metadata").isJsonObject()) {
				JsonObject md = obj.get("metadata").getAsJsonObject();
				
				Metadata m = new Metadata();
				
				for(Map.Entry<String,JsonElement> entry : md.entrySet()) {
					String key = entry.getKey();					
					String val = null;
					
					JsonElement v = entry.getValue();
					if(!v.isJsonPrimitive()) {
						log.warning("Skipping metadata key/value " + key + " => " + v + " because value isn't primitive.");
						continue;
					} else {
						val = v.getAsJsonPrimitive().getAsString();
					}
					
					m.put(key, val); 
				} // End for
				
				o.getMetadata().putAll(m);
			}
			
			// Check owner status.
			String aid = obj.get("ownerid").getAsString();
			
			log.info("Deserializing " + o + " actorID = " + aid + " and owner=" + obj.get("owner"));
			if(isBlank(aid) && obj.has("owner")) {
				JsonElement ownerJson = obj.get("owner");
				if(!ownerJson.isJsonObject()) throw new JsonParseException("Property 'owner' must be an object on " + obj);
				
				PLUSActor owner = convertActor((JsonObject)ownerJson);
				if(owner != null) {
					log.info("Set using converted owner property " + owner);
					o.setOwner(owner);
				}
			} else if(aid != null) {
				if(contextCollection.containsActorID(aid)) {
					log.info("Set using provided context collection " + contextCollection.getActor(aid));
					o.setOwner(contextCollection.getActor(aid));
				} else { 
					log.severe("Deserializer cannot find actor by dangling reference " + aid + " - provenance context collection is needed to identify this actor without database access.");
					o.setOwner(null);
				}
			} else {
				log.info("Can't set owner for " + o + " none of my tricks work.");
			}
			
			return o;
		} catch(PLUSException exc) { 
			exc.printStackTrace();
			throw new JsonParseException(exc.getMessage());
		}
	} // End convertObject

	protected static boolean isBlank(String val) { 
		return (val == null) || "".equals(val) || "".equals(val.trim());
	}
	
	protected static NonProvenanceEdge convertNPE(JsonObject obj) throws JsonParseException { 
		String from = obj.get("from").getAsString();
		String to = obj.get("to").getAsString();
		String oid = obj.get("npeid").getAsString();
		
		// NPEs have a type field=npe to indicate that they're non-provenance edges.
		// The actual type of edge ("md5sum") is stored in the label.
		String type = obj.get("label").getAsString();
		long created = obj.get("created").getAsLong();
		
		if(from == null || "null".equals(from)) throw new JsonParseException("Missing from on NPE " + obj);
		if(to == null || "null".equals(to)) throw new JsonParseException("Missing to on NPE " + obj);
		if(type == null || "null".equals(type)) throw new JsonParseException("Missing label on NPE " + obj);
		if(oid == null) {
			log.warning("NPEID mising on " + obj);
			oid = PLUSUtils.generateID();
		}
				
		try { 
			return new NonProvenanceEdge(oid, from, to, type, created);			
		} catch(PLUSException exc) { 
			exc.printStackTrace();
			throw new JsonParseException(exc.getMessage());
		}
	} // End convertNPE
	
	/**
	 * Thi
	 * @param nodeOID
	 * @return
	 */
	protected static PLUSObject resurrect(String nodeOID, ProvenanceCollection col) throws PLUSException {
		if(col.containsObjectID(nodeOID)) return col.getNode(nodeOID);
		// else return Neo4JPLUSObjectFactory.newObject(nodeOID);
		
		log.severe("Cannot recall node by ID " + nodeOID + " because it isn't in provenance context collection.   Database lookups during " + 
		           "deserialization are disabled.  This likely means ");
		
		return null;
	}
	
	protected static PLUSEdge convertEdge(JsonObject obj, ProvenanceCollection col) throws JsonParseException {
		try { 
			String from = obj.get("from").getAsString();
			String to = obj.get("to").getAsString();
			String wfid = obj.get("workflow").getAsString();
			String type = obj.get("type").getAsString();
			
			if(from == null || "null".equals(from)) throw new JsonParseException("Missing from on edge " + obj);
			if(to == null || "null".equals(to)) throw new JsonParseException("Missing to on edge " + obj);
			if(type == null || "null".equals(type)) throw new JsonParseException("Missing type/label on edge " + obj);
			if(wfid == null || "null".equals(wfid)) wfid = PLUSWorkflow.DEFAULT_WORKFLOW.getId();
			
			if(!PLUSEdge.isProvenanceEdgeType(type)) 
				throw new JsonParseException("Edge type " + type + " on edge " + obj + " isn't provenance.");
			
			PLUSObject fromObj = null, toObj = null;
			
			try { 
				fromObj = resurrect(from, col);
			} catch(PLUSException exc) { 
				log.warning("Ignoring edge because of non-existant from ID " + from);
				return null;				
			}
			
			try { 
				toObj = resurrect(to, col);
			} catch(PLUSException exc2) {
				log.warning("Ignoring edge because of non-existant to ID " + to); 
				return null;
			}
						
			PLUSWorkflow wf = PLUSWorkflow.DEFAULT_WORKFLOW;
			
			if(wfid != null && !PLUSWorkflow.DEFAULT_WORKFLOW.getId().equals(wfid)) {
				if(col.containsObjectID(wfid)) wf = (PLUSWorkflow)col.getNode(wfid);
				else {
					// TODO -- there's a design argument that this should be a fatal error/exception.
					log.severe("Cannot re-load workflow " + wfid + " because it isn't in context provenance collection.  " + 
				               "Database lookups are disabled on deserialization.");
					wf = PLUSWorkflow.DEFAULT_WORKFLOW;
				}
			}
						
			return new PLUSEdge(fromObj, toObj, wf, type);
		} catch(NullPointerException exc) { 
			exc.printStackTrace();
			throw new JsonParseException("Edge missing one or more properties of from, to, workflow, label:  " + obj); 
		}
	} // End convertEdge	
	
}
