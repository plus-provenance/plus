package org.mitre.provenance.asias;

import java.util.Iterator;

import org.mitre.provenance.client.RESTProvenanceClient;
import org.mitre.provenance.plusobject.*;

/**
 *  @author piekut
 *	Simple call to http://localhost:8080/plus/api/graph/search/{query:.*} via RESTProvenanceClient,
 *  which corresponds to method search() in DAGServices.java	
 */

public class Test_ExecuteCypherQuery {

	public static void main(String [] args) throws Exception { 
		RESTProvenanceClient client = new RESTProvenanceClient("localhost", "8080");
		
		// Edit this string to produce different results.  Must be valid Cypher query!
		String query = "MATCH (n:Provenance {name: \"tt-upgrade\"}) return n;";
		
		ProvenanceCollection queryMatchResults = client.query(query);
		
		// print collection info, save to disk.
		System.out.println("Collection matching Cypher query:  " + query);
		
		System.out.println("  # nodes= " + queryMatchResults.getNodes().size() + ": ");
		Iterator<PLUSObject> nodeIt = queryMatchResults.getNodes().iterator();
		while (nodeIt.hasNext()) {
			PLUSObject o = nodeIt.next();
			System.out.println("  OID: " + o.getId());
		}
		
		System.out.println("  # provenance edges= " + queryMatchResults.getEdges().size() + ": ");
		System.out.println("  # non-prov edges= " + queryMatchResults.getNonProvenanceEdges().size() + ": ");
		
		System.out.println("Done!");		
		System.exit(0); 
	} // End main
	
}
