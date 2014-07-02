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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.mitre.provenance.EdgeMarking;
import org.mitre.provenance.Metadata;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.npe.NonProvenanceEdge;
import org.mitre.provenance.plusobject.FocusedCollection;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.plusobject.marking.Taint;
import org.mitre.provenance.surrogate.SignPost;
import org.mitre.provenance.surrogate.SurrogateDetail;
import org.mitre.provenance.tools.PLUSUtils;
import org.mitre.provenance.user.User;

/**
 * This object represents a limited lineage DAG view composed of PLUSObjects and PLUSEdges.  
 * Actual DAGs could grow to be very large, so we'll always be functionally limited by a starting node, 
 * and a specified maximum number of nodes.
 * 
 * <p>Important: DAGs are not tied to workflows per se, they are only bound by BLING and FLING.  So provenance links that
 * go outside of workflows will be honored and added to the DAG.  (I.e. you create a workflow A->B.  Later someone tacks
 * on B->C that wasn't part of that workflow.  A, B, and C are all in the same DAG) 
 * 
 * <p>This class contains most of the implementation of the surrogate function described in the "Surrogate Parenthood"
 * paper.  To generate new LineageDAGs from raw provenance, using the surrogate algorithm, see the fromCollection() method.
 * @author DMALLEN
 */
public class LineageDAG extends ViewedCollection implements FocusedCollection {
	private static final Logger log = Logger.getLogger(LineageDAG.class.getName());
			
	/** This tag will be associated with a node OID in the graph, when it is known that
	 * the database contains more information than the graph.
	 * @see LineageDAG#getTags(String)
	 */
	public static final String TAG_MORE_AVAILABLE = "more";
	
	/** An object that stores a statistical summary of various aspects of the graph. */
	protected FingerPrint fingerPrint; 
	
	/** Nodes in DAG that are a source of taint, i.e. those directly tainted. **/
	protected Map <PLUSObject,List<Taint>> taintSources = new HashMap<PLUSObject,List<Taint>> ();      			
			
	/** The starting node of the DAG */
	protected PLUSObject focus; 
	
	/**
	 * Create a new lineage dag for a given viewer.
	 * @param viewer the user who is viewing the DAG
	 */
	public LineageDAG(User viewer) {
		super(viewer); 		 
		
		metadata = new Metadata(); 
		fingerPrint = new FingerPrint();
		fingerPrint.setDagId(getId()); 
		fingerPrint.setCreated(); 
		empty(); 
	}
	
	public LineageDAG clone() { 
		LineageDAG dag = (LineageDAG)super.clone();
		dag.taintSources = this.taintSources;
		dag.fingerPrint = this.fingerPrint;
		dag.focus = this.focus;
		
		return dag;
	} // End clone
	
	/** Get the fingerprint associated with this DAG */
	public FingerPrint getFingerPrint() { return fingerPrint; } 
	
	/**
	 * Get the metadata associated with this DAG.  At this point, it has stats on the DAG and the amount of time
	 * spent building it, useful for experiment papers.
	 * @return a Metadata object.
	 */
	public Metadata getMetadata() { 		
		Metadata base = super.getMetadata();
		
		Metadata fp = fingerPrint.asMetadata();
		
		for(String k : fp.keySet())
			base.put(k,  ""+fp.get(k));
		
		return base;
	} // End getMetadata	
	
	/**
	 * Returns strings corresponding to the OIDs of the objects that taint the object provided.
	 * @param obj 
	 * @return an array of OIDs
	 */
	public String [] getTaintSources(PLUSObject obj) { 
		if(!isTainted(obj)) return new String [] {};		
		String val = getTags(obj.getId()).get(TAINT_FLAG);
		log.fine("getTaintSources raw string '" + val + "'");
		String [] toks = val.split(",");
		return toks;
	}
	
	/**
	 * Returns true if obj is known to be tainted under this graph, false otherwise.
	 * Warning: because graph objects are not necessarily complete, this is not a guarantee of taint/no-taint, it is only
	 * a statement of whether any taint is discoverable within this graph.
	 * @param obj 
	 * @return true if tainted, false otherwise.
	 */
	public boolean isTainted(PLUSObject obj) { 
		return hasTag(obj.getId(), TAINT_FLAG); 
	}
	
	/**
	 * Empty the contents of the DAG.
	 */
	protected void empty() { 
		super.empty(); 				
		taintSources = new HashMap<PLUSObject,List<Taint>> ();      
	} // End empty()

	public DAGPath getPath(String fromOID, String toOID) throws PLUSException { 
		PLUSObject from = getNode(fromOID);
		PLUSObject to = getNode(toOID);
		
		if(from == null) throw new PLUSException("No such object " + fromOID + " in DAG");
		if(to == null) throw new PLUSException("No such object " + toOID + " in DAG");
		
		return new DAGPath(this, from, to); 
	}
			
	/**
	 * @return a ProvenanceCollection containing this object's graph feet, that is, the objects that have no outbound
	 * provenance edges under this DAG.  This is not a guarantee that there is no further provenance available, only that
	 * there is no further provenance available in this object instance.
	 */
	public ProvenanceCollection getGraphFeet() { 
		ViewedCollection c = new ViewedCollection(getViewer()); 
		
		for(PLUSObject o : getNodes()) { 
			if(isFoot(o.getId())) c.addNode(o);
		}
			
		return c;		
	} // End getGraphFeet
	
