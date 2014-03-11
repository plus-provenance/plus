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

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;

import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.tools.PLUSUtils;
import org.mitre.provenance.workflowengine.activity.Activity;

/**
 * This object encapsulates a runnable workflow.  If you're trying to load some workflow from the 
 * database that has already been executed, this isn't what you want.  Check PLUSWorkflow instead.
 * <p>Workflows contain a bunch of activities, each of which has inputs and outputs.
 * <p>This is <b>not</b> a fully-featured workflow engine.  There are numerous limitations to the
 * proper functioning of the engine.  As of 3/2008, the PLUS MSR plans to later add hooks into 
 * more fully-featured workflow engines to make up for the shortcomings and limitations of this code.
 * @author DMALLEN
  */
public class Workflow {
	protected static Logger log = Logger.getLogger(Workflow.class.getName());
	/**
	 * Controls whether or not the workflow engine will change confidence values of the data.
	 * The workflow engine will *always* change the confidence level of data items whose confidence is not
	 * valid (i.e. less than 0 or more than 1).  This variable here though controls what the workflow engine
	 * will do when the confidence is already valid.  If this is set to true, the workflow engine will always
	 * change the uncertainty scores according to the specified policy.  If it is set to false, the engine
	 * will only change the uncertainty scores if they are invalid.
	 */
	public static final boolean ALWAYS_COMPUTE_CONFIDENCE = false;
	
	/**
	 * Uncertainty policy: when uncertainty isn't present for an output, assume 1.0
	 * @see Workflow#validateUncertaintyMeasures(Hashtable, Hashtable, int)
	 */
	public static final int UNCERTAINTY_POLICY_ALWAYS_1 = 1;
	
	/**
	 * Uncertainty policy:  when uncertainty isn't present for an output, make the uncertainty
	 * equal to the product of the inputs.
	 * @see Workflow#validateUncertaintyMeasures(Hashtable, Hashtable, int)
	 */
	public static final int UNCERTAINTY_POLICY_MULTIPLY = 2;

	/**
	 * Uncertainty policy: when uncertainty isn't present for an output, make the uncertainty 
	 * equal to the minimum input uncertainty.
	 * @see Workflow#validateUncertaintyMeasures(Hashtable, Hashtable, int)	 
	 */
	public static final int UNCERTAINTY_POLICY_MIN_INPUTS = 3;
	
	/**
	 * Uncertainty policy: when uncertainty isn't present for an output, make the uncertainty 
	 * equal to the maximum input uncertainty.
	 * @see Workflow#validateUncertaintyMeasures(Hashtable, Hashtable, int)	 
	 */
	public static final int UNCERTAINTY_POLICY_MAX_INPUTS = 4;

	/**
	 * Uncertainty policy: when uncertainty isn't present for an output, make the uncertainty
	 * equal to the maximum input uncertainty * the degrade factor. 
	 * @see Workflow#validateUncertaintyMeasures(Hashtable, Hashtable, int)
	 */
	public static final int UNCERTAINTY_POLICY_GRADUAL_DEGRADE = 5;
	
	/**
	 * When using UNCERTAINTY_POLICY_DEGRADE, this controls how fast uncertainty degrades.
	 * @see Workflow#validateUncertaintyMeasures(Hashtable, Hashtable, int)	 
	 */
	public static final double DEGRADE_FACTOR = 0.9;
	
	/** The name of the workflow as created. */
	String workflowName;
	/** The starting point provided by the user */
	Activity startingPoint;
	/** The tracer object that will be used to log the lineage of the workflow. */
	LineageTracer tracer;    
    
    /**
     * Used only as an internal exception.
     * Nothing to see here, move along.
     */
	public class BackTrackingException extends Exception { 
		static final long serialVersionUID = 123124123;
		public BackTrackingException() { super(); } 
		public BackTrackingException(String msg) { super(msg); } 
	} // End BackTrackingException
	
