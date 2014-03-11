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

import java.util.Map;

import org.mitre.provenance.PLUSException;
import org.neo4j.graphdb.PropertyContainer;

/**
 * An abstract activity description.  Note that this is *not* an instance of the activity's execution.  For that,
 * you want PLUSInvocation.
 * @author DMALLEN
 */
public class PLUSActivity extends PLUSObject
{
	/** Constant used to define an unknown activity */
	public static final PLUSActivity UNKNOWN_ACTIVITY = new PLUSActivity(); 
	
	/** Stores the number of inputs associated with the PLUS activity */
	private int inputs = 0;
	
	/** Stores the number of outputs associated with the PLUS activity */
	private int outputs = 0;
	
	/** Stores the description associated with the PLUS activity */
	private String description = "Uninitialized";
	
	public static final String PLUS_SUBTYPE_REGISTRATION = "registration";
	
	static {
		UNKNOWN_ACTIVITY.setId("urn:uuid:plus:999990000000000000000000000000000000");
		UNKNOWN_ACTIVITY.setCreated(0); 
		UNKNOWN_ACTIVITY.setName("Unknown Activity");
	}
	
	/** Constructs a PLUS activity */
	public PLUSActivity()
		{ super(); setObjectType(PLUS_TYPE_ACTIVITY); setObjectSubtype(PLUS_SUBTYPE_REGISTRATION); }

	/** Constructs a named PLUS activity */
	public PLUSActivity(String name)
		{ this(); setName(name); }
	
	/** Constructs a copy of the PLUS activity */
	public void copy(PLUSActivity activity)
	{
		super.copy(activity); 
		inputs = activity.getInputs();
		outputs = activity.getOutputs();
		description = activity.getDescription();
		setObjectType(PLUS_TYPE_ACTIVITY);
		setObjectSubtype(PLUS_SUBTYPE_REGISTRATION); 
	}
	
	/** Creates a clone of this PLUS activity */
	public PLUSActivity clone()
		{ PLUSActivity activity = new PLUSActivity(); activity.copy(this); return activity; }

	// PLUS activity getters
	public int getInputs() { return inputs; } 
	public int getOutputs() { return outputs; }
	public String getDescription() { return description==null ? "No description provided" : description; } 

	/** Specify the number of inputs that this activity generally has.
	 * @param inputs the number of inputs any invocation of this activity type would expect.
	 */
	public void setInputs(int inputs) { this.inputs = inputs; } 

	/** Specify the number of outputs that this activity generally produces.
	 * @param outputs the number of outputs any invocation of this activity type would be expected to produce.
	 */	
	public void setOutputs(int outputs) { this.outputs = outputs; }
	public void setDescription(String description) { this.description = description; } 
	
	/** Displays the PLUS activity as a string */
	public String toString()
		{ return new String("[Activity: " + getName() + "]"); }
			
	public Map<String,Object> getStorableProperties() {
		Map<String,Object> m = super.getStorableProperties();
		m.put("inputs", ""+getInputs());
		m.put("outputs", ""+getOutputs());
		m.put("description", getDescription());
		return m;
	}
	
	public PLUSObject setProperties(PropertyContainer props) throws PLUSException { 
		super.setProperties(props);
		setInputs(Integer.parseInt((String)props.getProperty("inputs", "0")));
		setOutputs(Integer.parseInt((String)props.getProperty("outputs", "0")));
		setDescription((String)props.getProperty("description"));
		return this;
	}	
} // End PLUSActivity