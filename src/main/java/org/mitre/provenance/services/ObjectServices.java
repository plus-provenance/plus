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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.dag.ViewedCollection;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.plusobject.marking.Taint;
import org.mitre.provenance.user.User;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * Object services encompass RESTful services for access to individual provenance objects.
 * @author moxious
 */
@Path("/object")
@Api(value = "/object", description = "Provenance Objects: data, invocations, workflows, activities, etc.")
public class ObjectServices {
	protected static Logger log = Logger.getLogger(ObjectServices.class.getName());
	
	@Context
	UriInfo uriInfo;
			
	@Path("/search")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
        // @Consumes("application/x-javascript")
	@ApiOperation(value = "Search for provenance objects by a given search term", notes="", response=ProvenanceCollection.class)
	@ApiResponses(value = {
	  @ApiResponse(code = 400, message="Error processing search")	  
	})			
	public Response search(@Context HttpServletRequest req,
			@ApiParam(value="the search term to use", required=true) 
	        @FormParam("searchTerm") String searchTerm,
	        @ApiParam(value="maximum items to return", required=true) @DefaultValue("50") @QueryParam("n") int n) {
		log.info("SEARCH POST '" + searchTerm + "'");
		try { 			
			//TODO : user
			ProvenanceCollection col = Neo4JPLUSObjectFactory.searchFor(searchTerm, User.DEFAULT_USER_GOD, n);			
			return ServiceUtility.OK(col, req);			
		} catch(Exception exc) { 
			exc.printStackTrace();
			return ServiceUtility.ERROR(exc.getMessage());			
		}
	} // End search
	
	@Path("/search/{term:.*}")	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Search for provenance objects by a given search term", notes="", response=ProvenanceCollection.class)
	@ApiResponses(value = {
	  @ApiResponse(code = 400, message="Error processing search")	  
	})			
	public Response searchTerm(@Context HttpServletRequest req,
			@ApiParam(value = "The ID of the actor", required=true) 
	        @PathParam("term") String term,
	        @ApiParam(value="maximum items to return", required=true) @DefaultValue("50") @QueryParam("n") int n) { 
		log.info("SEARCH GET '" + term + "'");
		try { 
			//TODO
			ProvenanceCollection col = Neo4JPLUSObjectFactory.searchFor(term, ServiceUtility.getUser(req), n);
			return ServiceUtility.OK(col, req);			
		} catch(Exception exc) { 
			exc.printStackTrace();
			return ServiceUtility.ERROR(exc.getMessage());			
		}		
	}	
	
	@Path("/taint/{oid:.*}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get a list of taint nodes that are upstream influencers for a given OID", 
				  notes="Returns only nodes that represent taint (no edges).  It is assumed that these are non-local.", 
				  response=ProvenanceCollection.class)
	@ApiResponses(value = {
	  @ApiResponse(code = 404, message="Object not found"),
	  @ApiResponse(code = 400, message="Error processing request")	  
	})				
	/**
	 * Get a list of taint nodes that are upstream influencers for a given OID.
	 * @param oid the OID you want to know the taints for
	 * @return a D3 JSON document containing only the nodes that represent taint (no edges).  It is assumed
	 * that these are non-local.
	 * @throws PLUSException
	 */
	public Response getTaint(@Context HttpServletRequest req, 
			@ApiParam(value = "Object OID", required=true) @PathParam("oid") String oid) {
		// log.info("GET TAINT " + oid);
		
		User user = ServiceUtility.getUser(req);
		
		try { 
			Node n = Neo4JStorage.oidExists(oid);
			if(n == null) return ServiceUtility.NOT_FOUND("Invalid/non-existant oid");
			PLUSObject obj = Neo4JPLUSObjectFactory.newObject(n); 			
			ProvenanceCollection col = Neo4JPLUSObjectFactory.getAllTaintSources(obj, user);
			return ServiceUtility.OK(col, req);			
		} catch(PLUSException exc) { 
			exc.printStackTrace();
			return ServiceUtility.ERROR(exc.getMessage());
		} // End catch
	} // End getTaint
	