	/**
	 * This is the ugly part.  We use this to store references to intermediate data products in the workflow.
	 * If we were more sophisticated, this would be stored in a relational database, but we aren't so it isn't.
	 * This stores two types of things; 
	 * <ul><li>Strings mapped to PLUSObjects representing particular data values that have
	 * already been computed by activities in the workflow,
	 * <li>and Activity objects mapped to their output sets to determine which activities we've already executed.
	 * </ul> 
	 * Not currently used much, but necessary for backtracking and other functionality that will probably be required.
	 */
	Hashtable <Object, Object> cache; 
	
	/**
	 * Default constructor, creates an empty workflow called "Unnamed Workflow"
	 */
	public Workflow() { 
		this(new LineageTracer(), "Unnamed Workflow");
	} // End default constructor
	
	/**
	 * Create a new workflow with a particular lineage tracer
	 * @param tracer the object to use to trace the lineage execution of the workflow.
	 */
	public Workflow(LineageTracer tracer) { 
		this(tracer, "Unnamed Workflow");
	} // End Workflow
	
	public Workflow(LineageTracer tracer, String name) {
		this.workflowName = name;
		cache = new Hashtable <Object,Object>();
		this.tracer = tracer;		        
	} // End Workflow
	
	/**
	 * Sometimes workflows contain data objects that were not the output of any process.  The workflow begins execution
	 * with these data objects "precomputed".  In order to use one of those, call this function.  If you don't add such
	 * a precomputed object to the workflow, then the workflow engine will fail to use it as an input to some other
	 * activity and your workflow won't run.
	 * @param name the name of the precomputed input.  This should match the input name of whatever activity is expecting
	 * to consume the object.
	 * @param object the object itself.
	 */
	public void addPrecomputedObject(String name, PLUSObject object) { 
		cache.put(name, object); 
	}
		
	/**
	 * Get the name of the workflow.
	 * @return a string representing the workflow's name.
	 */
	public String getName() { return workflowName; } 

	/**
	 * Set the workflow's name
	 * @param name the new name to use.
	 */
	public void setName(String name) { workflowName = name; } 
	
	/**
	 * Have we already determined what this variable's value is?
	 * @param varName the name of the variable
	 * @return true if we already have a cached value, false otherwise.
	 */
	public boolean alreadyComputed(String varName) { 
		return cache.contains(varName);
	} // End alreadyComputed
			
	/**
	 * Execute the workflow.  Assume the use of an "Always 1" uncertainty policy, meaning that any 
	 * data item that does not have a valid uncertainty associated with it will be marked with 1.0.
	 * This is equivalent to calling execute(start, UNCERTAINTY_POLICY_GRADUAL_DEGRADE);
	 * @param start the start node to use.
	 * @return the Hashtable containing the outputs of the last activity executed.
	 * @throws Exception
	 */	
	public Hashtable <String,PLUSObject> execute(Activity start) throws Exception { 
		return execute(start, UNCERTAINTY_POLICY_GRADUAL_DEGRADE);
	} // End execute

	/**
	 * Get the minimum uncertainty from a series of data items.
	 * @param varSet a hashtable containing string variable names mapped to PLUSObjects
	 * @return the minimum uncertainty from all data items in the list.  If the actual min is invalid, 
	 * this will return 0.
	 */
	public static float minUncertainty(Hashtable <String,PLUSObject> varSet) { 
		Enumeration<String> e = varSet.keys();
		float min = (float)1.0;
		
		while(e.hasMoreElements()) { 
			String key = (String)e.nextElement();
			PLUSObject val = varSet.get(key);
			float u = val.getUncertainty();
			if(u < min) min = u;
		} // End while
		
		if(min < 0) { 
			log.warning("WARNING:  Variable set has minimum uncertainty < 0!");
			min = 0;
		} // End if
		
		return min;
	} // End minUncertainty
	
