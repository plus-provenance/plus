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
package org.mitre.provenance.client;

import java.util.List;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.dag.TraversalSettings;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.user.User;

/**
 * This class defines the methods that all provenance clients must implement.
 * @author moxious
 * @TODO this class should have functionality that unifies the RESTful API with Neo4JStorage.  Many more methods to add.
 */
public abstract class AbstractProvenanceClient {
	protected User viewer = User.PUBLIC;
	
	/**
	 * Get the user object that will be used for all requests.
	 * @return the user
	 */
	public User getViewer() { return viewer; } 
	
	/**
	 * Modify the user object that will be used for all requests. 
	 * @param user the new user to use for all subsequent requests.
	 */
	public void setUser(User user) { 
		this.viewer = user; 
	}
	
	/**
	 * Report new provenance to the provenance store.  This has the effect of creating new objects.
	 * @param col the collection to report
	 * @return true if successful, false if unsuccessful.
	 * @throws ProvenanceClientException
	 */
	public abstract boolean report(ProvenanceCollection col) throws ProvenanceClientException;
	
	/**
	 * Fetch provenance from a store.
	 * @param oid the ID of the object to start from
	 * @param desc a set of traversal settings describing how provenance should be discovered.
	 * @return a provenance collection
	 * @throws ProvenanceClientException
	 * @see org.mitre.provenance.dag.TraversalSettings
	 */
	public abstract ProvenanceCollection getGraph(String oid, TraversalSettings desc) throws ProvenanceClientException;

	/**
	 * Determine whether or not an object exists.
	 * @param obj the object to check
	 * @return the object that's stored by the same ID, or null if it does not exist.
	 * @throws ProvenanceClientException 
	 */
	public PLUSObject exists(PLUSObject obj) throws ProvenanceClientException { 
		return exists(obj.getId());
	}
	
	/** 
	 * Checks to see if an actor exists by an AID. 
	 * @param aid
	 * @return the PLUSActor, or null if it doesn't exist.
	 * @throws ProvenanceClientException
	 */
	public abstract PLUSActor actorExists(String aid) throws ProvenanceClientException;
	
	/**
	 * Determine whether or not an object exists.
	 * @param oid
	 * @return
	 * @throws ProvenanceClientException 
	 */
	public PLUSObject exists(String oid) throws ProvenanceClientException { 
		TraversalSettings s = new TraversalSettings()
		 						.excludeEdges()
		 						.excludeNPEs()
		 						.includeNodes()
		 						.setMaxDepth(1)
		 						.dontExpandWorkflows()
		 						.ignoreNPIDs()
		 						.setN(1);
		
		ProvenanceCollection col = getGraph(oid, s);
		if(col.isEmpty()) return null;
		return col.getNode(oid);
	} // End exists
	
	/**
	 * Fetch provenance from a store, using the default traversal settings.
	 * @param oid the ID of the object to start from
	 * @return a provenance collection
	 */
	public ProvenanceCollection getGraph(String oid) throws ProvenanceClientException { return getGraph(oid, new TraversalSettings()); }
	
	/**
	 * List workflows in the catalog (most recent) up to a certain max number.
	 * @param max
	 * @return
	 * @throws ProvenanceClientException
	 */
	public abstract List<PLUSWorkflow> listWorkflows(int max) throws ProvenanceClientException;
	
	public abstract PLUSObject getSingleNode(String oid) throws ProvenanceClientException;
	public abstract ProvenanceCollection getWorkflowMembers(String oid, int max) throws ProvenanceClientException;
	
	/**
	 * @return a collection containing the latest reported objects.
	 * @throws ProvenanceClientException
	 */
	public abstract ProvenanceCollection latest() throws ProvenanceClientException;
	
	/**
	 * Return a collection containing the latest reported actors.
	 * @param max the maximum number of items to return.
	 * @return a ProvenanceCollection containing only actors.
	 * @throws ProvenanceClientException
	 */
	public abstract ProvenanceCollection getActors(int max) throws ProvenanceClientException;
	
	public ProvenanceCollection getActors() throws ProvenanceClientException {
		return getActors(100);
	}
	
	/**
	 * Search for provenance matching a certain term
	 * @param searchTerm the search term
	 * @param max the maximum number of items to return.
	 * @return a provenance collection.
	 * @throws ProvenanceClientException
	 */
	public abstract ProvenanceCollection search(String searchTerm, int max) throws ProvenanceClientException;
	
	/**
	 * Search for provenance matching a certain set of metadata properties.
	 * @param parameters the properties being sought.
	 * @param max the maximum number of items to return.
	 * @return a provenance collection.
	 * @throws ProvenanceClientException
	 */
	public abstract ProvenanceCollection search(Metadata parameters, int max) throws ProvenanceClientException;
} // End AbstractProvenanceClient
