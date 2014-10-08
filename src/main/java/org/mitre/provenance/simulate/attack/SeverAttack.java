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

/**
 * Attack where the base graph is severed into two distinct graphs by removing a single edge.
 * @author moxious
 */
public class SeverAttack extends BaseGraph {
	public SeverAttack() throws Exception { 
		super();
		attack();
	}
	
	private void attack() throws Exception { 
		addDefaultNodes();
		addDefaultEdges();
		sever(pdu.getId(), filter2.getId()); 
	} // End attack
} // End SeverAttack
