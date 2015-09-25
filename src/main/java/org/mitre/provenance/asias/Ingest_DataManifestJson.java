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

import org.mitre.provenance.Metadata;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.client.AbstractProvenanceClient;
import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.client.ProvenanceClientException;
import org.mitre.provenance.client.RESTProvenanceClient;
import org.mitre.provenance.plusobject.*;
import org.mitre.provenance.npe.*;
import org.mitre.provenance.plusobject.json.JSONConverter;
import org.mitre.provenance.user.User;
import org.mitre.provenance.contenthash.ContentHasher;
import org.mitre.provenance.contenthash.MD5ContentHasher;
import org.mitre.provenance.contenthash.SHA256ContentHasher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.List;
import java.util.Iterator;

import javax.ws.rs.client.Client;

/**
 * @author piekut
 * This class ingests the JSON for Evan's Data Manifest records and parses them into a Provenance collection, 
 * which is then reported (saved) to the PLUS repository as specified in the local or remote "client" as specified below.
 * The ingest attempts to capture the content in as general form as possible.  
 * However, actions for several of the top level keys in the ingested JSON *must* be hardcoded to determine 
 * specific aspects of the provenance graph (e.g., "name" as invocation node name)
 */
public class Ingest_DataManifestJson {
	
	public static TreeMap<String,PLUSObject> nodeLookup = new TreeMap<String,PLUSObject>();
	public static TreeMap<String,List<PLUSObject>> hashedParamNodeLookup = new TreeMap<String,List<PLUSObject>>();
	public static List<TreeMap<String,String>> edgeMap = new java.util.ArrayList<TreeMap<String,String>>();			
	
 	// Create a provenance collection to hold the items we're reporting via REST.
    public static ProvenanceCollection col;
    public static Gson g = new GsonBuilder().create();
    public static RESTProvenanceClient client;
    
    public static void main(String [] args) throws Exception { 
		FileInputStream fis = null;
		
		client = new RESTProvenanceClient("localhost", "8080");
		ArrayList<String> jsonContent = new ArrayList();
		System.out.println("Opening JSON file.");
		try {
			
			// EDIT VALUE here if the file is to be loaded from another location.
			String ingestDIR = System.getProperty("user.home").toString()+File.separator.toString()+"Desktop";
			
			// EDIT VALUE here if the ingest file has a different name. 
			String ingestJSON = "/seq/dataManifest.json"; 
			
			File userDesktopJSON = new File(ingestDIR+File.separator+ingestJSON); 						
			fis = new FileInputStream(userDesktopJSON);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));

			String strLine;

			//Read File Line By Line
			int idx = 0;
			while ((strLine = br.readLine()) != null)   {
				if (strLine.contains("ObjectId")) {
					strLine = strLine.replaceFirst("ObjectId\\(", "");
					strLine = strLine.replaceFirst("\\),", ",");
				}
				if (strLine==null || strLine.equals("")) { continue; }
			    jsonContent.add(idx, strLine);
			    idx++;
			}
			br.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
        finally { fis.close(); }  
		
		// see processDataManifestJSON function directly below for details.
		ProvenanceCollection col = processDataManifestJSON(jsonContent);
		
		// REST client reports via localhost:8080/api/plus/graph/new/
		client.report(col);
		
