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
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.user.User;

/**
 * This class permits the use of a provenance client attached to a local database.  This local class is essentially a 
 * wrapper around a number of methods found in the storage layer.
 * @author david
 */
public class LocalProvenanceClient extends AbstractProvenanceClient {
	protected User user = User.PUBLIC;
	
	public LocalProvenanceClient() { this(User.PUBLIC); }
	
	/**
	 * Create a local client with a given user; all requests will be made as that user.
	 * @param u the user to use.
	 */
	public LocalProvenanceClient(User u) {		
		this.user = u;
	}
	
	public boolean report(ProvenanceCollection col) throws PLUSException {
		if(Neo4JStorage.store(col) > 0) return true;
		return false;
	}

	public ProvenanceCollection getGraph(String oid, TraversalSettings desc)
			throws PLUSException {
		// TODO Auto-generated method stub
		return Neo4JPLUSObjectFactory.newDAG(oid, user, desc);
	}

	public ProvenanceCollection latest() throws PLUSException {
		// TODO Auto-generated method stub
		return Neo4JPLUSObjectFactory.getRecentlyCreated(user, 20);	
	}
	
	public ProvenanceCollection getActors(int max) throws PLUSException {
		return Neo4JStorage.getActors(max);
	}

	public ProvenanceCollection search(String searchTerm, int max)
			throws PLUSException {
		return Neo4JPLUSObjectFactory.searchFor(searchTerm, viewer, max);
	}
	
	public ProvenanceCollection search(Metadata parameters, int max)
			throws PLUSException {
		return Neo4JPLUSObjectFactory.loadByMetadata(viewer, parameters, max);
	}	
} // End LocalProvenanceClient
