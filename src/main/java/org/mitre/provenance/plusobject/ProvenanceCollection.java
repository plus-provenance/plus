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
package org.mitre.provenance.plusobject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.npe.NonProvenanceEdge;
import org.mitre.provenance.tools.PLUSUtils;

/**
 * A collection of provenance objects, nodes and edges.
 * Basic provenance collections cannot be assumed to be well-connected.  They do not necessarily comprise a graph.  See LineageDAG
 * for an example of an actual graph, which extends this class.
 * @author DMALLEN
 * @see org.mitre.provenance.dag.LineageDAG
 */
public class ProvenanceCollection implements Cloneable {
	protected static Logger log = Logger.getLogger(ProvenanceCollection.class.getName());
	public static final String TAINT_FLAG = "dag:taint";
	public static final String TAG_VALUE_TRUE = "true";
	public static final String TAG_FOOT = "foot";
	public static final String TAG_HEAD = "head";
		
	/**
	 * This comparator will sort first by created, then name, then type, then subtype, then ID.
	 */
	public static final Comparator<PLUSObject> SORT_BY_CREATION = new Comparator <PLUSObject> () { 
		public int compare(PLUSObject object1, PLUSObject object2) { 
			int cmp = Long.valueOf(object2.getCreated()).compareTo(object1.getCreated());
			if(cmp != 0) return cmp;
			
			cmp = object1.getName().compareTo(object2.getName());
			if(cmp != 0) return cmp;
			
			cmp = object1.getObjectType().compareTo(object2.getObjectType());
			if(cmp != 0) return cmp;
			
			cmp = object1.getObjectSubtype().compareTo(object2.getObjectSubtype());
			if(cmp != 0) return cmp;
			
			// Last resort.
			return object1.getId().compareTo(object2.getId());
		} 
	};
	
	public static final Comparator<PLUSObject> SORT_BY_NAME = new Comparator<PLUSObject> ()
		{ public int compare(PLUSObject o1, PLUSObject o2) 
		{ return o1.getName().compareTo(o2.getName()); } };
	
	/** Maps an actor ID to an actor */
	protected Map <String,PLUSActor> actors;
	
	/** Maps object or surrogate IDs to objects */
	protected Map <String,PLUSObject> nodes;
	
	/** Maps a special edge string key (id1-id2) to an edge */
	protected Map <String,PLUSEdge> edges;         
	
	/** Maps a special edge string key (id1-id2) to an NPE */
	protected Map <String,NonProvenanceEdge> npes; 
	
	/** ID for this collection */
	protected String id; 

	/** A way of associating text tags with nodes for later processing */	
	protected Map <String, HashMap<String,String>> nodeTags;  
	
	/** A way of storing extra junk about the collection we'll need later */
	protected Metadata metadata; 

	public ProvenanceCollection() { 
		id = PLUSUtils.generateID();		
		empty();
	} // End ProvenanceCollection
	
	public ProvenanceCollection(Iterable<SourcedObject> objs) {
		this();
		for(SourcedObject obj : objs) {
			if(obj instanceof PLUSEdge) addEdge((PLUSEdge)obj);
			else addNode((PLUSObject)obj);
		}
	} // End ProvenanceCollection
	
	public String getId() { return id; } 
	public void setId(String id) { this.id = id; } 
	
	/**
	 * Empties the contents of the collection, including metadata.
	 */
	protected void empty() {
		nodes = new HashMap<String,PLUSObject>();
		edges = new HashMap<String,PLUSEdge>();
		npes  = new HashMap<String,NonProvenanceEdge>();
		actors = new HashMap<String,PLUSActor>();
		metadata = new Metadata();
		metadata.setOwnerOID(id);
		nodeTags = new HashMap<String,HashMap<String,String>>();
	} // End empty
	
	/**
	 * Gets the metadata object associated with the collection.
	 * @return a metadata object
	 */
	public Metadata getMetadata() { return metadata; } 	
	public void setMetadata(Metadata md) {
		this.metadata = new Metadata();
		for(Object k : md.keySet()) metadata.put(k.toString(), ""+md.get(k));
		this.metadata.setOwnerOID(id);
	} // End setMetadata
	
	/**
	 * @param aid an actor's ID
	 * @return true if the collection contains the actor, false otherwise.
	 */
	public boolean containsActorID(String aid) {
		if(aid == null) return false;
		return actors.containsKey(aid);
	}
	
	/**
	 * NOTE!  Because of surrogates, keep in mind that there can be multiple different objects
	 * by the same ID - you might want to use the regular contains() method if you're looking for a particular
	 * object instance.
	 * @param oid a PLUSObject oid from the database
	 * @return true if the collection contains an object by that ID
	 */
	public boolean containsObjectID(String oid) { 
		return nodes.containsKey(oid);
	}
	
