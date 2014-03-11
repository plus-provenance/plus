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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;

import org.mitre.provenance.PLUSException;

/**
 * A conjunction of PrivilegeClass objects.  Users can have privilege sets, or 
 * PLUS objects can require them for access.
 * @author DMALLEN, ACHAPMAN
 */
public class PrivilegeSet {
	protected Logger log = Logger.getLogger(PrivilegeSet.class.getName());
	
	public ArrayList <PrivilegeClass> set;
	private boolean conjunctive = true;	
	
	/**
	 * Create a new empty PrivilegeSet
	 */
	public PrivilegeSet() {		
		set = new ArrayList <PrivilegeClass> ();
	}
	
	/**
	 * Create a new PrivilegeSet with a single entry
	 * @param pc the PrivilegeClass for the set to contain.
	 */
	public PrivilegeSet(PrivilegeClass pc) { 		
		this();
		addPrivilege(pc);
	}
	
	public List <PrivilegeClass> getPrivilegeSet(){
		return set;
	}
	
	public boolean contains(PrivilegeClass p) { return set.contains(p); }	
	
	public void addPrivilege(PrivilegeClass p) {
		if(p != null)
			set.add(p);
		else {
			log.warning("Attempt to add a privilege class of NULL to PrivilegeSet");
		}
	}
	
	/**
	 * Indicate whether this set of privileges dominates a particular class.  If a member of the set dominates a particular
	 * class, then the user posessing that set is authorized to see data at that privilege class.
	 * @param c the class to check
	 * @return true if any privilege class in the set dominates c
	 * @throws PLUSException
	 */
	public boolean dominates(PrivilegeClass c) throws PLUSException {
		for(int x=0; x<set.size(); x++) { 
			PrivilegeClass p = set.get(x);
			if(p.dominates(c)) return true;
		}
		
		return false;
	} // End dominates
	
	/**
	 * Determine whether this privilege set dominates another.
	 * @param other the other you want to check.
	 * @return true if this privilege set contains a set of privilege classes that dominates all privilege classes in the
	 * other privilege set, false otherwise.  In the case of incomparable PrivilegeSets, this method will return 
	 * false.
	 * @throws PLUSException
	 */
	public boolean dominates(PrivilegeSet other) throws PLUSException { 
		if (!conjunctive)
			return dominatesDisjunctive(other);
		try {
			ListIterator <PrivilegeClass> otherprivileges = (other.getPrivilegeSet()).listIterator();
			while (otherprivileges.hasNext()) {
				PrivilegeClass otherprivilege = otherprivileges.next();
				if (!dominates(otherprivilege)) 	
					return false;
			} // End while
			return true;
		} catch(PLUSException e) { 
			throw new PLUSException("PrivilegeSet#dominates: " + e, e); 
		} // End catch
	} // End dominates(PrivilegeSet)
	
	public boolean dominatesDisjunctive(PrivilegeSet other) throws PLUSException{
		try {
			ListIterator <PrivilegeClass> otherprivileges = (other.getPrivilegeSet()).listIterator();
			while (otherprivileges.hasNext()) {
				PrivilegeClass otherprivilege = otherprivileges.next();
				if (dominates(otherprivilege)) 	
					return true;
			} // End while
			return false;			
		} catch(PLUSException e) { 
			throw new PLUSException("PrivilegeSet#dominatesDisjunctive: " + e, e); 
		}
	}
	
	public String toString() { 
		StringBuffer buf = new StringBuffer("(PS: ");
		
		if(set.size() == 0) { return new String("None"); } 
		
		for(int x=0; x<set.size(); x++) buf.append(set.get(x).toString());
		buf.append(")");
		
		return buf.toString();		
	}
} // End PrivilegeSet
