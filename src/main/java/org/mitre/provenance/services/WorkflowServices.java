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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.user.User;

/**
 * Workflow services encompass RESTful services which provide access to workflows and their members.
 * @author david
 */
@Path("/workflow/")
public class WorkflowServices {

	@Path("/{oid:.*}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Returns the members of a particular workflow
	 * @param oid the oid of the workflow node in question.
	 * @return a D3 JSON formatted provenance collection
	 */
	public Response getWorkflowMembers(@Context HttpServletRequest req, @PathParam("oid") String oid, @DefaultValue("50") @QueryParam("n") int n) {
		if(oid == null || "".equals(oid)) return ServiceUtility.BAD_REQUEST("Must specify an oid");
		
		if(n > 50 || n <= 0) n = 50;
		
		User user = ServiceUtility.getUser(req);
		
		try { 
			PLUSObject obj = Neo4JPLUSObjectFactory.newObject(Neo4JStorage.oidExists(oid));
			if(obj == null) return ServiceUtility.NOT_FOUND("No object by oid " + oid);
			if(!obj.isWorkflow()) return ServiceUtility.BAD_REQUEST("Cannot list workflow members for a non-workflow object");
		
			// TODO user
			ProvenanceCollection col = Neo4JStorage.getMembers((PLUSWorkflow)obj, user, n);
		
			return ServiceUtility.OK(col);
		} catch(PLUSException exc) { 
			return ServiceUtility.ERROR(exc.getMessage());
		}				
	} // End getWorkflowMembers
	
	@Path("/latest")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Gets the list of the latest workflows reported
	 * @param n return the most recent n items
	 * @return D3 JSON formatted collection of objects.
	 */
	public Response getLatestWorkflows(@Context HttpServletRequest req, @DefaultValue("50") @QueryParam("n") int n) { 
		try { 			
			if(n > 50 || n <= 0) n = 50;
			
			User user = ServiceUtility.getUser(req);
			
			// TODO user
			List<PLUSWorkflow> workflows = Neo4JStorage.listWorkflows(user, n);
		
			ProvenanceCollection col = new ProvenanceCollection();
			for(PLUSWorkflow wf : workflows) col.addNode(wf); 
		
			return ServiceUtility.OK(col);
		} catch(PLUSException exc) { 
			return ServiceUtility.ERROR(exc.getMessage()); 
		}
	} // End getLatestWorkflows
}
