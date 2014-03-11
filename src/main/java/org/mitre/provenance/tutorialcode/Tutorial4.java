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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.contenthash.ContentHasher;
import org.mitre.provenance.contenthash.SHA256ContentHasher;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.npe.NonProvenanceEdge;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSInvocation;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.plusobject.json.JSONConverter;
import org.mitre.provenance.user.User;

/**
 * This tutorial demonstrates the use of content hashing, lookup by hash, and JSON serialization.
 * @author DMALLEN
 */
public class Tutorial4 {
	public static void main(String [] args) throws Exception { 
		/*
		 * We're going to create a fake provenance graph.  The result should look like this:
		 * "My explorer.exe" (data) => "A process that ran" (invocation) => "An output data value" (data)
		 * 
		 * We're going to hash the explorer.exe executable just as an example, then after we save it, we're going
		 * to check the database to see if we can find it there.
		 */
		PLUSString someInput = new PLUSString("My explorer.exe");     // Simple object identified by a string.
		PLUSString someOutput = new PLUSString("An output data value");
		PLUSInvocation someProcess = new PLUSInvocation("A process that ran");
		
		// Now we hash a file - first we have to refer to it.
		File f = new File("c:\\windows\\explorer.exe");
		
		// We create an object that will do the hasher for us....
		SHA256ContentHasher myHasher = new SHA256ContentHasher();
		
		// Open the file as an input stream, give it to the hasher.  That will return a real hash
		// (which is a byte[]).   We want something we can store, so turn the byte[] into a hex string.
		FileInputStream fis = new FileInputStream(f);
		String hashAsString = ContentHasher.formatAsHexString(myHasher.hash(fis));
		fis.close();
		
		// Now put that magic hex string into the metadata of the corresponding provenance object.
		// NOTE!  We used a special reserved key (Metadata.CONTENT_HASH_SHA_256) - the reason we did it this
		// way is to create a convention so others will know where to find our sha256 hash.
		someInput.getMetadata().put(Metadata.CONTENT_HASH_SHA_256, hashAsString);
		
		// Provenance collections are just buckets of provenance objects...
		ProvenanceCollection col = new ProvenanceCollection();
		
		// We add a bunch of stuff to that bucket...
		col.addNode(someInput);
		col.addNode(someOutput);
		col.addNode(someProcess);
		
		// We also add edges connecting the input to the process, and so on.
		col.addEdge(new PLUSEdge(someInput, someProcess));
		col.addEdge(new PLUSEdge(someProcess, someOutput));
		
		// Just for kicks, we add a silly NPE.  It's labeled "Foo", because we get to label NPEs
		// whatever we want.
		col.addNonProvenanceEdge(new NonProvenanceEdge(someInput, someOutput, "Foo"));
		
		// All of the database storage is done here, in a single line.
		Neo4JStorage.store(col);
				
		System.out.println("The identity of the process is:  " + someProcess.getId());
		
		// Now let's check and see if the database actually contains what we saved.
		Metadata inSearchOfTheseItems = new Metadata();
		inSearchOfTheseItems.put(Metadata.CONTENT_HASH_SHA_256, hashAsString);
		
		// OK now this code basically says, 
		// Grab everything from the database that matches *ALL* of the metadata items in the
		// map we gave it (inSearchOfTheseItems); return a maximum of 10 results, and use the "god" user
		// (meaning we're permitted to see everything)
		ProvenanceCollection results = Neo4JPLUSObjectFactory.loadByMetadata(User.DEFAULT_USER_GOD, inSearchOfTheseItems, 10);
		
		for(PLUSObject result : results.getNodes()) {
			System.out.println("SHA256 hash match found for " + hashAsString + " => " + result);
		}
		
		// Now just for kicks, let's turn the original collection that we created into JSON, to show how that's done.
		String fileDestination = System.getProperty("user.home") + "\\Desktop\\provenance.txt";
		BufferedWriter mySavedProvenance = new BufferedWriter(new FileWriter(fileDestination));
		
		try { 
			String json = JSONConverter.provenanceCollectionToD3Json(col);
			mySavedProvenance.write(json);
		} finally { mySavedProvenance.close(); }
				
		System.out.println("Finished.");
	}
}
