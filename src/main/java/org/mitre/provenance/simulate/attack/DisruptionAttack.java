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
import org.mitre.provenance.plusobject.PLUSWorkflow;

/**
 * Disruption attack where the graph is drastically cut off because resources are
 * disrupted or taken off line.
 * @author moxious
 *
 */
public class DisruptionAttack extends BaseGraph {
	public DisruptionAttack() throws Exception { 
		super();
		attack();
	}
	
	protected void attack() { 
		col.addNode(_b);
		col.addNode(dis); 
		col.addEdge(new PLUSEdge(_b, dis, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_INPUT_TO)); 
	} // End attack
} // End DisruptionAttack
