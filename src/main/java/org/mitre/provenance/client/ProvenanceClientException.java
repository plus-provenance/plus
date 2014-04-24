package org.mitre.provenance.client;

import org.mitre.provenance.PLUSException;

public class ProvenanceClientException extends PLUSException {
	public ProvenanceClientException(String msg) { super(msg); }
	public ProvenanceClientException(String msg, Throwable t) { super(msg, t); }
	public ProvenanceClientException(Throwable t) { super(t); } 
}