	/**
	 * @return a ProvenanceCollection containing this object's graph heads, that is, the objects that have no inbound
	 * provenance edges under this DAG.  This is not a guarantee that there is no earlier provenance available, only that
	 * there is no earlier provenance available in this object instance.
	 */
	public ProvenanceCollection getGraphHeads() { 
		ViewedCollection c = new ViewedCollection(getViewer()); 
		
		for(PLUSObject o : getNodes()) { 
			if(isHead(o.getId())) c.addNode(o);
		}
		
		return c;
	} // End getGraphHeads
	
	/**
	 * @param oid
	 * @return true if the object is in the graph and is a graph head; false if the object is not a head (or isn't in the graph)
	 */
	protected boolean isHead(String oid) {  return containsObjectID(oid) && hasTag(oid, TAG_HEAD); }

	/**
	 * @param oid
	 * @return true if the object is in the graph and is a graph foot; false if the object is not a foot (or isn't in the graph)
	 */
	protected boolean isFoot(String oid) {  return containsObjectID(oid) && hasTag(oid, TAG_FOOT);  }
		
	/**
	 * @see ProvenanceCollection#removeNode(PLUSObject)
	 */
	public PLUSObject removeNode(PLUSObject node) {
		PLUSObject o = super.removeNode(node);
		if(o != null) fingerPrint.nodeRemoved(node); 
		return o;
	}
	
	/**
	 * @see ProvenanceCollection#removeNode(String)
	 */
	public PLUSObject removeNode(String oid) {
		if(oid == null) {
			log.severe("Cannot remove null from the graph");
			return null;
		}
		
		if(focus != null && focus.getId().equals(oid))
			log.warning("LineageDAG#removeNode is removing the root!"); 
		
		PLUSObject o = super.removeNode(oid);		
		if(o != null) fingerPrint.nodeRemoved(o); 		
		return o;
	} // End removeNode
	
	/**
	 * This method does the same thing as the super-class method, but keeps the graph
	 * fingerprint up to date.
	 * @see ProvenanceCollection#addAll(ProvenanceCollection, boolean)
	 */
	public int addAll(ProvenanceCollection col, boolean force) { 
		int i = 0; 
		
		for(PLUSObject o : col.getNodes()) { 
			if(super.addNode(o, force)) {
				fingerPrint.nodeAdded(o);
				i++;
			}
		}
		
		for(PLUSEdge e : col.getEdges()) { 
			if(super.addEdge(e, force)) {
				fingerPrint.edgeAdded(e); 
				i++;				
			}
		}
		
		for(NonProvenanceEdge npe : col.getNonProvenanceEdges()) {
			if(super.addNonProvenanceEdge(npe, force)) {
				i++;
			}
		}
		
		for(PLUSActor a : col.getActors()) {
			if(super.addActor(a, force)) {
				i++;
			}
		}
		
		return i;		
	} // End addAll

	/**
	 * This method does the same thing as the super-class method, but keeps the graph
	 * fingerprint up to date.
	 * @see ProvenanceCollection#addAll(ProvenanceCollection)
	 */
	public int addAll(ProvenanceCollection col) {
		return addAll(col, false); 
	}
	
	/**
	 * Add a particular node to the graph
	 * @param obj the node to add.
	 * @return true if it was added, false if it was already present in the DAG.
	 */
	public boolean addNode(PLUSObject obj) {
		boolean s = super.addNode(obj);
		
		if(s) fingerPrint.nodeAdded(obj);
		return s;
	} // End addNode
	
	/**
	 * Modify the focus of the DAG 
	 */
	public void setFocus(PLUSObject focus) {
		this.focus = focus;
		fingerPrint.setStartId(focus.getId());  
	} 
	
	/**
	 * Return true if the node in question has outbound or inbound edges under this graph, false otherwise.
	 * @param node
	 * @return
	 */
	protected boolean nodeIsConnected(PLUSObject node) { 
		if(!contains(node)) return false;
		String id = node.getId();

		return (getOutboundEdgesByNode(id).size() > 0) || (getInboundEdgesByNode(id).size() > 0);
	} // End nodeIsConnected
	
	/**
	 * Add an edge to the DAG.
	 * @param edge the edge to add.
	 * @param force if true, this will be added overwriting anything similar already there.  If false, the edge
	 * will be added only if its candidate nodes aren't already connected.
	 * @return true if the edge was added, false otherwise.
	 */
	public boolean addEdge(PLUSEdge edge, boolean force) {
		// boolean previouslyHad = contains(edge);
		boolean s = super.addEdge(edge, force); 
		if(s) fingerPrint.edgeAdded(edge); 
		return s;
	} // End addEdge	
	
