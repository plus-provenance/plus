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

import java.util.HashSet;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.npe.NonProvenanceEdge;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.user.User;

public class TestNonProvenance {
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// Let's create three items, A B and C, and connect them all via simple edges.
		// All three of these will just be dumb strings.  There are all kinds of different
		// types of objects you might want to create, but this is the easiest for now.
		PLUSString A = new PLUSString("A", "I am a simple string named A");
		PLUSString B = new PLUSString("B", "I am a simple string named B");

		LocalProvenanceClient client = new LocalProvenanceClient();
		
		client.report(ProvenanceCollection.collect(A, B));
		
		try {
			NonProvenanceEdge failureEdge = new NonProvenanceEdge(A.getId(), "awjawejklf", 
															NonProvenanceEdge.NPE_TYPE_CONTAINMENT);			
			client.report(ProvenanceCollection.collect(failureEdge));
		}
		catch (PLUSException ex) {
			System.err.println("Caught expected exception when trying to add a NonProvenanceEdge between two non-PLUSObjects.");
			System.err.println(ex.getMessage());
		}
		
		// Create a non-provenance edge from A -> B.
		// EDGE_TYPE_CONTAINS means that object A contains object B
		NonProvenanceEdge AtoB = new NonProvenanceEdge(A.getId(),   // The edge goes from A... 
				                     				   B.getId(),   // to B....
				                     				   NonProvenanceEdge.NPE_TYPE_CONTAINMENT);
				
		client.report(ProvenanceCollection.collect(AtoB));

		for(String anIdentifier : new String[] {A.getId(), B.getId() }) {
			AtoB = null;
			HashSet<String> identifiers = new HashSet<String>();
			identifiers.add(anIdentifier);
						
			ProvenanceCollection col = Neo4JPLUSObjectFactory.getIncidentEdges(identifiers, User.DEFAULT_USER_GOD);
			if (col.countNPEs() == 0) { break; }
			
			for(NonProvenanceEdge anEdge : col.getNonProvenanceEdges()) {
				System.out.println("Found an edge from " + anEdge.getFrom() + " to " + anEdge.getTo()
						+ " with ID " + anEdge.getId() + ", which was created at " + anEdge.getCreatedAsDate().toString() + ".");
			}
		}
		
		System.out.println("Done!");
		System.exit(0); 
	}

}
