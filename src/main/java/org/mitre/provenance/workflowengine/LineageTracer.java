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

import java.util.Hashtable;

import java.util.logging.Logger;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.workflowengine.activity.Activity;
/**
 * Extend this class to do all sorts of nifty things with lineage.
 * 
 * The Workflow class uses this and calls methods as it processes through a workflow.  By subclassing this and
 * writing the appropriate methods, lineage capture is easy.
 * 
 * This particular class, while concrete, does nothing but print out debugging messages about the events that are
 * being fired.  You need to subclass this to do something useful.
 * 
 * @author DMALLEN
 */
public class LineageTracer {
	protected static Logger log = Logger.getLogger(LineageTracer.class.getName());
	public LineageTracer() { ; } 
	
	/**
	 * This method will be called when a workflow starts execution.  This should be the first method called, prior
	 * to any activity or transition being triggered.
	 * If your LineageTracer needs to do some work at startup, this is the place to do it.
	 * @param wf the workflow that is beginning
	 * @param startingPoint the first Activity reached, which may be a START_STATE
	 */	
	public void startWorkflow(Workflow wf, Activity startingPoint) { 
		log.fine("START WORKFLOW");
	} // End startWorkflowRun

	/**
	 * This method will be called when a workflow finishes execution.  After this method is called, you can
	 * expect that no other activities or transitions will be triggered.
	 * If your LineageTracer needs to do some cleanup, this is the place to do it.
	 * @param wf the workflow that is ending.
	 * @param endingPoint the final Activity reached, which may be an END_STATE
	 */
	public void endWorkflow(Workflow wf, Activity endingPoint) { 
		log.fine("END WORKFLOW");
	} // End endWorkflow

	/**
	 * This method will be called when an activity's execution is about to start.
	 * @param current The activity that is about to be executed
	 * @param parameters the parameters being passed to that activity.
	 */
	public void startActivity(Activity current, Hashtable <String,PLUSObject> parameters) { 
		log.fine("*TRACER* Starting " + current + " with " + parameters.size() + " parameters");
	}
	
	/**
	 * This method will be called when a transition is followed - after an activity has finished
	 * executing, before the next one starts.  Warning: because of dependencies and backtracking, following
	 * a transition from A -> B does not guarantee that startActivity for B is the next signal to be called.  
	 * @param t the transiton being followed.
	 */
	public void transition(Transition t) { 
		log.fine("*TRACER* Transitioning... " + t);
	}
	
	/**
	 * This method will be called after an activity has finished executing.
	 * @param current the activity that just completed
	 * @param results the results that it produced.
	 */
	public void finishActivity(Activity current, Hashtable <String,PLUSObject> results) { 
		log.fine("*TRACER* Finishing " + current + " with " + results.size() + " outputs.");
	}
} // End LineageTracer