	/**
	 * Return a list of siblings of a particular node under this graph.  Siblings are nodes that have a common parent.
	 * Note that if the node you are asking about has more than one parent node, it may have siblings from multiple 
	 * different parents.
	 * @param obj the object in question.
	 * @return an empty list if the node is not in the graph, otherwise a list of siblings.
	 */
	public List<PLUSObject> getSiblings(PLUSObject obj) { 
		HashMap<String,PLUSObject> results = new HashMap<String,PLUSObject>();
		
		if(!contains(obj)) return new ArrayList<PLUSObject>();
		
		List<PLUSObject> parents = getBLING(obj);
		
		for(PLUSObject p : parents) { 
			List<PLUSObject>siblings = getFLING(p);
			for(PLUSObject s : siblings)
				results.put(s.getId(), s); 
		}
				
		return new ArrayList<PLUSObject>(results.values());
	} // End getSiblings
	
	/**
	 * @param obj the object of interest
	 * @return all objects that are one step away from the given object in this DAG, via either FLING or BLING.  In other words, this treats edges as undirected.
	 */
	public List <PLUSObject> getNeighbors(PLUSObject obj) { 
		List <PLUSObject> b = getBLING(obj);
		b.addAll(getFLING(obj));
		return b;
	} // End getNeighbors
	
	/**
	 * @param obj the object of interest
	 * @return all objects that are one step of BLING away from the given object in this DAG.
	 */
	public List <PLUSObject> getBLING(PLUSObject obj) { 
		return getLineageOfNode(obj, "bling");
	}
	
	/**
	 * @param obj the object of interest
	 * @return all objects that are one step of FLING away from the given object in this DAG.
	 */
	public List <PLUSObject> getFLING(PLUSObject obj) { 
		return getLineageOfNode(obj, "fling");
	}
	
 	/**
	 * Get the set of objects in a particular lineage direction.
	 * @param obj the starting point
	 * @param direction either "bling" or "fling" only.
	 * @return the set of objects in the "bling" or "fling" of the given object in this DAG.
	 */
	private List <PLUSObject> getLineageOfNode(PLUSObject obj, String direction) { 
		List <PLUSObject> objs = new ArrayList <PLUSObject> ();
		List <PLUSEdge> es;
		
		if("bling".equals(direction)) es = getInboundEdgesByNode(obj.getId());
		else es = getOutboundEdgesByNode(obj.getId());
		
		for(int x=0; x<es.size(); x++) { 
			PLUSObject o = null;
			if("bling".equals(direction)) o = es.get(x).getFrom();
			else o = es.get(x).getTo();
			
			// Some edges may be dangling so don't assume the other end of the edge is in the DAG.
			if(o != null) objs.add(o);
		}
		
		return objs;
	} // End getLineage
	
	/**
	 * @deprecated
	 */
    public List <PLUSObject> getFullFlingForExperiments(PLUSObject obj) { 
    	List <PLUSObject> objs = new ArrayList <PLUSObject> ();
    	List <PLUSEdge> es = getOutboundEdgesByNode(obj.getId());

    	for(int x=0; x<es.size(); x++) { 
    		PLUSObject o = es.get(x).getTo();

    		// Some edges may be dangling so don't assume the other end of the edge is in the DAG.
    		if(o != null){
    			objs.add(o);
    			List <PLUSObject> dec = getFullFlingForExperiments( o );
    			ListIterator <PLUSObject> di = dec.listIterator();
    			while (di.hasNext()) {
    				PLUSObject p = (PLUSObject) di.next();
    				if ( !objs.contains(p) ){ 
    					objs.add(p);
    				}
    			} 
    		}
    	}

    	return objs;
    } // End getLineage
	
    /**
     * @deprecated
     */
    public List <PLUSObject> getLineageOfMyNode(PLUSObject obj, String direction) {
    	return getLineageOfNode(obj, direction);
    }
		
	/**
	 * Remove an edge from the DAG.
	 * @param edge the edge to remove.
	 */
	public void removeEdge(PLUSEdge edge) { 		
		fingerPrint.edgeRemoved(edge); 
		super.removeEdge(edge);
	} // End removeEdge
		
	public void traverse(LineageDAGTraverseFn function, String direction, PLUSObject startingPoint) throws PLUSException { 
		if(!contains(startingPoint)) throw new PLUSException("DAG doesn't contain that object!"); 
		ArrayList<String>queue = new ArrayList<String>();		
		queue.add(startingPoint.getId()); 
		traverse(function, queue, direction); 
	} // End traverse
	
	/**
	 * Traverse the entire DAG, beginning with the heads.  Apply the provided function upon visiting each node.
	 * @param function the action to take upon visiting each node.
	 * @throws PLUSException
	 */
	public void traverse(LineageDAGTraverseFn function) throws PLUSException { 
		ArrayList<String>queue = new ArrayList<String>();

		for(PLUSObject obj : getGraphHeads().getNodes()) {  
			queue.add(obj.getId());
		}
		
		assert(queue.size() != 0); 
		
		// Entire graph can be traversed by starting at the heads, and going FLING.
		traverse(function, queue, "fling"); 
	} // End traverse
		
