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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.plusobject.ProvenanceCollection;

/**
 * Services related to non-provenance identifiers.
 * @author moxious
 */
@Path("/npid")
public class NPIDServices {
	@Path("/{npid:.*}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get a list of provenance objects associated with a given NPID
	 * @param the NPID you want to lookup
	 * @return a D3 JSON document containing only the nodes that represent provenance objects attached to that NPID
	 */
	public Response getProvenanceForNPID(@Context HttpServletRequest req, @PathParam("npid") String npid) {
		if(npid == null || "".equals(npid)) return ServiceUtility.BAD_REQUEST("Missing npid");
		
		if(Neo4JStorage.getNPID(npid, false) == null)
			return ServiceUtility.NOT_FOUND("No such non-provenance ID " + npid);
		
		try { 
			ProvenanceCollection col = Neo4JPLUSObjectFactory.getIncidentProvenance(npid, 50);
			return ServiceUtility.OK(col, req);
		} catch(PLUSException exc) { 
			exc.printStackTrace();
			return ServiceUtility.ERROR(exc.getMessage());
		}
	} // End getProvenanceForNPID
}
