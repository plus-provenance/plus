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
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.dag.TraversalSettings;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.user.PrivilegeClass;
import org.mitre.provenance.user.User;

/**
 * This class permits the use of a provenance client attached to a local database.  This local class is essentially a 
 * wrapper around a number of methods found in the storage layer.
 * 
 * <p>Use this class to report provenance to a local database on disk.
 * 
 * @author moxious
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
	
	public boolean report(ProvenanceCollection col) throws ProvenanceClientException {
		try { 
			if(Neo4JStorage.store(col) > 0) return true;
			return false;
		} catch(PLUSException exc) { 
			throw new ProvenanceClientException(exc);  
		}
	}

	public ProvenanceCollection getGraph(String oid, TraversalSettings desc)
			throws ProvenanceClientException {
		// TODO Auto-generated method stub
		try {
			return Neo4JPLUSObjectFactory.newDAG(oid, user, desc);
		} catch (PLUSException e) { throw new ProvenanceClientException(e); } 
	}

	public PLUSObject exists(String oid) throws ProvenanceClientException { 
		org.neo4j.graphdb.Node n = Neo4JStorage.oidExists(oid);
		if(n == null) return null;
		try {
			return Neo4JPLUSObjectFactory.newObject(n);
		} catch (PLUSException e) {
			throw new ProvenanceClientException("Cannot convert object node", e);			
		}		
	}
	
	public ProvenanceCollection latest() throws ProvenanceClientException {
		// TODO Auto-generated method stub
		return Neo4JPLUSObjectFactory.getRecentlyCreated(user, 20);	
	}
	
	public ProvenanceCollection getActors(int max) throws ProvenanceClientException {
		try {
			return Neo4JStorage.getActors(max);
		} catch (PLUSException e) {
			throw new ProvenanceClientException(e); 
		}
	}

	public ProvenanceCollection search(String searchTerm, int max)
			throws ProvenanceClientException {
		return Neo4JPLUSObjectFactory.searchFor(searchTerm, viewer, max);
	}
	
	public ProvenanceCollection search(Metadata parameters, int max)
			throws ProvenanceClientException {
		try {
			return Neo4JPLUSObjectFactory.loadByMetadata(viewer, parameters, max);
		} catch (PLUSException e) {
			throw new ProvenanceClientException(e); 
		}
	}

	public List<PLUSWorkflow> listWorkflows(int max) throws ProvenanceClientException {
		try {
			return Neo4JStorage.listWorkflows(user, max);
		} catch (PLUSException e) {
			throw new ProvenanceClientException(e); 
		}
	}

	public ProvenanceCollection getWorkflowMembers(String oid, int max) throws ProvenanceClientException {
		PLUSObject obj = getSingleNode(oid);
		if(obj == null) throw new ProvenanceClientException("Can not load workflow members from non-existant node " + oid);
		if(!obj.isWorkflow()) throw new ProvenanceClientException("Can not load workflow members from non-workflow " + oid);		
		return Neo4JStorage.getMembers((PLUSWorkflow)obj, user, max);
	} // End getWorkflowMembers
	
	public PLUSObject getSingleNode(String oid) throws ProvenanceClientException {
		try {
			return Neo4JPLUSObjectFactory.load(oid, user);
		} catch (PLUSException e) {
			throw new ProvenanceClientException(e); 
		} 
	} // End getSingleNode

	public PLUSActor actorExists(String aid) throws ProvenanceClientException {
		org.neo4j.graphdb.Node n = Neo4JStorage.actorExists(aid);
		if(n != null) {
			try {
				return Neo4JPLUSObjectFactory.newActor(n);
			} catch (PLUSException e) {
				throw new ProvenanceClientException("Cannot convert actor", e);
			}
		} 
		
		return null;
	}

	public PLUSActor actorExistsByName(String name) throws ProvenanceClientException {
		org.neo4j.graphdb.Node n = Neo4JStorage.actorExistsByName(name);
		
		if(n != null) {
			try {
				return Neo4JPLUSObjectFactory.newActor(n);
			} catch (PLUSException e) {
				throw new ProvenanceClientException("Cannot convert actor", e);
			}
		} 
		
		return null;
	}
		
	public boolean dominates(PrivilegeClass a, PrivilegeClass b) throws ProvenanceClientException {
		try {
			return Neo4JStorage.dominates(a, b);
		} catch (PLUSException e) {
			throw new ProvenanceClientException(e);
		}
	}
} // End LocalProvenanceClient
