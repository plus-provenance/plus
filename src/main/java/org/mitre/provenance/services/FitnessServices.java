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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.dag.TraversalSettings;
import org.mitre.provenance.dag.ViewedCollection;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.tools.PLUSUtils;
import org.mitre.provenance.user.User;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.graphdb.traversal.TraversalDescription;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * Services which drive the "fitness widget" functionality.
 * These services provide small custom analyses of provenance graphs, 
 * and return the structures for display.
 * 
 * <p>Most of these functions are menat to operate over graph structures in the 
 * database that are too large to be loaded into memory.  For that reason, they do not
 * provide interfaces that return ProvenanceCollection objects.
 * 
 * @author moxious
 */
@Path("/fitness")
@Api(value = "/fitness", description = "Provenance Fitness Assessments")
public class FitnessServices {
	protected static Logger log = Logger.getLogger(FitnessServices.class.getName());
	
	@GET
	@Path("/{oid:.*}/timelag")
	@ApiOperation(value = "Assess the time lag (oldest to newest) in a graph", notes="")
	@ApiResponses(value = {
	  @ApiResponse(code = 404, message="No such object"),	  
	})
	public Response timeLag(@Context HttpServletRequest req, 
			@ApiParam(value="Object ID as starting point", required=true) @PathParam("oid") String oid) throws PLUSException {
		Node n = Neo4JStorage.oidExists(oid);
		if(n == null) return ServiceUtility.NOT_FOUND("No such object");
					
		TraversalSettings settings = new TraversalSettings();
		settings.forward = false;
		settings.backward = true;
		settings.n = 500;
		settings.maxDepth = 500;
		settings.breadthFirst = false; // Go depth first to discover old stuff.
		settings.followNPIDs = false;
		settings.includeEdges = true;
		settings.includeNodes = true;
		settings.includeNPEs = false;

		User user = ServiceUtility.getUser(req);
				
		final HashSet<Long>seen = new HashSet<Long>();
		final PLUSObject [] oldestAndNewest = new PLUSObject[]{null, null};
		
		try (Transaction tx = Neo4JStorage.beginTx()) { 
			TraversalDescription desc = Neo4JPLUSObjectFactory.buildTraversalDescription(settings);
			
			// Visit each node, update oldest and newest accordingly.
			for(Node visit : desc.traverse(n).nodes()) {
				if(seen.contains(visit.getId())) continue;
				try {
					PLUSObject obj = Neo4JPLUSObjectFactory.newObject(visit);
					
					if(obj == null) { 
						log.warning("Null object on node " + visit);
						continue;
					}
					
					obj = obj.getVersionSuitableFor(user);
					
					if(obj == null) return ServiceUtility.FORBIDDEN("You do not have access to information necessary to complete this request.");
					
					if(oldestAndNewest[0]==null || obj.getCreated()<oldestAndNewest[0].getCreated()) 
						oldestAndNewest[0]=obj;
					if(oldestAndNewest[1]==null || obj.getCreated()>oldestAndNewest[1].getCreated()) 
						oldestAndNewest[1]=obj;					
				} catch(PLUSException exc) { 
					exc.printStackTrace();
				}
			} // End for
			
			tx.success();
		} catch(TransactionFailureException exc) { 
			log.severe("Failed transaction: " + exc.getMessage());
		}
		
		HashMap<String,Object> map = new HashMap<String,Object>();
		long span = oldestAndNewest[1].getCreated() - oldestAndNewest[0].getCreated();
		// Display the newest and oldest nodes if the DAG has nodes
		 			
		map.put("oldest", oldestAndNewest[0].getId());
		map.put("newest", oldestAndNewest[1].getId());
		map.put("timespan", PLUSUtils.describeTimeSpan(span));
		
		return ServiceUtility.OK(map);								
	} // End timeLag
	
