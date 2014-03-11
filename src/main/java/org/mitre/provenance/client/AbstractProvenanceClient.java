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

import org.mitre.provenance.Metadata;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.dag.TraversalSettings;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.user.User;

/**
 * This class defines the methods that all provenance clients must implement.
 * @author david
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
	 * @throws PLUSException
	 */
	public abstract boolean report(ProvenanceCollection col) throws PLUSException;
	
	/**
	 * Fetch provenance from a store.
	 * @param oid the ID of the object to start from
	 * @param desc a set of traversal settings describing how provenance should be discovered.
	 * @return a provenance collection
	 * @throws PLUSException
	 * @see org.mitre.provenance.dag.TraversalSettings
	 */
	public abstract ProvenanceCollection getGraph(String oid, TraversalSettings desc) throws PLUSException;
	
	/**
	 * Fetch provenance from a store, using the default traversal settings.
	 * @param oid the ID of the object to start from
	 * @return a provenance collection
	 */
	public ProvenanceCollection getGraph(String oid) throws PLUSException { return getGraph(oid, new TraversalSettings()); }
	
	
	/**
	 * @return a collection containing the latest reported objects.
	 * @throws PLUSException
	 */
	public abstract ProvenanceCollection latest() throws PLUSException;
	
	/**
	 * Return a collection containing the latest reported actors.
	 * @param max the maximum number of items to return.
	 * @return a ProvenanceCollection containing only actors.
	 * @throws PLUSException
	 */
	public abstract ProvenanceCollection getActors(int max) throws PLUSException;
	
	public ProvenanceCollection getActors() throws PLUSException {
		return getActors(100);
	}
	
	/**
	 * Search for provenance matching a certain term
	 * @param searchTerm the search term
	 * @param max the maximum number of items to return.
	 * @return a provenance collection.
	 * @throws PLUSException
	 */
	public abstract ProvenanceCollection search(String searchTerm, int max) throws PLUSException;
	
	/**
	 * Search for provenance matching a certain set of metadata properties.
	 * @param parameters the properties being sought.
	 * @param max the maximum number of items to return.
	 * @return a provenance collection.
	 * @throws PLUSException
	 */
	public abstract ProvenanceCollection search(Metadata parameters, int max) throws PLUSException;
} // End AbstractProvenanceClient
