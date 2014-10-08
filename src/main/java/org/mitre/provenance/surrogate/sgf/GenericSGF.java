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

import org.mitre.provenance.plusobject.PLUSActivity;
import org.mitre.provenance.plusobject.PLUSFile;
import org.mitre.provenance.plusobject.PLUSGeneric;
import org.mitre.provenance.plusobject.PLUSInvocation;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.PLUSURL;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.surrogate.SignPost;
import org.mitre.provenance.surrogate.SurrogateDetail;
import org.mitre.provenance.surrogate.SurrogateException;
import org.mitre.provenance.surrogate.SurrogateGeneratingFunction;
import org.mitre.provenance.surrogate.SurrogateQuality;
import org.mitre.provenance.user.PrivilegeClass;
import org.mitre.provenance.user.User;

/**
 * A generic surrogate generating function that creates a redacted node of the same type.
 * @author moxious
 */
public class GenericSGF extends SurrogateGeneratingFunction {
	public PLUSObject generateSurrogate(PLUSObject object, User user) 
	throws SurrogateException {
		PLUSObject g = new PLUSGeneric();
		
		if(object.isInvocation()) {
			g = new PLUSInvocation();
			((PLUSInvocation)g).setWorkflow(((PLUSInvocation)object).getWorkflow());
			((PLUSInvocation)g).setActivity(((PLUSInvocation)object).getActivity());
		} else if(object.isWorkflow()) {
			g = new PLUSWorkflow();			
		} else if(object.isActivity()) {
			g = new PLUSActivity();
		} else if(object.isDataItem()) {
			if(object instanceof PLUSString) 
				g = new PLUSString("Redacted", "Redacted");
			else if(object instanceof PLUSFile) 
				g = new PLUSFile("Redacted");
			else if(object instanceof PLUSURL) 
				g = new PLUSURL("Redacted");
		}
		
		g.setId(object.getId());
		g.setName("Redacted");
		//g.setCreated(object.getCreated());
		try { g.setOwner(object.getOwner()); }
		catch(Exception exc) { ; } 
		
		g.getPrivileges().addPrivilege(PrivilegeClass.PUBLIC);
		g.setUncertainty(object.getUncertainty());
		
		SurrogateDetail sd = new SurrogateDetail(new SurrogateQuality(), SignPost.SRC_HINTS_LOCAL);
		sd.setEdgePolicy(SurrogateDetail.EDGE_POLICY_INFER_ALL);
		g.setSurrogateDetail(sd);
		
		return g;
	} // End generateSurrogate
}
