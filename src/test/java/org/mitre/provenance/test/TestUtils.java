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
import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.client.ProvenanceClient;
import org.mitre.provenance.npe.NonProvenanceEdge;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.surrogate.SurrogateGeneratingFunction;
import org.mitre.provenance.surrogate.sgf.GenericSGF;
import org.mitre.provenance.user.PrivilegeClass;

public class TestUtils {	
    @Before
    public void setUp() {
        ProvenanceClient.instance = new LocalProvenanceClient();
    }
    
	public static void equivalent(ProvenanceCollection a, ProvenanceCollection b) {
		assertTrue("Collections have same number of nodes", a.countNodes() == b.countNodes());
		assertTrue("Collections have same number of edges", a.countEdges() == b.countEdges());
		assertTrue("Collections have same number of NPEs", a.countNPEs() == b.countNPEs());
		assertTrue("Collections have same number of actors", a.countActors() == b.countActors());
		
		for(PLUSObject ai : a.getNodes()) {
			assertTrue("b has a's object (" + ai + ")", b.containsObjectID(ai.getId()));
		}
		
		for(PLUSEdge e : a.getEdges()) {
			assertTrue("b has a's edge (" + e + ")", b.contains(e));
		}
		
		for(NonProvenanceEdge npe : a.getNonProvenanceEdges()) {
			assertTrue("b has a's NPE", b.contains(npe));			
		}
		
		for(PLUSActor ai : a.getActors()) {
			assertTrue("b has a's actor (" + ai + ")", b.containsActorID(ai.getId()));
			assertTrue("a and b actors are equivalent", equivalent(ai, b.getActor(ai.getId()))); 
		}
	}
	
	public static boolean equivalent(NonProvenanceEdge a, NonProvenanceEdge b) {
		assertTrue("NPIDs are equal", a.getId().equals(b.getId()));
		assertTrue("Types are equal", a.getType().equals(b.getType()));
		assertTrue("FROM is equal", a.getFrom().equals(b.getFrom()));
		assertTrue("TO is equal", a.getTo().equals(b.getTo()));
		return true;
	}
	
	public static boolean equivalent(PLUSActor a, PLUSActor b) { 
		assertTrue("names are equal", a.getName().equals(b.getName()));
		assertTrue("types are equal", a.getType().equals(b.getType()));
		assertTrue("Created is equal", a.getCreated() == b.getCreated());
		assertTrue("ids are equal", a.getId().equals(b.getId()));
		return true;
	}
	
	public static void equivalent(PLUSEdge a, PLUSEdge b) {
		assertTrue("FROM Edge IDs equal", a.getFrom().getId().equals(b.getFrom().getId()));
		assertTrue("TO edge IDs equal", a.getTo().getId().equals(b.getTo().getId()));
		assertTrue("Edge type equal", a.getType().equals(b.getType()));
		assertTrue("Workflows equal", a.getWorkflow().getId().equals(b.getWorkflow().getId()));
	}
	
	public static void equivalent(PLUSObject a, PLUSObject b) {
		assertTrue("Names equal", a.getName().equals(b.getName()));
		assertTrue("Types equal", a.getType().equals(b.getType()));
		assertTrue("IDs equal", a.getId().equals(b.getId()));
		assertTrue("Created equal", a.getCreated() == b.getCreated());
		
		assertTrue("PrivSet size", a.getPrivileges().getPrivilegeSet().size() == b.getPrivileges().getPrivilegeSet().size());
		assertTrue("SGF set size", a.getSGFs().size() == b.getSGFs().size());
		
		for(PrivilegeClass pc : a.getPrivileges().getPrivilegeSet()) {
			assertTrue("Contains PrivClass " + pc, b.getPrivileges().getPrivilegeSet().contains(pc));
		}
		
		for(String s : a.getSGFs()) {
			assertTrue("Contains SGF " + s, b.getSGFs().contains(s));
		}
		
		assertTrue("Uncertainty equal", a.getUncertainty().equals(b.getUncertainty()));
		
		assertTrue("Owner equal", (a.getOwner() == null && b.getOwner() == null) ||
				   (a.getOwner().equals(b.getOwner())));
		
		assertTrue("Metadata equal", a.getMetadata().equals(b.getMetadata()));
	}
	
	public static PLUSObject genObject() {
		return new PLUSString("UnProtected", "Object");
	}
	
	public static PLUSObject genProtectedObject() {
		return genProtectedObject(new GenericSGF());
	}
	
	public static PLUSObject genProtectedObject(SurrogateGeneratingFunction sgf) {
		PLUSString s = new PLUSString("Protected", "Object");
		s.useSurrogateComputation(sgf);
		s.getPrivileges().addPrivilege(PrivilegeClass.ADMIN);
		return s;
	}
}
