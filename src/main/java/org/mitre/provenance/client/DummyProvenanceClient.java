package org.mitre.provenance.client;

import java.util.List;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.dag.TraversalSettings;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.user.PrivilegeClass;

/**
 * A dummy provenance client.   All methods throw RuntimeExceptions that the method is not implemented, with a message indicating
 * how to configure the correct client.
 * 
 * @author moxious
 */
public class DummyProvenanceClient extends AbstractProvenanceClient {
	public static final String msg = "This method is not implemented in the dummy client.  Please configure your tool to use the appropriate client, via ProvenanceClient.instance";
	
	public boolean report(ProvenanceCollection col)
			throws ProvenanceClientException {
		throw new RuntimeException(msg);
	}

	public ProvenanceCollection getGraph(String oid, TraversalSettings desc)
			throws ProvenanceClientException {
		throw new RuntimeException(msg);
	}

	public PLUSActor actorExists(String aid) throws ProvenanceClientException {
		throw new RuntimeException(msg);
	}

	public List<PLUSWorkflow> listWorkflows(int max)
			throws ProvenanceClientException {
		throw new RuntimeException(msg);
	}

	public PLUSObject getSingleNode(String oid)
			throws ProvenanceClientException {
		throw new RuntimeException(msg);
	}

	public ProvenanceCollection getWorkflowMembers(String oid, int max)
			throws ProvenanceClientException {
		throw new RuntimeException(msg);
	}

	public ProvenanceCollection latest() throws ProvenanceClientException {
		throw new RuntimeException(msg);
	}

	public ProvenanceCollection getActors(int max)
			throws ProvenanceClientException {
		throw new RuntimeException(msg);
	}

	public ProvenanceCollection search(String searchTerm, int max)
			throws ProvenanceClientException {
		throw new RuntimeException(msg);
	}

	public ProvenanceCollection search(Metadata parameters, int max)
			throws ProvenanceClientException {
		throw new RuntimeException(msg);
	}

	public boolean dominates(PrivilegeClass a, PrivilegeClass b)
			throws ProvenanceClientException {
		throw new RuntimeException(msg);
	}
}
