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

import java.util.HashSet;

import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSInvocation;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;

/**
 * This is a model of a base NCEL graph, re-created in code.
 * Subclasses will launch different kinds of attacks on this graph.
 * @author DMALLEN
 */
public class BaseGraph {
	PLUSObject _b; 
	PLUSObject dis;
	PLUSObject pdu;
	PLUSObject filter1; 
	PLUSObject filter2; 
	PLUSObject payload1; 
	PLUSObject payload2; 
	
	protected boolean written = false;
	
	protected ProvenanceCollection col = new ProvenanceCollection();
	
	public BaseGraph() throws Exception { 
		_b = new PLUSString("class [B");
		Thread.sleep(1); 
		dis = new PLUSInvocation("dis");
		Thread.sleep(1); 
		pdu = new PLUSString("TransmitterPDU");
		Thread.sleep(1); 
		filter1 = new PLUSString("dis_pdu_to_cot");
		Thread.sleep(1); 
		filter2 = new PLUSString("detonation_pdu_filter");
		Thread.sleep(1); 
		payload1 = new PLUSString("NullPayload");
		Thread.sleep(1); 
		payload2 = new PLUSString("TransmitterPDU"); 
	} // End BaseGraph
	
	public ProvenanceCollection getCollection() { return col; } 
	
	protected void addDefaultNodes() { 
		col.addNode(_b); 
		col.addNode(dis); 
		col.addNode(pdu);
		col.addNode(filter1); 
		col.addNode(filter2); 
		col.addNode(payload1);
		col.addNode(payload2); 
	}
	
	protected void addDefaultEdges() { 
		col.addEdge(new PLUSEdge(_b, dis, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_INPUT_TO));
		col.addEdge(new PLUSEdge(dis, pdu, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_GENERATED));
		col.addEdge(new PLUSEdge(pdu, filter1, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_INPUT_TO));
		col.addEdge(new PLUSEdge(pdu, filter2, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_INPUT_TO));
		col.addEdge(new PLUSEdge(filter1, payload1, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_GENERATED));
		col.addEdge(new PLUSEdge(filter2, payload2, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_GENERATED));		
	} // End buildDefaultEdges()
	
	/**
	 * Remove a link from one node to another.
	 * @param from
	 * @param to
	 * @return true if the link was severed, false otherwise (such as if it does not exist)
	 * @throws Exception
	 */
	public boolean sever(String from, String to) throws Exception { 
		HashSet<PLUSEdge> severSet = new HashSet<PLUSEdge>();
		
		for(PLUSEdge pe : col.getEdges()) { 
			if(pe.getFrom().equals(from) && pe.getTo().equals(to)) {
				severSet.add(pe);
				return true; 
			} // End if
		} // End for
		
		for(PLUSEdge pe : severSet) col.removeEdge(pe);
		
		return false;
	} // End 
	
	public boolean remove(PLUSObject o) throws Exception { 
		HashSet<PLUSObject> severSet = new HashSet<PLUSObject>();
		
		for(PLUSObject po : col.getNodes()) {
			if(po.getId().equals(o.getId())) {
				severSet.add(po);
				return true;
			}
		} // End for

		for(PLUSObject po : severSet) col.removeNode(po);
		
		return false;
	} // End remove
} // End BaseGraph
