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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
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
import org.mitre.provenance.user.PrivilegeClass;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
	
	protected static final String PRIVILEGE_PATH = "/plus/api/privilege/dominates/";
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
		
		ClientConfig cc = new ClientConfig().property(ClientProperties.FOLLOW_REDIRECTS, true);		
		client = ClientBuilder.newClient(cc);
		// client.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS, true);
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
		WebTarget r = client.target(buildURL(NEW_GRAPH_PATH));

		MultivaluedMap<String,String> formData = new MultivaluedHashMap<String,String>();
		String json = JSONConverter.provenanceCollectionToD3Json(col);
		// System.out.println("POSTING:\n" + json + "\n\n");
	    formData.add("provenance", json);
	    
	    Response response = r
	    		 .request(MediaType.APPLICATION_JSON_TYPE)
	    		 .accept(MediaType.APPLICATION_JSON_TYPE)
	    		 .header("User-Agent", UA)
	    		 .post(Entity.form(formData));	    				 
	    
		String output = response.readEntity(String.class);
	    
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
		WebTarget r = client.target(buildURL(GET_GRAPH_PATH + oid));
		
		MultivaluedMap<String,String> params = desc.asMultivaluedMap();
		
		for(String key : params.keySet()) 
			r = r.queryParam(key, params.get(key));
		System.out.println(r);
		
		Response response = r
				 .request(MediaType.APPLICATION_JSON_TYPE)
				 .accept(MediaType.APPLICATION_JSON_TYPE)
				 .header("User-Agent", UA)				 
				 .get();

		String resultingJSON = response.readEntity(String.class);
		System.out.println(resultingJSON);
		
		return provenanceCollectionFromResponse(resultingJSON);
	} // End getGraph
	
	public ProvenanceCollection latest() throws ProvenanceClientException {
		WebTarget r = client.target(buildURL(GET_LATEST_PATH));
		
		Response response = r				
				 .request(MediaType.APPLICATION_JSON_TYPE)				
				 .header("User-Agent", UA)				 
				 .get();
		
		return provenanceCollectionFromResponse(response.readEntity(String.class));
	}
	
	public ProvenanceCollection getActors(int max) throws ProvenanceClientException {
		WebTarget r = client.target(buildURL(GET_ACTORS_PATH) + "&n=" + max);				
		
		Response response = r
				 .request(MediaType.APPLICATION_JSON_TYPE)				 
				 .header("User-Agent", UA)				 
				 .get();

		return provenanceCollectionFromResponse(response.readEntity(String.class));
	} // End getActors
	
	public ProvenanceCollection search(String searchTerm, int max)
			throws ProvenanceClientException {		
		WebTarget r = client.target(buildURL(SEARCH_PATH) + "&n=" + max);
		
		Response response = r.request(MediaType.APPLICATION_JSON_TYPE)				 
				 .header("User-Agent", UA)				 
				 .get();		
		
		return provenanceCollectionFromResponse(response.readEntity(String.class));
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
		WebTarget r = client.target(buildURL(LIST_WORKFLOWS_PATH) + "&n=" + max);
		
		Response response = r
				 .request(MediaType.APPLICATION_JSON_TYPE)				 
				 .header("User-Agent", UA)				 
				 .get();		
		
		String txt = response.readEntity(String.class);
		
		System.out.println(txt);
		ProvenanceCollection col = provenanceCollectionFromResponse(txt);
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
		
		WebTarget r = client.target(buildURL(GET_WORKFLOW_MEMBERS_PATH) + n.getId() + "?format=json&n=" + max);
		
		Response response = r
				 .request(MediaType.APPLICATION_JSON_TYPE)				 
				 .header("User-Agent", UA)				 
				 .get();		
		
		return provenanceCollectionFromResponse(response.readEntity(String.class));
	} // End getWorkflowMembers

	public PLUSObject getSingleNode(String oid) throws ProvenanceClientException {
		WebTarget r = client.target(buildURL(GET_SINGLE_NODE_PATH) + oid + "?format=json");
		
		Response response = r
				 .request(MediaType.APPLICATION_JSON_TYPE)				 
				 .header("User-Agent", UA)				 
				 .get();		
		
		ProvenanceCollection col = provenanceCollectionFromResponse(response.readEntity(String.class));
		
		if(col.containsObjectID(oid)) return col.getNode(oid);		
		return null;
	} // End getSingleNode

	public PLUSActor actorExists(String aid) throws ProvenanceClientException {
		WebTarget r = client.target(buildURL(GET_ACTOR_PATH) + aid + "?format=json");
		
		Response response = r
				 .request(MediaType.APPLICATION_JSON_TYPE)				 
				 .header("User-Agent", UA)				 
				 .get();		
		
		Gson g = new GsonBuilder().create();
		JsonElement elem = g.fromJson(response.readEntity(String.class), JsonElement.class);
		if(!elem.isJsonObject()) throw new ProvenanceClientException("Server response wasn't a JSON object " + elem);
		
		return ProvenanceCollectionDeserializer.convertActor((JsonObject)elem); 
	}

	public boolean dominates(PrivilegeClass a, PrivilegeClass b)
			throws ProvenanceClientException {
		WebTarget r = client.target(buildURL(PRIVILEGE_PATH + a.getId() + "/" + b.getId()));

		Response response = r
				 .request(MediaType.APPLICATION_JSON_TYPE)			
				 .header("User-Agent", UA)				 
				 .get();		
		
		Gson g = new GsonBuilder().create();
		
		String txt = response.readEntity(String.class);
		
		JsonElement elem = g.fromJson(txt, JsonElement.class);
		
		if(elem.isJsonPrimitive()) return elem.getAsBoolean();
		
		throw new ProvenanceClientException(txt);
	} // End dominates
} // End RESTProvenanceClient