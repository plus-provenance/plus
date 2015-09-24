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
 * This class ingests the JSON that ASIAS processes generate and parses them into a Provenance collection, 
 * which is then reported (saved) to the PLUS repository as specified in the local or remote "client" as specified below.
 * The ingest attempts to capture the content in as general form as possible.  
 * However, actions for several of the top level keys in the ingested JSON *must* be hardcoded to determine 
 * specific aspects of the provenance graph (e.g., "name" as invocation node name, 
 * "params_json" broken into nodes representing input parameters, etc.)
 */
public class Ingest_AsiasJson {
	
	public static TreeMap<String,PLUSObject> nodeLookup = new TreeMap<String,PLUSObject>();
	public static TreeMap<String,List<PLUSObject>> hashedParamNodeLookup = new TreeMap<String,List<PLUSObject>>();
	public static List<TreeMap<String,String>> edgeMap = new java.util.ArrayList<TreeMap<String,String>>();			
	public static RESTProvenanceClient client;
    public static Gson g = new GsonBuilder().create();
    
    public static void main(String [] args) throws Exception { 
		FileInputStream fis = null;
		client = new RESTProvenanceClient("localhost", "8080");
		
		ArrayList<String> jsonContent = new ArrayList();
		System.out.println("Opening JSON file.");
		try {
			
			// EDIT VALUE here if the file is to be loaded from another location.
			String ingestDIR = System.getProperty("user.home").toString()+File.separator.toString()+"Desktop";
			
			// EDIT VALUE here if the ingest file has a different name. 
			String ingestJSON = "/seq/dataManifestDownstream.json";
			//String ingestJSON = "/seq/metademo2.json";
			//String ingestJSON = "/seq/metadev5.json";			
			
			
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
		
		// see processASIASJSON function directly below for details.
		ProvenanceCollection col = processASIASJSON(jsonContent);
		
		// REST client reports via localhost:8080/api/plus/graph/new/
		client.report(col);
		
		System.out.println("Done!");		
		System.exit(0); 
    }
    
    public static ProvenanceCollection processASIASJSON(ArrayList<String> asiasJSON) throws Exception { 

    	ProvenanceCollection col = new ProvenanceCollection();
		System.out.println("Parsing submitted JSON.");
		
		System.out.println("Starting to add nodes...");
		for (int i=0; i<asiasJSON.size(); i++) {
			// The below call will create invocation node, params (if exists) and output-data node, for each JSON obj.
			createInvocationAndDataNode(asiasJSON.get(i), col);  
		}				
		System.out.println("Done adding nodes.");
		
		// Edges connecting the DAG components were specified in the previous loop, and stored in a Map until all
		// nodes were processed.  This step loops through the edge map and instantiates them in the collection.
		System.out.println("Starting to add edges...");
	    Iterator<TreeMap<String,String>> edgeIt = edgeMap.iterator();
		while (edgeIt.hasNext()) {
			TreeMap<String,String> edge = edgeIt.next();
			String from = edge.firstKey();
			String to = edge.get(from);
			if (nodeLookup.get(from)!=null) {
				System.out.println("  adding edge from "+ nodeLookup.get(from).getName() + " to " + nodeLookup.get(to).getName());				
				col.addEdge(new PLUSEdge(nodeLookup.get(from), nodeLookup.get(to), PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_INPUT_TO));
			}
			else {  // This handles nodes previously loaded.
				System.out.println("  adding edge from '"+ from + "' to " + nodeLookup.get(to).getName());				
				System.out.println("      INFO: input node not in load set.  Checking datastore for preexisting match...");
				
				Metadata parameters = new Metadata();
		    	parameters.put("joins", from);
		    	
				Iterator<PLUSObject> joinsNodes = client.search(parameters, 500).getNodes().iterator();				
				PLUSObject previouslyLoadedNode = null;
				if (joinsNodes.hasNext()) { previouslyLoadedNode = joinsNodes.next(); }
				if (previouslyLoadedNode!=null) {
					col.addNode(previouslyLoadedNode);
					col.addEdge(new PLUSEdge(previouslyLoadedNode, nodeLookup.get(to), PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_INPUT_TO));
				}
				else { 
					System.out.println("      WARNING: node with meta_id '" + from + "' was not found!");
					System.out.println("        Skipping over edge joining '" + from + "' to '"+to+"'.");
				}
			}
		}
		edgeMap.clear(); nodeLookup.clear();
		System.out.println("Done adding edges.");
		System.out.println("ASIAS ingest complete.");
		return col;
	} // End main
	
	
	private static void createInvocationAndDataNode(String jsonString, ProvenanceCollection col) throws Exception {					
		 // System.out.println(jsonString);
		JsonElement elem = g.fromJson(jsonString, JsonElement.class);
		if(!elem.isJsonObject()) throw new Exception("Server response wasn't a JSON object " + elem);
		JsonObject obj = elem.getAsJsonObject();
		
		// "name" is a required field for ASIAS invocation nodes.
		String analytic_name = obj.get("name").getAsString();
		PLUSInvocation invocation= new PLUSInvocation(analytic_name);
		System.out.println("Adding Node '" + analytic_name+"'. ("+obj.get("meta_id").getAsString()+")");
		
		
		String output_name = analytic_name+"_output";  
		if (obj.get("output_name")!=null) { output_name = obj.get("output_name").getAsString(); }
		PLUSString data = new PLUSString(output_name);
		
		String content = obj.get("output_schemas_json").getAsString();
		data.setContent(content);
		SHA256ContentHasher myHasher = new SHA256ContentHasher();
		String hashAsString = ContentHasher.formatAsHexString(myHasher.hash(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))));
		
