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

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import java.util.logging.Logger;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.plusobject.PLUSActivity;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSInvocation;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.surrogate.SurrogateGeneratingFunction;
import org.mitre.provenance.user.PrivilegeSet;
import org.mitre.provenance.workflowengine.activity.Activity;

/**
 * This object implements the lineage tracer API and is used for capturing information about workflows
 * that are being executed.  The captured information is stored in an PLUS-specific schema within a 
 * relational database.
 * @author DMALLEN
 *
 */
public class RelationalLineageCapture extends LineageTracer {
	protected Logger log = Logger.getLogger(RelationalLineageCapture.class.getName());	
	protected int workflowID;
	protected Hashtable <String,PLUSObject> currentInputs;
	protected PLUSInvocation curInvoke; 
	protected Date start;
	protected Date end;
	protected boolean writeImmediate = false; 
	protected PLUSWorkflow tracing;
	protected ProvenanceCollection col; 
		
	public RelationalLineageCapture() {
		col = new ProvenanceCollection();		
		currentInputs = null;
		workflowID    = -1;
	} // End RelationalLineageCapture
	
	/**
	 * Get a provenance collection that contains all objects captured by this tracer so far.
	 * @return a ProvenanceCollection
	 */
	public ProvenanceCollection getCollection() { return col; } 
	
	/**
	 * Controls whether objects are immediately written to the database as soon as they 
	 * are observed.  Set to true if you want to write things as they're found; set to 
	 * false if you intend to pull the entire provenance collection out at a later time.
	 * @param writeImmediate
	 */
	public void setWriteImmediate(boolean writeImmediate) { 
		this.writeImmediate = writeImmediate; 
	}
	
	/**
	 * @see LineageTracer#startWorkflow(Workflow, Activity)
	 */	
	public void startWorkflow(Workflow wf, Activity startingPoint) {
		start = new Date();
		
		tracing = new PLUSWorkflow();
		tracing.setName(wf.getName());
		tracing.setWhenStart(start.toString());
		
	} // End startWorkflowRun

	/**
	 * @see LineageTracer#endWorkflow(Workflow, Activity)
	 */
	public void endWorkflow(Workflow wf, Activity endingPoint) { 
		end = new Date();

		tracing.setWhenEnd(end.toString());

		try { 
			col.addNode(tracing);
			if(writeImmediate) Neo4JStorage.store(tracing);
			else log.finest("Skipping write on workflow " + tracing.getName()); 
		} catch(Exception e) { 
			log.severe("RelationalLineageCapture.endWorkflow: " + e.getMessage());
			e.printStackTrace();
		}
	} // End endWorkflow

	/**
	 * @see LineageTracer#startActivity(Activity, Hashtable)
	 */
	public void startActivity(Activity current, Hashtable <String,PLUSObject> parameters) { 
		// Don't need to do anything here except keep a reference to the inputs for later ...
		currentInputs = parameters;
		
		curInvoke = new PLUSInvocation(current.getName());
		curInvoke.setId((String)current.getMetadata().get("invokeid"));
		
		Enumeration<String> e = parameters.keys();
		while(e.hasMoreElements()) { 
			String n = (String)e.nextElement();
			PLUSObject input = parameters.get(n);
			
			try { 
				col.addNode(input);
				if(writeImmediate) Neo4JStorage.store(input); 
				else log.finest("Skipping data write for " + input); 
			} catch(Exception exc) { 
				log.severe("RelationalLineageCapture.finishActivity: (data logging) " + exc);
			}
			
			curInvoke.addInput(n, input.getId());
			PLUSEdge edge = new PLUSEdge(input, curInvoke, tracing, PLUSEdge.EDGE_TYPE_INPUT_TO);
			
			try { 
				col.addEdge(edge);
				if(writeImmediate) Neo4JStorage.store(edge);
				else log.finest("Skipping edge write on " + edge);
			} catch(Exception exc) { 
				log.severe("startActivity: Error writing edge: " + exc);
				exc.printStackTrace();
			} // End catch
		} // End while
	} // End startActivity
	