	private void traverse(LineageDAGTraverseFn function, ArrayList<String>queue, String direction) throws PLUSException { 
		HashSet<String>seen = new HashSet<String>();		
			
		if(queue.size() <= 0 && countNodes() > 0) throw new PLUSException("No nodes to traverse!"); 
		if(!"fling".equals(direction) && !"bling".equals(direction)) throw new PLUSException("Direction may only be 'fling' or 'bling'!");
		
		while(!queue.isEmpty()) {
			String oid = queue.remove(0);
			if(seen.contains(oid)) continue; 
			
			PLUSObject obj = getNode(oid);
			function.visitNode(this, obj);
			
			// Because we started with graph heads (and they're already in the queue)
			// we only need to examine the FLING (not the BLING).
			List<PLUSObject>next = null;
			
			if("fling".equals(direction)) next = getFLING(obj);
			if("bling".equals(direction)) next = getBLING(obj); 
			
			for(PLUSObject o : next) { if(!seen.contains(o.getId())) queue.add(o.getId()); } 			
			seen.add(oid);
		} // End while
	} // End traverse
	
	/**
	 * Given a provenance collection, and a starting point ID that it was loaded from, determine
	 * which object in the collection should be the focus.  Only PLUSObjects can be the focus, but
	 * this can be tricky because the starting point might have been an NPE-ID.
	 * @param col the collection
	 * @param startingPointID the starting point used to load the collection
	 * @return the suggested object to use as a focus.  (This object may be null).
	 */
	public static PLUSObject chooseFocus(ProvenanceCollection col, String startingPointID) { 
		if(col.countNodes() <= 0) return null;  // Can't be any focus.
		
		// If the starting point is actually in the graph, that's the focus.
		if(PLUSUtils.isPLUSOID(startingPointID) && col.containsObjectID(startingPointID))
			return col.getNode(startingPointID);

		if(!PLUSUtils.isPLUSOID(startingPointID)) { 
			ProvenanceCollection incidentToNPE = new ProvenanceCollection();
			for(NonProvenanceEdge npe : col.getNonProvenanceEdges()) { 
				if(npe.getFrom().equals(startingPointID) && PLUSUtils.isPLUSOID(npe.getTo()) && col.containsObjectID(npe.getTo())) {
					incidentToNPE.addNode(col.getNode(npe.getTo()));
				} else if(npe.getTo().equals(startingPointID) && PLUSUtils.isPLUSOID(npe.getFrom()) && col.containsObjectID(npe.getFrom())) {
					incidentToNPE.addNode(col.getNode(npe.getFrom()));
				}							
			} // End for
				
			if(incidentToNPE.countNodes() > 0) { 
				// Get the most recent object incident to that NPE.
				try { 
					return incidentToNPE.getNodesInOrderedList(ProvenanceCollection.SORT_BY_CREATION).get(0);
				} catch(Exception exc) { 
					log.severe("Failed to get first temporal object incident to NPE: " + exc.getMessage()); 
				}
			} else { 
				log.warning("THIS MAY BE A BUG.  Starting point ID '" + startingPointID + "' isn't in this graph!");
			}
		} // End if
					
		// No other trick worked, so we basically have to choose some focus somewhere.
		PLUSObject artificialFocus = (PLUSObject)(col.getNodes().toArray()[0]);		
		log.warning("Failed to discover node for starting point " + startingPointID + 
				 " assigning instead: " + artificialFocus.getName()); 
		return artificialFocus;		
	} // End chooseFocus
	
	/**
	 * Look through the edge list in a LineageDAG and tag nodes as a "head" or "foot" depending
	 * on whether or not there is anything further upstream/downstream.
	 * @param col the LineageDAG to tag
	 * @return the same DAG, with its markings updated.
	 */
	public static LineageDAG tagHeadAndFeet(LineageDAG col) { 
		col.getFingerPrint().startTimer("TagHeadAndFeet");
		
		for(PLUSObject fo : col.getNodes()) {
			List<PLUSEdge> fling = col.getOutboundEdgesByNode(fo.getId());
			List<PLUSEdge> bling = col.getInboundEdgesByNode(fo.getId());
			
			if(fling.size() <= 0) col.tagNode(fo.getId(), LineageDAG.TAG_FOOT, "true");
			else { 
				boolean downstreamConnection = false;
				for(PLUSEdge e : fling) { if(col.contains(e.getTo())) { downstreamConnection = true; break; } }
				if(!downstreamConnection) col.tagNode(fo.getId(), LineageDAG.TAG_FOOT, "true"); 
			}
			
			if(bling.size() <= 0) col.tagNode(fo.getId(), LineageDAG.TAG_HEAD, "true"); 
			else { 
				boolean upstreamConnection = false;
				for(PLUSEdge e : fling) { if(col.contains(e.getFrom())) { upstreamConnection = true; break; } }
				if(!upstreamConnection) col.tagNode(fo.getId(), LineageDAG.TAG_HEAD, "true"); 				
			}
		} // End for
		col.getFingerPrint().stopTimer("TagHeadAndFeet");
		return col;
	} // End tagHeadAndFeet
	
	/**
	 * Given a LineageDAG, detect edges that are in the collection which "dangle" or point to 
	 * nodes that the collection doesn't contain.
	 * @param col
	 * @return a list of dangling edges.
	 */
	public static List<PLUSEdge>detectDanglers(LineageDAG col) { 
		ArrayList<PLUSEdge>edges = new ArrayList<PLUSEdge>();
		for(PLUSEdge e : col.getEdges()) { 
			if(!col.contains(e.getFrom()) || !col.contains(e.getTo()))
				edges.add(e);
		}
		
		return edges;
	} // End detectDanglers
	
