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

/** 
 * Inverted tree graph motif.
 * @author moxious
 */
public class InvertedTree extends Motif {
	public InvertedTree() { 
		super("Inverted Tree"); 
		init(); 
	}
	
	protected void init() { 
		PLUSString l1a = new PLUSString("Inverted Tree: Level 1a"); 
		PLUSString l1b = new PLUSString("Inverted Tree: Level 1b"); 
		PLUSString l2a = new PLUSString("Inverted Tree: Level 2a ##"); 
		PLUSString l2b = new PLUSString("Inverted Tree: Level 2b"); 
		PLUSString l3a = new PLUSString("Inverted Tree: Level 3a"); 
		
		add(l1a); add(l1b); add(l2a); add(l2b); add(l3a);  
		addEdge(new PLUSEdge(l1a, l2a, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(l1b, l2a, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(l2a, l3a, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(l2b, l3a, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));				
	} // End init
} // End InvertedTree
