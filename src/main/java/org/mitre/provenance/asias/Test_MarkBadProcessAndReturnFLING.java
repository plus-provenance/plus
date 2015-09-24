/* Copyright 2015 MITRE Corporation
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
package org.mitre.provenance.asias;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Iterator;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.client.RESTProvenanceClient;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.plusobject.json.JSONConverter;

/**
 *  Test class, which calls /object/taint/marktaintandfling/{id:.*} via RESTProvenanceClient,
 *  which corresponds to method taintFLING() in ObjectServices.java	
 * @author piekut
 * 
 */
public class Test_MarkBadProcessAndReturnFLING {
	
	public static void main(String [] args) throws Exception { 
		
		// first specify the client.
		RESTProvenanceClient client = new RESTProvenanceClient("localhost", "8080");
		
		// Edit the below line for the meta_id of the process that is to be marked bad.
		// Note:  if the node does not have a meta_id in its metadata, specify the PLUS-generated unique ID.
		String meta_id = "4xv_vwEzU-J_OOeV";
		
		// Transform Step necessary here!
		Metadata parameters = new Metadata();
		parameters.put("meta_id", meta_id);
		
		ProvenanceCollection match = client.search(parameters, 2);
		int matchCount = match.getNodes().size();
		if (matchCount==1) { 
			String id = match.getNodesInOrderedList().get(0).getId();
		
			//  the client makes the call and retrieves the results (ProvenanceCollection of affected FLING)
			ProvenanceCollection col = client.markTaintAndRetrieveFLING(id);
		
			// This part just reports on the summary results.  To get the full JSON of the ProvenanceCollection, 
			// uncomment the block below the summary print statements.
			System.out.println("# FLING nodes: " + col.getNodes().size());	
			Iterator<PLUSObject> nodeIt = col.getNodesInOrderedList().iterator();
			while (nodeIt.hasNext()) {
			System.out.println("   " +nodeIt.next().getName());
			}
			System.out.println("# FLING edges: " + col.getEdges().size());
			//System.out.println("# FLING NPEs: " + col.getNonProvenanceEdges().size());
		
		
			// Prints JSON of affected FLING to a file.  Edit the line below to specify a different filepath. 
			String fileDestination = System.getProperty("user.home") + "\\Desktop\\taintAffectedFLING.json";
			BufferedWriter mySavedProvenance = new BufferedWriter(new FileWriter(fileDestination));		
			try { 
				String json = JSONConverter.provenanceCollectionToD3Json(col);
				mySavedProvenance.write(json);
			} finally { mySavedProvenance.close(); }
		}
		else if (matchCount==0) {
			System.err.println("ERROR:  No nodes were found with meta_id value = '"+ meta_id+"'.");	
		}
		else {
			System.err.println("ERROR:  More than one node found with meta_id value = '"+ meta_id+"'.");	
		}
		
		System.out.println("Done!");		
		System.exit(0); 
		
	} // End main
	
} // End Test_MarkBadProcessAndReturnFLING
