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
 * Attack where one extra side-thread is added representing theft of a data item.
 * @author DMALLEN
 *
 */
public class DataStealingAttack extends BaseGraph {
	public DataStealingAttack() throws Exception { 
		super();
		attack();
	}
	
	protected void attack() { 
		addDefaultNodes();
		addDefaultEdges();
		
		PLUSObject capture = new PLUSInvocation("Capture");
		PLUSObject x = new PLUSString("X");
		PLUSObject transmit = new PLUSInvocation("Transmit"); 
		
		col.addNode(capture);
		col.addNode(x);
		col.addNode(transmit);
		
		col.addEdge(new PLUSEdge(pdu, capture, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_INPUT_TO));
		col.addEdge(new PLUSEdge(capture, x, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_GENERATED));
		col.addEdge(new PLUSEdge(x, transmit, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_INPUT_TO)); 		
	} // End attack
} // End DataStealingAttack
