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

import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.user.PrivilegeClass;

/**
 * This tutorial steps through how to set various options on the nodes to be more informative.
 * @author DMALLEN
 */
public class Tutorial2 {
	public static void main(String [] args) throws Exception {
		// Create a new object in a lineage graph.
		PLUSString node = new PLUSString("X", "A simple string");
		
		// Each node has its own "metadata" associated with it.  At this point, this is 
		// just a simple hashtable containing key/value pairs.  Let's associate some metadata
		// with this object.
		node.getMetadata().put("operating system", "Red Hat Linux");
		node.getMetadata().put("original owner", "dmallen@mitre.org");
		node.getMetadata().put("format", "text/plain");
		
		// Let's associate some permissions with this node, so that not everybody can see it.
		// See the PrivilegeClass javadocs for some pre-defined privilege classes.   A 
		// privilege class is just some generic attribute the user must have in order to be able
		// to see something.
		//
		// Objects that have more than one privilege class require the user to have *all* of them
		// in order to see the information.
		//
		// Right now, the PLUS repertoire of privileges is limited.  The API does not allow the
		// creation of new privileges, but we can create as many as we need at the DB level by 
		// prior agreement.
		node.getPrivileges().addPrivilege(PrivilegeClass.PRIVATE_MEDICAL);
		node.getPrivileges().addPrivilege(PrivilegeClass.NATIONAL_SECURITY);
		
		// We're not 100% sure of this, so let's adjust the confidence downwards.
		// It's called uncertainty....but this floating point number represents your assessment of
		// the chances that this information is correct.  So 0.85 means that you think there is an
		// 85% chance this data is accurate.
		node.setUncertainty((float)0.85);
		
		// Let's create ourselves as a new "actor".
		PLUSActor ncel = new PLUSActor("NCEL");
		Neo4JStorage.store(ncel);
		
		node.setOwner(ncel);
		
		// Write the finished object to the database, and quit.
		Neo4JStorage.store(node);
		
		System.out.println("Done!");
	} // End main
} // End Tutorial2
