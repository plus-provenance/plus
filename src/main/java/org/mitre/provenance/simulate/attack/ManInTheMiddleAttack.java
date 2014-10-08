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
import org.mitre.provenance.plusobject.PLUSWorkflow;

/**
 * Attack where extra nodes are introduced into the graph at each junction,
 * representing either monitoring, data stealing, etc. 
 * @author moxious
 *
 */
public class ManInTheMiddleAttack extends BaseGraph {
	public ManInTheMiddleAttack() throws Exception { 
		super();
		attack();
	}
	
	protected void attack() throws Exception { 
		addDefaultNodes();
		
		// Put a M-I-M node between every pair.
		intercept(_b, dis);
		intercept(dis, pdu);
		intercept(pdu, filter1);
		intercept(pdu, filter2);
		intercept(filter1, payload1);
		intercept(filter2, payload2); 
	} // End attack
	
	protected void intercept(PLUSObject one, PLUSObject two) { 
		PLUSInvocation x = new PLUSInvocation("X"); 
		col.addNode(x); 
		
		col.addEdge(new PLUSEdge(one, x, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		col.addEdge(new PLUSEdge(x, two, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_CONTRIBUTED));		
	} // End intercept
} // End ManInTheMiddleAttack
