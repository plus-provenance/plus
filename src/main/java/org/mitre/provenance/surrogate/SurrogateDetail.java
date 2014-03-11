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
package org.mitre.provenance.surrogate;

import java.util.Hashtable;

import org.mitre.provenance.EdgeMarking;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSObject;

/**
 * Wrapper class for a surrogate, with a little extra magic.
 * Surrogates in PLUS consist of the following:  (1) An optional SignPost, indicating where the user can get 
 * more information.  (2) An (optionally populated) surrogate quality data structure, containing extra 
 * hints (such as that the surrogate incomplete).  (3) a set of "edge markings".
 * <p>Edge markings have to do with how an individual surrogate interacts with the broader lineage DAG.  By using
 * edge markings, surrogates can indicate that edges to and from them should not be shown, or should be inferred.
 * Surrogates can either specify each edge individually on a case-by-case basis, or they can specify an "edge policy",
 * which has the effect of always hiding/showing/inferring edges.
 * @see SurrogateGeneratingFunction#generateSurrogate(PLUSObject, org.mitre.provenance.user.User)
 * @author DMALLEN
 */
public class SurrogateDetail {
	protected SurrogateQuality quality;
	protected SignPost signpost;	
	protected int edgePolicy; 
	protected EdgeVoter voter = null; 
	
	/** Use this policy to vote on edges on a case-by-case basis.
	 * @see SurrogateDetail#addEdgeMarking(String, EdgeMarking)
	 * @see SurrogateDetail#setEdgePolicy(int)
	 */
	public static final int EDGE_POLICY_INDIVIDUAL = 0;
	
	/** Use this policy to mark all edges as hide.
	 * @see SurrogateDetail#setEdgePolicy(int)
	 */
	public static final int EDGE_POLICY_HIDE_ALL = 1;
	/** Use this policy to mark all edges as show.
	 * @see SurrogateDetail#setEdgePolicy(int)
	 */
	public static final int EDGE_POLICY_SHOW_ALL = 2;
	/** Use this policy to mark all edges as infer.
	 * @see SurrogateDetail#setEdgePolicy(int)
	 */
	public static final int EDGE_POLICY_INFER_ALL = 3;
	
	/**
	 * Use this policy to mark incoming edges infer, and outgoing edges visible.
	 * @see SurrogateDetail#setEdgePolicy(int)
	 */
	public static final int EDGE_POLICY_IN_INFER_OUT_VISIBLE = 4; 
	
	/**
	 * Use this policy to mark incoming edges visible, and outgoing edges infer.
	 * @see SurrogateDetail#setEdgePolicy(int) 
	 */
	public static final int EDGE_POLICY_IN_VISIBLE_OUT_INFER = 5; 
	
	/**
	 * Use this policy to mark edges through the use of a voter.
	 * @see SurrogateDetail#setEdgePolicy(int)
	 */
	public static final int EDGE_POLICY_USE_VOTER = 6; 
	
	/** Stores a mapping of OIDs to edge markings.  Let's say A wants to hide its edge to B.  Then 
	 * A should add an edge marking with B's OID.
	 */
	protected Hashtable <String,EdgeMarking> edgeMarkings; 
	
	/**
	 * Create a SurrogateDetail with a default policy of individual edge voting.
	 * @param quality extra quality hints
	 * @param signpost pointers to more information.
	 */
	public SurrogateDetail(SurrogateQuality quality, SignPost signpost) { 		
		this.quality = quality;
		this.signpost = signpost;		
		
		setEdgePolicy(SurrogateDetail.EDGE_POLICY_INDIVIDUAL);
	} // End DataSurrogateDetail
	
	/**
	 * Set the edge voting policy for this surrogate.
	 * @param newPolicy the new policy to use.
	 * @see SurrogateDetail#EDGE_POLICY_HIDE_ALL
	 * @see SurrogateDetail#EDGE_POLICY_INDIVIDUAL
	 * @see SurrogateDetail#EDGE_POLICY_SHOW_ALL
	 */
	public void setEdgePolicy(int newPolicy) { 
		edgePolicy = newPolicy;
	}
	
	/**
	 * @return the edge voter for this object, or null if there is none.
	 */
	public EdgeVoter getEdgeVoter() { return voter; } 
	
	public void setEdgeVoter(EdgeVoter voter) {
		this.edgePolicy = SurrogateDetail.EDGE_POLICY_USE_VOTER;
		this.voter = voter; 
	}
	
	public SurrogateQuality getQuality() { return quality; } 
	public SignPost getSignPost() { return signpost; } 
	
	public String toString() { 
		return new String("[SURROGATE: SignPost " + signpost + " quality " + quality + "]");
	}
	
	/**
	 * Indicate that this item is a surrogate.  If you're using this object,
	 * then this is always a surrogate.  This method always returns true.
	 */
	public boolean isSurrogate() { return true; } 	
	
	/**
	 * Get the marking for a particular edge.  The input parameter specifies the object on the other end of
	 * the edge.  There is no guarantee whether the edge is outgoing or incoming.
	 * <p>This function may return null if the surrogate isn't sophisticated enough to vote on the edge, or 
	 * has no opinion.
	 * @param other the object at the other end of the edge that's being marked.
	 */
	public EdgeMarking getMarking(PLUSEdge edge, PLUSObject other) { 		
		if(edgePolicy == SurrogateDetail.EDGE_POLICY_HIDE_ALL) return EdgeMarking.HIDE;
		if(edgePolicy == SurrogateDetail.EDGE_POLICY_SHOW_ALL) return EdgeMarking.SHOW;
		if(edgePolicy == SurrogateDetail.EDGE_POLICY_INFER_ALL) return EdgeMarking.INFER; 
		if(edgePolicy == SurrogateDetail.EDGE_POLICY_USE_VOTER) return voter.getMarking(other);
		
		if(edgePolicy == SurrogateDetail.EDGE_POLICY_IN_INFER_OUT_VISIBLE) {
            // If other is the 'to' end, then I'm the 'from' end.
			if(edge.getTo().equals(other.getId())) return EdgeMarking.SHOW;
			else return EdgeMarking.INFER;				
		}
		
		if(edgePolicy == SurrogateDetail.EDGE_POLICY_IN_VISIBLE_OUT_INFER) {
			// If other is the 'to' end, then I'm the 'from' end.
			if(edge.getTo().equals(other.getId())) return EdgeMarking.INFER;
			else return EdgeMarking.SHOW;
		}
		
		if(edgeMarkings == null) edgeMarkings = new Hashtable<String,EdgeMarking> (); 
		
		// Last resort -- case by case edge marking. 
		return edgeMarkings.get(other.getId()); 
	} // End getMarking

	/**
	 * Add a case-by-case basis edge marking for a particular object.  Whenever an object at the other end of 
	 * an edge has an ID matching the input, the provided EdgeMarking will be used.
	 * <p>Note: this method has a side effect of always setting the edge policy to EDGE_POLICY_INDIVIDUAL
	 * @param oid the ID of the object on the other end of the edge you want to mark.
	 * @param marking the marking to use
	 * @see SurrogateDetail#EDGE_POLICY_INDIVIDUAL
	 */
	public void addEdgeMarking(String oid, EdgeMarking marking) {
		edgePolicy = SurrogateDetail.EDGE_POLICY_INDIVIDUAL;
		
		if(edgeMarkings == null) edgeMarkings = new Hashtable <String,EdgeMarking>(); 
		
		edgeMarkings.put(oid, marking);
	} // End addEdgeMarking	
} // End SurrogateDetail