	@GET
	@Path("/{oid:.*}/termFinder")	
	public Response termFinder(@Context HttpServletRequest req, @PathParam("oid") String oid, @QueryParam("term") String term) throws PLUSException {
		if(term == null || "".equals(term) || "".equals(term.trim()))
			return ServiceUtility.BAD_REQUEST("Missing term");
		
		User user = ServiceUtility.getUser(req);
		ViewedCollection col = new ViewedCollection(user);
		
		final Node startingPoint = Neo4JStorage.oidExists(oid);
		if(startingPoint == null) return ServiceUtility.NOT_FOUND("No such object " + oid);
		
		try (Transaction tx = Neo4JStorage.beginTx()) { 		
			TraversalSettings s = new TraversalSettings();			
			s.n = 500;
			s.maxDepth = -1;
			s.backward = true;
			s.forward = false;
			s.breadthFirst = true;
			s.followNPIDs = false;
			s.includeNodes = true;
			s.includeEdges = false;
			s.includeNPEs = false;
			
			TraversalDescription desc = Neo4JPLUSObjectFactory.buildTraversalDescription(s);
			
			final String termToFind = term.toLowerCase().trim();
			
			for(Node n : desc.traverse(startingPoint).nodes()) {
				String name = (""+n.getProperty("name", "")).toLowerCase();
				
				if(name.indexOf(termToFind) != -1)
					col.addNode(Neo4JPLUSObjectFactory.newObject(n));
			} // End for

			tx.success();
		} catch(TransactionFailureException exc) { 
			log.severe("Failed transaction: " + exc.getMessage());
		}
			
		return ServiceUtility.OK(col, req);
	} // End termFinder
	
	@GET 
	@Path("/{oid:.*}/summary")
	public Response summary(@Context HttpServletRequest req, @PathParam("oid") String oid) throws PLUSException {
		HashMap<String,Object> map = new HashMap<String,Object>();
		
		User user = ServiceUtility.getUser(req);
		Node n = Neo4JStorage.oidExists(oid);
		if(n == null) return ServiceUtility.NOT_FOUND("No such object " + oid);
		
		try { 
			PLUSObject o = Neo4JPLUSObjectFactory.newObject(n);
			if(o == null) return ServiceUtility.ERROR("Could not load object " + oid);
			
			// make sure to give the user one they can see.
			o = o.getVersionSuitableFor(user);
			
		    String type = o.getObjectType() + "/" + o.getObjectSubtype();
		    Date d = o.getCreatedAsDate();
		    
		    map.put("name", o.getName());
		    map.put("type", type);
		    map.put("created", ""+d);
		    
		    PLUSActor owner = o.getOwner();
		    map.put("summary", o.getName() + " (" + type + ") created " + d + " by " +
		    		(owner == null ? "unknown" : owner.getName()) + 
		            " with " + o.getMetadata().size() + " metadata items.");
		    
		    return ServiceUtility.OK(map);		    
		} catch(Exception exc) { 
			exc.printStackTrace();
			return ServiceUtility.ERROR(exc.getMessage());
		}
	} // End summary
				
	@GET 
	@Path("/{oid:.*}/custody")
	public Response custody(@Context HttpServletRequest req, @PathParam("oid") String oid) throws PLUSException {
		/*
		 * sampe query for path collationl.
		 * start c = node:node_auto_index ( object_id = '10179'  )
MATCH path =  c <- [ : PARENT_OF* ] – p
return  distinct
length ( path )  AS PATH_LENGTH
, extract ( n in nodes ( path ) : n.object_id ) as the_path
order by length ( path )
		 */
		
		String query = "start myTarget=node:node_auto_index(oid={oid}) " + 
	                   "match m-[r:contributed|marks|`input to`|unspecified|triggered|generated*]->myTarget " +
	                   "where has(m.ownerid) " + 
				       "return m.ownerid as ownerid";
		
		List<HashMap<String,Object>> owners = new ArrayList<HashMap<String,Object>>();
		
		try (Transaction tx = Neo4JStorage.beginTx()) { 			
			User user = ServiceUtility.getUser(req);
			log.info("custody(" + oid + ", " + user + ")");
			HashMap<String,Object>params = new HashMap<String,Object>();
			params.put("oid", oid);
			ExecutionResult r = Neo4JStorage.execute(query, params);
			ResourceIterator<Object> ownerids = r.columnAs("ownerid");
			
			while(ownerids.hasNext()) {
				String id = (String) ownerids.next();
				if(id == null || "".equals(id)) {
					log.info("Found empty ownerid " + id); 
					continue;
				} else {
					log.info("Found ownerid " + id);
					PLUSActor a = Neo4JPLUSObjectFactory.newActor(Neo4JStorage.actorExists(id));
					
					HashMap<String,Object> jsonObj = new HashMap<String,Object>();
					
					jsonObj.put("name", a.getName());
					jsonObj.put("type", a.getType());
					jsonObj.put("created", a.getCreated());
					jsonObj.put("aid", a.getId());
									
					owners.add(jsonObj);
					log.info("Added actor " + a); 
				} // End else
			}

			ownerids.close();
			tx.success();
		} catch(TransactionFailureException exc) { 
			log.severe("Failed transaction: " + exc.getMessage());
		}
		
		return ServiceUtility.OK(owners);
	} // End custody
} // End FitnessServices