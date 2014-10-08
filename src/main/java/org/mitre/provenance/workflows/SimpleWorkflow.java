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
package org.mitre.provenance.workflows;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSInvocation;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.workflowengine.RelationalLineageCapture;
import org.mitre.provenance.workflowengine.Transition;
import org.mitre.provenance.workflowengine.Workflow;
import org.mitre.provenance.workflowengine.activity.Activity;

/**
 * The simplest, most straightforward workflow available.
 * A transitions to B, transitions to C.  End of story.
 * @author moxious
 *
 */
public class SimpleWorkflow extends ProvenanceCollection {
	protected PLUSWorkflow wf = null;
	
	public SimpleWorkflow() throws PLUSException {
		super();
		
		wf = new PLUSWorkflow();
		wf.setName("Simple ABC Workflow");
		addNode(wf);
		
		PLUSInvocation a = new PLUSInvocation("A");
		PLUSInvocation b = new PLUSInvocation("B");
		PLUSInvocation c = new PLUSInvocation("C");
		
		addNode(a); addNode(b); addNode(c);
		
		PLUSString inputToA = new PLUSString("Input to A");
		PLUSString outputOfA = new PLUSString("Output of A");
		PLUSString outputOfB = new PLUSString("Output of B");
		PLUSString outputOfC = new PLUSString("Output of C");
		
		addNode(inputToA); addNode(outputOfA); addNode(outputOfB);
		addNode(outputOfC);
		
		addEdge(new PLUSEdge(inputToA, a, wf));
		addEdge(new PLUSEdge(a, outputOfA, wf)); 
		addEdge(new PLUSEdge(outputOfA, b, wf));
		addEdge(new PLUSEdge(b, outputOfB, wf));
		addEdge(new PLUSEdge(outputOfB, c, wf)); 
		addEdge(new PLUSEdge(c, outputOfC, wf));
	}
} // End SimpleWorkflow
