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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.PropertySet;
import org.mitre.provenance.db.neo4j.DoesNotExistException;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;

/**
 * Models an invocation as a type of PLUS node.
 * Invocations depend on activities and workflows.  Note that this object initially stores only the ID suitable
 * for fetching those objects.  It does not automatically load those objects.  Because they are mutually dependent,
 * you have to be careful not to have automatic code that causes one to load the other recursively.
 * @author moxious
 */
public class PLUSInvocation extends PLUSObject {
	/** Stores the workflow associated with this invocation */
	private PLUSWorkflow workflow = PLUSWorkflow.DEFAULT_WORKFLOW;
	
	/** Stores the activity associated with this invocation */
	private PLUSActivity activity = PLUSActivity.UNKNOWN_ACTIVITY;
	
	/** Stores the list of input parameters */
	private Hashtable <String,String> inputParams = new Hashtable<String,String> ();
	
	/** Stores the list of output parameters */
	private Hashtable <String,String> outputParams = new Hashtable<String,String> ();
	
	public static final String PLUS_SUBTYPE_INVOCATION = "invocation";

	/** Constructs the PLUS invocation */
	public PLUSInvocation() { 
		super(); 
		setObjectType(PLUS_TYPE_INVOCATION); 
		setObjectSubtype(PLUS_SUBTYPE_INVOCATION); 
		setWorkflow(PLUSWorkflow.DEFAULT_WORKFLOW);
		setActivity(PLUSActivity.UNKNOWN_ACTIVITY);
	}	

	/** Constructs a named PLUS invocation */
	public PLUSInvocation(String name)
		{ this(); setName(name); }
	
	/** Constructs a copy of the PLUS invocation */
	public void copy(PLUSInvocation invocation) {
		super.copy(invocation);
		workflow = invocation.getWorkflow();
		activity = invocation.getActivity();		
		inputParams = new Hashtable<String,String>(invocation.inputParams);
		outputParams = new Hashtable<String,String>(invocation.outputParams);
		setObjectType(PLUS_TYPE_INVOCATION);
		setObjectSubtype(PLUS_SUBTYPE_INVOCATION); 
	}
	
	/** Creates a clone of the PLUS invocation */
	public PLUSInvocation clone() { 
		PLUSInvocation invocation = new PLUSInvocation(); 
		invocation.copy(this); 
		return invocation; 
	}
	
	public PLUSWorkflow getWorkflow() { return workflow; } 
	public PLUSActivity getActivity() { return activity; } 
	
	/** @deprecated */
	public String getInput(String name) { return inputParams.get(name); }
	
	/** @deprecated */
	public String getOutput(String name) { return outputParams.get(name); } 

	// PLUS invocation setters
	public void setWorkflow(PLUSWorkflow wf) {
		if(wf == null) wf = PLUSWorkflow.DEFAULT_WORKFLOW;
		this.workflow = wf;		
	} 
	
	public void setActivity(PLUSActivity ac) {
		if(ac == null) ac = PLUSActivity.UNKNOWN_ACTIVITY;
		this.activity = ac; 
	} 

	/** 
	 * Adds an input parameter to the invocation
	 * @deprecated 
	 */
	public void addInput(String name, String oid)
		{ inputParams.put(name, oid); }
	
	/** Adds an output parameter to the invocation
	 * @deprecated 
	 */
	public void addOutput(String name, String oid)
		{ outputParams.put(name, oid); }

	/** 
	 * Returns the input parameters for the PLUS invocation
	 * @deprecated 
	 */
	public Enumeration <String> getInputNames()
		{ return inputParams.keys(); }

	/** 
	 * Returns the output parameters for the PLUS invocation
	 * @deprecated 
	 */
	public Enumeration <String> getOutputNames()
		{ return outputParams.keys(); }

	/** 
	 * Returns the number of inputs to this PLUS invocation
	 * @deprecated 
	 */
	public int getInputCount()
		{ return inputParams.size(); }

	/** Returns the number of outputs to this PLUS invocation */	
	public int getOutputCount()
		{ return outputParams.size(); } 

	/** Displays the PLUS invocation as a string */
	public String toString()
		{ return new String("[Invocation: " + getName() + "]"); }
	
	/**
	 * Returns a map of known input parameters to this invocation.  Keys are the names of the parameters, 
	 * values are PLUSObject oids.
	 * @return a map of inputs.
	 * @deprecated
	 */
	public Map<String,String> getInputParameters() { return inputParams; }
	
	/**
	 * Returns a map of known output parameters to this invocation.  Keys are the names of the parameters, 
	 * values are PLUSObject oids.
	 * @return a map of inputs.
	 * @deprecated
	 */
	public Map<String,String> getOutputParameters() { return outputParams; } 
	
	public Map<String,Object> getStorableProperties() {
		Map<String,Object> m = super.getStorableProperties();
		m.put("workflow", getWorkflow().getId());
		m.put("activity", getActivity().getId());
		return m;
	}
	
	public PLUSObject setProperties(PropertySet props, ProvenanceCollection contextCollection) throws PLUSException { 
		super.setProperties(props, contextCollection);
				
		String aid = ""+props.getProperty("activity");		
		setActivity(PLUSActivity.UNKNOWN_ACTIVITY);
		
		if(aid != null && !PLUSActivity.UNKNOWN_ACTIVITY.getId().equals(aid)) {
			try {	
				PLUSActivity activity = (PLUSActivity)Neo4JPLUSObjectFactory.newObject(aid);
				setActivity(activity);
			} catch(DoesNotExistException dne) {
				log.warning("Cannot set activity for " + this + " because activity " + aid + " doesn't exist.");
			} catch(Exception exc) {							
				log.severe("Cannot set activity for " + this + ": " + exc.getMessage());
				exc.printStackTrace();
				throw new PLUSException("Invalid activity " + aid); 
			}
		}		
		
		String wfid = ""+props.getProperty("workflow");
		setWorkflow(PLUSWorkflow.DEFAULT_WORKFLOW);
		
		if(wfid != null && !PLUSWorkflow.DEFAULT_WORKFLOW.getId().equals(wfid)) {
			try {					
				PLUSWorkflow wf = (PLUSWorkflow)Neo4JPLUSObjectFactory.newObject(wfid);
				setWorkflow(wf); 
			} catch(DoesNotExistException dne) {
				log.warning("Cannot set workflow for " + this + " because workflow " + wfid + " doesn't exist.");
			} catch(Exception exc) {			
				log.severe("Cannot set wf for " + this + ": " + exc.getMessage());
				exc.printStackTrace();
				throw new PLUSException("Invalid workflow " + wfid ); 
			}
		}		
		
		return this;
	}
} // End PLUSInvocation