	/**
	 * Determine whether a provenance object is in the collection. It must be the same actual reference;
	 * a surrogate for a given OID will not match the original object.
	 * @param obj a PLUSObject
	 * @return true if the object is in the provenance collection, false otherwise.
	 */
	public boolean contains(PLUSObject obj) { 
		return nodes.containsKey(obj.getId()) && (nodes.get(obj.getId()) == obj); 
	}
	
	/**
	 * Determine whether an actor is in the collection.  It must be the same actual reference.
	 * @param actor the actor to check
	 * @return true if that object is in the collection, false otherwise.
	 */
	public boolean contains(PLUSActor actor) {
		return actors.containsKey(actor.getId()) && (actors.get(actor.getId()) == actor);
	}
	
	/**
	 * @param npe a non-provenance edge 
	 * @return true if the collection contains that NPE, false otherwise.
	 */
	public boolean contains(NonProvenanceEdge npe) { 
		return npes.containsKey(getHashKey(npe)); 
	} // End contains
	
	/**
	 * @param edge a PLUSEdge 
	 * @return true if the collection contains that edge, false otherwise.
	 */
	public boolean contains(PLUSEdge edge) {
		return nodes.containsKey(getHashKey(edge)); 
	}
	
	/**
	 * @param oid a PLUSObject oid 
	 * @return the PLUSObject, or null if it is not in the collection
	 */
	public PLUSObject getNode(String oid) {
		PLUSObject obj = nodes.get(oid);
		// if(obj == null) log.warn("getNode: " + oid + " not present!");
		return obj;
	} // End getNode
	
	/** Return the total number of actors in the collection. */
	public int countActors() { return actors.size(); } 
	/** Return the total number of nodes in the collection */
	public int countNodes() { return nodes.size(); } 
	/** Return the total number of edges in the collection */ 
	public int countEdges() { return edges.size(); } 
	/** Return the total number of NPEs in the collection */ 
	public int countNPEs() { return npes.size(); } 
	
	/**
	 * Get a particular edge from the collection.
	 * @param from_oid ID of the from side
	 * @param to_oid ID of the to side
	 * @return a PLUSEdge that's in the collection, or null if there is no such edge.
	 */
	public PLUSEdge getEdge(String from_oid, String to_oid) { 		
		return edges.get(getHashKey(from_oid, to_oid));
	}
	
	/**
	 * Get a particular edge from the collection
	 * @param from an object that originates the edge
	 * @param to an object that terminates the edge
	 * @return a PLUSEdge that's in the collection, or null if there is no such edge.
	 */
	public PLUSEdge getEdge(PLUSObject from, PLUSObject to) {
		return getEdge(from.getId(), to.getId()); 
	}
	
	/**
	 * Remove an edge from the collection.
	 * @param edge the edge to remove.
	 */
	public void removeEdge(PLUSEdge edge) { 		
		edges.remove(getHashKey(edge)); 
	} // End removeEdge
	
	/**
	 * Remove a non-provenance edge from the collection.
	 * @param npe the NPE to remove.
	 */
	public void removeNonProvenanceEdge(NonProvenanceEdge npe) { 
		npes.remove(getHashKey(npe)); 
	}
	
	/** Retrieves all inbound edges for the specified node */
	public List<PLUSEdge> getInboundEdgesByNode(String oid)
		{ return getEdgesByPattern("->" + oid); }
	
	/** Retrieves all outbound edges for the specified node */
	public List<PLUSEdge> getOutboundEdgesByNode(String oid)
		{ return getEdgesByPattern(oid + "->"); }
	
	/** Retrieves all edges incident to the specified node. */
	public List<PLUSEdge> getEdgesByNode(String oid) 
		{ return getEdgesByPattern(oid); }  
	
	/** Retrieves all edges based on the specified pattern */
	protected List<PLUSEdge> getEdgesByPattern(String pattern) { 
		ArrayList <PLUSEdge> ret = new ArrayList <PLUSEdge> ();			
		for(String key : edges.keySet())
			if(key.contains(pattern)) ret.add(edges.get(key));
		return ret;
	}
    
    public List<PLUSEdge> getEdgesByString(String pattern) {
    	return getEdgesByPattern(pattern);
    }
        
    /** Get the actors in this ProvenanceCollection */
    public Collection <PLUSActor> getActors() { return actors.values(); } 
    
    /** Get the nodes in this ProvenanceCollection */
	public Collection <PLUSObject> getNodes() { return nodes.values(); } 
	
