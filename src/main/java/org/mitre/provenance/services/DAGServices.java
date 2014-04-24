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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.dag.LineageDAG;
import org.mitre.provenance.dag.TraversalSettings;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.plusobject.json.JsonFormatException;
import org.mitre.provenance.plusobject.json.ProvenanceCollectionDeserializer;
import org.mitre.provenance.user.User;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponses;
import com.wordnik.swagger.annotations.ApiResponse;

/**
 * DAGServices encompassess RESTful services that operate over provenance "DAGs" (directed acyclic graphs).
 * @author dmallen
 */
@Path("/graph")
@Api(value = "/graph", description = "Operations about provenance graphs")
public class DAGServices {
	protected static Logger log = Logger.getLogger(DAGServices.class.getName());
	
	public class CollectionFormatException extends Exception {
		private static final long serialVersionUID = 2819285921155590440L;
		public CollectionFormatException(String msg) { super(msg); } 
	}
	
	@Path("/{oid:.*}")	
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	@GET
	@ApiOperation(value = "Get a provenance graph", notes = "More notes about this method", response = ProvenanceCollection.class)
	@ApiResponses(value = {
	  @ApiResponse(code = 400, message = "Error loading graph"),
	  @ApiResponse(code = 404, message = "Base object ID not found") 
	})	
	public Response getGraph(@Context HttpServletRequest req, 
			@ApiParam(value = "The ID of the starting point from which to discover the graph", required = true)
			@PathParam("oid") String oid, 
			@ApiParam(value = "Maximum number of nodes to return", required = false)
			@DefaultValue("50") @QueryParam("n") int maxNodes,
			@ApiParam(value = "Maximum number of hops from starting point to traverse", required = false)
			@DefaultValue("8") @QueryParam("maxHops") int maxHops,
			@ApiParam(value = "Whether or not to include nodes in result", required = false)
			@DefaultValue("true") @QueryParam("includeNodes") boolean includeNodes,
			@ApiParam(value = "Whether or not to include edges in result", required = false)
			@DefaultValue("true") @QueryParam("includeEdges") boolean includeEdges,
			@ApiParam(value = "Whether or not to include non-provenance edges in result", required = false)
			@DefaultValue("true") @QueryParam("includeNPEs") boolean includeNPEs,
			@ApiParam(value = "Whether or not to follow non-provenance IDs in traversal", required = false)
			@DefaultValue("true") @QueryParam("followNPIDs") boolean followNPIDs,
			@ApiParam(value = "Return results forward of the starting point", required = false)
			@DefaultValue("true") @QueryParam("forward") boolean forward,
			@ApiParam(value = "Return results backward of the starting point", required = false)
			@DefaultValue("true") @QueryParam("backward") boolean backward,
			@ApiParam(value = "If true, traverse via BFS.  If false, use DFS", required = false)
			@DefaultValue("true") @QueryParam("breadthFirst") boolean breadthFirst) {				

		TraversalSettings ts = new TraversalSettings();
		ts.n = maxNodes;
		ts.maxDepth = maxHops;
		ts.backward = backward;
		ts.forward = forward;
		ts.includeNodes = includeNodes;
		ts.includeEdges = includeEdges;
		ts.includeNPEs = includeNPEs;
		ts.followNPIDs = followNPIDs;
		ts.breadthFirst = breadthFirst;
		
		log.info("GET D3 GRAPH " + oid + " / " + ts);
		
		if(maxNodes <= 0) return ServiceUtility.BAD_REQUEST("n must be greater than zero");		
		if(maxHops <= 0) return ServiceUtility.BAD_REQUEST("Max hops must be greater than zero");		
		
		try { 
			if((Neo4JStorage.oidExists(oid) == null) && (Neo4JStorage.getNPID(oid, false) == null))  
				return Response.status(Response.Status.NOT_FOUND).entity("Entity not found for " + oid).build();
			
			LineageDAG col = Neo4JPLUSObjectFactory.newDAG(oid, ServiceUtility.getUser(req), ts);
			log.info("D3 Graph for " + oid + " returned " + col); 
			
			return ServiceUtility.OK(col, req);					
		} catch(PLUSException exc) { 
			log.severe(exc.getMessage());
			exc.printStackTrace();
			return ServiceUtility.ERROR(exc.getMessage());					
		} // End catch
	} // End getD3Graph
	
