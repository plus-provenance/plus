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
package org.mitre.provenance.dag;

import java.net.URLDecoder;
import java.util.List;
import java.util.logging.Logger;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.tools.PLUSUtils;
import org.mitre.provenance.user.User;

/** 
 * Generates LineageDAG objects
 * @deprecated 
 */
public class LineageDAGFactory {	
	private static final Logger log = Logger.getLogger(LineageDAGFactory.class.getName());
	
	public static LineageDAG generateDAG(String objectID, User user, Integer maxNodes) throws PLUSException { 
		return generateDAG(objectID, user, maxNodes, true); 
	}
	
	public static LineageDAG generateDAG(String objectID, User user, 
										 Integer maxNodes, boolean expandWorkflows) throws PLUSException { 		
		if(maxNodes==null) maxNodes = 50;		
		// ProvenanceObjectManager manager = ProvenanceObjectManager.getInstance();
		//LineageDAG dag = new DAGBuilder().discoverCollection(manager, objectID, user, maxNodes);
		
		TraversalSettings s = new TraversalSettings();		
		LineageDAG dag = Neo4JPLUSObjectFactory.newDAG(objectID, user, s);
		log.info("Generated dag from neo4j: " + dag); 
		
		PLUSObject obj = dag.getNode(objectID);
		if(expandWorkflows && obj != null && obj.isWorkflow()) { 
			log.warning("Selected object " + objectID + " " + obj.getName() + " is a workflow: re-fetching local workflow details."); 
			PLUSWorkflow wf = (PLUSWorkflow)obj;
			
			try { 
				ProvenanceCollection pcol = Neo4JStorage.getMembers(wf, user, 10);						
				
				if(pcol.countNodes() <= 0) {
					// This should never happen; it corresponds to a nested workflow that's empty.  But it's possible,
					// so in this case we should just return the higher-level workflow.
					log.severe("Nested workflow " + objectID + " contains no members locally!  Defaulting to base lineage graph."); 
					return dag;
				}
				
				List<PLUSObject> members = pcol.getNodesInOrderedList(ProvenanceCollection.SORT_BY_CREATION);
				
				// This could contain nested workflow members, and we don't want to hop down more than one level at a time.
				// As a result, we want to re-call generateDAG on the first non-workflow object.  We sorted by creation date, so
				// this should be the *earliest created* object in the workflow.
				for(PLUSObject o : members) { 
					if(o.isWorkflow()) continue;
					return generateDAG(o.getId(), user, maxNodes); 
				}
			} catch(PLUSException exc) { 
				throw new PLUSException("Failed to fetch local workflow details for selected object " + objectID, exc); 
			} // End catch
		} // End if
		
		return dag;
	} // End generateDAG
	
	public static String normalizeID(String objectID) { 
		if(PLUSUtils.isPLUSOID(objectID)) return objectID;
		
		if(objectID == null) {
			log.warning("Warning: cannot normalize ID null");
			return null;
		}
		
		if(objectID.toLowerCase().contains("mashablelogic") && objectID.toLowerCase().contains("uuid")) {
			// Links will look like this:
			// http://mashablelogic.mitre.org:8080/acme/execServerAssembly.jsp?name=CruiseShips2&assembly_uuid=f0db3a8c0ac346038a97933f9f74d994
			
			String [] tokens = new String [] { "assembly_uuid", "part_uuid", "mashup_uuid" } ;
			
			for(int x=0; x<tokens.length; x++) {
				log.warning("Looking for " + tokens[x] + " in MashableLogic URI " + objectID);
				int pos = objectID.indexOf(tokens[x]);				
				if(pos == -1) { log.warning("No " + tokens[x]); continue; }
				
				try { 
					String sub = objectID.substring(pos + tokens[x].length() + 1);
					log.warning("Substring is '" + sub + "'"); 
					String ML_uuid = null;
					if(sub.indexOf("&") == -1) ML_uuid = sub;
					else ML_uuid = sub.substring(0, sub.indexOf("&"));
					
					if(ML_uuid != null) return ML_uuid;
					else throw new Exception("Couldn't find " + tokens[x] + " MashableLogic UUID in '" + sub + "'"); 
				} catch(Exception exc) { 
					log.warning("Failed to parse ID '" + objectID + "': " + exc.getMessage());
					return objectID;
				}
			} 
		} else if(!PLUSUtils.isPLUSOID(objectID)) {
		   	// External identifiers must be URL-decoded, because they can contain things like & characters.
		   	try { objectID = URLDecoder.decode(objectID, "UTF-8"); }
		   	catch(Exception exc) { System.err.println(exc); }
		   	
		   	return objectID;		   	
		} 
		
		return objectID;
	} // End normalizeID
} // End LineageDAGFactory