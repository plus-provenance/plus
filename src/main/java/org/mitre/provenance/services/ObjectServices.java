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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
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

import org.mitre.provenance.Metadata;
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

	@Path("/taint/marktaintandfling/{id:.*}")	
	@GET
	@Produces(MediaType.APPLICATION_JSON)	
	@ApiOperation(value = "Mark taint and return a provenance graph containing contaminated FLING", notes = "", response = ProvenanceCollection.class)
	@ApiResponses(value = {
	  @ApiResponse(code = 400, message = "Invalid data provided")	  
	})
	/**
	 * Given a node id or (ASIAS-specific) meta_id value in node Metadata, mark node as tainted and return the FLING of that marking.
	 * @param id the oid or meta_id (ASIAS-specific) of the node that should be marked tainted.
	 * @return a D3 JSON formatted provenance collection
	 * @author piekut
	 */
	public Response taintFLING(@Context HttpServletRequest req,
			@ApiParam(value="oid or meta_id (ASIAS-specific) of node to be marked tainted", required=true) 
			@PathParam("id") String id,
			@ApiParam(value="maximum items to return", required=true) @DefaultValue("50") @QueryParam("n") int n) { 
		int limit = 100;		
		      		
		if(id == null || "".equals(id)) {
			return ServiceUtility.BAD_REQUEST("No node oid or meta_id (ASIAS-specific) value specified");
		}
		
		boolean use_meta_id = false;  // Generic case, mark by node OID.
		if (!id.startsWith("urn:uuid:")) { use_meta_id=true; }  // ASIAS-specific (meta_id) if not recognized OID format.
		
		ProvenanceCollection col =  new ProvenanceCollection();
		try { 						
			PLUSObject node;
			
			if (use_meta_id) { // ASIAS-specific
				// find node that was identified by meta_id.
				ProvenanceCollection matchSet = org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory
					.loadBySingleMetadataField(ServiceUtility.getUser(req), "meta_id", id);		
				int numMatches = matchSet.getNodes().size();  

				if (numMatches==0) { return ServiceUtility.NOT_FOUND("No object found with meta_id='"+id+"'."); }
				else if (numMatches!=1) { return ServiceUtility.ERROR("ERROR:  duplicate matches found for meta_id='"+id+"'."); }
				
				node = matchSet.getNodes().iterator().next();
			}
			else {  //  default condition, retrieve node be OID.
				node = org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory.load(id, ServiceUtility.getUser(req));
			}
			
			// Mark the specified node as tainted.
			try {
				org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory.taint(node, ServiceUtility.getUser(req), "using REST service to taint node and return FLING");
			}
			catch (Exception e) {
				return ServiceUtility.ERROR("Failed to mark node with meta_id='"+id+"':  " + e.getMessage());
			}
		
			// Finally retrieve collection of tainted nodes downstream (i.e., FLING).
			try {
				col.addNode(node);  // add affected node, for completeness.
				col.addAll(org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory.getFullFLING(node.getId(), ServiceUtility.getUser(req)));
			}
			catch (Exception e) {
				return ServiceUtility.ERROR("Taint mark successful for node with id value '"+id+"', but FLING could not be retrieved:  " + e.getMessage());
			}
		} catch(Exception exc) { 
			exc.printStackTrace();
			return ServiceUtility.ERROR(exc.getMessage());
		}
		return ServiceUtility.OK(col, req);		
	} // End 
	
	
	
	@Path("/groupbyhash/{process_name:.*}")	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Retrieve all nodes belonging to particular type (name), and group based on their hashed value", 
		notes = "", response = HashMap.class)
	@ApiResponses(value = {
	  @ApiResponse(code = 400, message = "Invalid data provided")	  
	})
	/**
	 * Retrieve all nodes belonging to particular type (name), and report on the hash-match of specified types.
	 * @param process_name the name of the process nodes to examine.
	 * @return a Map of nodes grouped by matching hash value.
	 * @author piekut
	 */
	public Response groupTypeNodesByHashValue(@Context HttpServletRequest req,
			@ApiParam(value="name of node type to traverse", required=true) 
			@PathParam("process_name") String process_name) { 
		      		
		if (process_name == null || "".equals(process_name)) {
			return ServiceUtility.BAD_REQUEST("No process_name specified.");
		}
		
		ProvenanceCollection col =  new ProvenanceCollection();
		// variable storing groupings of hash matches.
		Map<String, ArrayList<String>> hashvalueMap = new TreeMap<String, ArrayList<String>>();
		try { 
			// Below line retrieves all nodes with name {process_name}
			col =  org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory.searchFor(process_name, ServiceUtility.getUser(req));
			int countRecs = 0;
			
			Iterator<PLUSObject> nodeIt = col.getNodes().iterator();
			while (nodeIt.hasNext()) {  // looping through all nodes with name {process_name}
				PLUSObject node = nodeIt.next();
				
				// since Neo4JPLUSObjectFactory.searchFor is fuzzy match, filter out to be exact 			
				if (node.getName().equals(process_name)) {  
					countRecs++; 
					
					// get hashValue of node, if it exists.
					String hashValue = (String) node.getMetadata().get(Metadata.CONTENT_HASH_SHA_256);
					if (hashValue==null) { hashValue="[No Content]"; }
					hashValue = process_name + " with hash value " + hashValue + ":";
					
					//Shove it into hashmap for later retrieval.
					ArrayList<String> hashArray = new ArrayList<String>();
					
					if (node.getMetadata().containsKey("meta_id")) {   // ASIAS-specific block!
						// Special case handling for ASIAS.  Generic case handled in the "else" block.
						String hashArrayValue = node.getId();
						ProvenanceCollection backwardsLineage = org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory.getFullBLING(node.getId(), ServiceUtility.getUser(req));
						Iterator<PLUSObject> blingNodes= backwardsLineage.getNodesInOrderedList().iterator();
						while (blingNodes.hasNext()) {
							PLUSObject blingNode = blingNodes.next();
							if (blingNode.getMetadata().containsKey("input_partition")) { 
								// The hash value is the input_partition value of the root node of the FLING.
								// Since that value potentially does not uniquely distinguish the runs, the meta_id
								// of the node is appended to the return set.
								hashArrayValue = (String) blingNode.getMetadata().get("input_partition")
										+ " [" +node.getMetadata().get("meta_id") + "]"; 
								break;
							}
						}
						hashArray.add(hashArrayValue);
					}
					else {  
						// This is the default scenario, for nodes in any other format than ASIAS.  
						// Returns OID as identifier.
						hashArray.add(node.getId()); 
					}
					
					// Here the code looks for previously grouped values for this hash, and if found, appends to 
					// hashArray and stores in hashvalueMap, the return variable. 
					if (!hashvalueMap.isEmpty() && hashvalueMap.containsKey(hashValue)) {
						hashArray.addAll(hashvalueMap.get(hashValue));
					}
					hashvalueMap.put(hashValue, hashArray);
				}
			}
						
			
			if (countRecs==0) { // warning if no nodes found.
				return ServiceUtility.NOT_FOUND("No records found with name '"+process_name+"'");
			}		
			
		} catch(Exception exc) { 
			exc.printStackTrace();
			return ServiceUtility.ERROR(exc.getMessage());
		}
		
		// If hashvalueMap was successfully populated, return that object as JSON.
		Map<String,Object> map =  new HashMap<String,Object>();
		map.putAll(hashvalueMap);
		return ServiceUtility.OK(map);	
	} // End 
			
	
	
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