	/**
	 * This function is needed to check the format of incoming collections to see if they are 
	 * loggable.
	 * @param col
	 * @return the same collection, or throws an exception on error.
	 */
	public ProvenanceCollection checkGraphFormat(ProvenanceCollection col) throws CollectionFormatException { 
		for(PLUSObject o : col.getNodes()) {
			if(Neo4JStorage.oidExists(o.getId()) != null)
				throw new CollectionFormatException("Node named " + o.getName() + " / " + o.getId() + " has a duplicate ID");
		}
		
		return col;
	} // End checkGraphFormat
	
	@SuppressWarnings("unchecked")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/new")
	@ApiOperation(value = "Report a new provenance graph", notes = "Write the contents of new provenance to the database", response = ProvenanceCollection.class)
	@ApiResponses(value = {
	  @ApiResponse(code = 400, message = "Invalid data provided")	  
	})
	/**
	 * Creates a new graph in the provenance store.  The parameters posted must include an item called "provenance" whose value
	 * is a D3 JSON object corresponding to the provenance graph that will be created.
	 * <p>This service will re-allocate new IDs for everything in the graph, and will *not* store the objects under the IDs provided by
	 * the user, to avoid conflicts on uniqueness.
	 * @param req 
	 * @param queryParams a set of parameters, which must contain an element "provenance" mapping to a D3 JSON object.
	 * @return a D3 JSON graph of the provenance that was stored, with new IDs.
	 * @throws JsonFormatException
	 */
	public Response newGraph(@Context HttpServletRequest req,
			@ApiParam(value = "D3-JSON formatted provenance graph", required = true)
			@FormParam("provenance") String provenance, 
			MultivaluedMap<String, String> queryParams) throws JsonFormatException {
		//String jsonStr = queryParams.getFirst("provenance");
		User reportingUser = ServiceUtility.getUser(req);		
		log.info("NEW GRAPH msg len " + (provenance == null ? "null" : provenance.length()) + " REPORTING USER " + reportingUser);
		
		if(provenance == null)  {
			Map<String,String[]> params = req.getParameterMap();
			
			System.err.println("DEBUG:  bad parameters to newGraph");
			for(String k : params.keySet()) {
				String[] val = params.get(k);
				System.err.println(k + " => " + (val != null && val.length > 0 ? val[0] : "null"));
			}
			
			return ServiceUtility.BAD_REQUEST("You must specify a provenance parameter that is not empty.");
		}
		
		Gson g = new GsonBuilder().registerTypeAdapter(ProvenanceCollection.class, new ProvenanceCollectionDeserializer()).create();
		
		ProvenanceCollection col = null;
		try {
			col = g.fromJson(provenance, ProvenanceCollection.class);
			System.out.println("Converted from D3 JSON:  " + col);

			// Check format, and throw an exception if it's no good.
			col = checkGraphFormat(col);
			
			System.out.println("Tagging source...");
			col = tagSource(col, req);
			
			/* for many reasons, this is a bad idea.  leave stubbed out for now.
			System.out.println("Resetting IDs...");
			col = resetIDs(col);
			*/
			Neo4JStorage.store(col);
		} catch(CollectionFormatException gfe) {
			log.warning("Failed storing collection: " + gfe.getMessage());
			return ServiceUtility.BAD_REQUEST("Your collection contained a format problem: " + gfe.getMessage());
		} catch(JsonParseException j) {
			j.printStackTrace();
			return ServiceUtility.BAD_REQUEST(j.getMessage());			
		} catch(PLUSException exc) { 
			return ServiceUtility.ERROR(exc.getMessage());				
		}

		return ServiceUtility.OK(col);
	} // End newGraph

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/search")	
	/**
	 * Search the provenance store for objects with a particular cypher query.
	 * @param cypherQuery the cypher query
	 * @return a D3 JSON formatted provenance collection
	 * @deprecated
	 */
	public Response search(@ApiParam(value = "A cypher query", required=true) @FormParam("query") String cypherQuery) {
		int limit = 100;		

		// log.info("SEARCH " + cypherQuery);
		
		if(cypherQuery == null || "".equals(cypherQuery)) {
			return ServiceUtility.BAD_REQUEST("No query");
		}
		
		// Ban certain "stop words" from the query to prevent users from updating, deleting, or
		// creating data. 
		String [] stopWords = new String [] { "create", "delete", "set", "remove", "foreach", "merge" };
		String q = cypherQuery.toLowerCase();
		for(String sw : stopWords) { 
			if(q.contains(sw))  
				return ServiceUtility.BAD_REQUEST("Invalid query specified (" + sw + ")"); 			
		} // End for
		
		/* Begin executing query */

		ProvenanceCollection col = new ProvenanceCollection();
		try (Transaction tx = Neo4JStorage.beginTx()) { 
			log.info("Query for " + cypherQuery);
			ExecutionResult rs = Neo4JStorage.execute(cypherQuery);

			for(String colName : rs.columns()) {
				int x=0;							
				ResourceIterator<?> it = rs.columnAs(colName);

				while(it.hasNext() && x < limit) {
					Object next = it.next();
					
					if(next instanceof Node) { 
						if(Neo4JStorage.isPLUSObjectNode((Node)next))  
							col.addNode(Neo4JPLUSObjectFactory.newObject((Node)next));
						else { 
							log.info("Skipping non-provnenace object node ID " + ((Node)next).getId());
							continue;
						}
					} else if(next instanceof Relationship) { 
						Relationship rel = (Relationship)next;
						if(Neo4JStorage.isPLUSObjectNode(rel.getStartNode()) && 
						   Neo4JStorage.isPLUSObjectNode(rel.getEndNode())) {
							col.addNode(Neo4JPLUSObjectFactory.newObject(rel.getStartNode()));
							col.addNode(Neo4JPLUSObjectFactory.newObject(rel.getEndNode()));
							col.addEdge(Neo4JPLUSObjectFactory.newEdge(rel));
						} else { 
							log.info("Skipping non-provenace edge not yet supported " + rel.getId());
						}
					}
				} // End while
				
				it.close();
				
				if((col.countEdges() + col.countNodes()) >= limit) break;
			}			
			
			tx.success();
		} catch(Exception exc) { 
			exc.printStackTrace();
			return ServiceUtility.ERROR(exc.getMessage());
		}

		return ServiceUtility.OK(col);		
	} // End search
	
