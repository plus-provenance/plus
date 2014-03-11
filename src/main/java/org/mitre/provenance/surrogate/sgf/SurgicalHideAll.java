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

import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.surrogate.SurrogateDetail;
import org.mitre.provenance.user.User;

/**
 * This SGF sets an edge policy of "HIDE ALL" on the matched object.
 * @author DMALLEN
 * @see SurgicalSGF
 */
public class SurgicalHideAll extends SurgicalSGF {
	public SurgicalHideAll(String marker) {
		super(marker);
	}

	public PLUSObject generateSurrogate(PLUSObject object, User user) {
		PLUSObject r = super.generateSurrogate(object, user);
		if(!matches(object)) return r; 
		
		//		System.out.println("Generating 'hide all' surrogate for " + object);		
		SurrogateDetail sd = r.getSurrogateDetail();
		sd.setEdgeVoter(null);
		sd.setEdgePolicy(SurrogateDetail.EDGE_POLICY_HIDE_ALL);
		r.setSurrogateDetail(sd); 
	
		return r; 
	} // End generateSurrogate
}
