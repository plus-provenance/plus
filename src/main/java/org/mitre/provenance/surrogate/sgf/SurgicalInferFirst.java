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
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.surrogate.edgevoter.SurgicalEdgeVoter;
import org.mitre.provenance.user.User;

public class SurgicalInferFirst extends SurgicalSGF {
	public SurgicalInferFirst(String marker) {
		super(marker);
	}

	public PLUSObject generateSurrogate(PLUSObject object, User user) {
		PLUSObject r = super.generateSurrogate(object, user);
		if(!matches(object)) return r; 
		
		//		System.out.println("Generating 'infer first' surrogate for " + object); 
		((SurgicalEdgeVoter)r.getSurrogateDetail().getEdgeVoter()).setMarking(EdgeMarking.INFER);
		return r; 
	} // End generateSurrogate
} // End SurgicalInferFirst