	/** Get the edges in this ProvenanceCollection */
	public Collection <PLUSEdge> getEdges() { return edges.values(); }
	
	/** Get the non-provenance edges in this provenance collection */
	public Collection <NonProvenanceEdge> getNonProvenanceEdges() { return npes.values(); } 
	
	/**
	 * Get the non-provenance edges that are incident to a given object.
	 * @param obj an object in the collection
	 * @return the list of NPEs incident to that object, or an empty list if there are none, or if the object isn't in the collection.
	 */
	public List<NonProvenanceEdge> getNonProvenanceEdges(PLUSObject obj) { 
		ArrayList<NonProvenanceEdge> r = new ArrayList<NonProvenanceEdge>();
		
		for(String k : npes.keySet()) {
			if(k.contains(obj.getId())) r.add(npes.get(k));
		}
		
		return r;
	} // End getNonProvenanceEdge
	
	/**
	 * Equivalent to addEdge(edge, false)
	 * @see ProvenanceCollection#addEdge(PLUSEdge, boolean)
	 */
	public boolean addEdge(PLUSEdge edge) { return addEdge(edge, false); } 
	
	/**
	 * Add an edge to the collection.   
	 * @param edge the edge to add.
	 * @param force if true, this will be added overwriting anything similar already there.  If false, the edge
	 * will be added only if its candidate nodes aren't already connected.
	 * @return true if the edge was added, false otherwise.
	 */
	public boolean addEdge(PLUSEdge edge, boolean force) {
		if(edge == null) return false;
		String key = getHashKey(edge);
		
		if(force) { 
			edges.put(key, edge); 
			return true; 
		} else if(!edges.containsKey(key)) { 
			edges.put(key, edge);
			return true; 
		} // End else if
		
		return false; 
	} // End addEdge	
	
	/** Used to calculate the key under which an edge will be stored. */
	private String getHashKey(String from_oid, String to_oid) { 
		return from_oid + "->" + to_oid;
	}
	
	/** Used to calculate the key under which the edge will be stored. */
	private String getHashKey(PLUSEdge e) { 
		return getHashKey(e.getFrom().getId(), e.getTo().getId()); 
	}

	/** Used to calculate the key under which the edge will be stored. */
	private String getHashKey(NonProvenanceEdge npe) { 
		return getHashKey(npe.getFrom(), npe.getTo());
	}
	
	/**
	 * Add a specified NPE to the provenance collection
	 * @param npe the NPE to add
	 * @return true if it was added, false if it was not (i.e. the edge already existed)
	 */
	public boolean addNonProvenanceEdge(NonProvenanceEdge npe) { 
		return addNonProvenanceEdge(npe, false); 
	}
	
	/**
	 * Add a specified NPE to the provenance collection.
	 * @param npe the NPE to add
	 * @param force if true, the NPE is guaranteed to be added.  If false, the NPE will be added only if it doesn't already exist.
	 * @return true if the NPE was added, false otherwise.
	 */
	public boolean addNonProvenanceEdge(NonProvenanceEdge npe, boolean force) { 
		if(npe == null) return false;
		String key = getHashKey(npe);
		
		if(force) { 
			npes.put(key, npe);
			return true;
		} else if(!npes.containsKey(key)) { 
			npes.put(key, npe);
			return true;
		}
		
		return false;
	} // End addNonProvenanceEdge
	
	/** Remove an actor */
	public PLUSActor removeActor(PLUSActor a) { return actors.remove(a); } 
	
	/** Equivalent to addActor(a, false)  */
	public boolean addActor(PLUSActor a) { return addActor(a, false); } 
	
	/** Equivalent to addNode(obj, false); **/
	public boolean addNode(PLUSObject obj) { return addNode(obj, false); } 
	
	/**
	 * Add a particular actor to the graph
	 * @param a the actor to add
	 * @param force if true, add this actor even if it is already present, overwriting what was previously there.
	 * If false, ignore the actor if it is already present.
	 * @return true if it was added, false otherwise.
	 */
	public boolean addActor(PLUSActor a, boolean force) {
		if(a == null) return false;
		if(actors.containsKey(a.getId()) && !force) return false;
		actors.put(a.getId(), a);
		return true;
	}
	
	/**
	 * Add a particular node to the graph
	 * @param obj the node to add.
	 * @param force if true, add this node to the graph even if it is already present, overwriting what was previously there.
	 * If false, ignore the node if it is already present.
	 * @return true if it was added, false otherwise.
	 */
	public boolean addNode(PLUSObject obj, boolean force) { 
		if(obj == null) return false;		
		if(nodes.containsKey(obj.getId()) && !force) return false;
		
		// log.debug("***addNode: Adding object for " +obj.getName() + " id " + obj.getId() + ": " + obj);
		nodes.put(obj.getId(), obj); 		
		return true; 
	} // End addNode
	