	@Path("/taint/{oid:.*}") 
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Mark an item as tainted", 
	  notes="This modifies the graph to assert that a given item is tainted", 
	  response=ProvenanceCollection.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message="Object not found"),
			@ApiResponse(code = 400, message="Error processing request")	  
	})				
	public Response setTaint(@ApiParam(value = "Object OID", required=true) @PathParam("oid") String oid, 
			@ApiParam(value = "Description of reason for taint", required=true) @FormParam("reason") String reason, 
			@Context HttpServletRequest req, 
			MultivaluedMap<String, String> queryParams) {
		User user = ServiceUtility.getUser(req);

		// String reason = queryParams.getFirst("reason");
		// log.info("SET TAINT " + oid + " reason '" + reason + "'");
				
		if(oid == null || "".equals(oid)) return ServiceUtility.BAD_REQUEST("Missing oid");
		if(reason == null || "".equals(reason.trim())) return ServiceUtility.BAD_REQUEST("Must specify non-empty reason");
		
		Node n = Neo4JStorage.oidExists(oid);
		
		if(n == null) return ServiceUtility.BAD_REQUEST("Cannot POST taint to a non-existant node");

		try { 
			PLUSObject object = Neo4JPLUSObjectFactory.newObject(n);		
			Taint t = Neo4JPLUSObjectFactory.taint(object, user, reason);
			ProvenanceCollection c = new ProvenanceCollection();
			
			c.addNode(t);
			return ServiceUtility.OK(c, req);
		} catch(PLUSException exc) {
			exc.printStackTrace();
			return ServiceUtility.ERROR(exc.getMessage());
		} // End catch
	} // End setTaint
	
	@Path("/taint/{oid:.*}") 
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value="Remove taint marking from an object.", 
	  notes="This modifies the graph to remove the assertion that a given item is tainted", 
	  response=ProvenanceCollection.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message="Object not found"),
			@ApiResponse(code = 400, message="Error processing request or no oid provided")	  
	})					
	public Response removeTaint(@Context HttpServletRequest req, 
			@ApiParam(value = "Object OID", required=true) @PathParam("oid") String oid) {
		User user = ServiceUtility.getUser(req);

		// log.info("REMOVE TAINT " + oid);
		if(oid == null || "".equals(oid)) return ServiceUtility.BAD_REQUEST("Must specify oid");
		
		try {
			Node n = Neo4JStorage.oidExists(oid);
			if(n == null) return ServiceUtility.NOT_FOUND("No such object by oid " + oid);
			
			PLUSObject obj = Neo4JPLUSObjectFactory.newObject(n);
			
			if(obj.isHeritable() && obj instanceof Taint)
				return ServiceUtility.BAD_REQUEST("Remove taint from the tainted object, do not remove the taint object itself.");
			
			n = null;
			
			Neo4JPLUSObjectFactory.removeTaints(obj);
			
			// Return the list of taints found.
			return ServiceUtility.OK(Neo4JPLUSObjectFactory.getAllTaintSources(obj, user), req);
		} catch(PLUSException exc) { 
			exc.printStackTrace();
			return ServiceUtility.ERROR(exc.getMessage());
		} // End catch
	} // End removeTaint
	
	@Path("/edges/{oid:.*}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value="Get the set of edges associated with an object", 
	  notes="Returns a collection of edges incident to an object.", 
	  response=ProvenanceCollection.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message="Object not found"),
			@ApiResponse(code = 400, message="Error processing request or no oid provided")	  
	})					
	public Response getEdges(@Context HttpServletRequest req, 
			@ApiParam(value = "Object OID", required=true) @PathParam("oid") String oid) {
		// log.info("EDGES " + oid);
		try { 
			ArrayList<String>oids = new ArrayList<String>();
			oids.add(oid);
			
			if(Neo4JStorage.oidExists(oid) == null)
				return ServiceUtility.NOT_FOUND("No object by that oid"); 				
			
			ProvenanceCollection col = Neo4JPLUSObjectFactory.getIncidentEdges(oids, ServiceUtility.getUser(req), "both", true, false);
			
			return ServiceUtility.OK(col, req);			
		} catch(PLUSException exc) { 
			exc.printStackTrace();
			return ServiceUtility.ERROR(exc.getMessage());			
		} // End catch
	} // End getEdges
	
	@Path("/metadata/{field:.*}/{value:.*}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value="Get objects with a particular metadata field value", 
	  notes="Returns a collection of objects with the specified metadata field name and value.", 
	  response=ProvenanceCollection.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message="Error processing request or no invalid fields provided")	  
	})						
	public Response getObjectBySingleMetadataField(@Context HttpServletRequest req, 
			@ApiParam(value="Metadata field name", required=true) @PathParam("field") String field, 
			@ApiParam(value="Metadata field value", required=true) @PathParam("value") String value) {
		if(field == null || "".equals(field) || field.length() > 128)
			return ServiceUtility.BAD_REQUEST("Invalid field specified.");
		if(value == null || "".equals(value) || value.length() > 256)
			return ServiceUtility.BAD_REQUEST("Invalid value specified.");
				
		User user = ServiceUtility.getUser(req);
		try {
			ProvenanceCollection col = Neo4JPLUSObjectFactory.loadBySingleMetadataField(user, field, value);			
			return ServiceUtility.OK(col, req);
		} catch (PLUSException e) {
			e.printStackTrace();
			return ServiceUtility.ERROR(e.getMessage());
		}		
	}
	
	@Path("/npid/{npid:.*}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value="Get a list of provenance objects by their available non-provenance IDs, if any", 
	  notes="Returns a collection of objects", 
	  response=ProvenanceCollection.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message="Error processing request or no invalid fields provided")	  
	})							
	public Response getObjectByNPID(@Context HttpServletRequest req, 
			@ApiParam(value = "Non-provenance ID", required=true) @PathParam("npid") String npid) {
		// log.info("OBJECT BY NPID " + npid);
		if(npid == null || "".equals(npid)) return ServiceUtility.BAD_REQUEST("Must specify npid");
		
		try (Transaction tx = Neo4JStorage.beginTx()) { 
			ViewedCollection col = new ViewedCollection(ServiceUtility.getUser(req));			
			Node n = Neo4JStorage.getNPID(npid, false);
			
			if(n == null) {
				tx.success();
				return ServiceUtility.NOT_FOUND("No such npid " + npid);				
			}
				
			Iterator<Relationship> rels = n.getRelationships(Direction.BOTH, Neo4JStorage.NPE).iterator();
			
			int x=0; 
			while(rels.hasNext()) { 
				Relationship r = rels.next();
				if(r.getStartNode().equals(n)) { 
					col.addNode(Neo4JPLUSObjectFactory.newObject(r.getEndNode()));
				} else { 
					col.addNode(Neo4JPLUSObjectFactory.newObject(r.getStartNode()));
				} // End else
				
				x++;
				if(x >= 10) break;
			} // End while
			
			tx.success();
			return ServiceUtility.OK(col, req);			
		} catch(PLUSException exc) { 
			exc.printStackTrace();
			return ServiceUtility.ERROR(exc.getMessage());			
		} // End catch
	} // End getObjectByNPID
	
	@Path("/{oid:.*}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value="Get an individual provenance object", 
	  notes="Returns a collection containing one object", 
	  response=ProvenanceCollection.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message="Error processing request or invalid fields provided"),	  
			@ApiResponse(code = 404, message="No such object")
	})								
	public Response getObject(@Context HttpServletRequest req, 
			@ApiParam(value = "Object ID", required=true) @PathParam("oid") String oid) {
		// log.info("GET OBJECT " + oid);
		if(oid == null || "".equals(oid)) return ServiceUtility.BAD_REQUEST("Must specify oid");
		
		try { 		
			Node n = Neo4JStorage.oidExists(oid);
			
			if(n == null) return ServiceUtility.NOT_FOUND("No such oid " + oid);			
			
			PLUSObject obj = Neo4JPLUSObjectFactory.newObject(n);								
			ViewedCollection col = new ViewedCollection(ServiceUtility.getUser(req));
			col.addNode(obj);
						
			return ServiceUtility.OK(col, req);			
		} catch(PLUSException exc) { 
			exc.printStackTrace();
			return ServiceUtility.ERROR(exc.getMessage());			
		} // End catch
	} // End getObject
} // End ObjectServices
