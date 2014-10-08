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

import org.mitre.provenance.EdgeMarking;
import org.mitre.provenance.surrogate.SignPost;

/**
 * This class models an edge in a provenance graph.  This class is ONLY for provenance edges
 * <p>The type of a PLUSEdge <b>must always be</b> one of the enumerated strings in this class.  For any other type of edge (bearing non-
 * provenance semantics) use NonProvenanceEdge
 * @see org.mitre.provenance.npe.NonProvenanceEdge
 * @author moxious
 */
public class PLUSEdge extends AbstractDirectedEdge {
	/** A marks edge indicates that one provenance object provides additional information about another.  As an example, Taint objects mark what they taint */
	public static final String EDGE_TYPE_MARKS = "marks";
	
	/** A triggered edge goes between two invocations, indicating that the from end of the edge triggered the "to" end of the edge. */
	public static final String EDGE_TYPE_TRIGGERED = "triggered";
	
	/** A generated edge goes between an invocation and a data item, indicating that the invocation generated the data item */
	public static final String EDGE_TYPE_GENERATED = "generated";
	
	/** Input to goes between a data item and an invocation, denoting that the data was input to the invocation */
	public static final String EDGE_TYPE_INPUT_TO  = "input to"; 
	
	/** Contributed is a mostly unspecified auxilary edge type that indicates one data item contributed to another (i.e. excerpting) */
	public static final String EDGE_TYPE_CONTRIBUTED = "contributed";
	
	/** Unspecified edges are meant for later revision; in general they should be avoided, but may be created in cases where it is known that a 
	 * relationship exists between two objects, but when the nature of the relationship isn't known  
	 */
	public static final String EDGE_TYPE_UNSPECIFIED = "unspecified";
	
	/** The source/head/origin of the edge */
	protected PLUSObject from;
	
	/** The tail/end/termination point of the edge */
	protected PLUSObject to;
	
	/** The type of edge */
	protected String type;
	
	/** The workflow that this edge is a part of */
	protected PLUSWorkflow workflow;	
			
	/**
	 * Create a new edge from one object to another, under the default workflow.  The edge type will be chosen automatically, depending on 
	 * the types of the incident objects.
	 * @param from
	 * @param to
	 */
	public PLUSEdge(PLUSObject from, PLUSObject to) {
		this(from, to, PLUSWorkflow.DEFAULT_WORKFLOW); 
	}
	
	/**
	 * Create a new PLUSEdge
	 * @param from the origin of the edge
	 * @param to the destination of the edge
	 * @param wf the workflow the edge belongs to.  If null, the default workflow will be used.
	 * @param type the type of the edge; should be one of the edge types defined in this class.
	 */
	public PLUSEdge(PLUSObject from, PLUSObject to, PLUSWorkflow wf, String type) {
		this(from, to, wf);
		setType(type);
	}
	
	/**
	 * Create a new edge from one object to another, under the workflow specified.  The edge type will be chosen automatically, depending on 
	 * the types of the incident objects.
	 * @param from
	 * @param to
	 */
	public PLUSEdge(PLUSObject from, PLUSObject to, PLUSWorkflow wf) {
		this.from = from;
		this.to = to;
		
		if(wf == null) {
			log.warning("Attempt to use null workflow in creating PLUSEdge");
			this.workflow = PLUSWorkflow.DEFAULT_WORKFLOW;
		} else this.workflow = wf;		
		
		setType(EDGE_TYPE_CONTRIBUTED);
		setFromMarking(EdgeMarking.SHOW);
		setToMarking(EdgeMarking.SHOW);
		setSourceHints(SignPost.SRC_HINTS_LOCAL);
		
		if(from.isInvocation() && to.isInvocation()) setType(EDGE_TYPE_TRIGGERED);
		if(from.isDataItem() && to.isInvocation()) setType(EDGE_TYPE_INPUT_TO);
		if(from.isInvocation() && to.isDataItem()) setType(EDGE_TYPE_GENERATED);
		if(from.isDataItem() && to.isDataItem()) setType(EDGE_TYPE_CONTRIBUTED);		 
	} // End PLUSEdge

	public PLUSEdge clone() {
		PLUSEdge copy = new PLUSEdge(getFrom(), getTo(), getWorkflow(), getType());
		copy.setFromMarking(getFromMarking());
		copy.setToMarking(getToMarking());
		copy.setSourceHints(getSourceHints());
		return copy;
	} // End clone
	
	/**
	 * @param type
	 * @return true if a given type is one of the enumerated provenance edge types; false otherwise.
	 */
	public static boolean isProvenanceEdgeType(String type) { 
		return EDGE_TYPE_TRIGGERED.equals(type) ||
				EDGE_TYPE_GENERATED.equals(type) ||
				EDGE_TYPE_INPUT_TO.equals(type)  ||
				EDGE_TYPE_CONTRIBUTED.equals(type) ||
				EDGE_TYPE_UNSPECIFIED.equals(type) ||
				EDGE_TYPE_MARKS.equals(type);
	}
	
	public int hashCode() { 
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((from == null) ? 0 : from.hashCode());
		result = prime * result
				+ ((to == null) ? 0 : to.hashCode());
		result = prime * result
				+ ((workflow == null) ? 0 : workflow.hashCode());
		result = prime * result
				+ ((type == null) ? 0 : type.hashCode());
			
		return result;
	}
	
	public boolean equals(Object other) {
		if(!(other instanceof PLUSEdge) || other == null) return false;
		
		return ((PLUSEdge)other).getFrom().equals(getFrom()) && 
		       ((PLUSEdge)other).getTo().equals(getTo());
	}
	
	public String toString() { return new String("<PLUSEdge (" + getType() + ") " + getFrom() + " -> " + getTo() + ">"); }
	
	public boolean isBLINGOf(PLUSObject obj) { 
		return getTo().equals(obj.getId()); 
	}
	
	public boolean isFLINGOf(PLUSObject obj) { 
		return getFrom().equals(obj.getId()); 
	}
	
	public PLUSObject getFrom() { return from; } 
	public PLUSObject getTo() { return to; } 
	public String getType() { return type; }
	public PLUSWorkflow getWorkflow() { return workflow; }  
	
	public void setWorkflow(PLUSWorkflow wf) { this.workflow = wf; } 
	public void setFrom(PLUSObject from) { this.from = from; }
	public void setTo(PLUSObject to) { this.to = to; } 
	
	public void setType(String type) {
		if(!isProvenanceEdgeType(type)) {
			log.severe("Edge " + this + ": attempt to set edge type to non-provenance edge type '" + type + "': using 'unspecified' instead.");
			this.type = EDGE_TYPE_UNSPECIFIED;
		} else {
			this.type = type;
		} // End else
	} // End setType

	public SignPost getSourceHints() { return this.srcHints; }
	
	public void setSourceHints(SignPost sp) { this.srcHints = sp; }
} // End PLUSEdge