	/**
	 * Remove a node from the provenance collection.
	 * @param node the node to remove
	 * @param removeIncidentEdges if true, incident edges to that node will also be removed.  If false, they will remain.
	 * @return the node removed (if it was in the collection) or null if it did not exist in the collection.
	 */
	public PLUSObject removeNode(PLUSObject node, boolean removeIncidentEdges) {
		if(node == null) return null;
		if(!removeIncidentEdges) return removeNode(node.getId());
		else { 
			for(PLUSEdge e : getEdgesByNode(node.getId()))
				removeEdge(e);
			
			return removeNode(node.getId());
		}
	}
	
	/**
	 * Remove a node from the provenance collection.  Does not remove incident edges.
	 * @param node the node to remove
	 * @return the node removed (if it was in the collection) or null if the node did not exist in the collection.
	 * @see ProvenanceCollection#removeNode(PLUSObject, boolean)
	 */
	public PLUSObject removeNode(PLUSObject node) { return removeNode(node, false); }
	
	/**
	 * Remove a node from the collection identified by an OID.   Does not remove the edges incident to that node.
	 * @param oid the ID of the node to remove
	 * @return the removed node, or null if it was not in the collection.
	 * @see ProvenanceCollection#removeNode(PLUSObject, boolean)
	 */
	public PLUSObject removeNode(String oid) {
		return nodes.remove(oid);		 
	} // End removeNode

	public PLUSObject removeNode(String oid, boolean removeIncidentEdges) { 
		return removeNode(getNode(oid), removeIncidentEdges);
	}
	
	public String toString() { 
		return ("ProvenanceCollection with " + countNodes() + " nodes " + countEdges() + " edges " + 
				metadata.size() + " metadata " + countActors() + " actors " + countNPEs() + " NPEs");
	} // End toString()
	
	/** Equivalent to addAll(other, false); */
	public int addAll(ProvenanceCollection other) { return addAll(other, false); } 
	
	public ProvenanceCollection clone() { 		
		ProvenanceCollection pc;
		try {
			pc = (ProvenanceCollection)super.clone();
		} catch (CloneNotSupportedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}
		
		Metadata md = new Metadata();
		for(String k : getMetadata().keySet()) md.put(k, getMetadata().get(k));
		pc.setMetadata(md);
		pc.setId(getId());
		
		for(PLUSActor actor : getActors()) pc.addActor(actor);
		for(PLUSObject o : getNodes()) pc.addNode(o); 
		for(PLUSEdge e : getEdges()) pc.addEdge(e); 
		for(NonProvenanceEdge npe : getNonProvenanceEdges()) pc.addNonProvenanceEdge(npe); 
		
		for(String k : nodeTags.keySet()) {
			pc.nodeTags.put(k, (HashMap<String,String>)nodeTags.get(k).clone());
		}
		
		return pc;
	} // End makeCopy
	
	/** 
	 * Take all items from another provenance collection and add them to this one.
	 * @param other the other provenance collection
	 * @param force if true, things in "other" will override whatever may be present.
	 * If false, only items that don't already exist will be copied.
	 */
	public int addAll(ProvenanceCollection other, boolean force) {
		int itemsAdded = 0; 
		Map<String,PLUSObject> on = other.nodes;
		Map<String,PLUSEdge> oe = other.edges;
		Map<String,NonProvenanceEdge> npes = other.npes;
		
		for(String k : on.keySet()) {
			if(addNode(on.get(k), force)) itemsAdded++;  
		}
		
		for(String k : oe.keySet()) {
			if(addEdge(oe.get(k), force)) itemsAdded++; 
		}
		
		for(String k : npes.keySet()) { 
			if(addNonProvenanceEdge(npes.get(k), force)) itemsAdded++;
		}
		
		for(String k : other.getMetadata().keySet()) {
			if(force || !metadata.contains(k))
				metadata.put(k, other.getMetadata().get(k));			
		} // End for
		
		return itemsAdded;
	} // End addAll
	
	public void tagNode(PLUSObject node, String tag, String value) { tagNode(node.getId(), tag, value); } 
	
	/**
	 * Associate a particular string tag with a node.
	 * @param oid the node you want to tag
	 * @param tag the tag to associated with it.
	 */
	public void tagNode(String oid, String tag, String value) { 
		HashMap<String,String> tags = nodeTags.get(oid);
		if(tags == null) {
			tags = new HashMap<String,String>();
			nodeTags.put(oid, tags);
		}
		
		if(!tags.containsKey(tag)) tags.put(tag, value);
	} // End tagNode
	
