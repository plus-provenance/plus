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
 * Lattice graph motif.
 * @author DMALLEN
 *
 */
public class Lattice extends Motif {
	public Lattice() { 
		super("Lattice"); 
		init(); 
	}
	
	protected void init() { 
		PLUSString begin = new PLUSString("Lattice: BEGIN");
		PLUSString m1 = new PLUSString("Lattice: Middle 1"); 
		PLUSString m2 = new PLUSString("Lattice: Middle 2 ##");
		PLUSString m3 = new PLUSString("Lattice: Middle 3"); 
		PLUSString end = new PLUSString("Lattice: END"); 
		 		
		add(begin); add(m1); add(m2); add(m3); add(end); 
		addEdge(new PLUSEdge(begin, m1, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(begin, m2, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(begin, m3, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(m1, end, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(m2, end, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));		
		addEdge(new PLUSEdge(m3, end, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));		
	} // End init
} // End Lattice
