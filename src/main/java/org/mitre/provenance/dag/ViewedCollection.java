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
package org.mitre.provenance.dag;

import java.util.ArrayList;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.user.User;

/**
 * A collection of provenance nodes and edges that has the context of all being viewable by a given user.  ViewedCollection objects also 
 * have a "focus" or a node that the user is most interested in, or caused the graph to be generated in the first place.  The focus acts
 * as an entry point into the graph.
 * @author DMALLEN
 */
public class ViewedCollection extends ProvenanceCollection {
	/** The user that's viewing the DAG */
	protected User viewer;
		
	/** The starting node of the DAG */
	protected PLUSObject focus; 

	public ViewedCollection(User viewer) {
		super();
		this.viewer = viewer;
	}
	
	/**
	 * Update the focus of the graph; this node should be thought of as the entry point to the graph, or the user's focus on 
	 * the graph.
	 * @param focus
	 */
	protected void setFocus(PLUSObject focus) { this.focus = focus; } 
	
	/**
	 * Change the viewer of this graph.  It is <b>highly</b> recommended that you call populate() after doing this, since
	 * your new graph viewer may not be permitted to see some of the items cached in the graph.
	 * <b>This function does not automatically repopulate the graph according to the new user's credentials!</b>
	 * @param user the user who views this DAG
	 * @see LineageDAG#populate()
	 */
	protected void setViewer(User user) { this.viewer = user; }
	
	/** Returns the "focus" of the DAG.
	 * The focus is the object used to create and populate the DAG from the beginning.  This
	 * usually corresponds to a  "node of interest" for a user.
	 * @return the focus of the DAG
	 */
	public PLUSObject getFocus() { 
		return focus; 
	}
	
	/**
	 * The user associated with this lineage DAG.  The DAG contains only items this viewer is permitted to see, 
	 * or suitable surrogates for those items.
	 * @return a User whose permissions are used to generate the DAG.
	 */
	public User getViewer() { return viewer; } 
	
	/**
	 * Determine whether or not this collection can contain a given object.  Because a ViewedCollection is
	 * tied to a user, the user must have permissions to see the object before this collection can contain it.
	 * @param obj
	 * @return true if the object can be contained as is, false otherwise.
	 */
	public boolean canContain(PLUSObject obj) { 
		try { if(getViewer() != null && !getViewer().canSee(obj)) return false; }
		catch(PLUSException exc) { 
			log.severe("Cannot determine user's rights on " + obj + ": " + exc.getMessage());
			return false;
		}
		
		return true; 
	} // End canContain
	
	/** @see ProvenanceCollection#addEdge(PLUSEdge) */
	public boolean addEdge(PLUSEdge edge) { return addEdge(edge, false); } 

	/**
	 * Note that if you're adding an edge to a protected version of an object that
	 * is in the graph, the edge will be re-written to point to the surrogate version of the object that is in the graph.
	 * This method sometimes manipulates the edge you give it, because it needs to ensure that you're not adding an edge
	 * into the collection that contains an endpoint the user is not supposed to see.  So for example, if you add an 
	 * edge going from A -> B, occasionally an edge will be added from A to B's surrogate.
	 * @see ProvenanceCollection#addEdge(PLUSEdge, boolean)
	 */
	public boolean addEdge(PLUSEdge edge, boolean force) {
		boolean fromNeedsUpdate = false;
		boolean toNeedsUpdate = false;
		
		if(containsObjectID(edge.getFrom().getId()) && !contains(edge.getFrom())) {
			// We have the OID, but the actual object isn't in the collection.  This means we have
			// a surrogate of a more protected version of the object, and we need to update the edge
			// so it doesn't point to something more sensitive than we should contain.
			fromNeedsUpdate = true;
		} else if(!containsObjectID(edge.getFrom().getId())) {
			// Edge references an object we don't even contain, so just add it.
			// TODO:  If the edge goes to something for which there is no suitable surrogate,
			// that's a problem.
			// log.info("Adding missing from object to collection.");
			if(!addNode(edge.getFrom(), false)) {
				log.warning("Adding edge requires adding missing FROM node, but it cannot be added to this collection: skipping edge add");
				return false;
			}
		}
		
		if(containsObjectID(edge.getTo().getId()) && !contains(edge.getTo())) {
			// Same deal as above, on other end of the edge.
			toNeedsUpdate = true;
		} else if(!containsObjectID(edge.getTo().getId())) {
			// TODO:  If the edge goes to something for which there is no suitable surrogate,
			// that's a problem.
			// log.info("Adding missing to object to collection.");
			if(!addNode(edge.getTo(), false)) {
				log.warning("Adding edge requires adding missing TO node, but it cannot be added to this collection.");
				return false;
			}
		}
		
		if(fromNeedsUpdate|| toNeedsUpdate) {
			PLUSEdge copy = edge.clone();
			
			// Replace the from side with the version we have for the object of the same ID
			if(fromNeedsUpdate) copy.setFrom(getNode(edge.getFrom().getId()));
			if(toNeedsUpdate) copy.setTo(getNode(edge.getTo().getId()));
			
			log.fine("Adding surrogate edge " + copy); 
			return super.addEdge(copy, force);
		} else 
			return super.addEdge(edge, force);
	} // End addEdge
	
