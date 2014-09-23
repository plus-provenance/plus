package org.mitre.provenance.test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.client.ProvenanceClient;
import org.mitre.provenance.dag.TraversalSettings;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.npe.NonProvenanceEdge;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.ProvenanceCollection;

public class TestNPEs {
    @Before
    public void setUp() {
    	Neo4JStorage.initialize();
        ProvenanceClient.instance = new LocalProvenanceClient();
    }
	
	@Test
	public void testNPEs() throws PLUSException {
		ProvenanceCollection col = new ProvenanceCollection();
		
		PLUSString a = new PLUSString("A");
		PLUSString b = new PLUSString("B");
		PLUSString c = new PLUSString("C");
		
		col.addNode(a); 
		col.addNode(b);
		col.addNode(c);
		
		NonProvenanceEdge prov2ProvNPE = new NonProvenanceEdge(a, b, "FooEdge");
		NonProvenanceEdge prov2ExternalNPE = new NonProvenanceEdge(a.getId(), "SomeExternalID", "hasExternalId");
		
		col.addNonProvenanceEdge(prov2ProvNPE);
		col.addNonProvenanceEdge(prov2ExternalNPE);
		col.addEdge(new PLUSEdge(a, c)); 
				
		ProvenanceClient.instance.report(col);
		
		TraversalSettings s = new TraversalSettings().includeNPEs().includeEdges().includeNodes();
		ProvenanceCollection reloaded = ProvenanceClient.instance.getGraph(a.getId(), s);
		
		assertTrue("NPEs can be saved/loaded between PLUSObjects", checkContainsNPE(reloaded, prov2ProvNPE));
		assertTrue("External NPEs work too", checkContainsNPE(reloaded, prov2ExternalNPE));		
	} // End testNPEs
	
	public boolean checkContainsNPE(ProvenanceCollection col, NonProvenanceEdge base) {
		for(NonProvenanceEdge npe : col.getNonProvenanceEdges()) {
			if(npe.getFrom().equals(base.getFrom()) && 
			   npe.getTo().equals(base.getTo()) &&
			   npe.getType().equals(base.getType())) return true;
		}
		
		return false;
	}
}
