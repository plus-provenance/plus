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
package org.mitre.provenance.npe;

import java.util.Calendar;
import java.util.Date;

import java.util.logging.Logger;
import org.mitre.provenance.EdgeMarking;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.plusobject.AbstractDirectedEdge;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.tools.PLUSUtils;

/**
 * A Non-Provenance Edge (NPE) is an edge that expresses a relationship between a PLUSObject, and a non-PLUSObject data structure
 * that is stored outside of a provenance store.  For example, to express the fact that a PLUSObject is equivalent to, or
 * the same as some other item of data in another store, a non-provenance edge between the PLUSObject and the other data item
 * might be created.
 * 
 * <p>Non-Provenance edges always have the constraint that the "from" portion of the edge must be a provenance object, that is,
 * it must have a valid PLUS OID.
 * 
 * <p>NPEs may connect two provenance objects.  If an NPE connects a PLUSObject to something else, it is essentially only a single atomic
 * value.  That is, if the "to" end of the NPE does not refer to a PLUSObject, there is no further way to elaborate that data.  This is
 * an intentional constraint placed on NPEs; they are envisioned to be used as a way to connect to external sources of data by reference,
 * but it isn't within the scope of PLUS to store arbitrary graph information.  As a result, there is no way to connect a PLUSObject to 
 * another node with an arbitrary set of properties or metadata.
 * 
 * <p>Common use cases for NPEs will be when you want to associate external identifiers for items (like a URI/URL, or a unique ID in a 
 * different database outside of the scope of PLUS)
 * 
 * <p>Here are a few examples of valid NPEs, let's start by assuming that "A" refers to a PLUSDataObject node, and "B" refers to another 
 * "PLUSDataObject" node.  <ul>
 *   <li>A -- (similar to) --&gt; B</li>
 *   <li>A -- (URI) --&gt; http://www.google.com/</li>
 *   <li>A -- (reviewed by) --&gt; "James Smith"</li>
 *   <li>A -- (comment) --&gt; "I think this looks goofy"</li> 
 * </ul>
 * 
 * @author dmallen
 */
public class NonProvenanceEdge extends AbstractDirectedEdge {
	/** The unique ID of the NPE itself */
	protected String oid;
	
	/** The time when the NPE was created, in milliseconds since the epoch */
	protected long created;
	
	/** The ID of the head of the edge */
	protected String from;
	
	/** The ID of the tail of the edge */
	protected String to;
	
	/** The type of the edge */
	protected String type;
	
	/** Default NPE edge type to use when you are using an NPE to indicate hashed content. */
	public static final String NPE_TYPE_CONTENT_HASH = "content hash";
	
	/** Default NPE edge type to indicate when you are using an NPE to indicate a URI for a resource */
	public static final String NPE_TYPE_URI = "URI";
	
	/** Default NPE edge type indicating that the edge expresses an "alias" relationship. */
	public static final String NPE_TYPE_ALIAS = "alias";
	
	/** Default NPE edge type indicating that the edge expresses a "containment" relationship */
	public static final String NPE_TYPE_CONTAINMENT = "contains";
	
	/** Default NPE edge type indicating a set of sequenced steps */
	public static final String NPE_TYPE_SEQUENCE_STEP = "sequence step";
		
	protected static Logger log = Logger.getLogger(NonProvenanceEdge.class.getName());
	
	/**
	 * Create a new NPE
	 * @param from the ID of the starting object.  This must be a PLUSObject ID.
	 * @param to the destination of the edge (can be any string, see examples)
	 * @param type the type of the edge (can be anything, but see provided example edge types)
	 * @throws PLUSException
	 */
	public NonProvenanceEdge(String from, String to, String type) throws PLUSException {
		// this is for a NEW NPE.
		this(PLUSUtils.generateID(), from, to, type, System.currentTimeMillis());
	} // End NonProvenanceEdge

	/**
	 * Create a new NPE between two PLUSObjects.
	 * @param from the head of the edge
	 * @param to the tail of the edge
	 * @param type the type of the edge (can be anything, but see provided example edge types)
	 * @throws PLUSException
	 */
	public NonProvenanceEdge(PLUSObject from, PLUSObject to, String type) throws PLUSException { 
		this(PLUSUtils.generateID(), from.getId(), to.getId(), type, System.currentTimeMillis()); 
	}
	
	/**
	 * Create a new NPE 
	 * @param from the head of the edge
	 * @param npid a non-provenance identifier (can be anything, but see provided examples for how to use this)
	 * @param type the type of the edge (can be anything, but see provided example edge types)
	 * @throws PLUSException
	 */
	public NonProvenanceEdge(PLUSObject from, String npid, String type) throws PLUSException { 
		this(PLUSUtils.generateID(), from.getId(), npid, type, System.currentTimeMillis());
	}
	
	/**
	 * Raw constructor.  No touchy!   You probably shouldn't be using this. 
	 * @param oid
	 * @param from
	 * @param to
	 * @param type
	 * @param created
	 * @throws PLUSException
	 */
	public NonProvenanceEdge(String oid, String from, String to, String type, long created) throws PLUSException {
		// this is for any NPE, including those populated from the database.
		if(!PLUSUtils.isPLUSOID(from) && !PLUSUtils.isPLUSOID(to)) throw new PLUSException("Either from or to must be a PLUS OID.");
		setCreated(created);
		setId(oid);
		setFrom(from);
		setTo(to);
		setType(type);
		setFromMarking(EdgeMarking.SHOW);
		setToMarking(EdgeMarking.SHOW);
	}
	
	/** Return the incident foreign ID (that is, the ID on the edge that isn't a PLUSObject ID */
	public String getIncidentForeignID() { if(PLUSUtils.isPLUSOID(from)) return to; else return from; }
	
	/** Return the incident object ID (that is, the ID on the edge that is a PLUSObject ID */
	public String getIncidentOID() { if(PLUSUtils.isPLUSOID(from)) return from; else return to; } 
	
	/** Return the head identifier of the edge */
	public String getFrom() { return from; } 
	
	/** Set the head identifier of the edge */
	protected void setFrom(String from) { this.from = from; }
	
	/** Get the tail identifier of the edge */
	public String getTo() { return to; } 
	
	/** Set the tail identifier of the edge */
	protected void setTo(String to) { this.to = to; }
	
	/** Return the type of the edge */
	public String getType() { return type; }
	
	/** Set the type of the edge */
	public void setType(String type) { this.type = type; }
	
	/** Get the edge's ID.  Note that this is *not* an incident node ID, it is the edge itself's ID */
	public String getId() { return oid; } 
	
	/** Set the edge's ID. */
	protected void setId(String oid) { this.oid = oid; }
	
	/** Get the edge's create time (ms since the epoch) */
	public long getCreated() { return created; }
	
	/** Return the edge's creation time as a java Date. */
	public Date getCreatedAsDate() { return new java.util.Date(created); }
	
	/** Set the edge's created time */
	protected void setCreated(long creationTime) { created = creationTime; } 
	
	/**
	 * Sets the object's created timestamp to this moment in milliseconds since the epoch, UTC
	 */
	public void setCreated() { 
		Calendar calInitial = Calendar.getInstance();  
        int offsetInitial = calInitial.get(Calendar.ZONE_OFFSET)  
                + calInitial.get(Calendar.DST_OFFSET);  
  
        long current = System.currentTimeMillis();  
          
        // Check right time  
        created = current - offsetInitial;
	}
	
	public String toString() { 
		return "NPE: from=" + getFrom() + " to=" + getTo() + " type=" + getType(); 
	}
} // End NonProvenanceEdge
