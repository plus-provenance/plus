package org.mitre.provenance.services;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.client.AbstractProvenanceClient;
import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.user.PrivilegeClass;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * Privilege services exposes RESTful services that allow users to interact with privilege sets in the PLUS system.
 * @author moxious
 */
@Path("/privilege")
@Api(value = "/privilege", description = "Privileges involved in provenance")
public class PrivilegeServices {
	@Path("/dominates/{pid1:.*}/{pid2:.*}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Determine whether one privilege class dominates another", notes="", response=Boolean.class)
	@ApiResponses(value = {
	  @ApiResponse(code = 404, message = "PrivilegeClass doesn't exist or can't be found")	  
	})			
	public Response getActor(@Context HttpServletRequest req,
			@ApiParam(value = "The ID of the first PrivilegeClass", required=true) @PathParam("pid1") String pid1,
			@ApiParam(value = "The ID of the second PrivilegeClass", required=true) @PathParam("pid1") String pid2) { 				
		try { 
			AbstractProvenanceClient client = new LocalProvenanceClient(ServiceUtility.getUser(req));
			PrivilegeClass pc1 = Neo4JPLUSObjectFactory.newPrivilegeClass(Neo4JStorage.privilegeClassExistsById(pid1));
			PrivilegeClass pc2 = Neo4JPLUSObjectFactory.newPrivilegeClass(Neo4JStorage.privilegeClassExistsById(pid2));
			
			boolean result = Neo4JStorage.dominates(pc1, pc2);
			return ServiceUtility.OK(new Boolean(result));
		} catch(PLUSException exc) { 
			exc.printStackTrace();
			return ServiceUtility.ERROR(exc.getMessage());
		} // End catch
	} // End getActor
}
