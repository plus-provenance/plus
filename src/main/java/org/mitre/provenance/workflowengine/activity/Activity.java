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
package org.mitre.provenance.workflowengine.activity;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.user.PrivilegeClass;
import org.mitre.provenance.workflowengine.Transition;

/**
 * This object models a particular activity in a Workflow.  
 * Activities are considered black-box by default; they have only inputs, outputs, and transitions.
 * 
 * There are two types of transitions; "transitions" are arcs <b>out of</b> the activity going 
 * elsewhere.  "introductions" are arcs <b>into</b> the activity coming from elsewhere.  In this
 * sense, they can be thought of as a graph or form of doubly linked list.
 * @author moxious
 *
 */
public class Activity {	
	protected String name;	
	protected Hashtable <String, Metadata> inputs;
	protected Hashtable <String, Metadata> outputs;
	protected Hashtable <String, PLUSObject> outputMappings;
	protected Metadata metadata;
	
	/** Arcs out of this activity elsewhere **/
	public Vector <Transition> transitions; 
	/** Arcs into this activity from elsewhere **/
	public Vector <Transition> introductions;
	
	/** Special activity representing the terminating point of a workflow **/
	public static Activity END_STATE   = new Activity("END");
	/** Special activity representing the starting point of a workflow **/
	public static Activity START_STATE = new Activity("START");
	
	public Activity(String name) { 
		this.name = name;
		
		introductions = new Vector <Transition> (); 
		transitions = new Vector <Transition> ();
		metadata = new Metadata();
		inputs  = new Hashtable <String, Metadata> ();
		outputs = new Hashtable <String, Metadata> ();
		outputMappings = new Hashtable <String,PLUSObject> ();
	} // End Activity
	
	public String toString() {
		// return ("<Act: " + getName() + " ID=" + getMetadata().get("id") + 
		//		" IID=" + getMetadata().get("invokeid") + ">");
		
		return "<Activity: " + getName() + ">"; 
	} 
	
	public Metadata getMetadata() { return metadata; } 
	public String getName() { return name; } 
			
	/**
	 * Subclass this method and have it return a the variables that are needed in order for this activity to run 
	 * properly.  If your activity relies on having particular inputs with particular names, this is the way to 
	 * tell your caller about those requirements.
	 * This particular method returns an empty hash.
	 * @return a list of input variable names mapped to Class objects describing the types needed.
	 */
	public Hashtable <String,Class<?>> getRequiredInputNames() {
		Hashtable <String,Class<?>> x = new Hashtable <String,Class<?>> ();
		return x;
	} // End getRequiredInputNames
	
	/**
	 * Given a list of inputs, this method will determine whether or not the activities requirements have been met.
	 * If all of the necessary inputs are present, it will do nothing.  
	 * @param inputs the inputs to the activity execution.
	 * @throws ActivityException if some required input is missing, or of the wrong type.s
	 */
	public void checkInputTypes(Hashtable <String,PLUSObject> inputs) throws ActivityException { 
		Hashtable <String,Class<?>> req = getRequiredInputNames();
		
		Enumeration<String> z = inputs.keys();
		while(z.hasMoreElements()) { 
			System.out.println("check inputs: has input " + z.nextElement());
		}
		
		Enumeration<String> e = req.keys();
		while(e.hasMoreElements()) { 
			String key = (String)e.nextElement();
			Class<?> val = req.get(key);
			
			PLUSObject item = inputs.get(key);
			if(item == null) throw new ActivityException("check input types(" + getName() + 
					                                     "): missing input named " + key);
			
			if(!val.isAssignableFrom(item.getClass()))
				throw new ActivityException("check input types: input " + key + " is of type " + item.getClass() + 
						                    " which is incompatible with required type " + val);			                       
		} // End while
		
		// Everything checked out.
		return;
	} // End checkInputTypes
	
	public void registerInput(String inputParameterName) { 
		registerInput(inputParameterName, new Metadata());
	}
	
	public void registerInput(String inputParameterName, Metadata descriptionOfInput) { 
		inputs.put(inputParameterName, descriptionOfInput);
	} // End registerInput	
	
	public void addIntroduction(Transition t) throws Exception { 
		if(t.to != this)
			throw new Exception("You can only add an introduction that ends up at this activity!");
		
		introductions.add(t);
	} // End addIntroduction
	
