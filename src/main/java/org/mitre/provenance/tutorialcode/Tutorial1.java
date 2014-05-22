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
package org.mitre.provenance.tutorialcode;

import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;

/**
 * <b>Tutorial 1</b> - creating basic provenance graph in the PLUS store.
 * 
 * <p>This particular tutorial creates a simple string of items in the provenance store.  The intent of 
 * this tutorial is to show the basic graph structure of provenance, and how to create simple nodes and 
 * edges in a directed graph.
 *    
 * @author moxious
 */
public class Tutorial1 {
	public static void main(String [] args) throws Exception { 
		// Let's create three items, A B and C, and connect them all via simple edges.
		// All three of these will just be dumb strings.  There are all kinds of different
		// types of objects you might want to create, but this is the easiest for now.
		PLUSString A = new PLUSString("A", "I am a simple string named A");
		PLUSString B = new PLUSString("B", "I am a simple string named B");
		PLUSString C = new PLUSString("C", "I am a simple string named C");
		
		// Create an edge from A -> B.
		// The DEFAULT_WORKFLOW_OID means that this edge wasn't a part of some formal,
		// pre-identified workflow.  
		// EDGE_TYPE_CONTRIBUTED means that there's a vague relationship between A and B....
		// A "contributed" to B.  See PLUSEdge for other types of edges.  You can also create
		// an edge with any kind of String as a type.
		PLUSEdge AtoB = new PLUSEdge(A,   // The edge goes from A... 
				                     B,   // to B....
				                     PLUSWorkflow.DEFAULT_WORKFLOW, 
				                     PLUSEdge.EDGE_TYPE_CONTRIBUTED);
		
		// Create an edge from B -> C.
		PLUSEdge BtoC = new PLUSEdge(B,
				                     C,
				                     PLUSWorkflow.DEFAULT_WORKFLOW,
				                     PLUSEdge.EDGE_TYPE_CONTRIBUTED);
	
		
		// Create a client to talk to the local database.
		LocalProvenanceClient client = new LocalProvenanceClient();
		
		// Create a provenance collection to hold the items we're reporting.
		ProvenanceCollection col = new ProvenanceCollection();
		
		col.addNode(A);
		col.addNode(B);
		col.addNode(C);
		col.addEdge(AtoB);
		col.addEdge(BtoC);
		
		// Report the collection.   This actually stores the whole thing in the local database.
		client.report(col);
				
		System.out.println("Done!");		
		System.exit(0); 
	} // End main
} // End Tutorial1