	/**
	 * Iterates through all of the nodes in the LineageDAG and tags the head and feet for quick retrieval later. 
	 * @param col
	 * @return the same collection passed.
	 */
	protected static LineageDAG tagGraphHeadsAndFeet(LineageDAG col) {
		col.getFingerPrint().startTimer("HeadsAndFeet");
		for(PLUSObject o : col.getNodes()) {
			if(col.getOutboundEdgesByNode(o.getId()).size()<=0) 
				col.tagNode(o.getId(), ProvenanceCollection.TAG_FOOT, ProvenanceCollection.TAG_VALUE_TRUE);
			else if(col.getInboundEdgesByNode(o.getId()).size()<=0)
				col.tagNode(o.getId(), ProvenanceCollection.TAG_HEAD, ProvenanceCollection.TAG_VALUE_TRUE);			
		}		
		col.getFingerPrint().stopTimer("HeadsAndFeet");
		
		return col;
	} // End tagGraphHeadsAndFeet
	
	/**
	 * Part of the surrogate algorithm calls for "edge voting", which is the process by which each node incident to an
	 * edge gets to vote whether the edge is shown, hidden, or inferred.  This function implements that voting, and replaces
	 * relevant edges in the DAG with "MarkedEdge" objects corresponding to the result of the voting.
	 * <b>This function modifies its argument and returns the same object.</b>
	 */
	public static LineageDAG computeEdgeVoting(LineageDAG dag) {		
		dag.getFingerPrint().startTimer("EdgeVoting"); 
		Iterator <PLUSEdge> edgeIt = dag.getEdges().iterator();
		
		int votesShow = 0; 
		int votesHide = 0; 
		int votesInfer = 0; 
				
		while(edgeIt.hasNext()) { 			
			PLUSEdge pedge = (PLUSEdge)edgeIt.next();
			PLUSObject fromObj = pedge.getFrom();
			PLUSObject toObj   = pedge.getTo();
			
			EdgeMarking fromVote = null;
			EdgeMarking toVote = null;
			
			//log.info("pruneByEdgeVoting: " + fromObj + " , " + toObj);
			//log.info("pruneEdgeVoting: " + pedge.getFrom() + " -> " + pedge.getTo());
						
			// This happens when the from or the to node was never added to the graph because
			// there was no suitable surrogate.  (User not authorized to see any version) 
			if(fromObj == null || toObj == null) continue; 			
			
			if(fromObj.isSurrogate()) {
				SurrogateDetail fromSurrogate = fromObj.getSurrogateDetail();				
				fromVote = fromSurrogate.getMarking(pedge, toObj);
				// System.out.println("FROMVOTE: " + fromVote); 
			}
			
			if(toObj.isSurrogate()) {
				SurrogateDetail toSurrogate = toObj.getSurrogateDetail();
				toVote = toSurrogate.getMarking(pedge, fromObj);
				// System.out.println("TOVOTE:  " + toVote); 
			}
			
			if(fromVote == null) fromVote = EdgeMarking.SHOW; 
			if(toVote == null) toVote = EdgeMarking.SHOW; 
						
			if(EdgeMarking.SHOW.equals(fromVote)) votesShow++;
			else if(EdgeMarking.HIDE.equals(fromVote)) votesHide++;
			else votesInfer++; 
			
			if(EdgeMarking.SHOW.equals(toVote)) votesShow++;
			else if(EdgeMarking.HIDE.equals(toVote)) votesHide++;
			else votesInfer++; 
			
			// log.info("Setting fromVote " + fromVote + " toVote " + toVote);
			pedge.setFromMarking(fromVote);
			pedge.setToMarking(toVote);
			
			log.fine("computeEdgeVoting: " + 
					 pedge.getFrom().getName() + " => " + 
					 pedge.getTo().getName() + " " + 
					 pedge.getFromMarking() + ", " + pedge.getToMarking() + "=" +
					 pedge.getVerdict()); 
			
			// Add it back in its new form.  True means force it in.
			dag.addEdge(pedge, true);  
						
			if(!pedge.getVerdict().equals(EdgeMarking.SHOW)) {
				// Mark these nodes as connected.  There is an edge that connects them to something
				// else.
				dag.tagNode(fromObj.getId(), "connected", "true");
				dag.tagNode(toObj.getId(), "connected", "true");
			} // End if
		} // End while		
						
		dag.getMetadata().put("Votes-Show", ""+votesShow);
		dag.getMetadata().put("Votes-Hide", ""+votesHide); 
		dag.getMetadata().put("Votes-Infer", ""+votesInfer); 
		dag.getFingerPrint().stopTimer("EdgeVoting");
		
		return dag;
	} // End computeEdgeVoting
	