		data.getMetadata().put(Metadata.CONTENT_HASH_SHA_256, hashAsString);

		/* Adding properties with as few exceptions as possible, in light of requirements. */
		for (Map.Entry<String,JsonElement> entry : obj.entrySet()) {
		    JsonElement jsonElement = entry.getValue();
		    String topLevelElement = entry.getKey();
		    if (topLevelElement.equals("params_json")) {
		    	//first, transform out to JSON Object from string
		    	JsonObject jParams = (JsonObject) toJsonElement(jsonElement);
		    	
		    	// second, establish separate data nodes that feed into the invocation node.
		    	// Caveat:  This assumes json_params depth is 1.  There has been no discussion on "nesting" in param nodes.
		    	for (Map.Entry<String,JsonElement> param : jParams.entrySet()) {
				    JsonElement paramElement = param.getValue();
				    String paramKey = param.getKey();
				    String paramValue;
				    if (paramElement.isJsonPrimitive()) { paramValue = paramElement.getAsString(); }
				    else { paramValue = paramElement.toString(); }
				    PLUSString paramNode = new PLUSString(paramKey);
				    paramNode.setContent(paramValue);
				    
				    // criteria for hashing/uniqueness of param nodes is key plus its value.  (possibly open to revision)
					String paramValueUnique = paramKey+"_"+paramValue;
				    String hashString = ContentHasher.formatAsHexString(myHasher.hash(new ByteArrayInputStream(paramValueUnique.getBytes(StandardCharsets.UTF_8))));
					paramNode.getMetadata().put(Metadata.CONTENT_HASH_SHA_256, hashString);

					// Now add hash of value plus process it feeds.  This is so we can make the distinction for NPE types later.
					String paramValueProcessUnique = paramValueUnique + analytic_name;
					String hashStringProcessUnique = ContentHasher.formatAsHexString(myHasher.hash(new ByteArrayInputStream(paramValueProcessUnique.getBytes(StandardCharsets.UTF_8))));
					paramNode.getMetadata().put("sha256hashSpecficProcess", hashStringProcessUnique);

				    col.addNode(paramNode);
				    
				    // third, establish link from *parameter* node to *invocation* node.
				    col.addEdge(new PLUSEdge(paramNode, invocation, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_INPUT_TO));
				    
					// Add non-provenance edges for params, but only if they match a previously-loaded parameter.
				    if (hashString!=null)  { 
				    	//  First look through already loaded nodes to see if other params match.
				    	List<PLUSObject> loadList = new ArrayList<PLUSObject>();
				    	if (hashedParamNodeLookup.get(hashString)!=null) {
				    		loadList = hashedParamNodeLookup.get(hashString);
				    		
				    		Iterator<PLUSObject> listIterator = loadList.iterator();
				    		while (listIterator.hasNext()) {
				    			PLUSObject otherParam = listIterator.next();
				    			String type = "Same Parameter Value, Different Process";
					    		if (otherParam.getMetadata().get("sha256hashSpecficProcess")
					    			.equals(paramNode.getMetadata().get("sha256hashSpecficProcess"))) { 
					    			type = "Same Parameter Value, Same Process"; 
					    		}
					    		NonProvenanceEdge npe = new NonProvenanceEdge(paramNode, otherParam, type);
					    		System.out.println("adding '"+type+"' NPE from "+ paramNode.getId()+ " to " +otherParam.getId());
					    		col.addNonProvenanceEdge(npe);
				    		}
				    	}
				    	loadList.add(paramNode);
				    	hashedParamNodeLookup.put(hashString, loadList);
				    	
				    	//  Secondly, do the same thing for previously-loaded nodes in the database.
				    	ProvenanceCollection match = new ProvenanceCollection();
				    	PLUSObject otherParam = null;
				    	Metadata parameters = new Metadata();
				    	parameters.put(Metadata.CONTENT_HASH_SHA_256, hashString);
				    	match = client.search(parameters, 500);
				    	Iterator<PLUSObject> equivalentValues = match.getNodes().iterator();
				    	while (equivalentValues.hasNext()) {
				    		otherParam = equivalentValues.next();
				    		if (!col.contains(otherParam)) {
				    			col.addNode(otherParam);  // duplicate, only to add NPE.
				    		}
				    		String type = "Same Parameter Value, Different Process";
				    		if (otherParam.getMetadata().get("sha256hashSpecficProcess")
				    			.equals(paramNode.getMetadata().get("sha256hashSpecficProcess"))) { 
				    			type = "Same Parameter Value, Same Process"; 
				    		}
				    		NonProvenanceEdge npe = new NonProvenanceEdge(paramNode, otherParam, type);
				    		System.out.println("adding '"+type+"' NPE from "+ paramNode.getId()+ " to " +otherParam.getId());
				    		col.addNonProvenanceEdge(npe);
				    	}
				    	equivalentValues = null; 
				    }
		    	}
		    	
		    }
		    else if (topLevelElement.equals("job_counters") ) { 
		    	// only need special handling for "job_counters" because it comes in as a string.
		    	// if it changes to be JSON in its natural state, this block can be removed.
		    	addProperty(invocation, topLevelElement, toJsonElement(jsonElement));
		    }
		    else if (!topLevelElement.equals("name") 
		    		//&& !topLevelElement.equals("meta_id") 
		    		&& !topLevelElement.equals("output_name") 
		    		&& !topLevelElement.equals("output_schemas_json") 
		    		&& !topLevelElement.equals("path")) {  // If not one of our special use elements
		    	addProperty(invocation, topLevelElement, jsonElement);
		    }
		    else if (topLevelElement.equals("output_schemas_json") || topLevelElement.equals("path")) {
		    	// special handling for the output data node's parameters.
		    	addProperty(data, topLevelElement, jsonElement);
		    }
		    if (topLevelElement.equals("input_schemas_json"))  {
			    String hashString = ContentHasher.formatAsHexString(myHasher.hash(new ByteArrayInputStream(jsonElement.toString().getBytes(StandardCharsets.UTF_8))));
				invocation.getMetadata().put(Metadata.CONTENT_HASH_SHA_256, hashString);
		    }
		}

