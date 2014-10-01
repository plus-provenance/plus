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

import java.util.List;

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
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;

public class TestClient {
    @Before
    public void setUp() throws ProvenanceClientException {
        ProvenanceClient.instance = new RESTProvenanceClient("localhost", "8080");
    }
	
    @Test
    public void testActors() throws PLUSException { 
    	ProvenanceCollection col = ProvenanceClient.instance.getActors();
    	assertTrue("Actors are not empty", col.countActors() > 0);
    }
    
    @Test
    public void testLatest() throws PLUSException { 
    	ProvenanceCollection col = ProvenanceClient.instance.latest();
    	assertTrue("Latest objects reported aren't empty", col.countNodes() > 0);
    }
    
    @Test
    public void testWorkflows() throws PLUSException { 
    	List<PLUSWorkflow> workflows = ProvenanceClient.instance.listWorkflows(10);
    	
    	assertTrue("Workflows came back", workflows != null);
    	assertTrue("More than zero workflows came back", workflows.size() > 0);
    	assertTrue("Server didn't respond with too many", workflows.size() <= 10); 
    }
    
	@Test
	/**
	 * This test does quite a lot...we can't necessarily make assumptions about what will be in a store during
	 * the testing phase.  So here we're reporting stuff; and all of the services that require knowing something
	 * ahead of time are tested here.
	 * @throws PLUSException
	 */
	public void testReporting() throws PLUSException {
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
		
		PLUSActor locatedActor = ProvenanceClient.instance.actorExists(someOwner.getId());
		
		assertTrue("Logged actor " + someOwner.getId() + " can be found.", locatedActor != null);
		assertTrue("Logged actor " + someOwner.getId() + " name matches", locatedActor.getName().equals(someOwner.getName()));
		
		ProvenanceCollection searchResults = ProvenanceClient.instance.search("Foo", 10);
		assertTrue("Received search results", searchResults != null);
		assertTrue("Found something for 'Foo'", searchResults.countNodes() > 0);
		assertTrue("Total search results (" + searchResults.countNodes() + ") should be <= 10", searchResults.countNodes() <= 10); 
	}
}
