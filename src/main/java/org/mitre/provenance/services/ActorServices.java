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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.neo4j.graphdb.Node;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * Actor services exposes RESTful services that allow users to interact with actors in the PLUS system.
 * @author dmallen
 */
@Path("/actor")
@Api(value = "/actor", description = "Actors who are involved in provenance")
public class ActorServices {
	@Path("/{aid:.*}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get information about an actor", notes="", response=HashMap.class)
	@ApiResponses(value = {
	  @ApiResponse(code = 404, message = "Actor doesn't exist or can't be found")	  
	})			
	public Response getActor(
			@ApiParam(value = "The ID of the actor", required=true) @PathParam("aid") String actorID) { 
		Map<String,Object> map = new HashMap<String,Object>();		
		try { 
			Node n = Neo4JStorage.actorExists(actorID);		
			if(n == null) return ServiceUtility.NOT_FOUND("No such actor " + actorID);		
			PLUSActor a = Neo4JPLUSObjectFactory.newActor(n);			
			map = a.getStorableProperties();			
			return ServiceUtility.OK(map);
		} catch(PLUSException exc) { 
			exc.printStackTrace();
			return ServiceUtility.ERROR("Unable to fetch actor " + actorID + " please consult the systems administrator");
		} // End catch
	} // End getActor
} // End ActorServices
