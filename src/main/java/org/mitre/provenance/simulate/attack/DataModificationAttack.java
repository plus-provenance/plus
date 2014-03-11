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
package org.mitre.provenance.simulate.attack;

import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSInvocation;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.PLUSWorkflow;

/**
 * Attack where an extra thread is added to the graph to represent illicit modification
 * of data, and re-injection into the workflow.
 * @author DMALLEN
 */
public class DataModificationAttack extends BaseGraph {
	public DataModificationAttack() throws Exception { 
		super();
		attack();
	}
	
	protected void attack() throws Exception { 
		addDefaultNodes();
		addDefaultEdges();
		
		PLUSObject modify = new PLUSInvocation("MODIFY");
		PLUSObject x = new PLUSString("X");		 
		
		col.addNode(modify);
		col.addNode(x);		
		
		col.addEdge(new PLUSEdge(pdu, modify, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_INPUT_TO));		
		col.addEdge(new PLUSEdge(modify, x, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_GENERATED));				
		col.addEdge(new PLUSEdge(x, filter1, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_INPUT_TO)); 		
		col.addEdge(new PLUSEdge(x, filter2, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_INPUT_TO));
		
		sever(pdu.getId(), filter1.getId());
		sever(pdu.getId(), filter2.getId()); 
	} // End attack
} // End DataModificationAttack
