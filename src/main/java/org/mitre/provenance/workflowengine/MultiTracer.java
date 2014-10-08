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
import java.util.Vector;

import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.workflowengine.activity.Activity;
/**
 * All this object does is act like a single LineageTracer when in fact it invokes multiple tracers in the background.
 * This way, a workflow can use only one tracer, but have it do many different things.
 * @author moxious
 */
public class MultiTracer extends LineageTracer {
	protected Vector <LineageTracer> tracers;

	public MultiTracer() { 
		super(); 
		tracers = new Vector <LineageTracer> ();
		
		// TODO: Fix XML Lineage Capture (all kinds of weird CLOB errors)
		// tracers.add(new XMLLineageCapture());
		tracers.add(new RelationalLineageCapture());
	} // End MultiTracer 

	public void addTracer(LineageTracer t) { tracers.add(t); } 
	
	public void startWorkflow(Workflow wf, Activity startingPoint) { 
		for(int x=0; x<tracers.size(); x++) { 
			((LineageTracer)tracers.get(x)).startWorkflow(wf, startingPoint);
		} // End for
	} // End startWorkflowRun

	public void endWorkflow(Workflow wf, Activity endingPoint) { 
		for(int x=0; x<tracers.size(); x++) { 
			((LineageTracer)tracers.get(x)).endWorkflow(wf, endingPoint);
		} // End for
	} // End endWorkflow

	public void startActivity(Activity current, Hashtable <String,PLUSObject> parameters) {
		for(int x=0; x<tracers.size(); x++) { 
			((LineageTracer)tracers.get(x)).startActivity(current, parameters);
		} // End for
	}
	
	public void transition(Transition t) { 
		for(int x=0; x<tracers.size(); x++) { 
			((LineageTracer)tracers.get(x)).transition(t);
		} // End for
	}
	
	public void finishActivity(Activity current, Hashtable <String,PLUSObject> results) { 
		for(int x=0; x<tracers.size(); x++) { 
			((LineageTracer)tracers.get(x)).finishActivity(current, results);
		} // End for
	}
} // End MultiTracer
