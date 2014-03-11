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
package org.mitre.provenance.simulate.motif;

import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSString;

public class Tree extends Motif {
	public Tree() { 
		super("Tree"); 
		init(); 
	}
	
	protected void init() { 
		PLUSString l1 = new PLUSString("Tree: Level 1"); 
		PLUSString l2a = new PLUSString("Tree: Level 2a ##"); 
		PLUSString l2b = new PLUSString("Tree: Level 2b"); 
		PLUSString l3a = new PLUSString("Tree: Level 3a"); 
		PLUSString l3b = new PLUSString("Tree: Level 3b"); 
		
		add(l1); add(l2a); add(l2b); add(l3a); add(l3b); 
		addEdge(new PLUSEdge(l1, l2a, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(l1, l2b, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(l2a, l3a, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(l2a, l3b, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));		
	} // End init
} // End Tree