	/**
	 * @see LineageTracer#transition(Transition)
	 */
	public void transition(Transition t) {		
		Activity from = t.getFrom();
		Activity to = t.getTo();
		String fromID = (String)from.getMetadata().get("invokeid");
		String toID = (String)to.getMetadata().get("invokeid");

		// Don't log START and END.
		/*
		if(from != Activity.START_STATE && to != Activity.END_STATE) {		
			PLUSEdge edge = new PLUSEdge(fromID, toID, tracing.getId(), 
					                     PLUSEdge.EDGE_TYPE_TRIGGERED);
			
			try {
				col.addEdge(edge);
				if(writeImmediate && !edge.wasWritten()) edge.writeToDB();
				else log.finest("Skipping write on edge " + edge);
			} catch(Exception e) { 
				log.severe("RelationalLineageCapture.transition: " + e, e);
			}
		} // End if
		*/
	} // End transition
	
	/**
	 * @see LineageTracer#finishActivity(Activity, Hashtable)
	 */
	public void finishActivity(Activity current, Hashtable <String,PLUSObject> results) { 
		String activityID = (String)current.getMetadata().get("id");
		String invokeID   = (String)current.getMetadata().get("invokeid");		
				
		PLUSActivity pact = new PLUSActivity(current.getName());
		pact.setId(activityID);
		pact.setInputs(currentInputs.size()); 
		pact.setOutputs(results.size());
		pact.setDescription("None available.");
		
		if(current.getMetadata().get("PrivSet") != null) { 
			try { 
				PrivilegeSet ps = (PrivilegeSet)current.getMetadata().get("PrivSet");
				log.finest("Tagged activity with PrivSet " + ps);
				curInvoke.setPrivileges(ps);
			} catch(Exception e) { 
				log.warning("Activity had bad PrivSet tag: " + current.getMetadata().get("PrivSet") + "/" + e.getMessage());
			}
		}
		
		if(current.getMetadata().get("actor") != null) { 
			try { 
				PLUSActor actor = (PLUSActor)current.getMetadata().get("actor");
				log.finest("Tagged activity with actor " + actor);
				curInvoke.setOwner(actor); 
			} catch(Exception e) { 
				log.severe("Activity had bad actor tag: " + current.getMetadata().get("actor"));
				e.printStackTrace(); 
			} // End catch
		} // End if
		
		if(current.getMetadata().get("SGF") != null) {
			try { 
				SurrogateGeneratingFunction c = (SurrogateGeneratingFunction)current.getMetadata().get("SGF");
				log.finest("Tagged activity with SGF " + c); 
				curInvoke.useSurrogateComputation(c);
			} catch(Exception e) { 
				log.severe("Activity had bad SGF tag: " + current.getMetadata().get("SGF"));
			}
		} // End if
		
		Enumeration<String> e = results.keys();
		while(e.hasMoreElements()) { 
			String inName = (String)e.nextElement();
			PLUSObject result = results.get(inName);
			curInvoke.addOutput(inName, result.getId());
						
			try { 
				col.addNode(result);
				if(writeImmediate) Neo4JStorage.store(result); 
				else log.finest("Skipping data write for " + result); 
			} catch(Exception exc) { 
				log.severe("finishActivity: (data logging) " + exc.getMessage());
			}
			
			PLUSEdge edge = new PLUSEdge(curInvoke, result, tracing, PLUSEdge.EDGE_TYPE_GENERATED);
			try { col.addEdge(edge); if(writeImmediate) Neo4JStorage.store(edge); }
			catch(Exception exc) { 
				log.severe("finishActivity: Failed writing edge " + exc.getMessage());
			} // End catch			
		} // End while
				
		// Write the activity....
		try { 
			col.addNode(pact);
			if(writeImmediate) Neo4JStorage.store(pact);
			else log.finest("finishActivity: Skipping write on activity " + pact.getName()); 
		} catch(Exception exc) { 
			log.severe("RelationalLineageCapture.finishActivity: " + exc.getMessage());
		} // End catch
		
		// Finish up setting some fields so that things line up properly in the DB
		curInvoke.setId(invokeID);
		curInvoke.setActivity(pact);
		curInvoke.setWorkflow(tracing);		
		curInvoke.getMetadata().copy(current.getMetadata());  
		
		// Write the invocation.
		try { 
			col.addNode(curInvoke);
			if(writeImmediate) Neo4JStorage.store(curInvoke);
			else log.finest("finishActivity: Skipping write on invocation " + curInvoke.getName()); 
		} catch(Exception exc) { 
			log.severe(exc.getMessage());
		}
	} // End finishActivity	
} // End RelationalLineageCapture
