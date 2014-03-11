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
 * Bipartite graph motif.
 * @author DMALLEN
 */
public class Bipartite extends Motif {
	public Bipartite() { 
		super("Bipartite"); 
		init(); 
	}
	
	protected void init() { 	
		PLUSString l1a = new PLUSString("Bipartite: Level 1a"); 
		PLUSString l1b = new PLUSString("Bipartite: Level 1b"); 
		PLUSString l1c = new PLUSString("Bipartite: Level 1c"); 
		PLUSString l2a = new PLUSString("Bipartite: Level 2a ##"); 
		PLUSString l2b = new PLUSString("Bipartite: Level 2b"); 
		
		add(l1a); add(l1b); add(l1c); add(l2a); add(l2b);   
		addEdge(new PLUSEdge(l1a, l2a, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(l1b, l2a, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(l1b, l2b, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(l1c, l2a, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(l1c, l2b, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
	} // End init
}
