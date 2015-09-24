package org.mitre.provenance.asias;

import java.util.Iterator;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.client.RESTProvenanceClient;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.ProvenanceCollection;

/**
 *  @author piekut
 *	Simple call to http://localhost:8080/plus/api/object/metadata/{field:.*}/{value:.*} via RESTProvenanceClient,
 *  which corresponds to method search() in DAGServices.java	
 */

public class Test_SearchByMetadata {
	
	public static void main(String [] args) throws Exception { 
		RESTProvenanceClient client = new RESTProvenanceClient("localhost", "8080");
		
		Metadata parameters = new Metadata();
		// Edit the below line to search by other metadata.
		String key = "execution_id";  String value = "-31988842";
		parameters.put(key, value);
		
		int maxReturn = 500;
		ProvenanceCollection matchResults = client.search(parameters, maxReturn);
		
		// print collection info, save to disk.
		System.out.println("Collection of results with metadata field "+key+" possessing value '" + value +"': ");
		
		System.out.println("  # nodes= " + matchResults.getNodes().size() + ": ");
		Iterator<PLUSObject> nodeIt = matchResults.getNodes().iterator();
		while (nodeIt.hasNext()) {
			PLUSObject o = nodeIt.next();
			System.out.println("  OID: " + o.getId());
		}		
		
		System.out.println("Done!");		
		System.exit(0); 
	} // End main
	
}