		/* Lookups getting set here so that the backwards/forwards lineage edges can be determined afterward. */
		String meta_id = obj.get("meta_id").getAsString();
		nodeLookup.put(meta_id, data);             // might be non-intuitive, but we're assigning meta_id as key for data node. 
		nodeLookup.put(analytic_name+"_" +meta_id, invocation); // analytic name is lookup key for invocation node.
		
		data.getMetadata().put("joins", meta_id);  // This is for lookup later, should later runs reference this output.
		
		invocation.setCreated();  // timestamp "now".
		//invocation.getMetadata().put("ingest", "ASIAS-JSON");  // tag for deleting draft nodes later.	Uncomment if desired.	
		if (obj.get("input_meta_ids")!=null) {
			JsonArray input_meta_id = obj.getAsJsonArray("input_meta_ids");
			for(JsonElement in_meta_id : input_meta_id) { 
				TreeMap<String,String> addEdge = new TreeMap<String,String>();
				addEdge.put(in_meta_id.getAsString(), analytic_name+"_" +meta_id);
				edgeMap.add(addEdge);
			}
		}
    	col.addNode(invocation);
    	col.addNode(data);
    	col.addEdge(new PLUSEdge(invocation, data, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_GENERATED));
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
	
} // End ASIAS Ingest 
