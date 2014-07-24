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
package org.mitre.provenance.services;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.dag.ViewedCollection;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSSerializer;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.plusobject.json.JSONConverter;
import org.mitre.provenance.user.User;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;

import com.google.gson.GsonBuilder;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * This class provides utility methods as helpers for other service implementations.
 * @author moxious
 */
public class ServiceUtility {
	public static User getUser(HttpServletRequest req) {
		Object o = req.getSession().getAttribute("plus_user");		
		if(o instanceof User) return (User)o;
		
		// TODO
		return User.DEFAULT_USER_GOD;
	} // End getUser
	
	/**
	 * Execute a simple cypher query that returns *ONLY* a list of nodes, and craft a response based on the query results.
	 * @param req the request object
	 * @param cypherQuery the query to execute
	 * @param nodeColumn the name of the column that the query will result in, which contains the node information
	 * @return a JSON response of the query results.
	 * @throws PLUSException
	 */
	public static Response OK_ExecuteQuery(HttpServletRequest req, String cypherQuery, String nodeColumn) throws PLUSException {
		ViewedCollection col = new ViewedCollection(ServiceUtility.getUser(req));
		
		try (Transaction tx = Neo4JStorage.beginTx()) { 
			ExecutionResult result = Neo4JStorage.execute(cypherQuery);
			Iterator<Node> nodes = result.columnAs(nodeColumn);
			
			while(nodes.hasNext()) {
				try {
					PLUSObject obj = Neo4JPLUSObjectFactory.newObject(nodes.next());
					if(obj != null) col.addNode(obj);
				} catch (PLUSException e) {
					e.printStackTrace();
					return ServiceUtility.ERROR(e.getMessage());
				}
			}
			
			tx.success();
		} catch(TransactionFailureException exc) {
			// TODO
			// Again this is bad, but it's a workaround.
			// IGNORE exception.
			// exc.printStackTrace();
			// return ServiceUtility.ERROR(exc.getMessage());
		}
		
		return ServiceUtility.OK(col);
	}
	
	public static Response OK(Boolean b) { 
		return Response.ok(new GsonBuilder().setPrettyPrinting().create().toJson(b), 
				MediaType.APPLICATION_JSON).build();		
	}
	
	public static Response OK(Map<String,Object> map) {
		return Response.ok(new GsonBuilder().setPrettyPrinting().create().toJson(map), 
				MediaType.APPLICATION_JSON).build();
	}
		
	public static Response OK(List<?> list) { 
		String json = new GsonBuilder().setPrettyPrinting().create().toJson(list);		
		return Response.ok(json, MediaType.APPLICATION_JSON).build();		
	}
	
	/**
	 * Examine the "format" parameter, and the "Accept" header to guess at which representation is best for return.
	 * Options:<br/>
	 * text/turtle or format=ttl:  PROV-TTL<br/>
	 * application/rdf+xml or format=rdf:  PROV-RDF<br/>
	 * application/provenance+xml or format=xml: PROV-XML<br/>
	 * json or format=json: D3 JSON<br/>
	 * Default: D3 JSON
	 * @param req an original request
	 * @return suggested format for the response to a given request
	 */
	public static PLUSSerializer.Format suggestFormat(HttpServletRequest req) { 
		String acceptedTypes = req.getHeader("Accept");
		if(acceptedTypes == null) acceptedTypes = "";
			
		String format = req.getParameter("format");
		if(format == null) format = "";

		boolean acceptsTTL  = acceptedTypes.contains("text/turtle") || "ttl".equals(format) || "prov-ttl".equals(format);
		boolean acceptsRDF  = acceptedTypes.contains("application/rdf+xml") || "rdf".equals(format) || "prov-rdf".equals(format);
		boolean acceptsXML  = acceptedTypes.contains("application/provenance+xml") || "xml".equals(format) || "prov-xml".equals(format);
		boolean acceptsJSON = acceptedTypes.contains("json") || "json".equals(format);

		if(acceptsJSON) return PLUSSerializer.Format.D3_JSON;
		if(acceptsRDF) return PLUSSerializer.Format.PROV_RDF;
		if(acceptsXML) return PLUSSerializer.Format.PROV_XML;
		if(acceptsTTL) return PLUSSerializer.Format.PROV_TTL;
		
		return PLUSSerializer.Format.D3_JSON;
	} // End suggestFormat
	
	public static MediaType formatToMediaType(PLUSSerializer.Format fmt) { 
		switch(fmt) {
		case PROV_XML:
			return new MediaType("application", "provenance+xml");
		case PROV_TTL:
			return new MediaType("application", "x-turtle");
		case PROV_RDF:
			return new MediaType("application", "rdf+xml"); 
		case D3_JSON:
		default:
			return MediaType.APPLICATION_JSON_TYPE;
		}
	}
	
	/**
	 * Convenience function for returning HTTP OK response, with variable representation format depending on user request.
	 * @param col the collection
	 * @param req the request
	 * @return an OK response containing a serialized collection
	 */
	public static Response OK(ProvenanceCollection col, HttpServletRequest req) {		
		PLUSSerializer.Format fmt = suggestFormat(req);

		PLUSSerializer serializer = new PLUSSerializer();
		try { 
			String data = serializer.serialize(col, fmt);				
			MediaType responseType = formatToMediaType(fmt);
			
			return Response.ok(data, responseType).build();
		} catch(Exception exc) { 
			exc.printStackTrace();
			return ERROR(exc.getMessage());
		}
	} // End OK
	
	/**
	 * Convenience function for returning an HTTP OK response, with the D3 formatted JSON results from a 
	 * provenance collection.  If you want the response type to depend on the paramters of the request, then see alternative methods.
	 * @param col
	 * @return an application/json response containing D3-JSON
	 * @see ServiceUtility#OK(ProvenanceCollection, HttpServletRequest)
	 */
	public static Response OK(ProvenanceCollection col) {		
		return Response.ok(JSONConverter.provenanceCollectionToD3Json(col), MediaType.APPLICATION_JSON).build();
	}
	
	/**
	 * Convenience function for serializing a feed as rss/xml and returning an OK response.
	 * @param feed
	 * @return an OK response containing an RSS/XML feed.
	 * @throws FeedException
	 */
	public static Response OK(SyndFeed feed) throws FeedException {
		return Response.ok(new SyndFeedOutput().outputString(feed), "application/rss+xml").build();
	}
	
	/**
	 * Convenience function for returning an internal server error response, with a message.
	 * @param msg
	 * @return an INTERNAL_SERVER_ERROR response containing a message
	 */
	public static Response ERROR(String msg) { 
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
	}
	
	/**
	 * Indicate that access to an item is forbidden.
	 * @param msg
	 * @return a FORBIDDEN response with a given message
	 */
	public static Response FORBIDDEN(String msg) {
		return Response.status(Response.Status.FORBIDDEN).entity(msg).build();
	}
	
	/**
	 * Convenience function for returning an HTTP not found response, with a message.
	 * @param msg
	 * @return a NOT_FOUND response containing a given message
	 */
	public static Response NOT_FOUND(String msg) { 
		return Response.status(Response.Status.NOT_FOUND).entity(msg).build();
	}
	
	/**
	 * Convenience function for returning an HTTP bad request response, with a message.
	 * @param msg
	 * @return a BAD_REQUEST response containing a given message
	 */
	public static Response BAD_REQUEST(String msg) { 
		return Response.status(Response.Status.BAD_REQUEST).entity(msg).build();
	}	
}
