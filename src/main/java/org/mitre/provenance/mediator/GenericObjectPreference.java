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
package org.mitre.provenance.mediator;

import java.util.logging.Logger;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.surrogate.SurrogateQuality;
import org.mitre.provenance.user.PrivilegeSet;

/**
 * A basic way to figure out preference for surrogates with no additional information.  
 * This is not the preferable way, but it beats having no default.
 * @author DMALLEN
 */
public class GenericObjectPreference extends ObjectPreference {
	protected static Logger log = Logger.getLogger(GenericObjectPreference.class.getName());
	
	public GenericObjectPreference() { ; }
	
	/**
	 * @see SurrogatePreference#preference(PLUSObject, PLUSObject)
	 */
	public int preference(PLUSObject a, PLUSObject b){
		PrivilegeSet aPrivs = a.getPrivileges();
		PrivilegeSet bPrivs = b.getPrivileges(); 
		
		try { 
			if(aPrivs.dominates(bPrivs)) return -1; 
			if(bPrivs.dominates(aPrivs)) return 1; 
		} catch(PLUSException e) { 
			log.severe("Can't determine priv domination: " +	e);
			return -1; 
		} // End catch
				
		if(a.isSurrogate() && b.isSurrogate()) {  
			// OK, so one doesn't really dominate the other.  How do we tell?
			// Here's a really lame way.  Return whichever has more surrogate quality metadata.
			SurrogateQuality asq = a.getSurrogateDetail().getQuality();
			SurrogateQuality bsq = b.getSurrogateDetail().getQuality();
			
			if(asq.size() > bsq.size()) return -1;
			else if(bsq.size() > asq.size()) return 1;  
		} // End if
		
		return -1; 
	} // End preference
} // End GenericSurrogatePreference
