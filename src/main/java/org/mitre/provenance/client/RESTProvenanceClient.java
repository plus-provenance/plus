/* Copyright 2014 MITRE Corporation
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
package org.mitre.provenance.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.dag.TraversalSettings;
import org.mitre.provenance.npe.NonProvenanceEdge;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.plusobject.json.JSONConverter;
import org.mitre.provenance.plusobject.json.ProvenanceCollectionDeserializer;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * This class acts as a client for a remote provenance server which can serve up provenance or permit reporting
 * of new provenance via RESTful services.
 * 
 * <p>Use this class if you want to report provenance to a foreign host that is running PLUS.
 * 
 * @author moxious
 */
public class RESTProvenanceClient extends AbstractProvenanceClient {
	protected static final String VERSION = "0.5";
	
	protected static final String UA = "RESTProvenanceClient " + VERSION;
	
	/** The host of the remote server */
	protected String host = "";
	/** Port where the remote server is located. */
	protected String port = "80";
	
	protected static final String SEARCH_PATH = "/plus/api/object/search?format=json";
	protected static final String GET_ACTOR_PATH = "/plus/api/actor/";
	protected static final String GET_ACTORS_PATH = "/plus/api/feeds/objects/owners?format=json";
	protected static final String NEW_GRAPH_PATH = "/plus/api/graph/new";	
	protected static final String GET_GRAPH_PATH = "/plus/api/graph/";
	protected static final String GET_LATEST_PATH = "/plus/api/feeds/objects/latest?format=json";
	protected static final String LIST_WORKFLOWS_PATH = "/plus/api/workflows/latest?format=json";
	protected static final String GET_WORKFLOW_MEMBERS_PATH = "/plus/api/workflow/";
	protected static final String GET_SINGLE_NODE_PATH = "/plus/api/object/";
	
	protected Client client = null;
	
	public RESTProvenanceClient(String host) throws ProvenanceClientException {
		this(host, "80");
	}
	
