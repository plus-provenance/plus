package org.mitre.provenance.test;

import java.util.List;

import javax.xml.bind.JAXBException;

import org.junit.Test;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.plusobject.prov.PROVConverter;
import org.mitre.provenance.user.User;

public class TestPROV {
	@Test
	public void dumpPROV() throws PLUSException, JAXBException {
		LocalProvenanceClient client = new LocalProvenanceClient(User.DEFAULT_USER_GOD);
				
		List<PLUSWorkflow> wfs = client.listWorkflows(20);
		
		for(PLUSWorkflow wf : wfs) { 
			System.out.println("Dumping " + wf + " as PROV"); 
			
			ProvenanceCollection col = client.getWorkflowMembers(wf.getId(), 2);			
			
			if(col.countNodes() <= 0) { 
				System.out.println("Workflow " + wf + " has no members.");
				continue;
			}
			
			ProvenanceCollection dag = client.getGraph(col.getNodesInOrderedList().get(0).getId());
			checkPROV(dag);
		}
	}
		
	public void checkPROV(ProvenanceCollection dag) throws PLUSException, JAXBException { 
		PROVConverter c = new PROVConverter();
		String xml = PROVConverter.asXMLString(c.provenanceCollectionToPROV(dag));
		System.out.println(xml);
	}
	
	
	public static void main(String [] args) throws Exception { 
		new TestPROV().dumpPROV();
	}
}
