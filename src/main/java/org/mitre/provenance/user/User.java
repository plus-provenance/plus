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

import java.util.Map;
import java.util.logging.Logger;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.PropertyCapable;
import org.mitre.provenance.PropertySet;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.ProvenanceCollection;

/**
 * A PLUS User, that is, an individual or organization using the provenance system.
 * It is extended from PLUSActor because users themselves can participate in provenance graphs.
 * @author moxious
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
	
	/**
	 * Create a new user with the specified username.
	 * @param username
	 */
	public User(String username) { 
		this(username, new PrivilegeSet());	
		this.displayName = username;
	}
	
	/**
	 * Create a new user with the specified username and associated privileges.
	 * @param username
	 * @param privileges
	 */
	public User(String username, PrivilegeSet privileges) {
		super(username);		
		
		if(privileges == null) privileges = new PrivilegeSet();
		else this.privileges = privileges;
		
		setType("user");
	}	
	
	/** Modify the users privileges */
	protected void setPrivileges(PrivilegeSet ps) { this.privileges = ps; } 
	public PrivilegeSet getPrivileges() { return privileges; }	
	public void addPrivilege(PrivilegeClass p) { privileges.addPrivilege(p); } 
	
	public String getDisplayName() { return displayName; }
	public void setDisplayName(String dname) { displayName = dname; }
		
	public String toString() { 
		return "[User:" + getName() + " level " + getPrivileges() + "]";
	} // End toString()
		
	/**
	 * @see PropertyCapable
	 */
	public Map<String,Object> getStorableProperties() {
		Map<String,Object> map = super.getStorableProperties();
		map.put("displayName", displayName);		
		return map;
	}	
	
	/**
	 * @see PropertyCapable
	 */
	public Object setProperties(PropertySet props, ProvenanceCollection contextCollection) throws PLUSException {		
		super.setProperties(props, contextCollection);
		displayName = ""+props.getProperty("displayName");
		return this;
	} 		
} // End class User