	public void addTransition(Transition t) throws Exception { 
		if(t.from != this)
			throw new Exception("You can only add transitions that start from this activity!");
		
		transitions.add(t);
	} // End addTransition
	
	/**
	 * Determines whether this activity is the end of the line or not.
	 * @return true if the activity has no outbound transitions, false otherwise.
	 */
	public boolean isTerminalNode() { return getTransitions().size() == 0; } 
	public Vector <Transition> getTransitions() { return transitions; } 
	public Vector <Transition> getIntroductions() { return introductions; } 
	
	public int countInputs() { return inputs.size(); } 
	public int countOutputs() { return outputs.size(); } 	 
	
	public void registerOutput(String outputParameterName) { 
		registerOutput(outputParameterName, new Metadata()); 
	}
	
	public void registerOutput(String outputParameterName, Metadata descriptionOfOutput) { 
		outputs.put(outputParameterName, descriptionOfOutput);
	} // End 
	
	public Hashtable <String,Metadata> getInputs() { return inputs; } 
	public Hashtable <String,Metadata> getOutputs() { return outputs; } 

	/**
	 * Determines whether this activity takes a given variable name as input.  
	 * @see Activity#registerInput(String, Metadata)
	 * @param name the name of the variable you're interested in
	 * @return true if the activity inputs that variable, false otherwise.
	 */
	public boolean hasInput(String name) {
		if(name == null) return false;
		return inputs.containsKey(name); 
	} // End hasInput
	
	/**
	 * Determines whether this activity will output a given variable name.  This applies both
	 * for registered outputs, and "parroted" outputs.
	 * @see Activity#parrotOutput(String, PLUSObject)
	 * @see Activity#registerOutput(String, Metadata)
	 * @param name the name of the variable you're interested in
	 * @return true if the activity will output that variable, false otherwise.
	 */
	public boolean hasOutput(String name) {
		if(name == null) return false;

		// We can say that this activity has a given output if it's either a registered output,
		// or also if it's a parroted variable name.
		return outputs.containsKey(name) || outputMappings.containsKey(name);
	} // End hasOutput
	
	/**
	 * This method is used when you want an activity to simply emit a particular output after it has finished executing,
	 * even if it wasn't part of the input.  For example, if you want the start state to emit a token, you could tell
	 * the start activity to "parrot" a particular value, which would serve as an input to your first activity.
	 */
	public void parrotOutput(String varName, PLUSObject value) { 
		outputMappings.put(varName, value);
	} // End parrotOutput
	
	/**
	 * Override this method to perform actual work in activities.  When an 
	 * activity in a workflow is executed, this method will be called.
	 * 
	 * <p>Subclasses should put all output variables into the hash outputMappings, which has 
	 * already been created for you, and stores needed "parrot" variables.
	 * 
	 * <p>This method by default will create output data items with a security code of 5.
	 * The outputs will also have an uncertainty equal to 0.5
	 * 
	 * @see Activity#outputMappings
	 * 
	 * @param inputVariableMappings a hashtable with keys that are the names of input
	 * paramters, and values that are specific PLUSObjects to be used for those inputs.
	 * @return a hashtable with keys that are the names of registered output parameters, 
	 * and values that are specific PLUSObjects to be used for those outputs.
	 */
	public Hashtable <String, PLUSObject> execute(Hashtable <String, PLUSObject> inputVariableMappings) 
		throws ActivityException { 
				
		// Dummy method. Does nothing but create dummy outputs with no processing.
        // For each variable name that is supposed to be an output...
		Enumeration<String> e = outputs.keys();
		while(e.hasMoreElements()) {			
			String varName = (String)e.nextElement();
			
			// Create a dumb string
			PLUSString outputDI = new PLUSString(varName);
			
			// Tag it with whatever metadata was attached to the output variable description.
			outputDI.setMetadata(outputs.get(varName));
						
			// Set its uncertainty to be the maximum of all inputs.
			outputDI.setUncertainty((float)0.5);
			
			// Set its security to be the maximum of all inputs.
			try { outputDI.getPrivileges().addPrivilege(new PrivilegeClass(5)); }
			catch(Exception exc) {
				System.err.println("Activity.execute: Error " + exc);
				throw new ActivityException("Error", exc); 
			} 
			
			// Put it into the hashtable
			outputMappings.put(varName, outputDI);
		} // End while
		
		return outputMappings;
	} // End execute
} // End Activity
