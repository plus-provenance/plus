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
import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.client.ProvenanceClient;
import org.mitre.provenance.dag.TraversalSettings;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.simulate.SyntheticGraphProperties;
import org.mitre.provenance.simulate.motif.RandomMotifCollection;
import org.mitre.provenance.surrogate.sgf.SurgicalInferAll;
import org.mitre.provenance.user.PrivilegeClass;
import org.mitre.provenance.user.PrivilegeSet;
import org.mitre.provenance.user.User;

public class TestBasicProvenance {
    @Before
    public void setUp() {
    	Neo4JStorage.initialize();
        ProvenanceClient.instance = new LocalProvenanceClient();
    }
	
	@Test
	public void testSearch() throws Exception { 
		ProvenanceCollection col = Neo4JPLUSObjectFactory.searchFor("track", User.DEFAULT_USER_GOD, 20);
		
		// System.out.println("Searching for track yielded " + col.countNodes() + " results."); 
		for(PLUSObject o : col.getNodes()) {
			assertTrue("Name contains track", o.getName().toLowerCase().contains("track"));
		}
	}
	
	@Test
	public void testPCs() throws Exception { 
		List<PrivilegeClass> pcs = Neo4JPLUSObjectFactory.listPrivilegeClasses();
		assertTrue("Privilege classes loaded", pcs.size() > 0);
	}
	
	@Test
	public void testCollections() throws Exception {		
		// Create a sample collection.
		PrivilegeSet ps = new PrivilegeSet();
		ps.addPrivilege(PrivilegeClass.PUBLIC);
		
		SyntheticGraphProperties p = new SyntheticGraphProperties().setComponents(20).setSGF(new SurgicalInferAll()).setPrivilegeSet(ps);
		RandomMotifCollection rmc = new RandomMotifCollection(p);
		
		int nodes = 0;
		int edges = 0;
		
		for(PLUSObject o : rmc.getNodes()) {
			nodes++;
			assertTrue("Collection owns its own node", rmc.contains(o));
			assertTrue("Collection owns own node ID", rmc.containsObjectID(o.getId()));
		}
		
		for(PLUSEdge e : rmc.getEdges()) {
			edges++;
			
			assertTrue("Edge objects agree with node collection: from", 
					   (e.getFrom() != null && rmc.contains(e.getFrom())));
			assertTrue("Edge objects agree with node collection: to", 
					   (e.getTo() != null && rmc.contains(e.getTo())));			
		}
		
		assertTrue("Node size reported correctly", rmc.countNodes() == nodes);
		assertTrue("Edge size reported correctly", rmc.countEdges() == edges); 		
	}
	
	@Test
	public void testRetrieval() throws Exception { 
		ProvenanceClient.instance.listWorkflows(5);
		
		List<PLUSWorkflow> wfs = ProvenanceClient.instance.listWorkflows(5);		
		ProvenanceCollection objs = Neo4JPLUSObjectFactory.getRecentlyCreated(User.DEFAULT_USER_GOD, 5);
		ProvenanceCollection actors = Neo4JStorage.getActors(5);

		assertTrue("Can get latest workflows", (wfs != null && wfs.size() > 0));
		assertTrue("Can get latest objects", (objs != null && objs.countNodes() > 0));
		assertTrue("Can get actors", (actors != null && actors.getActors().size() > 0)); 
		
		// System.out.println("Fetching members of workflow " + wfs.get(0)); 
		for(PLUSWorkflow wf : wfs) {
			ProvenanceCollection col = ProvenanceClient.instance.getWorkflowMembers(wf.getId(), 5);
			assertTrue("Can fetch workflow members", (col != null));
		}
			
		PLUSObject firstObj = objs.getNodesInOrderedList().get(0);		
		ProvenanceCollection col = ProvenanceClient.instance.getGraph(firstObj.getId(), new TraversalSettings());
		
		assertTrue("Can fetch a DAG", (col != null && col.countNodes() > 0));
	}
}