	/**
	 * Create a client object to send requests to a provenance service located at a particular location.
	 * @param host the host where the provenance service can be found.
	 * @param port the port the provenance service is running on.
	 * @throws when parameters are invalid
	 */
	public RESTProvenanceClient(String host, String port) throws ProvenanceClientException { 
		this.host = host;
		this.port = port;
		
		if(host == null || "".equals(host)) throw new ProvenanceClientException("No host specified.");
		if(port == null || "".equals(port)) throw new ProvenanceClientException("No port specified.");
		
		int i = 0;
		try { i = Integer.parseInt(port); } catch(Exception exc) { 
			throw new ProvenanceClientException("Invalid port: " + port); 
		}
		
		if(i < 1 || i > 65535) throw new ProvenanceClientException("Invalid port number: " + i); 
		
		client = Client.create(); 
		client.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS, true);
	} // End RESTProvenanceClient
		
	protected String buildURL(String endpointPath) {
		String u = "http://" + this.host + ":" + this.port + endpointPath;
		System.out.println("URL: " + u); 
		return u;
	}
	
	/**
	 * Report a provenance collection to a remote service.  This has the effect of writing the given
	 * provenance collection to the remote service's store.
	 * @param col the collection to report
	 * @return true if successful, false if not successful.
	 * @throws ProvenanceClientException
	 */
	public boolean report(ProvenanceCollection col) throws ProvenanceClientException {
		WebResource r = client.resource(buildURL(NEW_GRAPH_PATH));

		MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
		String json = JSONConverter.provenanceCollectionToD3Json(col);
		// System.out.println("POSTING:\n" + json + "\n\n");
	    formData.add("provenance", json);
	    
	    ClientResponse response = r.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
	    		 .accept(MediaType.APPLICATION_JSON_TYPE)
	    		 .header("User-Agent", UA)
	    		 .post(ClientResponse.class, formData);
	    
		String output = response.getEntity(String.class);
	    
	    System.out.println(response); 
	    System.out.println(response.getLength());
	    System.out.println(response.getStatus());
	    System.out.println(output);
		return true;
	}
	
	public ProvenanceCollection getGraph(String oid) throws ProvenanceClientException {
		return getGraph(oid, new TraversalSettings());
	}
	
	public ProvenanceCollection getGraph(String oid, TraversalSettings desc) throws ProvenanceClientException {
		WebResource r = client.resource(buildURL(GET_GRAPH_PATH + oid));
		
		MultivaluedMap<String,String> params = desc.asMultivaluedMap();
		
		String response = r.queryParams(params)
				 .accept(MediaType.APPLICATION_JSON_TYPE)
				 .header("User-Agent", UA)				 
				 .get(String.class);

		return provenanceCollectionFromResponse(response);
	} // End getGraph
	
	public ProvenanceCollection latest() throws ProvenanceClientException {
		WebResource r = client.resource(buildURL(GET_LATEST_PATH));
		
		String response = r.accept(MediaType.APPLICATION_JSON_TYPE)
				 .header("User-Agent", UA)				 
				 .get(String.class);
		
		return provenanceCollectionFromResponse(response);
	}
	
	public static void main(String [] args) throws Exception { 
		RESTProvenanceClient rpc = new RESTProvenanceClient("denim.mitre.org", "80");
		//for(PLUSActor a : rpc.getActors().getActors()) { 
		//	System.out.println(a);
		//}
		
		for(PLUSObject o : rpc.latest().getNodes()) { 
			System.out.println(o);
		}		
	}
	
	public static void __main(String [] args) throws Exception { 
		ProvenanceCollection col = new ProvenanceCollection();
		PLUSString s = new PLUSString("Foo", "Bar");
		PLUSString t = new PLUSString("Baz", "Quux");
		PLUSEdge e = new PLUSEdge(s, t, null);
		NonProvenanceEdge npe = new NonProvenanceEdge(s, t, "blah");
		
		col.addNode(s);
		col.addNode(t);
		col.addEdge(e);
		col.addNonProvenanceEdge(npe);		
		
		//RESTProvenanceClient pc = new RESTProvenanceClient("localhost", "8080");
		RESTProvenanceClient pc = new RESTProvenanceClient("denim.mitre.org");
		System.out.println("Reporting collection...");
		System.out.println("REPORT RESULT:  " + pc.report(col));
		
		System.out.println("Fetching graph...");
		ProvenanceCollection c = pc.getGraph(s.getId());
		System.out.println("After getting graph, contents:");
		for(PLUSObject o : c.getNodes()) {
			System.out.println(o);
		}
		
		for(PLUSEdge ed : c.getEdges()) {
			System.out.println(ed); 
		}
		
		for(NonProvenanceEdge n : c.getNonProvenanceEdges()) {
			System.out.println(n);
		}
		
		col = pc.latest();
		for(PLUSObject o : col.getNodes()) {
			System.out.println("LATEST:  " + o);
		}
		
		System.out.println("Reporter finished and exiting.");
	}
	
	public ProvenanceCollection getActors(int max) throws ProvenanceClientException {
		WebResource r = client.resource(buildURL(GET_ACTORS_PATH) + "&n=" + max);				
		
		String response = r
				 .accept(MediaType.APPLICATION_JSON_TYPE)
				 .header("User-Agent", UA)				 
				 .get(String.class);

		return provenanceCollectionFromResponse(response);
	} // End getActors
	
	public ProvenanceCollection search(String searchTerm, int max)
			throws ProvenanceClientException {		
		WebResource r = client.resource(buildURL(SEARCH_PATH) + "&n=" + max);
		
		String response = r
				 .accept(MediaType.APPLICATION_JSON_TYPE)
				 .header("User-Agent", UA)				 
				 .get(String.class);		
		
		return provenanceCollectionFromResponse(response);
	}
	
	public ProvenanceCollection search(Metadata parameters, int max)
			throws ProvenanceClientException {
		throw new ProvenanceClientException("Not yet implemented.");
	}
	
	/**
	 * Take a string JSON response, deserialize it as a ProvenanceCollection, and return it.
	 * @param response a JSON object that came back from a service
	 * @return an equivalent provenance collection.
	 */
	protected ProvenanceCollection provenanceCollectionFromResponse(String response) { 
		Gson g = new GsonBuilder().registerTypeAdapter(ProvenanceCollection.class, new ProvenanceCollectionDeserializer()).create();
		return g.fromJson(response, ProvenanceCollection.class);		
	}
	
	public List<PLUSWorkflow> listWorkflows(int max) throws ProvenanceClientException {
		WebResource r = client.resource(buildURL(LIST_WORKFLOWS_PATH) + "&n=" + max);
		
		String response = r
				 .accept(MediaType.APPLICATION_JSON_TYPE)
				 .header("User-Agent", UA)				 
				 .get(String.class);		
		
		ProvenanceCollection col = provenanceCollectionFromResponse(response);
		ArrayList<PLUSWorkflow> results = new ArrayList<PLUSWorkflow>();
		
		for(PLUSObject o : col.getNodesInOrderedList()) {
			if(o.isWorkflow()) results.add((PLUSWorkflow)o);
		}

		return results;
	}
	
	public ProvenanceCollection getWorkflowMembers(String oid, int max)
			throws ProvenanceClientException {
	
		PLUSObject n = getSingleNode(oid);
		if(n == null) throw new ProvenanceClientException("No such workflow node " + oid);
		if(!n.isWorkflow()) throw new ProvenanceClientException("Can't list members of non-workflow node " + n);
		
		WebResource r = client.resource(buildURL(GET_WORKFLOW_MEMBERS_PATH) + n.getId() + "?format=json&n=" + max);
		
		String response = r
				 .accept(MediaType.APPLICATION_JSON_TYPE)
				 .header("User-Agent", UA)				 
				 .get(String.class);		
		
		return provenanceCollectionFromResponse(response);
	} // End getWorkflowMembers

	public PLUSObject getSingleNode(String oid) throws ProvenanceClientException {
		WebResource r = client.resource(buildURL(GET_SINGLE_NODE_PATH) + oid + "?format=json");
		
		String response = r
				 .accept(MediaType.APPLICATION_JSON_TYPE)
				 .header("User-Agent", UA)				 
				 .get(String.class);		
		
		ProvenanceCollection col = provenanceCollectionFromResponse(response);
		
		if(col.containsObjectID(oid)) return col.getNode(oid);		
		return null;
	} // End getSingleNode

	public PLUSActor actorExists(String aid) throws ProvenanceClientException {
		WebResource r = client.resource(buildURL(GET_ACTOR_PATH) + aid + "?format=json");
		
		String response = r
				 .accept(MediaType.APPLICATION_JSON_TYPE)
				 .header("User-Agent", UA)				 
				 .get(String.class);		
		
		Gson g = new GsonBuilder().create();
		JsonElement elem = g.fromJson(response, JsonElement.class);
		return ProvenanceCollectionDeserializer.convertOwner(elem); 
	}
} // End RESTProvenanceClient