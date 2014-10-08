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

import org.mitre.provenance.plusobject.*;
import org.mitre.provenance.surrogate.*;
import org.mitre.provenance.user.User;

/**
 * This SGF is used for testing and experimentation purposes.  It has the effect of voting INFER randomly on edges,
 * according to the specified CHANCE percentage.
 * @author moxious
 */
public class RandomInferMarker extends SurrogateGeneratingFunction {
	public static final float CHANCE = (float)0.3; 
	
	public PLUSObject generateSurrogate(PLUSObject obj, User user) {
		PLUSString s = (PLUSString)obj;
		
		PLUSString result = new PLUSString("("+s.getName()+")", s.getContent());
		result.setId(s.getId());
		
		SurrogateDetail det = new SurrogateDetail(new SurrogateQuality(), new SignPost("None"));
		
		if(Math.random() <= CHANCE) {
			System.out.println(result.getName() + " is being marked all INFER"); 
			det.setEdgePolicy(SurrogateDetail.EDGE_POLICY_INFER_ALL);
		}
				
		result.setSurrogateDetail(det); 
		return result;
	} // End generateSurrogate
} // End RandomInferMarker