	/**
	 * Get the maximum uncertainty from a series of data items.
	 * @param varSet a hashtable containing string variable names mapped to PLUSObjects
	 * @return the maximum uncertainty from all data items in the list.  If the actual max is invalid, 
	 * this will return 1.
	 */
	public static float maxUncertainty(Hashtable <String,PLUSObject> varSet) { 
		Enumeration<String>e = varSet.keys();
		float max = (float)-1;
		
		while(e.hasMoreElements()) { 
			String key = (String)e.nextElement();
			PLUSObject val = varSet.get(key);
			float u = val.getUncertainty();
			// log.warning("maxUncertainty: evaluating " + u);
			if(u > max) 
				max = u;
		} // End while
		
		if(max > 1) {  
			// log.warning("WARNING:  Variable set has maximum uncertainty > 0!");
			max = 1;
		} else if(max < 0) {
			// log.warning("WARNING: No valid maximum uncertainty found.");
			max = 1;
		}
		
		return max;
	} // End maxUncertainty

	/**
	 * Get the product of uncertainty values from a series of data items.
	 * @param varSet a hashtable containing string variable names mapped to PLUSObjects
	 * @return the product of all uncertainty from all data items in the list.  If the actual product
	 * is invalid, this will return 0.
	 */
	public static float productUncertainty(Hashtable <String,PLUSObject> varSet) { 
		Enumeration<String>e = varSet.keys();
		float prod = (float)1;
		
		while(e.hasMoreElements()) { 
			String key = (String)e.nextElement();
			PLUSObject val = varSet.get(key);
			float u = val.getUncertainty();
			prod = prod * u;
		} // End while
		
		if(prod > 1 || prod < 0) {  
			log.warning("WARNING:  Variable set has product uncertainty " + prod + " out of range!");
			prod = 0;
		} // End if
		
		return prod;
	} // End maxUncertainty
	
	/**
	 * Convenience method used to check if a particular queue contains a particular activity.
	 * This just does a stupid linear scan which can very likely be improved upon.
	 * @param array the queue
	 * @param ac the activity
	 * @return true if array contains ac, false otherwise.
	 */
	private boolean queueContains(ArrayList<Object[]>array, Activity ac) { 
		
		for(int x=0; x<array.size(); x++) { 
			Object [] el = (Object [])array.get(x);
			if(el[0] == ac) return true;
		} // End for
		
		return false;
	} // End queueContains

