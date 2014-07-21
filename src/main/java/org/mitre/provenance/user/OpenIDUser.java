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
import org.mitre.provenance.PropertySet;
import org.mitre.provenance.plusobject.ProvenanceCollection;

public class OpenIDUser extends User {
	protected static Logger log = Logger.getLogger(OpenIDUser.class.getName());
	public static final String OPENID_USER_TYPE = "OpenIDUser";	
	
	protected String userIdentifier = null;
	protected String email = null;
	
	public OpenIDUser() { 
		super();
		setType(OPENID_USER_TYPE);
	}
	
	public OpenIDUser(String id, String displayName) {
		super();
		setType(OPENID_USER_TYPE);
		
		setName(id);
		setUserIdentifier(id);
		setDisplayName(displayName);
	}
	
	public String getEmail() { return email; } 
	public void setEmail(String email) { this.email = email; } 
	
	public String getUserIdentifier() { return userIdentifier; } 
	protected void setUserIdentifier(String userid) { this.userIdentifier = userid; } 
	
	public Map<String,Object> getStorableProperties() {
		Map<String,Object> map = super.getStorableProperties();
		map.put("userIdentifier", userIdentifier);
		map.put("email", email);
		return map;
	}	
	
	public Object setProperties(PropertySet props, ProvenanceCollection contextCollection) throws PLUSException {		
		super.setProperties(props, contextCollection);
		userIdentifier = ""+props.getProperty("userIdentifier");
		email = ""+props.getProperty("email", "");
		return this;
	} 
} // End OpenIDUser
