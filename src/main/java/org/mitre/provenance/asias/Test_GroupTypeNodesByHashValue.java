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

import java.util.Iterator;
import java.util.TreeMap;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.client.RESTProvenanceClient;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.ProvenanceCollection;

/**
 *  @author piekut
 *	Just a simple call to http://localhost:8080/plus/api/object/groupbyhash/{process_name:.*} via RESTProvenanceClient,
 *  which corresponds to method groupTypeNodesByHashValue in ObjectServices.java	
 */

public class Test_GroupTypeNodesByHashValue {
	
	public static void main(String [] args) throws Exception { 
		RESTProvenanceClient client = new RESTProvenanceClient("localhost", "8080");
		
		// Edit below line to specify the type (i.e., name) of the process that is to be compared across DAGs.		
		String nodeType = "phases-of-flight";  
		int maxNodes = 500;
		
		ProvenanceCollection typeNodes = client.search(nodeType, maxNodes);
		
		Iterator<PLUSObject> nodeIt = typeNodes.getNodes().iterator();
		
		// This variable is TreeMap<$hashValue, TreeMap<$ExectionTime+($oid)>, $node>>
		TreeMap<String, TreeMap<String, PLUSObject>> groupedAndOrderedNodes = new TreeMap<String, TreeMap<String, PLUSObject>>();		
		while (nodeIt.hasNext()) {
			PLUSObject o = nodeIt.next();
			
			String thisHashValue = "[hash value not set]";			
			if (o.getMetadata().get(Metadata.CONTENT_HASH_SHA_256)!=null) {
				thisHashValue = (String) o.getMetadata().get(Metadata.CONTENT_HASH_SHA_256);
			}
			
			String thisExecutionTime = "[effective_end_time not found]";
			if (o.getMetadata().get("effective_end_time__$numberLong")!=null) {
				thisExecutionTime = (String) o.getMetadata().get("effective_end_time__$numberLong");
			}
			else if (o.getMetadata().get("effective_end_time")!=null) {
				thisExecutionTime = (String) o.getMetadata().get("effective_end_time");
			}
			
			// Appending metadata_id or oid to string for complete uniqueness
			if (o.getMetadata().get("meta_id")!=null) {
				thisExecutionTime += " ("+(String) o.getMetadata().get("meta_id")+ ")";
			}
			else { thisExecutionTime += " ("+o.getId() + ")"; }
			
			TreeMap<String, PLUSObject> thisHashValueTree = groupedAndOrderedNodes.get(thisHashValue);
			if (thisHashValueTree==null) { thisHashValueTree = new TreeMap<String, PLUSObject> (); }
			thisHashValueTree.put(thisExecutionTime, o);
			
			groupedAndOrderedNodes.put(thisHashValue, thisHashValueTree);
		}
		
		//  Now we have the nested tree, present the ordered content back.
		Iterator<String> hashValueIt = groupedAndOrderedNodes.keySet().iterator();
		// Output will be sent to console for demonstration purposes.
		System.out.println();  System.out.println();
		System.out.println("************************* ORDERED NODE OUTPUT *******************************");
		System.out.println();
		while (hashValueIt.hasNext()) {
			String hashValue = hashValueIt.next();
			System.out.println("Set of nodes sharing schema with hashed value '"+hashValue+"'");
			TreeMap<String, PLUSObject> thisHashValueTree = groupedAndOrderedNodes.get(hashValue);
			Iterator<String> orderedByTime = thisHashValueTree.keySet().iterator();
			while (orderedByTime.hasNext()) {
				String executionCompleted = orderedByTime.next();
				PLUSObject o = thisHashValueTree.get(executionCompleted);
				System.out.println("     Node '"+nodeType+"', execution completed " + executionCompleted);
			}		
			System.out.println();
		}
		System.out.println("****************************************************************************");
		System.out.println();  System.out.println();
		
		System.out.println("Done!");		
		System.exit(0); 
	} // End main
	
} // End class
