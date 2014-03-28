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
package org.mitre.provenance.user;

import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSObject;
import org.neo4j.graphdb.PropertyContainer;

/**
 * A PLUS User, that is, an individual or organization using the provenance system.
 * It is extended from PLUSActor because users themselves can participate in provenance graphs.
 * @author DMALLEN
 */
public class User extends PLUSActor {
	protected static Logger log = Logger.getLogger(User.class.getName());
	
	public static final User DEFAULT_USER_PLUS = new User("Administrator", new PrivilegeSet(new PrivilegeClass(10)));
	
	public static final User DEFAULT_USER_ONE = new User("One", new PrivilegeSet(new PrivilegeClass(1)));
	public static final User DEFAULT_USER_TWO = new User("Two", new PrivilegeSet(new PrivilegeClass(2)));
	public static final User DEFAULT_USER_THREE = new User("Three", new PrivilegeSet(new PrivilegeClass(3)));
	public static final User DEFAULT_USER_FOUR = new User("Four", new PrivilegeSet(new PrivilegeClass(4)));
	public static final User DEFAULT_USER_FIVE = new User("Five", new PrivilegeSet(new PrivilegeClass(5)));
	public static final User DEFAULT_USER_SIX = new User("Six", new PrivilegeSet(new PrivilegeClass(6)));
	public static final User DEFAULT_USER_SEVEN = new User("Seven", new PrivilegeSet(new PrivilegeClass(7)));
	public static final User DEFAULT_USER_EIGHT = new User("Eight", new PrivilegeSet(new PrivilegeClass(8)));
	public static final User DEFAULT_USER_NINE = new User("Nine", new PrivilegeSet(new PrivilegeClass(9)));
	public static final User DEFAULT_USER_TEN = new User("Ten", new PrivilegeSet(new PrivilegeClass(10)));
	
	public static final User NATIONAL_SECURITY = new User("National Security User", new PrivilegeSet(PrivilegeClass.NATIONAL_SECURITY));
	public static final User PUBLIC = new User("Public", new PrivilegeSet(PrivilegeClass.PUBLIC)); 
	public static final User PRIVATE_MEDICAL = new User("Private Medical", new PrivilegeSet(PrivilegeClass.PRIVATE_MEDICAL)); 
	
	public static final User DEFAULT_USER_GOD = new User("Uber User Universal Access", 
			new PrivilegeSet(PrivilegeClass.ADMIN, new PrivilegeClass(10)));
	
	/** The set of privileges this user has, which determines what the user can see. */
	protected PrivilegeSet privileges;
	
	/**
	 * Many users will have a machine-readable, unique identifier.
	 * For display purposes, store a display name as well.
	 */
	protected String displayName;
		
	public User() { 
		this("Default username", new PrivilegeSet());
	}
	
	public User(String username) { 
		this(username, new PrivilegeSet());	
		this.displayName = username;
	}
	
	public User(String username, PrivilegeSet privileges) {
		super(username);		
		
		if(privileges == null) privileges = new PrivilegeSet();
		else this.privileges = privileges;
		
		setType("user");
	}	
	
	protected void setPrivileges(PrivilegeSet ps) { this.privileges = ps; } 
	public PrivilegeSet getPrivileges() { return privileges; }	
	public void addPrivilege(PrivilegeClass p) { privileges.addPrivilege(p); } 
	
	/**
	 * Determine whether a user has an exact privilege.
	 * @param p the class you want to check.
	 * @return true if the user has this exact privilege, false otherwise.
	 * @see User#authorizedFor(PrivilegeClass)
	 */
	public boolean hasPrivilege(PrivilegeClass p) {		
		boolean r = false;
		if(this == User.DEFAULT_USER_GOD) r = true;
		
		r = privileges.contains(p);
		// log.info(p + " => " + r); 
		return r;
	} // End hasPrivilege
	
	/**
	 * Determine whether this user can see information at privilege class p.
	 * @param p the class you want to check
	 * @return true if the user has this privilege, or has a privilege which dominates it.
	 * @see User#hasPrivilege(PrivilegeClass)
	 */
	public boolean authorizedFor(PrivilegeClass p) throws PLUSException {
		if(this == User.DEFAULT_USER_GOD) return true;
		return privileges.dominates(p);
	}
		
	public boolean canSee(PLUSObject obj, boolean conjunctive) throws PLUSException {
		try {
			if (conjunctive) return canSee(obj);
			return canSeeDisjunctive(obj);
		} catch(PLUSException e) { 
			throw new PLUSException("PrivilegeSet#dominatesDisjunctive: " + e, e); 
		}
	}
	
	public String getDisplayName() { return displayName; }
	
	public void setDisplayName(String dname) { displayName = dname; }
	
	/**
	 * @param obj
	 * @return
	 */
	private boolean canSeeDisjunctive(PLUSObject obj) throws PLUSException {		
		if(this == User.DEFAULT_USER_GOD) return true;
		if(hasPrivilege(PrivilegeClass.ADMIN)) return true; 
		
		PrivilegeSet ps = obj.getPrivileges();
		
		// Check each privilege class in the object.
		// If the user is authorized for one of them, they can see this object.
		for(int x=0; x<ps.set.size(); x++) { 
			PrivilegeClass c = ps.set.get(x);
			if(authorizedFor(c)) {
				return true;
			} // else System.out.print(c + " (check), ");
		} // End for

		return false;
	}

	/**
	 * Check and see if a given PLUSObject should be visible to this user.
	 * @param obj the object you want to check.
	 * @return true if the user has permissions enough to see this object, false otherwise.
	 * @throws PLUSException
	 */
	public boolean canSee(PLUSObject obj) throws PLUSException {
		// System.out.print("*** User " + getName() + " can see " + obj + "? ");
		
		if(this == User.DEFAULT_USER_GOD) return true;
		if(hasPrivilege(PrivilegeClass.ADMIN)) return true;
		
		PrivilegeSet ps = obj.getPrivileges();
		
		// Check each privilege class in the object.
		// If the user isn't authorized for one of them, they can't see this object.
		for(int x=0; x<ps.set.size(); x++) { 
			PrivilegeClass c = ps.set.get(x);
					
			if(!authorizedFor(c)) {
				// PLUSUtils.log.debug("Not authorized for " + c); 
				return false;
			} // else System.out.print(c + " (check), ");
		} // End for
		
		return true;
	}
	
	public String toString() { 
		return "[User:" + getName() + " level " + getPrivileges() + "]";
	} // End toString()
		
	public Map<String,Object> getStorableProperties() {
		Map<String,Object> map = super.getStorableProperties();
		map.put("displayName", displayName);		
		return map;
	}	
	
	public Object setProperties(PropertyContainer props) throws PLUSException {		
		super.setProperties(props);
		displayName = ""+props.getProperty("displayName");
		return this;
	} 		
} // End class User
