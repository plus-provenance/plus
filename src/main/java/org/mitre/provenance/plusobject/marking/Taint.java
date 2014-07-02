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
package org.mitre.provenance.plusobject.marking;

import java.util.Date;
import java.util.Map;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.PropertySet;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.user.User;

/**
 * Taints are special kinds of PLUSObjects that are asserted about other PLUSObjects.  They show
 * up in the lineage DAG as proper elements, but they are related to other nodes by non-lineage
 * edges (i.e. PLUSEdge.EDGE_TYPE_MARKING)
 * @author DMALLEN
 */
public class Taint extends HeritableMarking {
	public static final String MARK_TYPE = "taint";		
	public static final String PLUS_SUBTYPE_TAINT = MARK_TYPE;
	protected String description; 
	
	private static final String PROP_CLAIMANT = "claimant";
	private static final String PROP_DESCRIPTION = "description";
	private static final String PROP_WHEN_ASSERTED = "when_asserted";
	
	public Taint() { 
		super();
		setName("Taint");
		setObjectSubtype(MARK_TYPE);
		setDescription("");
		setWhenAsserted(new Date());
	}
	
	public Taint(User claimant, String description) {
		this();
		setClaimant(claimant);
		setDescription(description); 
	} // End Taint
		
	public void setDescription(String desc) { this.description = desc; } 
	public String getDescription() { return description; } 
		
	public Map<String,Object> getStorableProperties() {
		Map<String,Object> m = super.getStorableProperties();
		m.put(PROP_CLAIMANT, claimant.getName());
		m.put(PROP_DESCRIPTION, getDescription());
		m.put(PROP_WHEN_ASSERTED, getWhenAsserted().getTime());
		return m;
	}
	
	public PLUSObject setProperties(PropertySet props) throws PLUSException { 
		super.setProperties(props);
		setDescription(""+props.getProperty(PROP_DESCRIPTION));
				
		PLUSActor act = Neo4JPLUSObjectFactory.getActor(""+props.getProperty(PROP_CLAIMANT));
		User u = null;
				
		if(act == null) {
			log.severe("No such actor by claimant name " + claimant + " ...faking it.");
			u = new User(""+props.getProperty(PROP_CLAIMANT));
		} else if(!(act instanceof User)) {
			log.severe("Actor " + act + " isn't a user!");
			u = new User(""+props.getProperty(PROP_CLAIMANT));
		} else { 
			u = (User)act;
		}
		
		setClaimant(u);
		setWhenAsserted(new Date((Long)props.getProperty(PROP_WHEN_ASSERTED)));
		return this;
	} // End setProperties
} // End Taint
