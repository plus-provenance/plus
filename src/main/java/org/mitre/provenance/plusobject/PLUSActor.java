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

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.PropertyCapable;
import org.mitre.provenance.PropertySet;
import org.mitre.provenance.tools.PLUSUtils;

/**
 * An individual or organization that can do stuff.  Part of the OPM model, and useful to modeling multiple
 * participants in a PLUS federation. 
 * @author DMALLEN
 */
public class PLUSActor implements PropertyCapable {
	/** Name of the actor */
	protected String name;
	/** Unique ID in the database; same format as OIDs (that is, 52-char UUIDs) */
	protected String aid; 
	/** Type of actor **/
	protected String type;
	
	/** Time created */
	protected long created;
	
	public PLUSActor() { 
		aid = PLUSUtils.generateID();
		created = System.currentTimeMillis();
		type = "actor";
	} // End PLUSActor()
	
	/**
	 * Create a new PLUSActor with a given name.
	 * @param name the name of the actor. 
	 */
	public PLUSActor(String name) { 
		this();
		setName(name); 		
	} // End PLUSActor
	
	/**
	 * Determine whether this actor owns a particular object or not.
	 * @param obj the object to check
	 * @return true if this actor owns that object, false otherwise. 
	 */
	public boolean owns(PLUSObject obj) {
		if(obj.getOwner() == null) return false;
		
		return getId().equals(obj.getOwner().getId()); 
	} // End owns
	
	public boolean equals(Object o) { 
		if(!(o instanceof PLUSActor)) return false;
		PLUSActor b = (PLUSActor)o;
		
		return getId().equals(b.getId()) &&
			   getCreated() == b.getCreated() && 
			   getName().equals(b.getName()) && 
			   getType().equals(b.getType());
	}
	
	public Date getCreatedAsDate() { return new java.util.Date(getCreated()); }
	public long getCreated() { return created; } 
	protected void setCreated(long created) { this.created = created; } 
	
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
	
	public String getType() { return type; } 
	protected void setType(String type) { this.type = type; } 
	
	public String getId() { return aid; } 
	public void setId(String aid) { this.aid = aid; } 
	public String getName() { return name; } 
	protected void setName(String name) { this.name = name; } 
	
	public String toString() { return getName() + " (actor)"; }
	
	public Map<String, Object> getStorableProperties() {
		HashMap<String,Object> m = new HashMap<String,Object>();
		m.put("aid", getId());
		m.put("name", getName());
		m.put("created", getCreated());
		m.put("type", getType());
		return m;
	}
	
	public Object setProperties(PropertySet props) throws PLUSException {
		setId(""+props.getProperty("aid"));
		setName(""+props.getProperty("name"));
		setCreated((Long)props.getProperty("created"));
		setType(""+props.getProperty("type")); 
		return this;
	} // End setProperties
} // End PLUSActor
