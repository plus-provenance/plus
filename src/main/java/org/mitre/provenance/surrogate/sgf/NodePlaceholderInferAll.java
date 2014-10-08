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

import org.mitre.provenance.plusobject.PLUSGeneric;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.surrogate.SignPost;
import org.mitre.provenance.surrogate.SurrogateDetail;
import org.mitre.provenance.surrogate.SurrogateGeneratingFunction;
import org.mitre.provenance.surrogate.SurrogateQuality;
import org.mitre.provenance.user.PrivilegeClass;
import org.mitre.provenance.user.User;

/**
 * A "friendly" SGF that hides as much information as possible, but permits graph connections to exist.
 * This function always returns an unnamed PLUSGeneric node, no matter what the input is.
 * The edge policy is set to INFER_ALL.   Effectively, this destroys all information about the node except
 * for its ID, but retains graph connectivity.
 * @author moxious
 */
public class NodePlaceholderInferAll extends SurrogateGeneratingFunction {
	public PLUSObject generateSurrogate(PLUSObject object, User user) {
		PLUSGeneric g = new PLUSGeneric("Unnamed");
		g.setId(object.getId());
		
		SurrogateDetail sd = new SurrogateDetail(new SurrogateQuality(), SignPost.SRC_HINTS_LOCAL);
		sd.setEdgePolicy(SurrogateDetail.EDGE_POLICY_INFER_ALL);
		
		g.setSurrogateDetail(sd);
		
		g.getPrivileges().addPrivilege(PrivilegeClass.PUBLIC);
		return g;
	}
}
