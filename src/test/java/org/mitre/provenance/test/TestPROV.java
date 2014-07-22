package org.mitre.provenance.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.junit.Test;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.client.ProvenanceClient;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.plusobject.prov.PROVConverter;
import org.mitre.provenance.plusobject.prov.PROVConverter.Format;
import org.mitre.provenance.user.User;
import org.openprovenance.prov.model.Document;

/**
 * Test serialization of ProvenanceCollection objects to PROV-XML.
 * @author moxious
 */
public class TestPROV {	
	private ProvenanceCollection col = null;
	
    @Before
    public void setUp() {
        ProvenanceClient.instance = new LocalProvenanceClient();
    }
	
	@Test
	public void dumpPROV() throws PLUSException, JAXBException, IOException {
		LocalProvenanceClient client = new LocalProvenanceClient(User.DEFAULT_USER_GOD);
				
		List<PLUSWorkflow> wfs = client.listWorkflows(20);
		
		for(PLUSWorkflow wf : wfs) { 
			System.out.println("Dumping " + wf + " as PROV"); 
			
			col = client.getWorkflowMembers(wf.getId(), 2);			
			
			if(col.countNodes() <= 0) { 
				System.out.println("Workflow " + wf + " has no members.");
				continue;
			}
			
			col = client.getGraph(col.getNodesInOrderedList().get(0).getId());
			
			PROVConverter c = new PROVConverter();
			Document d = c.provenanceCollectionToPROV(col);
			
			String provXML = PROVConverter.consume(PROVConverter.formatAs(Format.XML, d));
			assertTrue("ProvCollection " + col + " can be serialized to PROV-XML", (provXML != null && !"".equals(provXML)));
			
			String provRDF = PROVConverter.consume(PROVConverter.formatAs(Format.RDF, d));
			assertTrue("ProvCollection " + col + " can be serialized to PROV-RDF", (provRDF != null && !"".equals(provRDF)));
			
			String provTTL = PROVConverter.consume(PROVConverter.formatAs(Format.TTL, d));
			assertTrue("ProvCollection " + col + " can be serialized to PROV-TTL", (provTTL != null && !"".equals(provTTL)));			
		}
	}
				
	public static void main(String [] args) throws Exception { 
		new TestPROV().dumpPROV();
	}
}
