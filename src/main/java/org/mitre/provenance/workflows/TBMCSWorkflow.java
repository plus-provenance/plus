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
import org.mitre.provenance.client.AbstractProvenanceClient;
import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.surrogate.sgf.GenericInvocationFuzzer;
import org.mitre.provenance.user.PrivilegeClass;
import org.mitre.provenance.user.PrivilegeSet;
import org.mitre.provenance.workflowengine.RelationalLineageCapture;
import org.mitre.provenance.workflowengine.Transition;
import org.mitre.provenance.workflowengine.Workflow;
import org.mitre.provenance.workflowengine.activity.Activity;
import org.mitre.provenance.workflowengine.activity.PLUSObjectEmittingActivity;

public class TBMCSWorkflow extends TracedWorkflow {	
	protected Workflow w;
	protected RelationalLineageCapture tracer; 
	protected AbstractProvenanceClient client = new LocalProvenanceClient();
	
	public TBMCSWorkflow() throws Exception { 
		tracer = new RelationalLineageCapture();
		tracer.setWriteImmediate(false);
		
		w = new Workflow(tracer, "TBMCS Workflow");
		PrivilegeSet mid = new PrivilegeSet();
		mid.addPrivilege(new PrivilegeClass(3));
				
		PLUSActor subord = new PLUSActor("Air Expeditionary Wings");
		PLUSActor actor = new PLUSActor("TBMCS");
		
		client.report(ProvenanceCollection.collect(subord, actor));
		
		PLUSObjectEmittingActivity GCCSI3 = new PLUSObjectEmittingActivity("GCCS-I3"); 
		PLUSObjectEmittingActivity MAAP   = new PLUSObjectEmittingActivity("MAAP and Theater Air Battle Planner (TAP)");
		PLUSObjectEmittingActivity IRIS   = new PLUSObjectEmittingActivity("IRIS Messaging Dispatcher");
		PLUSObjectEmittingActivity GCCSJ  = new PLUSObjectEmittingActivity("Task Execution: GCCS-J COP");
		PLUSObjectEmittingActivity ESTAT  = new PLUSObjectEmittingActivity("Task Execution: ESTAT");
		PLUSObjectEmittingActivity AAT    = new PLUSObjectEmittingActivity("Task Execution: AAT");
		
		GCCSI3.getMetadata().put("SGF", new GenericInvocationFuzzer());
		GCCSI3.getMetadata().put("PrivSet", mid);
		GCCSI3.getMetadata().put("actor", actor); 
			
		MAAP.getMetadata().put("SGF", new GenericInvocationFuzzer());
		MAAP.getMetadata().put("PrivSet", mid);
		MAAP.getMetadata().put("actor", actor);

		IRIS.getMetadata().put("SGF", new GenericInvocationFuzzer());
		IRIS.getMetadata().put("PrivSet", mid);
		IRIS.getMetadata().put("actor", subord);
		
		GCCSJ.getMetadata().put("SGF", new GenericInvocationFuzzer());
		GCCSJ.getMetadata().put("PrivSet", mid);
		GCCSJ.getMetadata().put("actor", subord);

		ESTAT.getMetadata().put("SGF", new GenericInvocationFuzzer());
		ESTAT.getMetadata().put("PrivSet", mid);
		ESTAT.getMetadata().put("actor", subord);

		AAT.getMetadata().put("SGF", new GenericInvocationFuzzer());
		AAT.getMetadata().put("PrivSet", mid);
		AAT.getMetadata().put("actor", subord);

		PLUSString JTT   = new PLUSString("Joint Targeting Toolkit (JTT)", "Joint Targeting Toolkit (JTT)");
		PLUSString ORB   = new PLUSString("Order of Battle", "FRoB and EOB");
		PLUSString WEBAD = new PLUSString("Web Airspace Deconfliction", "WEBAD");
		PLUSString ATOs  = new PLUSString("ATOs and Coordination", "ATOs and Coordination");
		PLUSString ATO1  = new PLUSString("Air Tasking Order 1", "Air Tasking Order 1");
		PLUSString ATO2  = new PLUSString("Air Tasking Order 2", "Air Tasking Order 2");
		PLUSString ATO3  = new PLUSString("Air Tasking Order 3", "Air Tasking Order 3");
		
		JTT.setOwner(actor); ORB.setOwner(actor); WEBAD.setOwner(actor); ATOs.setOwner(actor);
		ATO1.setOwner(subord); ATO2.setOwner(subord); ATO3.setOwner(subord); 

		JTT.getPrivileges().addPrivilege(new PrivilegeClass(5));
		ORB.getPrivileges().addPrivilege(new PrivilegeClass(5));
		WEBAD.getPrivileges().addPrivilege(new PrivilegeClass(5));
		ATOs.getPrivileges().addPrivilege(new PrivilegeClass(5));
		ATO1.getPrivileges().addPrivilege(new PrivilegeClass(5));
		ATO2.getPrivileges().addPrivilege(new PrivilegeClass(5));
		ATO3.getPrivileges().addPrivilege(new PrivilegeClass(5));
		
		// Note that we have to call "addEmittedObject" each time, because we're rigging
		// the results to be those particular tuples.  If the activities were actually
		// computing something on their own, that wouldn't be necessary.
		GCCSI3.registerOutput("Order of Battle", new Metadata());
		GCCSI3.addEmittedObject("Order of Battle", ORB);
		
		MAAP.registerOutput("ATOs and Coordination", new Metadata());
		MAAP.addEmittedObject("ATOs and Coordination", ATOs); 

		MAAP.registerInput("Joint Targeting Toolkit (JTT)", new Metadata());
		MAAP.registerInput("Order of Battle", new Metadata()); 
		MAAP.registerInput("Web Airspace Deconfliction", new Metadata()); 
		
		IRIS.registerOutput("ATO1", new Metadata()); 
		IRIS.registerOutput("ATO2", new Metadata());
		IRIS.registerOutput("ATO3", new Metadata()); 
		
		IRIS.addEmittedObject("ATO1", ATO1);
		IRIS.addEmittedObject("ATO2", ATO2);
		IRIS.addEmittedObject("ATO3", ATO3); 
		
		IRIS.registerInput("ATOs and Coordination", new Metadata()); 
		GCCSJ.registerInput("ATO1", new Metadata()); 
		ESTAT.registerInput("ATO2", new Metadata()); 
		AAT.registerInput("ATO3", new Metadata()); 
		
		Transition tSTART_GCCSI3 = new Transition(Activity.START_STATE, GCCSI3, "START STATE", null); 

		Transition tGCCSI3_MAAP = new Transition(GCCSI3, MAAP, "Order of Battle", "Order of Battle"); 
		Transition tMAAP_IRIS = new Transition(MAAP, IRIS, "ATOs and Coordination", "ATOs and Coordination"); 
		Transition tIRIS_GCCSJ = new Transition(IRIS, GCCSJ, "ATO1", "ATO1");
		Transition tIRIS_ESTAT = new Transition(IRIS, ESTAT, "ATO2", "ATO2");
		Transition tIRIS_AAT = new Transition(IRIS, AAT, "ATO3", "ATO3"); 

		tSTART_GCCSI3.register();
		tGCCSI3_MAAP.register();
		tMAAP_IRIS.register();
		tIRIS_ESTAT.register();
		tIRIS_GCCSJ.register();
		tIRIS_AAT.register();
						
		w.addPrecomputedObject("Web Airspace Deconfliction", WEBAD);
		w.addPrecomputedObject("Joint Targeting Toolkit (JTT)", JTT);
		
		w.execute(Activity.START_STATE);
		
		System.out.println("Done");		
	} // End TBMCSWorkflow
	
	public ProvenanceCollection getCollection() {
		return tracer.getCollection();
	}
} // End TBMCSWorkflow