	/** @see ProvenanceCollection#addNode(PLUSObject) */
	public boolean addNode(PLUSObject obj) { return addNode(obj, false); } 
	
	/**
	 * Add a node to the collection.  If the node is not visible to the user, an attempt will
	 * be made to calculate a suitable surrogate.  If a surrogate cannot be found, then the object
	 * will not be added.
	 * @param obj the object to add
	 * @param force if true, the object will be added even if it is already present.  If false, it will only
	 * be added if it is not already in the collection.
	 * @return true if the node was added, false otherwise.
	 */
	public boolean addNode(PLUSObject obj, boolean force) {		
		PLUSObject candidate = obj;
		
		if(!canContain(candidate)) {
			try {
				candidate = candidate.getVersionSuitableFor(viewer);
			} catch(PLUSException exc) {
				log.severe("Failed to find suitable version of " + obj + " for " + viewer + ":  " + exc.getMessage());
				exc.printStackTrace();
				candidate = null;
			}
		}
		
		if(candidate == null) {
			log.warning("Failed to add node " + obj + " because viewer can't see it!  Obj requires " + obj.getPrivileges() + " user presents " + viewer); 
			return false;
		} 
				
		boolean success = super.addNode(candidate, force);
		// log.info("Actually adding " + candidate + " from " + obj + " success?  " + success);
		
		if(success && (obj != candidate)) {
			// UGLY.   So here's a problem - if we're adding a surrogate, the PLUSEdges in this 
			// object still point at the old (un-surrogate) object.  So we have to weed out all of
			// the edges that refer to the old instance, and update them in this collection to point
			// to the new surrogate (candidate)
			// log.info("Node added successfully, but it was a surrogate, so consider " + getEdgesByNode(candidate.getId()) + " edges...");
			
			for(PLUSEdge edge : getEdgesByNode(candidate.getId())) {
				PLUSEdge modifiedCopy = edge.clone();
				if(edge.getFrom() == obj) {
					modifiedCopy.setFrom(candidate);
				} else if(edge.getTo() == obj) {
					modifiedCopy.setTo(candidate);
				} else {
					log.severe("Replacing edge on add surrogate: neither end of the edge has the sought object!");
					continue;
				}
				
				removeEdge(edge);
				addEdge(modifiedCopy);
				log.fine("After adding different surrogate node, replaced " + edge + " with " + modifiedCopy);
			}
		}
		
		return success;
	} // End addNode
		
	/**
	 * @see ProvenanceCollection#addAll(ProvenanceCollection, boolean)
	 */
	public int addAll(ProvenanceCollection other, boolean force) {		
		ProvenanceCollection copy = other.clone();
		ArrayList<String>toRemove = new ArrayList<String>();
		
		// Remove the ones that can't go in this graph.
		for(PLUSObject o : copy.getNodes()) {
			if(!canContain(o)) { 
				log.warning("Skipping node " + o + " which this graph cannot contain."); 
				toRemove.add(o.getId());
			}
		}
		
		for(String oid : toRemove) copy.removeNode(oid);
		return super.addAll(copy, force);
	}
	
	public ViewedCollection clone() { 
		ViewedCollection col = (ViewedCollection)super.clone();
		col.viewer = this.viewer;
		col.focus = this.focus;
		
		return col;
	} // End clone
	
	/** @see ProvenanceCollection#addAll(ProvenanceCollection) */
	public int addAll(ProvenanceCollection other) { return addAll(other, false); }
} // End ViewedCollection