	/**
	 * Validate that the uncertainy measures for a particular set of variables is accurate, according to the 
	 * chosen policy of the execution engine.  If the uncertainty measures are not correct, the uncertainty
	 * will be modified according to the specified policy.
	 * @param inputs the input variables to a particular activity.
	 * @param output the output variables to a particular activity
	 * @return modified hashtable corresponding to the outputs.
	 */
	public Hashtable <String,PLUSObject> validateUncertaintyMeasures(
			Hashtable <String,PLUSObject> inputs, Hashtable <String,PLUSObject> output, int POLICY) { 
        // Ensure that all variables are tagged with uncertainty, to account for uncertainty-naive
        // processes.
        Enumeration<String>okz = output.keys();
        float min = Workflow.minUncertainty(inputs);
        float max = Workflow.maxUncertainty(inputs);
        float prod = Workflow.productUncertainty(inputs);
        
        while(okz.hasMoreElements()) {           	
        	String kz = (String)okz.nextElement();
        	PLUSObject v = output.get(kz);
        	float u = v.getUncertainty();
        	if(ALWAYS_COMPUTE_CONFIDENCE || u < 0 || u > 1) { 
        		log.fine("Validate uncertainty: input " + u + " for " + v);
        		if(POLICY == UNCERTAINTY_POLICY_ALWAYS_1) {
        			v.getMetadata().put("computedUncertainty", "implus:POLICY_ALWAYS_1");
        			v.setUncertainty((float)1.0);
        			log.fine("New uncertainty: 1.0");
        		} else if(POLICY == UNCERTAINTY_POLICY_MIN_INPUTS) {        
        			v.getMetadata().put("computedUncertainty", "implus:POLICY_MIN_INPUTS");
        			v.setUncertainty(min);
        			log.fine("New uncertainty: (minimum) " + min);
        		} else if(POLICY == UNCERTAINTY_POLICY_MAX_INPUTS) {        		
        			v.getMetadata().put("computedUncertainty", "implus:POLICY_MAX_INPUTS");
        			v.setUncertainty(max);
        			log.fine("New uncertainty: (maximum) " + max);
        		} else if(POLICY == UNCERTAINTY_POLICY_MULTIPLY) {
        			v.getMetadata().put("computedUncertainty", "implus:POLICY_MULTIPLY");
        			v.setUncertainty(prod);
        			log.fine("New uncertainty: (product) " + prod);
        		} else if(POLICY == UNCERTAINTY_POLICY_GRADUAL_DEGRADE) {         
        			v.getMetadata().put("computedUncertainty", "implus:POLICY_GRADUAL_DEGRADE");
        			v.setUncertainty((float)(max * (float)DEGRADE_FACTOR));
        			log.fine("Max uncertainty: " + max);
        			log.fine("New uncertainty: (degraded) " + (max * (float)DEGRADE_FACTOR));
        		} else { 
        			v.getMetadata().put("computedUncertainty", "implus:NONEXISTANT_POLICY_DEFAULT_1");
        			log.severe("ILLEGAL POLICY: " + POLICY + " Assuming always 1.");
        			v.setUncertainty((float)1.0);
        		} // End else
        	} // End if            	
        } // End while
		
        return output;
	} // End validateUncertaintyMeasures
		