		System.out.println("Done!");		
		System.exit(0); 
    }
    
    public static ProvenanceCollection processDataManifestJSON(ArrayList<String> dataManifestJSON) throws Exception { 

    	ProvenanceCollection col = new ProvenanceCollection();
		System.out.println("Parsing submitted JSON.");
		
		System.out.println("Starting to add data manifest nodes...");
		for (int i=0; i<dataManifestJSON.size(); i++) {
			// The below call will create invocation node, params (if exists) and output-data node, for each JSON obj.
			createInvocationNode(dataManifestJSON.get(i), col);  
		}				
		System.out.println("Done adding nodes.");
				
		System.out.println("Data Manifest ingest complete.");
		return col;
	} // End main
	
	
	private static void createInvocationNode(String jsonString, ProvenanceCollection col) throws Exception {					
		 // System.out.println(jsonString);
		JsonElement elem = g.fromJson(jsonString, JsonElement.class);
		if(!elem.isJsonObject()) throw new Exception("Server response wasn't a JSON object " + elem);
		JsonObject obj = elem.getAsJsonObject();
		
		// "name" is a required field for ASIAS invocation nodes.
		String analytic_name = obj.get("name").getAsString();
		PLUSInvocation invocation= new PLUSInvocation(analytic_name);
		System.out.println("Adding Node '" + analytic_name+"'. ("+obj.get("meta_id").getAsString()+")");
		
		
		invocation.getMetadata().put(Metadata.CONTENT_HASH_SHA_256, obj.get("sha256").getAsString());

		/* Adding properties with as few exceptions as possible, in light of requirements. */
		for (Map.Entry<String,JsonElement> entry : obj.entrySet()) {
		    JsonElement jsonElement = entry.getValue();
		    String topLevelElement = entry.getKey();
		    if (!topLevelElement.equals("name") 
		    		//&& !topLevelElement.equals("meta_id") 
		    		&& !topLevelElement.equals("sha256")) {  // If not one of our special use elements
		    	addProperty(invocation, topLevelElement, jsonElement);
		    }
		}

		/* Lookups getting set here so that the backwards/forwards lineage edges can be determined afterward. */
		String meta_id = obj.get("meta_id").getAsString();
		
		invocation.setCreated();  // timestamp "now".
		//invocation.getMetadata().put("ingest", "DataManifest-JSON");  // tag for deleting draft nodes later.	Uncomment if desired.	
		
		col.addNode(invocation);
		
		// issue query to "peek ahead" at downstream nodes, should they already exist on the system.  If found, establish edges between.
		// If not found, skip edge creation.		
		Metadata parameters = new Metadata();
		parameters.put("input_meta_ids", meta_id);
		ProvenanceCollection downstreamMatch = client.search(parameters, 500);
		Iterator<PLUSObject> nodeIt = downstreamMatch.getNodes().iterator();
		while (nodeIt.hasNext()) {  System.out.println("X");
			PLUSObject o = nodeIt.next();
		col.addNode(o);
			col.addEdge(new PLUSEdge(invocation, o, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_INPUT_TO));		
		}
	}	
	
	public static void addProperty(PLUSObject node, String propertyName, JsonElement propertyValue) {		
		if (propertyValue.isJsonPrimitive()) {
			//System.out.println("  Adding property " + propertyName+".");
			node.getMetadata().put(propertyName, propertyValue.getAsString());
		}
		// ***************************************
		// For arrays and Objects, break out into 1st-level properties, with property label reflecting object hierarchy. 
		// ***************************************
		else if (propertyValue.isJsonArray()) {
			JsonArray arrayObj = propertyValue.getAsJsonArray();
			if (arrayObj.size()==1) {
				addProperty(node, propertyName, arrayObj.get(0));
			}
			else if (arrayObj.size()>1) {
				int count = 1;
				for(JsonElement arrayElement : arrayObj) { 
					addProperty(node, propertyName +"_" + count, arrayElement);
					count++;
				}
			}
		}
		else {
			JsonObject obj =  propertyValue.getAsJsonObject();
			for (Map.Entry<String,JsonElement> entry : obj.entrySet()) {
			    JsonElement subElement = entry.getValue();
			    String elementName = entry.getKey();
			    addProperty(node, propertyName +"__" + elementName, subElement);
			}
		}
	}
	
	public static JsonElement toJsonElement(JsonElement jsonElement) {
		JsonElement returnObj;
    	if (jsonElement.isJsonPrimitive()) {
    		String p_j = jsonElement.getAsString();
    		p_j = p_j.replaceAll("\\\"", "\"");  //clean up escaping so we can parse as JsonObj.
    		return g.fromJson(p_j, JsonElement.class);
    	}
    	else { return jsonElement; }
	}
	
} // End
