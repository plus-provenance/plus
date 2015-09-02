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

import java.io.IOException;
import java.util.List;
import java.util.ListIterator;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.dag.TraversalSettings;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.user.PrivilegeClass;
import org.mitre.provenance.user.PrivilegeSet;
import org.mitre.provenance.user.User;

/**
 * This class defines the methods that all provenance clients must implement.
 * @author moxious
 * TODO this class should have functionality that unifies the RESTful API with Neo4JStorage.  Many more methods to add.
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

	public ProvenanceCollection getGraph(PLUSObject obj, TraversalSettings desc) throws ProvenanceClientException { 
		return getGraph(obj.getId(), desc); 
	}
	
	/**
	 * Send a Cypher query and return the graph
	 * @param the query string written in Cypher
	 * @return the requested query stored as a ProvenanceCollection
	 * @throws IOException
	 */
	public abstract ProvenanceCollection query(String query) throws IOException; 
	
	
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
	 * Checks to see if an actor exists by a name.
	 * @param name the name
	 * @return the PLUSActor, or null if it doesn't exist.
	 * @throws ProvenanceClientException
	 */
	public abstract PLUSActor actorExistsByName(String name) throws ProvenanceClientException;
	
	/**
	 * Determine whether or not an object exists.
	 * @param oid the object ID of the object to check for existance
	 * @return true if the object exists, false otherwise.
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
	
	/**
	 * Indicate whether this set of privileges dominates a particular class.  If a member of this set dominates a particular
	 * class, then the user possessing that set is authorized to see data at that privilege class.   
	 * @param ps the set to check
	 * @param other the class to check
	 * @return true if any privilege class in the set dominates c
	 * @throws PLUSException
	 */
	public boolean dominates(PrivilegeSet ps, PrivilegeClass other) throws ProvenanceClientException {
		for(int x=0; x<ps.set.size(); x++) { 
			PrivilegeClass p = ps.set.get(x);
			if(dominates(p, other)) return true;
		}
			
		return false;
	}
	
	/**
	 * Determine whether this privilege set dominates another.
	 * @param one the first set to check.
	 * @param other the other you want to check.
	 * @return true if one privilege set contains a set of privilege classes that dominates all privilege classes in the
	 * other privilege set, false otherwise.  In the case of incomparable PrivilegeSets, this method will return 
	 * false.
	 * @throws PLUSException
	 */
	public boolean dominates(PrivilegeSet one, PrivilegeSet other) throws ProvenanceClientException { 
		try {
			ListIterator <PrivilegeClass> otherprivileges = (other.getPrivilegeSet()).listIterator();
			while (otherprivileges.hasNext()) {
				PrivilegeClass otherprivilege = otherprivileges.next();
				if (!dominates(one, otherprivilege)) 	
					return false;
			} // End while
			
			return true;
		} catch(PLUSException e) { 
			throw new ProvenanceClientException(e); 
		} // End catch
	} // End dominates(PrivilegeSet)
	
	public abstract boolean dominates(PrivilegeClass a, PrivilegeClass b) throws ProvenanceClientException;
	
	/**
	 * Determine whether this user can see information at privilege class p.
	 * @param user the user
	 * @param p the class you want to check
	 * @return true if the user has this privilege, or has a privilege which dominates it.
	 */
	public boolean authorizedFor(User user, PrivilegeClass p) throws ProvenanceClientException {
		if(user == User.DEFAULT_USER_GOD) return true;
		return dominates(user.getPrivileges(), p);
	}
		
	/**
	 * Determine whether or not a given user is permitted to see a given object.
	 * @param user the user whose permissions should be checked.
	 * @param obj the object, which may or may not be visible to the user.
	 * @param conjunctive if true, use a conjunctive check.  If false, use a disjunctive check.
	 * @return true if the user is permitted to see the object, false otherwise.
	 * @throws PLUSException
	 */
	public boolean canSee(User user, PLUSObject obj, boolean conjunctive) throws PLUSException {
		try {
			if (conjunctive) return canSee(user, obj);
			return canSeeDisjunctive(user, obj);
		} catch(PLUSException e) { 
			throw new PLUSException("PrivilegeSet#dominatesDisjunctive: " + e, e); 
		}
	}	
	
	/**
	 * Check and see if a given PLUSObject should be visible to this user.
	 * @param user the user in question
	 * @param obj the object you want to check.
	 * @return true if the user's privileges dominate ALL of the privileges required by the object, false otherwise.
	 * @throws PLUSException
	 */
	public boolean canSee(User user, PLUSObject obj) throws ProvenanceClientException {
		if(user == User.DEFAULT_USER_GOD) return true;
		if(user.getPrivileges().contains(PrivilegeClass.ADMIN)) return true;
		
		PrivilegeSet ps = obj.getPrivileges();
		
		// Check each privilege class in the object.
		// If the user isn't authorized for one of them, they can't see this object.
		for(int x=0; x<ps.set.size(); x++) { 
			PrivilegeClass c = ps.set.get(x);
					
			if(!authorizedFor(user, c)) {
				// PLUSUtils.log.debug("Not authorized for " + c); 
				return false;
			} // else System.out.print(c + " (check), ");
		} // End for
		
		return true;
	}	
	
	/**
	 * Performs a disjunctive check to see whether a user can see the object in question.  
	 * @param user
	 * @param obj
	 * @return true if the user is authorized for ANY ONE of the privileges required by the object (not necessarily all)
	 */
	private boolean canSeeDisjunctive(User user, PLUSObject obj) throws PLUSException {		
		if(user == User.DEFAULT_USER_GOD) return true;
		if(user.getPrivileges().contains(PrivilegeClass.ADMIN)) return true; 
		
		PrivilegeSet ps = obj.getPrivileges();
		
		// Check each privilege class in the object.
		// If the user is authorized for one of them, they can see this object.
		for(int x=0; x<ps.set.size(); x++) { 
			PrivilegeClass c = ps.set.get(x);
			if(authorizedFor(user, c)) {
				return true;
			} // else System.out.print(c + " (check), ");
		} // End for

		return false;
	}	
} // End AbstractProvenanceClient
