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
package org.mitre.provenance.test;

import java.io.File;
import java.io.IOException;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.client.AbstractProvenanceClient;
import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.npe.NonProvenanceEdge;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSFile;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.simulate.SyntheticGraphProperties;
import org.mitre.provenance.simulate.motif.RandomMotifCollection;
import org.mitre.provenance.surrogate.sgf.SurgicalInferAll;
import org.mitre.provenance.user.PrivilegeClass;
import org.mitre.provenance.user.PrivilegeSet;
import org.mitre.provenance.user.User;

public class BulkLoadSampleCreator {
	static AbstractProvenanceClient client = new LocalProvenanceClient();
	
	public static void addSomeMore() throws Exception {
		PrivilegeSet ps = new PrivilegeSet();
		ps.addPrivilege(PrivilegeClass.PUBLIC);
		
		SyntheticGraphProperties p = new SyntheticGraphProperties().setComponents(20).setSGF(new SurgicalInferAll()).setPrivilegeSet(ps);
		RandomMotifCollection rmc = new RandomMotifCollection(p);
		
		System.out.println("Storing some more, including: " + rmc.getNodesInOrderedList().get(0).getCreatedAsDate());
		client.report(rmc);		
	}
	
	public static ProvenanceCollection hashDir(File f) throws PLUSException, IOException {
		ProvenanceCollection pc = new ProvenanceCollection();
		
		PLUSFile pf = new PLUSFile(f);		
		pc.addNode(pf);
		
		for(File child : f.listFiles()) {
			PLUSFile pf1 = new PLUSFile(child);
			if(child.isFile()) {
				String hash = pf1.hash();
				System.out.println("Got hash " + hash);
				pf1.getMetadata().put(Metadata.CONTENT_HASH_SHA_256, hash);
			}
			
			pc.addNode(pf1);
			
			pc.addNonProvenanceEdge(new NonProvenanceEdge(pf, pf1, "contains"));
			pc.addEdge(new PLUSEdge(pf, pf1, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_UNSPECIFIED));
		}
		
		return pc;
	}
	
	public static void main(String [] args) throws Exception {
		// addSomeMore();
		
		client.report(hashDir(new File("C:\\Users\\dmallen\\Desktop\\eclipse\\configuration\\.settings")));
				
		ProvenanceCollection col = Neo4JPLUSObjectFactory.getRecentlyCreated(User.DEFAULT_USER_GOD, 5);
		
		System.out.println("LATEST 5"); 
		for(PLUSObject o : col.getNodes()) {
			System.out.println(o + " => " + o.getCreatedAsDate());
		}
	}
}