	/**
	 * Execute the workflow.
	 * @param start the start node to use.
	 * @param POLICY the uncertainty policy to use.
	 * @return the Hashtable containing the outputs of the last activity executed.
	 * @throws Exception
	 */
	protected Hashtable <String,PLUSObject> execute(Activity start, int POLICY) throws Exception { 
		Hashtable <String,PLUSObject> output = new Hashtable <String,PLUSObject> () ;
		ArrayList <Object []> open   = new ArrayList <Object []>();
		ArrayList <Activity> closed = new ArrayList <Activity>();
		Hashtable <Transition, Boolean> seenTransitions = new Hashtable <Transition, Boolean> ();
		int bt = 0;

		// Fire an event that says we're starting execution.
		tracer.startWorkflow(this, start);
		open.add(new Object [] {start, null} );
		
		Activity current = null;
		
		// Iterate through the list of open activities we still need to execute.
		while(open.size() != 0) {			
			// Each item in the "open" list is an array: [some activity, some transition] 
			Object [] stuff = open.remove(0);
			current = (Activity)stuff[0];
			Transition t = (Transition)stuff[1];
			
			// The open list contains the next activity to execute, and the particular transition that it was 
			// associated with.  Before executing the activity, notify the listener that we are transitioning.
			if(t != null) {
				Activity to = t.getTo();
				if(to.getMetadata().get("invokeid") == null) 
					to.getMetadata().put("invokeid", PLUSUtils.generateID());
					
				seenTransitions.put(t, new Boolean(true));
				tracer.transition(t);			
			} else if(current != Activity.START_STATE){ 
				log.warning("WARNING:  Transition to activity " + current + " was null.");
				log.warning("Not firing transition event to listener.");
			} // End else
			
            // As a special case, if the current activity is the end state,
			// then we're done...notify the listener.
			if(current == Activity.END_STATE) {				
				tracer.endWorkflow(this, current);
				return output;
			} // End if
			
			// Declare the structure that will hold inputs to the activity.
			Hashtable <String,PLUSObject> inputs = null;
			
			try { inputs = computeNeededInputs(current); } 
			catch(BackTrackingException e) {
				// Sometimes, all necessary inputs have not yet been computed because of branching
				// in the workflow.  In this situation, we just need to add this back to the "open" list,
				// and hope that one of the intervening activities in the list will end up calculating
				// the necessary input.  That way, when the activity comes around the next time, its inputs
				// will be there.
				log.fine("Cannot execute '" + current.getName() + "': " + e);				
				open.add(stuff); // Add it back to the open set.
				bt++;
				
				// In certain bad conditions, (such as loops or reflexive transitions) we can
				// get stuck backtracking on a node, only to find that it's the only node left in
				// our list to process.  When that happens, we gotta just quit.  This is pretty much
				// always caused by a malformed workflow, or an activity that requires a nonexistant 
				// input.
				if(bt > 10) throw new Exception("STOP!  I'm stuck in a loop!");
				
				// Go to the next item in the list.  Nothing more to do here if we're backtracking.
				continue;
			} // End catch
			
			// Special bizarre case: say an activity has 5 inputs, and has been waiting for execution.
			// At this point in the code, its golden moment in the sun has come.  Except so far we've only
			// followed one transition to the node, when it has 5 introductory transitions.  Now we have
			// to go back and fire "transition" events for all of the others that blocked until this point
			// because the activity's preconditions hadn't been met at the time.
			Vector <Transition> introductions = current.getIntroductions();
			if(introductions.size() > 1) { 
				for(int x=0; x<introductions.size(); x++) { 
					Transition catchUp = introductions.elementAt(x);
					if(seenTransitions.containsKey(catchUp)) continue;
					else { 
						log.fine("Catching up on transitions...");
						tracer.transition(catchUp);
						seenTransitions.put(catchUp, new Boolean(true));
					} // End else
				} // End for
			} // End if
					
			// Register the activity with the PLUS system.
			// Each activity has a generic "type signature" stating which inputs and outputs it produces/needs.
			// When an activity is specifically executed, then it becomes an "invocation".
			//String id = plus.registerActivity(current);
			
			String id = PLUSUtils.generateID();
			current.getMetadata().put("id", id);
			
			log.fine("WORKFLOW:  Starting to execute " + current);
			
			// At the time an activity is actually executed, it gets a distinct invocation ID to 
			// differentiate it from any other instance of that activity.
			// Doing this allows the lineage loggers also to track activities.
			if(current.getMetadata().get("invokeid") == null) {
				String invokeID = PLUSUtils.generateID();
				current.getMetadata().put("invokeid", invokeID);
				current.getMetadata().put("startExecution", (new Date()).toString());
			} // End if
			
			current.getMetadata().put("startExecution", (new Date()).toString());
			tracer.startActivity(current, inputs);    // Fire the start event to the listener
			output = current.execute(inputs);         // Actually execute the activity, capture outputs
						
			/* Do cleanup and log uncertainty values/ending time to the object **before** firing the 
			 * event indicating that the activity is finished.  This way, the logger gets the benefit
			 * of this extra work.
			 */ 
			
            // Ensure that all variables are tagged with uncertainty, to account for uncertainty-naive
            // processes.  So if some process was too naive to associate proper uncertainty, this will do 
			// it for the process.
			output = validateUncertaintyMeasures(inputs, output, POLICY);
			
			// Log pointers back to the original data in the item, so that 
			// we can fetch it and reconstitute it later.
			// logPLUSObjectPointersAndMetadata(inputs);
			// logPLUSObjectPointersAndMetadata(output);
			
            /******  At this point, all outputs are guaranteed to have valid uncertainty. ***/
            
            // Tag the activity with an ending date/time.  This allows calculations on duration of exec.
            current.getMetadata().put("endExecution", (new Date()).toString());
            
            // Log the metadata object for the current invocation.
            // plus.logMetadata((String)current.getMetadata().get("invokeid"), current.getMetadata());
            
            // Fire the finish event to the listener.  
            tracer.finishActivity(current, output);   

            // Do some caching.  We hold on to our output products so we can look them up later to see
            // if they are required inputs for something else.
			cacheActivity(current, output);
			cacheVariables(output);
			
			// Now all that's left is to look at the list of transitions that go out of this activity,
			// and add all of the ones to the "open" queue that aren't already there.
			Vector<Transition>transitions = current.getTransitions();
			for(int x=0; x<transitions.size(); x++) { 
				Transition trans = (Transition)transitions.get(x);
				Activity to = trans.getTo();
							
				if(!queueContains(open, to)) { 
					log.fine("WORKFLOW:  Adding " + to + " to worklist.");
					open.add(new Object [] { to, trans} );
				} // End if
			} // End for
			
			// Add the current item to the closed list.
			closed.add(current);			
		} // End while
		
		// Fire event saying that we're done with the workflow.
		// For most workflows that have an "end" event, this will never be executed, because
		// the end state is caught above.
		tracer.endWorkflow(this, current);
		
		return output;
	} // End execute
	
