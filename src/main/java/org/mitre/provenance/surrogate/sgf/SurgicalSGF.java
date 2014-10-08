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

import org.mitre.provenance.EdgeMarking;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.surrogate.SignPost;
import org.mitre.provenance.surrogate.SurrogateDetail;
import org.mitre.provenance.surrogate.SurrogateGeneratingFunction;
import org.mitre.provenance.surrogate.SurrogateQuality;
import org.mitre.provenance.surrogate.edgevoter.SurgicalEdgeVoter;
import org.mitre.provenance.user.User;

/**
 * A surgical SGF is one that can be applied to many objects, and that will only take "surgical action" on those
 * objects that match by name.   So the SGF can be applied as a matter of policy to all objects, but it will only act in 
 * certain tunable circumstances.
 * 
 * @author moxious
 */
public abstract class SurgicalSGF extends SurrogateGeneratingFunction {
	protected String MARKER = "##";	
	
	/**
	 * Create a new surgical SGF with a given marker; the only objects that surrogates will be generated for
	 * are those whose name contains the marker.
	 * @param marker
	 */
	public SurgicalSGF(String marker) { 
		this.MARKER = marker;
	}
	
	public boolean matches(PLUSObject obj) { 
		if(obj.getName().contains(MARKER)) return true;
		return false;
	} // End matches
	
	public PLUSObject identitySurrogate(PLUSObject obj) { 
		PLUSObject r = new PLUSString(obj.getName(), ((PLUSString)obj).getContent());
		r.setId(obj.getId()); 
		
		SurrogateDetail det = new SurrogateDetail(new SurrogateQuality(), new SignPost("None"));
		r.setSurrogateDetail(det); 
		
		return r;
	} // End identitySurrogate
	
	public PLUSObject generateSurrogate(PLUSObject object, User user) {
		if(!matches(object)) return identitySurrogate(object); 
				
		PLUSString f = new PLUSString("("+object.getName()+")", ((PLUSString)object).getContent());
		
		f.setId(object.getId()); 
		
		SurrogateDetail det = new SurrogateDetail(new SurrogateQuality(), new SignPost("None"));
		
		try { 
			ProvenanceCollection bling = Neo4JPLUSObjectFactory.getBLING(object.getId(), user);
			PLUSEdge target = bling.getEdgesInList().get(0);
			
			SurgicalEdgeVoter sev = new SurgicalEdgeVoter(target.getFrom().getId(), EdgeMarking.SHOW);			
			det.setEdgeVoter(sev); 		
			f.setSurrogateDetail(det);
		} catch(Exception exc) {
			exc.printStackTrace();
		} // End catch
		
		return f;
	} // End generateSurrogate
} // End SurgicalSGF
