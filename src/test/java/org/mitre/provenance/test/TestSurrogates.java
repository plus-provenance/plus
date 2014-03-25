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

import static org.junit.Assert.*;

import org.junit.Test;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.dag.LineageDAG;
import org.mitre.provenance.dag.ViewedCollection;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.surrogate.sgf.GenericSGF;
import org.mitre.provenance.surrogate.sgf.NodePlaceholderInferAll;
import org.mitre.provenance.user.PrivilegeClass;
import org.mitre.provenance.user.User;

public class TestSurrogates {
	public PLUSObject genObject() {
		PLUSString s = new PLUSString("Foo", "Bar");
		s.useSurrogateComputation(new GenericSGF());
		s.getPrivileges().addPrivilege(PrivilegeClass.ADMIN);
		return s;
	}
	
	@Test
	public void testSurrogateGeneration() {
		PLUSObject s = TestUtils.genProtectedObject();
		
		// Public isn't allowed to see this item.
		ViewedCollection vc = new ViewedCollection(User.PUBLIC);
		vc.addNode(s);
		
		PLUSObject out = vc.getNode(s.getId());
		
		System.err.println("OUT TYPE " + out.getType() + " S TYPE " + s.getType() + " EQUAL=" + out.getType().equals(s.getType()));
		
		assertTrue("Result was a surrogate", out.isSurrogate());
		assertTrue("Types are the same", out.getType().equals(s.getType()));
	}
	
	@Test 
	public void testSurrogateAlgorithm() throws PLUSException { 		
		PLUSObject a = TestUtils.genObject();
		PLUSObject b = TestUtils.genProtectedObject(new NodePlaceholderInferAll());
		PLUSObject c = TestUtils.genObject();		
		
		// We're testing the graph a -> b -> c, where b is inferred.
		// So after the surrogate algorithm, we expect the graph a -> c;
		
		ProvenanceCollection pc = new ProvenanceCollection();
		pc.addNode(a); pc.addNode(b); pc.addNode(c); 
		pc.addEdge(new PLUSEdge(a, b));
		pc.addEdge(new PLUSEdge(b, c)); 
		
		// System.out.println("Any edges for " + b + "? => " + pc.getEdgesByNode(b.getId()));
		
		LineageDAG dag = LineageDAG.fromCollection(pc, User.PUBLIC);
		
		for(PLUSObject o : dag.getNodes()) {
			System.out.println(o); 
		}
		
		System.out.println("And now the edges...");
		for(PLUSEdge e : dag.getEdges()) {
			//System.out.println(e.getFrom().getName() + " => " + e.getTo().getName());
			System.out.println("RESULTING EDGE: "  + e);
		}
		
		assertTrue("Resulting graph only has one edge", dag.getEdges().size() == 1);
		assertTrue("Resulting graph has two nodes", dag.getNodes().size() == 2); 
		assertTrue("Inferred edge created correctly", dag.getFLING(a).contains(c));
	}	
} // End TestSurrogates
