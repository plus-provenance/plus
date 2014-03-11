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

import org.mitre.provenance.plusobject.*;

/**
 * Bottleneck graph motif.  A bottleneck is where >= 2 nodes feed into a "choke point", which feeds out to >= 2 nodes.
 * @author DMALLEN
 */
public class Bottleneck extends Motif {
	public Bottleneck() { 
		super("Bottleneck"); 
		init(); 
	} // End constructor

	protected void init() {
		PLUSString b1 = new PLUSString("Bottleneck: Before 1");
		PLUSString b2 = new PLUSString("Bottleneck: Before 2"); 
		PLUSString bn = new PLUSString("Bottleneck: Bottleneck ##"); 
		PLUSString a1 = new PLUSString("Bottleneck: After 1"); 
		PLUSString a2 = new PLUSString("Bottleneck: After 2"); 
		
		add(b1); add(b2); add(bn); add(a1); add(a2); 
		addEdge(new PLUSEdge(b1, bn, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(b2, bn, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(bn, a1, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(bn, a2, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));		
	} // End init	
} // End Bottleneck