	/**
	 * Calculate which inputs should be used for this particular process.
	 * If an input isn't available, we have to defer execution until its precondition is met.
	 * Inputs generally come from two different places; either the last value passed as part of
	 * the transition, or something from the cache, which was computed earlier.
	 * @param a the Activity you are about to execute.
	 * @return a Hashtable containing variable names mapped onto PLUSObject's that are usable as inputs
	 * to this particular activity.
	 * @throws BackTrackingException if some needed input is not available or has not yet been created. 
	 * If this happens, then Activity a <b>should not be executed</b>.
	 */
	protected Hashtable <String,PLUSObject> computeNeededInputs(Activity a) throws BackTrackingException { 
		Enumeration<String>neededInputs = a.getInputs().keys();
		
		Vector <Transition> intros = a.getIntroductions();
		Hashtable <String,PLUSObject> inputs = new Hashtable <String,PLUSObject> ();
		
		while(neededInputs.hasMoreElements()) { 
			String neededVar = (String)neededInputs.nextElement();
			boolean found = false;
			
			for(int x=0; x<intros.size(); x++) { 
				Transition t = intros.elementAt(x);
			
				// Check and see if there is an introduction whose input variable is what we need.
				if(t != null && neededVar.equals(t.getInputVariableName())) {
					// The variable that's needed was the one just passed from the previous output. 
					log.fine("Using " + t.getFrom() + ":" + t.getOutputVariableName() + " as input for " + 
   				             t.getTo() + ":" + t.getInputVariableName());
					
					if(cache.containsKey(t.getOutputVariableName())) {						
						inputs.put(neededVar, (PLUSObject)cache.get(t.getOutputVariableName()));
						found = true;
					}
				} else { 					
					if(cache.containsKey(neededVar)) {
						log.fine("Using cached data item " + neededVar + " as input to " + a);
						found = true;
						inputs.put(neededVar, (PLUSObject)cache.get(neededVar));
					} // End else
				} // End else
			} // End for
			
			if(!found) 
				throw new BackTrackingException("Cannot find precomputed variable " + 
						                        neededVar + " for " + a);			
		} // End while
		
		return inputs;
	} // End computeNeededInputs
	
	/**
	 * Store a set of particular variables in the cache for later use.
	 * @param vars a hash mapping variable names onto PLUSObjects that are their values.
	 */
	public void cacheVariables(Hashtable <String,PLUSObject> vars) { 
		Enumeration<String>e = vars.keys();
		
		while(e.hasMoreElements()) { 
			String key = (String)e.nextElement();
			log.fine("Caching output variable " + key + "/" + vars.get(key));
			cache.put(key, (PLUSObject)vars.get(key));
		} // End while		
	} // End cacheVariables
	
	/**
	 * Keep track of an activity that has already been seen or used.
	 * @param a
	 * @param vars
	 */
	public void cacheActivity(Activity a, Hashtable <String,PLUSObject> vars) { 
		cache.put(a, vars);
	} // End cacheActivity
} // End Workflow
