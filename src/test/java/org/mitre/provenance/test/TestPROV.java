package org.mitre.provenance.test;

import static org.junit.Assert.*;

import java.util.List;

import javax.xml.bind.JAXBException;

import org.junit.Test;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.plusobject.prov.PROVConverter;
import org.mitre.provenance.user.User;

/**
 * Test serialization of ProvenanceCollection objects to PROV-XML.
 * @author moxious
 */
public class TestPROV {	
	private ProvenanceCollection col = null;
	
	@Test
	public void dumpPROV() throws PLUSException, JAXBException {
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
			String xml = PROVConverter.asXMLString(c.provenanceCollectionToPROV(col));
			
			assertTrue("ProvCollection " + col + " can be serialized to PROV", (xml != null && xml.length() > 0));			
		}
	}
				
	public static void main(String [] args) throws Exception { 
		new TestPROV().dumpPROV();
	}
}