	public Set<String> getTaggedNodes() { 
		return nodeTags.keySet();
	}
	
	/**
	 * Return the tags assocated with a node.  Always guaranteed to return an ArrayList (not null) so if no tags
	 * exist, an empty ArrayList will return.
	 * @param oid the ID of the PLUSObject.
	 * @return an ArrayList of String tags.
	 */
	public HashMap<String,String> getTags(String oid) {
		HashMap<String,String> tags = nodeTags.get(oid);
		if(tags == null) tags = new HashMap<String,String>(); 
		return tags; 
	} // End getTags
		
	public boolean hasTag(String oid, String tag) { 
		return hasTag(oid, tag, false); 
	}
	
	public boolean removeTag(String oid, String tag) { 
		HashMap<String,String> tags = getTags(oid);		
		return tags.remove(tag) != null;
	} // End removeTag
	
	public List<PLUSEdge>getEdgesInList() { 
		ArrayList<PLUSEdge>list = new ArrayList<PLUSEdge>();
		for(PLUSEdge e : getEdges()) list.add(e);
		return list;
	}
	
	/** Returns nodes in an ordered list, sorted by creation time. */
	public List<PLUSObject> getNodesInOrderedList() {
		return getNodesInOrderedList(SORT_BY_CREATION);
	}
	
	/**
	 * Return nodes in an ordered list, ordered by a given comparator.
	 * @param ordering if null, items will be sorted by creation time.
	 * @return a list of nodes in this provenance collection, sorted with the given comparator.
	 */
	public List<PLUSObject>getNodesInOrderedList(Comparator <PLUSObject> ordering) { 
		ArrayList<PLUSObject>list = new ArrayList<PLUSObject>(getNodes());
		if(ordering == null) ordering = SORT_BY_CREATION;		
		Collections.sort(list, ordering);
		return list;
	} // End getNodesInOrderedList
	
	/**
	 * @param oid the object that was tagged
	 * @param tag the tag you're checking for
	 * @param contains if true, this method will look for substring tags.  If false, it will look for exact matches.
	 * @return true if that object has that tag, false otherwise.
	 */
	public boolean hasTag(String oid, String tag, boolean contains) { 
		HashMap<String,String> tags = getTags(oid);		
			
		for(String s : tags.keySet()) { 
			if(contains) { 
				if(s.toLowerCase().contains(tag.toLowerCase())) return true; 
			} else { 
				if(s.equalsIgnoreCase(tag)) return true;
			} // End else
		} // End for
		
		return false; 
	} // End hasTag
	
	public boolean isEmpty() { 
		return (countNodes() <= 0) && (countEdges() <= 0);
	}	
	
	public List<String> graphLint() {
		ArrayList<String> issues = new ArrayList<String>();
		
		for(PLUSEdge e : getEdges()) {
			if(!contains(e.getFrom())) issues.add("Edge " + e + " names " + e.getFrom() + " which isn't in the collection.");
			if(!contains(e.getTo())) issues.add("Edge " + e + " names " + e.getTo() + " which isn't in the collection.");			
		}
		
		return issues;
	} // End graphLint
	
	/**
	 * Create a new collection from a list of objects.
	 * @param objs
	 * @return
	 */
	public static ProvenanceCollection collect(PLUSObject ... objs) { 
		ProvenanceCollection c = new ProvenanceCollection();
		for(PLUSObject o : objs) c.addNode(o);
		return c;
	}
	
	/**
	 * Create a new collection from a list of edges. 
	 * @param edges
	 * @return
	 */
	public static ProvenanceCollection collect(PLUSEdge ... edges) { 
		ProvenanceCollection c = new ProvenanceCollection();
		for(PLUSEdge e : edges) c.addEdge(e);
		return c;
	}
	
	/**
	 * Create a new collection from a list of actors.
	 * @param actors
	 * @return
	 */
	public static ProvenanceCollection collect(PLUSActor ... actors) { 
		ProvenanceCollection c = new ProvenanceCollection();
		for(PLUSActor a : actors) c.addActor(a);
		return c;
	}
	
	/**
	 * Create a new collection from a list of NPEs.
	 * @param npes
	 * @return
	 */
	public static ProvenanceCollection collect(NonProvenanceEdge ... npes) { 
		ProvenanceCollection c = new ProvenanceCollection();
		for(NonProvenanceEdge npe : npes) c.addNonProvenanceEdge(npe);
		return c;
	}
} // End ProvenanceCollection