	/**
	 * When a DAG is first built, the code comes across some list of nodes that are directly tainted.
	 * This function traces forwards in the graph, and marks everything downstream of any directly tainted node
	 * as also being tainted (indirectly).  
	 * TODO: Taint propagation "blockers".  Shouldn't some nodes have some signal that prevents further 
	 * propagation?  (I.e. the tainted input is too remote, or not important).  This method needs to get smarter.
	 */
	public static LineageDAG traceTaintSources(LineageDAG dag) {
		HashMap<String,ArrayList<Taint>> taintSources = Neo4JPLUSObjectFactory.getTaintSources(dag);
		
		Set <String> e = taintSources.keySet();
		dag.getFingerPrint().startTimer("TraceTaintSources");
		
		// Run through each object in the tainted sources, and mark everything forwards in the graph as tainted.
		// Note that some nodes will be tainted by multiple sources, and may get multiple markings.
		for(String taintedOID : e) {  
			dag.tagNode(taintedOID, "tainted-by-" + taintedOID, taintedOID); 
			
			// Check to see if the original source of taint is itself marked as tainted.
			// It should be.
			if(!dag.hasTag(taintedOID, ProvenanceCollection.TAINT_FLAG)) 
				dag.tagNode(taintedOID, ProvenanceCollection.TAINT_FLAG, taintedOID); 
			
			// Build a queue and a visitation list.
			List <PLUSObject> queue = new ArrayList <PLUSObject> (); 
			queue = dag.getFLING(dag.getNode(taintedOID));
			Hashtable <PLUSObject, Boolean> visited = new Hashtable <PLUSObject, Boolean> (); 
			while(queue.size() > 0) { 								
				PLUSObject next = queue.remove(0);
				if(visited.containsKey(next)) continue;  // Skip it if we've already seen it (avoids cycles) 
				
				// Tag that it's tainted, and by which ID.
				dag.tagNode(next.getId(), "tainted-by-" + taintedOID, taintedOID);
												
				HashMap<String,String> tflags = dag.getTags(next.getId()); 
				if(tflags == null || tflags.isEmpty()) {
					dag.tagNode(next.getId(), ProvenanceCollection.TAINT_FLAG, taintedOID); 
				} else if(!(""+tflags.get(ProvenanceCollection.TAINT_FLAG)).contains(taintedOID)) {
					String val = tflags.get(ProvenanceCollection.TAINT_FLAG);
					val = (val == null ? "" : ",") + taintedOID;
					dag.tagNode(next.getId(), ProvenanceCollection.TAINT_FLAG, val); 
				}
								
				List <PLUSObject> fling = dag.getFLING(next);
				for(int z=0; z<fling.size(); z++) { queue.add(fling.get(z)); } 
			} // End while			
		} // End while
		
		dag.getFingerPrint().stopTimer("TraceTaintSources");
		return dag;
	} // End traceTaintSources
	
