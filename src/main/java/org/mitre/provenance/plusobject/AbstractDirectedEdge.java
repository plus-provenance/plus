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

import java.util.logging.Logger;
import org.mitre.provenance.EdgeMarking;
import org.mitre.provenance.surrogate.SignPost;

/**
 * An abstract representation of a directed edge in a graph.
 * These edges may be marked, to provide support for the surrogate algorithm.  Each edge may have a from marking, and a to
 * marking, corresponding to the from and to sides of the edge.  By default, markings are always EdgeMarking.SHOW.
 * <p>Note that edge markings are not persisted in the database; typically they are computed for whomever is looking at
 * the edge.
 * <p>The originating portion of the edge is also called "from", while the terminating portion is called "to".  An edge that
 * goes A -&gt; B and is labeled "foo" would have from=A, to=B, type=foo.
 * @author dmallen
 */
public abstract class AbstractDirectedEdge implements SourcedObject {
	protected static Logger log = Logger.getLogger(AbstractDirectedEdge.class.getName());
	
	/** The marking placed on the edge by the origin node (from) */
	protected EdgeMarking fromMarking = EdgeMarking.SHOW; 
	/** The marking placed on the edge by the destination node (to) */
	protected EdgeMarking toMarking = EdgeMarking.SHOW;

	/** The SignPost indicating how this object was sourced; local by default. 
	 * @see SignPost#SRC_HINTS_LOCAL
	 */
	protected SignPost srcHints = SignPost.SRC_HINTS_LOCAL;
	
	/** 
	 * @see SourcedObject#getSourceHints()
	 */
	public SignPost getSourceHints() { return srcHints; }
	
	/**
	 * @see SourcedObject#setSourceHints(SignPost)
	 */
	public void setSourceHints(SignPost sourceHints) { srcHints = sourceHints; } 
	
	/**
	 * Get the object that originates this edge.
	 */
	public abstract Object getFrom();
	
	/**
	 * Get the object that terminates this edge.
	 */
	public abstract Object getTo();
	
	/**
	 * @param id a node identifier
	 * @return true if that identifier is either on the from end, or the to end.  False otherwise.
	 */
	public boolean isIncidentTo(String id) {
		return (id != null && 
				(getFrom().equals(id) || getTo().equals(id)));
	}
	
	/**
	 * @param one a node identifier
	 * @param two a node identifier
	 * @return true if the edge connects those two identifiers (in either direction), false otherwise.
	 */
	public boolean connects(String one, String two) { 
		return(one != null && two != null && !one.equals(two) && 
			   isIncidentTo(one) && isIncidentTo(two));
	}
	
	/**
	 * Get the type of this edge
	 * @return a string label indicating edge type.
	 */
	public abstract String getType();
	
	/**
	 * Set the type of this edge.
	 * @param type the new String label type.
	 */
	public abstract void setType(String type);	
	
	public boolean equals(Object obj) {
		if(!(obj instanceof AbstractDirectedEdge)) return false;
		
		AbstractDirectedEdge other = (AbstractDirectedEdge)obj;
		
		return getFrom().equals(other.getFrom()) &&
		       getTo().equals(other.getTo()) && 
		       fromMarking.equals(other.getFromMarking()) && 
		       toMarking.equals(other.getToMarking());  
	} // End equals
	
	/**
	 * Get the marking on the originating portion of the edge.
	 * @return a marking
	 */
	public EdgeMarking getFromMarking() { return fromMarking; }
	
	/**
	 * Get the marking on the terminating portion of the edge.
	 * @return a marking
	 */
	public EdgeMarking getToMarking() { return toMarking; } 
	
	public void setFromMarking(EdgeMarking marking) { fromMarking = marking; } 
	public void setToMarking(EdgeMarking marking) { toMarking = marking; } 
		
	/**
	 * Verdict is the overall determination on the edge.  So for example if either marking
	 * is "hide", this will return "hide".  If either is "infer" it will return "infer".  It
	 * will only return show if both nodes voted show, or if neither node voted.
	 * @return the verdict marking that should be used to decide whether to display an edge to users.
	 */
	public EdgeMarking getVerdict() { 
		if(fromMarking == EdgeMarking.SHOW && toMarking == EdgeMarking.SHOW) return EdgeMarking.SHOW; 
		
		if(fromMarking == null && toMarking == null) return EdgeMarking.SHOW;
		
		// String f = fromMarking == null ? "none" : fromMarking.toString();
		// String t = toMarking == null ? "none" : toMarking.toString(); 
		// String newLabel = new String(f + ", " + t);
		// setType(newLabel); 
		
		if(fromMarking== EdgeMarking.HIDE || toMarking == EdgeMarking.HIDE) return EdgeMarking.HIDE;
		if(fromMarking == EdgeMarking.INFER || toMarking == EdgeMarking.INFER) return EdgeMarking.INFER;
		
		log.fine("Atypical returning SHOW"); 
		return EdgeMarking.SHOW;		
	} // End getVerdict	
	
	public String toString() { return new String("<DirectedEdge (" + getType() + ") " + getFrom() + " -> " + getTo() + ">"); }
} // End AbstractDirectedEdge
