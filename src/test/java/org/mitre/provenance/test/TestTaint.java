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

import java.util.Set;

import org.junit.Test;
import org.mitre.provenance.client.AbstractProvenanceClient;
import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.plusobject.marking.Taint;
import org.mitre.provenance.simulate.SyntheticGraphProperties;
import org.mitre.provenance.simulate.motif.RandomMotifCollection;
import org.mitre.provenance.surrogate.sgf.GenericSGF;
import org.mitre.provenance.user.PrivilegeSet;
import org.mitre.provenance.user.User;

public class TestTaint {
	AbstractProvenanceClient client = new LocalProvenanceClient();
	
	@Test
	public void testTaint() throws Exception {		
		// Generate a bunch of random stuff.
		SyntheticGraphProperties p = new SyntheticGraphProperties().setComponents(3).setSGF(new GenericSGF()).setPrivilegeSet(new PrivilegeSet());
		RandomMotifCollection rmc = new RandomMotifCollection(p);
		
		System.out.println("Storing test collection.");
		client.report(rmc);
		
		// Pick the very first thing in the graph, and taint it (ensuring most everything else is tainted)
		PLUSObject earliest = rmc.getNodesInOrderedList(ProvenanceCollection.SORT_BY_CREATION).get(0);
		
		// This creates and saves the taint...
		System.out.println("Tainting first item.");
		Taint t = Neo4JPLUSObjectFactory.taint(earliest, User.DEFAULT_USER_GOD, "OMG! Totes tainted!");
		
		System.out.println("Taint claimant:  " + t.getClaimant() + " and " + t.getStorableProperties().get("claimant"));
		
		Set<Taint> taints = Neo4JPLUSObjectFactory.getDirectTaints(earliest, User.DEFAULT_USER_GOD);
				
		assertTrue("Direct taint works", taints.size() == 1 && taints.iterator().next().getId().equals(t.getId())); 
		
		for(PLUSEdge e : rmc.getOutboundEdgesByNode(earliest.getId())) {
			ProvenanceCollection indirectTaints = Neo4JPLUSObjectFactory.getIndirectTaintSources(e.getTo(), User.DEFAULT_USER_GOD);
			
			// Can't use the contains method on the indirectTaints object because that requires references to be the same.  Instead
			// do a weak compare.
			boolean contains = false;
			for(PLUSObject obj : indirectTaints.getNodes()) {
				if(t.getId().equals(obj.getId())) {
					contains = true;
					break;
				}
			}
			
			assertTrue("Indirect taint works", contains);						
			assertTrue("Indirectly tainted things don't show as directly tainted.",
					Neo4JPLUSObjectFactory.getDirectTaints(e.getTo(), User.DEFAULT_USER_GOD).isEmpty());
		}		
	}
}
