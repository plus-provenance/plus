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
package org.mitre.provenance.workflowengine;

import org.mitre.provenance.workflowengine.activity.Activity;


/**
 * A transition from one activity to another.  The most important thing that this class does is to map a 
 * particular output of one activity, to a particular input of another.
 * 
 * <p>A transition can be referred to in two ways.  Take the example of activities A -&gt; B connected by 
 * the arrow transition.  The transition is referred to as a "transition" of A, but also referred to 
 * as an "introduction to" B.  
 * 
 * <p>There are two special case inputs and outputs, the "start state" and "end state".
 * @author moxious
 *
 */
public class Transition {
	public Activity from;
	public Activity to;
	public String outName;
	public String inName;
	
	/**
	 * Marker is used for traversing transitions. 
	 */
	public boolean marker;
	
	/**
	 * Create a transition from one activity to another.
	 * @param from the activity the transition is coming from.
	 * @param to the activity the transition is going towards
	 * @param outputVarName the name of the output parameter in "from"
	 * @param inputVarName the name of the input parameter in "to"
	 * @throws Exception when there is an error, such as if the "from" activity has no output variable 
	 * named with outputVarName
	 */
	public Transition(Activity from, Activity to, String outputVarName, String inputVarName) throws Exception {
		outName = outputVarName;
		inName  = inputVarName;
		this.from = from;
		this.to = to;
	
		if(from == null) throw new Exception("null from is not allowed - use Activity.START_STATE");
		if(to == null) throw new Exception("null to is not allowed - use Activity.END_STATE");
		
		marker = false;
		
		// System.out.println("Creating transition " + from + " -> " + to + " ... " + outName + " => " + inName);
		
		if(this.from == Activity.END_STATE) 
			throw new Exception("No transitions are allowed from an end state");
		if(this.to == Activity.START_STATE)
			throw new Exception("No transitions are allowed to a start state");
					
		// Paired inputs/outputs don't apply if you are transitioning to a first activity from start, or to end.
		if(this.from!= Activity.START_STATE && 
		   this.to != Activity.END_STATE && 
		   !this.to.hasInput(inName))
			throw new Exception("Input activity " + this.to + " has no such input variable " + inName + "!");
		
		if(this.from != Activity.START_STATE && !this.from.hasOutput(outName))
			throw new Exception("Output activity " + this.from + " has no such output variable " + outName + "!");
		
	} // End Transition
	
	public String toString() { 
		return new String("<Transition " + from + " => " + to + " " + inName + "/" + outName + ">");
	}
	
	/**
	 * Registers a transition with its relevant activity nodes.  This really links them together, 
	 * rather than using this data object as a container.
	 */
	public void register() throws Exception { 
		from.addTransition(this);
		to.addIntroduction(this);
	} // End register()
	
	/** Return true if the transition has been visited, false otherwise. **/
	public boolean getMarker() { return marker; } 	
	public void toggleMarker() { marker = !marker; } 
	public void setMarker(boolean n) { marker = n; } 
	
	public Activity getFrom() { return from; }
	public Activity getTo() { return to; } 
	public String getOutputVariableName() { return outName; } 
	public String getInputVariableName() { return inName; } 
	
} // End Transition
