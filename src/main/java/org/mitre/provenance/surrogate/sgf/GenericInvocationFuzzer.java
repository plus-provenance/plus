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
package org.mitre.provenance.surrogate.sgf;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.plusobject.PLUSInvocation;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.surrogate.SignPost;
import org.mitre.provenance.surrogate.SurrogateDetail;
import org.mitre.provenance.surrogate.SurrogateException;
import org.mitre.provenance.surrogate.SurrogateGeneratingFunction;
import org.mitre.provenance.surrogate.SurrogateQuality;
import org.mitre.provenance.user.PrivilegeSet;
import org.mitre.provenance.user.User;

/**
 * An SGF that will work with any invocation.  Removes its name and all metadata.
 * @author moxious
 */
public class GenericInvocationFuzzer extends SurrogateGeneratingFunction {
	public PLUSObject generateSurrogate(PLUSObject object, User user) 
	throws SurrogateException { 
		PLUSInvocation i = null;
		
		try { i = (PLUSInvocation)object; } 
		catch(ClassCastException e) { 
			throw new SurrogateException("GenericInvocationFuzzer works only on invocations!"); 			
		}
		
		Metadata m = new Metadata ();
		PLUSInvocation result = new PLUSInvocation("(Invocation)");
		result.setId(i.getId());
		result.setMetadata(m); 
		result.setPrivileges(new PrivilegeSet());
		
		SurrogateQuality q = new SurrogateQuality();
		q.put("complete", "no");
		SignPost sp = new SignPost("No further information available.");
		
		SurrogateDetail detail = new SurrogateDetail(q, sp);
		result.setSurrogateDetail(detail);
		
		return result;
	} // End generateSurrogate	
} // End GenericInvocationFuzzer
