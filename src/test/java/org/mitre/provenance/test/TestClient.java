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
package org.mitre.provenance.test;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.client.AbstractProvenanceClient;
import org.mitre.provenance.client.ProvenanceClient;
import org.mitre.provenance.client.ProvenanceClientException;
import org.mitre.provenance.client.RESTProvenanceClient;
import org.mitre.provenance.npe.NonProvenanceEdge;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.ProvenanceCollection;

public class TestClient {
    @Before
    public void setUp() throws ProvenanceClientException {
        ProvenanceClient.instance = new RESTProvenanceClient("localhost", "8080");
    }
	
	@Test
	public void testClient() throws PLUSException {
		ProvenanceCollection col = new ProvenanceCollection();
		PLUSString s = new PLUSString("Foo", "Bar");
		PLUSString t = new PLUSString("Baz", "Quux");
		PLUSEdge e = new PLUSEdge(s, t);
		NonProvenanceEdge npe = new NonProvenanceEdge(s, t, "blah");
		
		col.addNode(s);
		col.addNode(t);
		col.addEdge(e);
		col.addNonProvenanceEdge(npe);		
		
		PLUSActor someOwner = new PLUSActor("Some owner");
		s.setOwner(someOwner);
		t.setOwner(someOwner);
		col.addActor(someOwner);
		
		AbstractProvenanceClient pc = ProvenanceClient.instance;
		System.out.println("Reporting collection " + col);
		assertTrue("Can report successfully to server", pc.report(col));
		
		System.out.println("Fetching graph...");
		ProvenanceCollection fetched = pc.getGraph(s.getId());
		System.out.println("Fetched " + fetched);
		//ProvenanceCollection fetched = col;

		assertTrue("All items logged", col.countNodes() == fetched.countNodes());
		assertTrue("All edges logged", col.countEdges() == fetched.countEdges());
		assertTrue("All NPEs logged", col.countNPEs() == fetched.countNPEs());
		assertTrue("All actors logged", col.countActors() == fetched.countActors());
	}
}