	/**
	 * Given a LineageDAG that contains MarkedEdges, this function tries to draw new lines to cover up the ones
	 * that were inferred.  It further DELETES all edges from the graph marked inferred.
	 * @param dag the LineageDAG to use -- this argument will be modified and returned.
	 * @throws PLUSException
	 */
	public static LineageDAG drawInferrableEdges(LineageDAG dag) throws PLUSException { 
		Iterator <PLUSEdge> edgeIt = dag.getEdges().iterator(); 
		// log.fine("Starting to look for inferrable edges in DAG..."); 
		ArrayList <PLUSEdge> hitList = new ArrayList <PLUSEdge> ();   // List of things we'll prune later.
		ArrayList <PLUSEdge> toAdd = new ArrayList <PLUSEdge> ();     // List of newly-generated edges.
		
		// Keep track of a list of IDs that have removed edges.  After the surrogate algorithm
		// runs, we'll want to remove any orphaned nodes that got disconnected by the algorithm.
		HashSet<String> checkForOrphanedNodes = new HashSet<String>();
		
		dag.getFingerPrint().startTimer("NewEdgeComputing");		
		int inferredEdges = 0; 
				
		// There's a bunch of variables in here that are just performance profiling junk.
		long visSetsTotal = 0; 
		long visSetsTimes = 0; 
		long visSetMax = -1000000; 
		long visSetMin = 100000000;

		// General algorithm:
		// For each edge in the graph marked "infer", do:
		// (1) Get upstream visible nodes
		// (2) Get downstream visible nodes.
		// (3) Draw edges from all of (1) to all of (2) 
		while(edgeIt.hasNext()) { 
			PLUSEdge pedge = null;
			
			try { pedge = (PLUSEdge)edgeIt.next(); }
			catch(ClassCastException exc) { 
				throw new PLUSException("This DAG does not contain marked edges!  Did you use computeEdgeVoting() first?");
			} // End catch
			
			// The verdict is the overall marking.  So if one side of the edge votes "show" and another side votes
			// "hide", the verdict is "hide" because that domainates show.
			EdgeMarking mark = pedge.getVerdict(); 
			
			if(!EdgeMarking.INFER.equals(mark)) {
				// If the edge is hidden, add it to the list to be removed.
				if(EdgeMarking.HIDE.equals(mark)) {
					checkForOrphanedNodes.add(pedge.getFrom().getId());
					checkForOrphanedNodes.add(pedge.getTo().getId());
					hitList.add(pedge); 
				}
				
				// Otherwise if it's not an infer edge, just skip it.  This method
				// is for drawing new edges that are hidden by infers, so "show" links
				// don't matter here.
				continue;
			}
		
			// OK so now we have to figure out which side of the edge wants this
			// inferred.
			EdgeMarking fMark = pedge.getFromMarking();
			EdgeMarking tMark = pedge.getToMarking(); 
			
			inferredEdges++; 
			//log.info("drawInferrableEdges: " + 
			//		 pedge.getFrom().getName() + " => " + 
			//		 pedge.getTo().getName() + " is inferred.");
			
			// All inferred edges need to get pruned later, so this always needs to 
			// happen with inferred links.
			hitList.add(pedge);  
			
			String from = pedge.getFrom().getId();
			String to = pedge.getTo().getId();
			
			checkForOrphanedNodes.add(from);
			checkForOrphanedNodes.add(to);
			
			String blingSetID = null;
			String flingSetID = null;
			
			// How you find the visible sets depends on how the edge is marked.
			// Remember the visible set is the set of the nearest visible nodes 
			// upstream and downstream in the graph.
			// At this point in the code, the edge could be marked:
			// (infer, show), (show, infer), or (infer, infer).
			// What we use as the basis for building the bling and fling visible sets depends on which case this is.
			if(fMark.equals(EdgeMarking.SHOW) && tMark.equals(EdgeMarking.INFER)) {
				// Get visible BLING and FLING only from to's perspective
				blingSetID = to;
				flingSetID = to; 
			} else if(fMark.equals(EdgeMarking.INFER) && tMark.equals(EdgeMarking.SHOW)) { 
				// Get visible BLING and FLING only from from's perspective.
				blingSetID = from; 
				flingSetID = from; 
			} else { 
				// If both marked it infer, then get BLING from downstream's perspective, and FLING 
				// from upstream's perspective.
				blingSetID = from;
				flingSetID = to; 
			} // End else
			
			long s = System.currentTimeMillis();
			ArrayList <PLUSObject> blingSet = buildVisibleSet(dag, dag.getNode(blingSetID), "bling");
			long e = System.currentTimeMillis();
			
			// Timing/performance junk.
			long i = e-s; visSetsTotal += i; visSetsTimes++;
			if(i > visSetMax) visSetMax = i; 
			if(i < visSetMin) visSetMin = i; 
			
			s = System.currentTimeMillis();			
			ArrayList <PLUSObject> flingSet = buildVisibleSet(dag, dag.getNode(flingSetID), "fling");
			e = System.currentTimeMillis();
			
			i = e-s;
			visSetsTotal += i; visSetsTimes++; 
			if(i > visSetMax) visSetMax=i; 
			if(i < visSetMin) visSetMin=i;  
			
			// Debugging...
			// log.info("Bling visible set for " + dag.getNode(to).getName() + ": " + blingSet.size());
			// for(PLUSObject o : blingSet) log.fine(o.getName());
			// log.info("Fling visible set for " + dag.getNode(from).getName() + ": " + flingSet.size());
			// for(PLUSObject o : flingSet) log.fine(o.getName());			
			
			// Now connect blingset * flingset with edges.
			for(int x=0; x<blingSet.size(); x++) {
				PLUSObject b = blingSet.get(x);
				for(int y=0; y<flingSet.size(); y++) { 					
					PLUSObject f = flingSet.get(y);
					// log.info("Drawing computable edge " + b.getName() + " => " + f.getName());
					PLUSEdge inferrable = new PLUSEdge(b, f, pedge.getWorkflow(), PLUSEdge.EDGE_TYPE_UNSPECIFIED);
					inferrable.setSourceHints(new SignPost("Surrogate Algorithm"));

					// Inferrable edges always are marked show.
					inferrable.setFromMarking(EdgeMarking.SHOW);
					inferrable.setToMarking(EdgeMarking.SHOW);
					toAdd.add(inferrable);
					
					// This shouldn't happen.  But we want to know about it if it does.
					if(b.getId().equals(f.getId())) log.info("LOOP EDGE!  " + b.getName()); 
				} // End for
			} // End for			
		} // End while
		
		dag.getFingerPrint().stopTimer("NewEdgeComputing");		
			
        // More performance logging junk.
		float visSetAvg = ((float)visSetsTotal/(float)visSetsTimes);
		
		if(visSetsTimes == 0) {
			visSetMin = 0;
			visSetMax = 0; 
			visSetAvg = 0; 
		} // End if
		
		dag.getMetadata().put("VisibleSets",  ""+visSetsTimes); 
		dag.getMetadata().put("VisibleSetAvg", ""+visSetAvg);
		dag.getMetadata().put("VisibleSetMax", ""+visSetMax);
		dag.getMetadata().put("VisibleSetMin", ""+visSetMin);
		dag.getMetadata().put("preMarkEdges", ""+dag.countEdges());
		dag.getMetadata().put("inferredEdges", ""+inferredEdges);
		
		dag.getFingerPrint().startTimer("AddComputedEdges"); 
		// Have to add these outside the loop to avoid concurrent modifications.
		for(int x=0; x<toAdd.size(); x++) {
			log.fine("Adding computed edge " + 
					 toAdd.get(x).getFrom().getName() + " -> " + 
					 toAdd.get(x).getTo().getName());
			
			dag.addEdge(toAdd.get(x), true); 
		} // End for
		dag.getFingerPrint().stopTimer("AddComputedEdges");
		
		dag.getFingerPrint().startTimer("PruneInferredEdges"); 
		for(int x=0; x<hitList.size(); x++) {
			log.fine("LineageDAG: Removing inferred edge " + 
					 hitList.get(x).getFrom().getName() + " -> " + 
					 hitList.get(x).getTo().getName());
			dag.removeEdge(hitList.get(x)); 
		} // End for
		dag.getFingerPrint().stopTimer("PruneInferredEdges"); 
		
		dag.getFingerPrint().startTimer("PruneOrphanedNodes");
		for(String id : checkForOrphanedNodes) {
			if(dag.getEdgesByNode(id).size() == 0) {
				// This node has no inbound or outbound edges.  It got 
				// orphaned by the algorithm, and is now disconnected and should
				// get pruned.
				log.fine("Removing orphaned  node " + dag.getNode(id)); 
				try { dag.removeNode(id); } catch(Exception exc) {
					exc.printStackTrace();
					log.severe("Exception removing orphaned node: " + exc.getMessage());
				}
			}
		}
		dag.getFingerPrint().stopTimer("PruneOrphanedNodes");
		
		dag.getMetadata().put("postMarkEdges", ""+dag.countEdges()); 
		return dag;
	} // End drawInferrableEdges	
	