	protected Object formatLimitedSearchResult(Object o) { 
		if(o instanceof Node) { 
			Node n = (Node)o;
			if(Neo4JStorage.isPLUSObjectNode(n)) {
				HashMap<String,Object> nodeProps = new HashMap<String,Object>();
				nodeProps.put("oid", n.getProperty("oid"));
				nodeProps.put("name", n.getProperty("name", "Unknown"));
				return nodeProps;
			} else { 
				log.info("Skipping non-provenance object node ID " + n.getId());
				return null;
			}
		} else if(o instanceof Relationship) { 
			Relationship r = (Relationship)o;

			if(Neo4JStorage.isPLUSObjectNode(r.getStartNode()) && 
			   Neo4JStorage.isPLUSObjectNode(r.getEndNode())) {
				//TODO ;
			}
			
			HashMap<String,Object> relProps = new HashMap<String,Object>();
			
			relProps.put("from", formatLimitedSearchResult(r.getStartNode()));
			relProps.put("to", formatLimitedSearchResult(r.getEndNode()));
			relProps.put("type", r.getType().name());
			return relProps;
		} else if(o instanceof Iterable) { 
			ArrayList<Object> things = new ArrayList<Object>();
			for(Object so : (Iterable<?>)o) {
				Object ro = formatLimitedSearchResult(so);
				if(ro != null) things.add(ro);
			}
			
			return things;
		} else {
			log.info("Unsupported query response type " + o.getClass().getCanonicalName());
		}
		
		return null;
	} // End formatLimitedSearchResult
	
	/**
	 * Tag each of the objects in a provenance collection with information about the user that
	 * posted them.
	 * @param col the provenance collection to tag
	 * @param req the request that created the provenenace collection
	 * @return the modified collection
	 */
	protected ProvenanceCollection tagSource(ProvenanceCollection col, HttpServletRequest req) {		
		String addr = req.getRemoteAddr();
		String host = req.getRemoteHost();
		String user = req.getRemoteUser();
		String ua = req.getHeader("User-Agent");

		String tag = (user != null ? user : "unknown") + "@" + 
				host + " " + 
				(host.equals(addr) ? "" : "(" + addr + ") ") + 
				ua;
		long reportTime = System.currentTimeMillis();

		for(PLUSObject o : col.getNodes()) { 
			o.getMetadata().put("plus:reporter", tag);
			o.getMetadata().put("plus:reportTime", reportTime);
		}

		return col;
	} // End tagSource
} // End DAGServices
