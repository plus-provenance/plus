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
package org.mitre.provenance.openid;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.mitre.openid.connect.model.UserInfo;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.client.AbstractProvenanceClient;
import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.user.OpenIDUser;
import org.mitre.provenance.user.PrivilegeClass;
import org.mitre.provenance.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.openid.OpenIDAttribute;
import org.springframework.security.openid.OpenIDAuthenticationToken;
import org.springframework.web.filter.GenericFilterBean;

/**
 * Spring filter which will populate the HTTPServletRequest with a PlusUser; translating
 * from an OpenID Connect (or OpenID 2.0) Java Security Principal object into either an
 * existing PlusUser or a new OpenIDUser which will be stored in the user database. 
 * 
 * In a JSP, the PlusUser can be accessed via the HTTPServletRequest as
 * request.getAttribute("plusUser"). 
 * 
 * @author Amanda Anganes
 */
public class OpenIDInterceptorFilter extends GenericFilterBean {
	protected static Logger log = Logger.getLogger(OpenIDInterceptorFilter.class.getName());
		
	AbstractProvenanceClient client = new LocalProvenanceClient();
	public static final String PLUS_USER = "plus_user";
	
	/**
	 * Handle turning an OpenID (2) token into a user.
	 */
	protected User handle(OpenIDAuthenticationToken oidToken) {				
		String oid2UniqueId = oidToken.getName();

		System.err.println("FILTER: OpenID2 Token ID " + oid2UniqueId + " cred " + oidToken.getCredentials() + 
				" details " + oidToken.getDetails()+ " principal " + oidToken.getPrincipal() + " message " + oidToken.getMessage());				
		
		User existingUser = null;
		
		try { 
			PLUSActor a = Neo4JPLUSObjectFactory.getActor(oid2UniqueId);		
			if(a instanceof User) existingUser = (User)a;
		} catch(PLUSException exc) { 
			log.severe("Could not load actor by ID " + oid2UniqueId);
			exc.printStackTrace();
		}
		
		if (existingUser != null) {
			// System.err.println("FILTER: OpenID2 existing user " + existingUser);
			return existingUser;			
		} else {									
			List<OpenIDAttribute> attributes = oidToken.getAttributes();
			
			System.err.println("FILTER: OpenID2 new user with " + attributes.size() + " attributes.");
			
			String oid2DisplayName = null;
			String oid2FirstName = null;
            String oid2LastName = null;
            String email = null;
            
			for (OpenIDAttribute attr : attributes) {
				String attrName = attr.getName();
		
				StringBuffer vals = new StringBuffer("");
				for(String val : attr.getValues()) vals.append(val + "/");
				System.err.println("OPEN ID ATTRIBUTE:  " + attrName + " type " + attr.getType() + " vals " + vals);
				
				if (attrName.equals("name")) {
					//This is the OpenID 2.0 display name.
                    //OpenID 2.0 Attribute Exchange (AX) is a little finicky, so this value
                    //may not be populated or may be stored uner a different attribute name.
					oid2DisplayName = attr.getValues().get(0);
				} else if (attrName.equals("firstName")) {
                    oid2FirstName = attr.getValues().get(0);
                } else if (attrName.equals("lastName")) {
                    oid2LastName = attr.getValues().get(0);
                } else if (attrName.equals("email")) {
                	email = attr.getValues().get(0);
                }                        
			}
            
            if (oid2DisplayName == null) {
                // Google sends first and last rather than "name"
                oid2DisplayName = oid2FirstName + oid2LastName;
            }
		
			OpenIDUser oid2User = new OpenIDUser(oid2UniqueId, (oid2DisplayName != null) ? oid2DisplayName : "Name Not Provided");
			oid2User.setEmail(email);

			// TODO:  Remove
			oid2User.addPrivilege(PrivilegeClass.ADMIN);
			oid2User.addPrivilege(PrivilegeClass.PUBLIC);
			
			try { 
				if(client.actorExists(oid2User.getId()) == null)
					client.report(ProvenanceCollection.collect(oid2User));
			} catch(PLUSException exc) { 
				log.severe("Could not save new user entry " + oid2User);
				exc.printStackTrace();
			}
        
			System.err.println("FILTER: set new OpenID2 user " + oid2User);
			return oid2User;
		}						
	} // End handle
	
	/**
	 * Handle turning an OpenID Connect token into a user.
	 */
	protected User handle(OIDCAuthenticationToken oidcToken) {
		//This object, defined by OpenID Connect, contains all sorts 
		//of user information. In the future, if you want to populate
		//more user data from it, it is available here.
		UserInfo userInformation = oidcToken.getUserInfo();
		
		//For federated identities, the guaranteed globally unique user identifier
		//is the combination of (in the case of OpenID Connect) the user's issuer
		//(authentication provider's URL) and their 'sub' value (unique id on the
		//issuer's server. Thus, we combine the two together to create a unique ID
		//for storage.
		
		String uniqueId = "" + oidcToken.getIssuer() + ":" + oidcToken.getSub();

		User existingUser = null;
		
		try { 
			PLUSActor a = Neo4JPLUSObjectFactory.getActor(uniqueId);		
			if(a instanceof User) existingUser = (User)a;
		} catch(PLUSException exc) { 
			log.severe("Could not load actor by ID " + uniqueId);
			exc.printStackTrace();
		}

		if (existingUser != null) {
			System.err.println("FILTER: existing user " + existingUser);
			return existingUser;
		} else {
			//Create a new user
			String displayName = userInformation.getGivenName() + " " + userInformation.getFamilyName();
			
			OpenIDUser newUser = new OpenIDUser(uniqueId, displayName);
			newUser.setEmail(userInformation.getEmail());

			// TODO:  Remove
			newUser.addPrivilege(PrivilegeClass.ADMIN);

			newUser.addPrivilege(PrivilegeClass.PUBLIC);			
			
			System.err.println("FILTER: new user " + newUser);

			try { 
				if(client.actorExists(newUser.getId()) == null)
					client.report(ProvenanceCollection.collect(newUser));
			} catch(PLUSException exc) { 
				log.severe("Could not save new user entry " + newUser);
				exc.printStackTrace();
			}
	
			return newUser;
		} // End else
	} // End handle
	
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain filterChain) throws IOException, ServletException {		
		HttpServletRequest httpRequest = (HttpServletRequest)request;
		HttpSession session = httpRequest.getSession();
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		
		//Only proceed if we have a logged-in user AND there is no PlusUser in the request
		//already. 
		if (auth != null && session.getAttribute(PLUS_USER) == null) {			
			System.err.println("FILTER: checking auth type");
			//If OpenID Connect:
			if (auth instanceof OIDCAuthenticationToken) {	
				System.err.println("FILTER: OIDC");
				User user = handle((OIDCAuthenticationToken)auth);
				session.setAttribute(PLUS_USER, user);
			} else if (auth instanceof OpenIDAuthenticationToken) {
				OpenIDAuthenticationToken oidToken = (OpenIDAuthenticationToken)auth;				
				String oid2UniqueId = oidToken.getName();

				System.err.println("FILTER: OpenID2 Token ID " + oid2UniqueId + " cred " + oidToken.getCredentials() + 
						" details " + oidToken.getDetails()+ " principal " + oidToken.getPrincipal() + " message " + oidToken.getMessage());				
				
				User user = handle(oidToken);
				session.setAttribute(PLUS_USER, user);
			} else 
				log.warning("Unrecognized token " + auth.getClass().getName());			
		} 
		
		//Continue the filter chain
		filterChain.doFilter(httpRequest, response);
	} // End doFilter
}
