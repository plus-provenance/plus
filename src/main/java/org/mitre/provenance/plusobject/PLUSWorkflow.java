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
import org.mitre.provenance.PropertySet;

/**
 * An abstract workflow.   This is usually used to group a set of provenance objects together into a cohesive set of operations.
 * @author DMALLEN
 */
public class PLUSWorkflow extends PLUSObject {	
	/**
	 * When the database is created, a default workflow is added to it that's usuable for all ad-hoc lineage with
	 * no reported workflow.  This is the ID of that workflow.
	 */
	public static final PLUSWorkflow DEFAULT_WORKFLOW = new PLUSWorkflow();
	public static final String PLUS_SUBTYPE_EXECUTION = "execution";
	
	public static final int DEFAULT_MAXIMUM_GET_MEMBERS = 500; 
	
	protected String when_start;
	protected String when_end;

	static { 		
		DEFAULT_WORKFLOW.setId("urn:uuid:implus:111110000000000000000000000000000000");
		DEFAULT_WORKFLOW.setCreated(0);
	}
	
	public PLUSWorkflow() {
		this("Workflow");
	}
	
	public PLUSWorkflow(String name) { 
		super(); 
		setName(name); 
		setObjectType(PLUS_TYPE_WORKFLOW);
		setObjectSubtype(PLUS_SUBTYPE_EXECUTION);
		when_start="Uninitialized"; 
		when_end="Uninitialized"; 
	}  
	
	public String getWhenStart() { return when_start; } 
	public String getWhenEnd() { return when_end; } 
	
	public void setWhenStart(String when_start) { this.when_start = when_start; } 
	public void setWhenEnd(String when_end) { this.when_end = when_end; } 

	public boolean isWorkflow() { return true; } 

	public Map<String,Object> getStorableProperties() {
		Map<String,Object> m = super.getStorableProperties();
		m.put("when_start", getWhenStart());
		m.put("when_end", getWhenEnd());
		return m;
	}
	
	public PLUSObject setProperties(PropertySet props) throws PLUSException { 
		super.setProperties(props);
		setWhenStart(""+props.getProperty("when_start"));
		setWhenEnd("" + props.getProperty("when_end"));
		return this;
	}
} // End PLUSWorkflow
