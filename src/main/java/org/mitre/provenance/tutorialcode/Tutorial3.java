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
package org.mitre.provenance.tutorialcode;

import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.surrogate.sgf.StringRedactionSurrogateFunction;
import org.mitre.provenance.user.PrivilegeClass;

/**
 * This tutorial provides a simple example of the use of surrogates with security.
 * A simple node with a secret is created, and assigned a privilege class.  We then
 * assign a separate "Surrogate computation function" to return a less secret version of
 * the same data to a different user.
 * 
 * @author moxious
 */
public class Tutorial3 {
	public static void main(String [] args) throws Exception { 
		PLUSString node = new PLUSString("Super Secret", 
			"The CIA recently found evidence of aliens at Roswell");
		
		// Require that users have the national security privilege in order to
		// see the content of the node.
		node.getPrivileges().addPrivilege(PrivilegeClass.NATIONAL_SECURITY);
		
		// If users don't have NATIONAL_SECURITY privilege, they will see the result of
		// this item run through the StringRedactionSurrogateFunction.  (See javadocs)
		node.useSurrogateComputation(new StringRedactionSurrogateFunction());
				
		ProvenanceCollection col = new ProvenanceCollection();
		col.addNode(node);
		
		// Write the node to database and finish up.
		new LocalProvenanceClient().report(col);
		
		System.out.println("Done!");		
	} // End main
} // End Tutorial3