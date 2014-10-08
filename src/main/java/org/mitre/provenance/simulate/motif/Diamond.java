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

import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSString;

/**
 * Diamond graph motif.   A diamond is where 1 node feeds downstream to >= 2 nodes, which then re-converge to one downstream node.
 * @author moxious
 *
 */
public class Diamond extends Motif {
	public Diamond() { 
		super("Diamond"); 
		init(); 
	}

	protected void init() {
		PLUSString begin = new PLUSString("Diamond: BEGIN");
		PLUSString m1 = new PLUSString("Diamond: Middle 1 ##"); 
		PLUSString m2 = new PLUSString("Diamond: Middle 2"); 
		PLUSString end = new PLUSString("Diamond: END"); 
		 		
		add(begin); add(m1); add(m2); add(end); 
		addEdge(new PLUSEdge(begin, m1, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(begin, m2, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(m1, end, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		addEdge(new PLUSEdge(m2, end, getWorkflow(), PLUSEdge.EDGE_TYPE_CONTRIBUTED));		
	} // End init	

	public static void main(String [] args) throws Exception { 
		Diamond d = new Diamond();
		new LocalProvenanceClient().report(d); 		
	}
} // End Diamond
