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
package 	org.mitre.provenance.user;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.db.neo4j.Neo4JCapable;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;

/**
 * A privilege class is a kind of identity that a user can have.  PLUSObjects can require that a user have a 
 * particular privilege class in order to see an object.  Privilege classes are a partially ordered domain.
 * <p>Worth noting though: users don't have privilege classes, they have privilege sets.
 * @see PrivilegeSet#PrivilegeSet()
 * @author DMALLEN
 */
public class PrivilegeClass implements Neo4JCapable {
	protected static Logger log = Logger.getLogger(PrivilegeClass.class.getName());
		
	protected static final String GOD_ID = "urn:uuid:plus:111111111111111111111111111111111111";
	protected static final String NATIONAL_SECURITY_ID = "urn:uuid:plus:000000000000000000000000000000000014";
	protected static final String PRIVATE_MEDICAL_ID = "urn:uuid:plus:000000000000000000000000000000000013";
	protected static final String EMERGENCY_LOW_ID = "urn:uuid:plus:000000000000000000000000000000000012";
	protected static final String EMERGENCY_HIGH_ID = "urn:uuid:plus:000000000000000000000000000000000011";
	protected static final String PUBLIC_ID = "urn:uuid:plus:000000000000000000000000000000000015";
	
	/* Static singletons for commonly used PCs */
	public static final PrivilegeClass ADMIN = new PrivilegeClass(GOD_ID, "Super User", "Super User");;
	public static final PrivilegeClass NATIONAL_SECURITY = new PrivilegeClass(NATIONAL_SECURITY_ID, "National Security");
	public static final PrivilegeClass PRIVATE_MEDICAL = new PrivilegeClass(PRIVATE_MEDICAL_ID, "Private Medical");
	public static final PrivilegeClass EMERGENCY_HIGH = new PrivilegeClass(EMERGENCY_HIGH_ID, "Emergency High");
	public static final PrivilegeClass EMERGENCY_LOW = new PrivilegeClass(EMERGENCY_LOW_ID, "Emergency Low");
	public static final PrivilegeClass PUBLIC = new PrivilegeClass(PUBLIC_ID, "Public");
	
	/** the name of the privilege class */
	protected String name;
	
	/** a PLUS OID */
	protected String id;
	
	/** Brief description */
	protected String description;
	
	/** When created */
	protected long created;
	
	/**
	 * As a special case you can create a privilege class that is a "security level" -- a totally ordered 0-10 setup
	 * similar to what was in the first iteration of the prototype.
	 * @param level the security level you want.  This must be 0-10
	 */
	public PrivilegeClass(int level) {
		if(level < 0) level = 0;
		if(level > 10) level = 10;
		
		// These special case security levels for totally-ordered integers are already hard-wired in the DB.		
		if(level == 10)
			id = "urn:uuid:plus:000000000000000000000000000000000010";
		else id = "urn:uuid:plus:00000000000000000000000000000000000" + level;
		
		name = "Security Level " + level;
		description = "Security Level " + level;
		setCreated(System.currentTimeMillis());
	} // End PrivilegeClass
		
	/**
	 * Create a new privilege class.  Note this constructor is used with data loaded from the database.  You 
	 * cannot create a new item in the database by using this call.
	 * @param id ID from database
	 * @param name name from database
	 * @param description description from database.
	 */
	protected PrivilegeClass(String id, String name, String description) {		
		setName(name);
		setId(id);
		setDescription(description);
		setCreated(System.currentTimeMillis());
	} // End PrivilegeClass
	
	protected PrivilegeClass(String id, String name) {
		this(id, name, name);
	}
	
	public String getId() { return id; } 
	
	public long getCreated() { return created; } 
	public String getDescription() { return description; } 
	public void setDescription(String description) { this.description = description; } 
	public void setId(String id) { this.id = id; } 
	
	public void setCreated(long d) { this.created = d; } 
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }	
	
	/**
	 * Comparison predicate for Privilege classes.  They are equal if and only if their database IDs are equal.
	 * @param p class to compare against
	 * @return true if they are functionally the same, false otherwise.
	 */
	public boolean equals(Object p) { 
		if(p == null) return false;
		if(!(p instanceof PrivilegeClass)) return false;
		
		return getId().equals(((PrivilegeClass)p).getId());
	} // End equals
	
	public String toString() { 
		return new String("[[" + getName() + "]]");
	}
	
	/**
	 * One privilege class dominates another when it is at an equal or higher level of security.  All classes
	 * trivially dominate themselves.
	 * @param other the class to compare against.
	 * @return true if this object dominates other, false otherwise.
	 * @throws PLUSException
	 */
	public boolean dominates(PrivilegeClass other) throws PLUSException {
		if(equals(other)) return true;   // Every class trivially dominates itself.

		String query = "start n=node:node_auto_index(pid=\"" + getId() + "\") " + 
                "match n-[r:" + Neo4JStorage.DOMINATES.name() + "*..100]->m " +   
		        "where has(m.pid) and m.pid = \"" + other.getId() + "\" " + 
                "return m ";
		
		try { 
			PrivilegeClass pc = Neo4JPLUSObjectFactory.newPrivilegeClass((Node)Neo4JStorage.execute(query).columnAs("m").next());			
			if(pc.getName().equals(other.getName())) return true;
			throw new PLUSException("Inconsistency:  " + pc.getName() + " vs " + other.getName());
		} catch(NoSuchElementException nse) {
			// This happens when no element was returned by the query, i.e. this privilege class doesn't dominate the other.
			return false;
		} catch(Exception exc) { 
			exc.printStackTrace();
			return false;
		}		
	} // End dominates
	
	public Map<String, Object> getStorableProperties() {
		Map<String,Object> m = new HashMap<String,Object>();
		
		m.put("name", getName());
		m.put("pid", getId());
		m.put("description", getDescription());
		m.put("type", "privilegeclass");
		m.put("created", getCreated());
		
		return m;
	}

	public PrivilegeClass setProperties(PropertyContainer props) throws PLUSException {
		setName(""+props.getProperty("name"));
		setId(""+props.getProperty("pid"));
		setDescription(""+props.getProperty("description"));
		
		Long c = (long)props.getProperty("created", null);
		if(c != null) setCreated(c);
		else setCreated(System.currentTimeMillis());
		
		return this;
	} // End setProperties
} // End PrivilegeClass