	/**
	 * Given a particular node, find the "visible set" in a particular direction (bling or fling).  The visible set
	 * is the list of nodes upstream or downstream whose outbound or inbound links are visible.
	 * @param dag the source DAG where the object exists
	 * @param source the object starting point
	 * @param operation "bling" or "fling"
	 * @return a list of objects that are related via that operation, that have no inferred links further upstream.
	 * @throws PLUSException
	 */
	protected static ArrayList <PLUSObject> buildVisibleSet(LineageDAG dag, PLUSObject source, String operation) throws PLUSException {
		if(!"bling".equals(operation) && !"fling".equals(operation))
			throw new PLUSException("Illegal operation"); 
		dag.getFingerPrint().startTimer("VisibleSet"); 
		ArrayList <PLUSObject> visibleSet = new ArrayList <PLUSObject> (); 
		List<PLUSEdge> oedges = null;
		
		// log.info("VISIBLE SET starting with " + source + " DIRECTION " + operation);
		
		if(operation.equals("fling")) oedges = dag.getOutboundEdgesByNode(source.getId());
		else oedges = dag.getInboundEdgesByNode(source.getId());
		
		for(PLUSEdge e : oedges) {			
			PLUSObject nextNode = null;
			PLUSEdge me = dag.getEdge(e.getFrom(), e.getTo());
			ArrayList <PLUSObject> appendList = null;
			
			// Get the next upstream or downstream node.
			if(me == null) { 
				log.warning("****** Marked Edge from " + e.getFrom().getName() + " => " + 
						e.getTo().getName() + " was null!"); 
				continue; 
			}
			
			if(me.getVerdict().equals(EdgeMarking.HIDE)) {
				// log.info("buildVisibleSet: Skipping HIDE link");
				continue; 
			}
			
			if("bling".equals(operation)) nextNode = me.getFrom();
			else nextNode = me.getTo();
			
			if(nextNode == source) 
				throw new PLUSException("Horrors!  " + source.getName() + " " + operation + " is topsy-turvy!"); 					
			
			// The marking that's relevant depends on whether it's upstream or downstream.
			// If we're going BLING-direction, then we want to know if nextNode's outgoing marking is visible.
			// If we're going FLING-direction, then we want to know if nextNode's incoming marking is visible.
			EdgeMarking relevant = null;
			if("bling".equals(operation)) relevant = me.getFromMarking();
			else relevant = me.getToMarking(); 
		
			// Remember that null also counts as visible.  If the node didn't vote...
			if(relevant == null) relevant = EdgeMarking.SHOW; 
			
			if(relevant.equals(EdgeMarking.SHOW))
				visibleSet.add(nextNode);
			else if(relevant.equals(EdgeMarking.INFER))
				appendList = buildVisibleSet(dag, nextNode, operation);				
			
			if(appendList != null) {
				for(PLUSObject p : appendList) visibleSet.add(p); 
			} // End if
		} // End for
		
		dag.getFingerPrint().stopTimer("VisibleSet");
		return visibleSet; 
	} // End buildVisibleSet
	
	/**
	 * Create a LineageDAG from a collection.   This is subject to the surrogate algorithm.
	 * @param col the collection of objects (some of which may not be vieweable by viewer)
	 * @param viewer the viewer for the final LineageDAG
	 * @return a LineageDAG consisting of an account of col viewable by viewer
	 * @throws PLUSException 
	 */
	public static LineageDAG fromCollection(ProvenanceCollection col, User viewer) throws PLUSException {
		LineageDAG d = new LineageDAG(viewer);
		d.addAll(col);
		
		d = LineageDAG.computeEdgeVoting(d);   // Edge voting for surrogates
		d = LineageDAG.traceTaintSources(d);   // Trace indirect taints from direct taints
		d = LineageDAG.drawInferrableEdges(d); // Draw inferred edges based on surrogate alg.
		d = LineageDAG.tagHeadAndFeet(d);
		
		return d;
	} // End fromCollection

	public PLUSObject getFocus() { return focus; }  
} // End LineageDAG
