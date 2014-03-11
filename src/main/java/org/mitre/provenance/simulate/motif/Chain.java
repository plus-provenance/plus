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
 * Chained graph motif.   A chain is just a linear progression of >= 2 nodes.
 * @author DMALLEN
 */
public class Chain extends Motif {
	public Chain() { 
		super("Chain");
		init(); 
	}
	
	protected void init() { 
		PLUSString o1 = new PLUSString("Chain: 1");
		PLUSString o2 = new PLUSString("Chain: 2"); 
		PLUSString o3 = new PLUSString("Chain: 3 ##"); 
		PLUSString o4 = new PLUSString("Chain: 4"); 
		PLUSString o5 = new PLUSString("Chain: 5");
		
		add(o1); add(o2); add(o3); add(o4); add(o5); 
		addEdge(new PLUSEdge(o1, o2, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(o2, o3, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(o3, o4, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(o4, o5, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
	} // End init
} // End Chain
