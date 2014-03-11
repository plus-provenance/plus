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
package org.mitre.provenance.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;

import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.npe.NonProvenanceEdge;
import org.mitre.provenance.plusobject.PLUSFile;

/**
 * A simple tool to ease hashing of local files.
 * @author DMALLEN
 */
public class FileHasher {	
	/**
	 * Takes a list of files as arguments; hashes each file, and creates a corresponding PLUSFile provenance object
	 * in the local repository.  Creates the corresponding hash metadata entry and an NPE to the hash value. 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String [] args) throws Exception { 
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		String line = null;
		
		while((line = br.readLine()) != null) { 
			String filename = line.trim();
			File f = new File(filename);
			
			if(!f.exists()) { 
				System.err.println(filename + " does not exist."); 
				continue;
			} else if(f.isDirectory()) continue;
				
			PLUSFile fl = new PLUSFile(f);			
			
			String hash = fl.hash();
			NonProvenanceEdge npe = new NonProvenanceEdge(fl, hash, NonProvenanceEdge.NPE_TYPE_CONTENT_HASH);
						
			Neo4JStorage.store(fl);
			Neo4JStorage.store(npe);
			
			try { 
				URI u = new URI("http", "//" + filename, null);	
				npe = new NonProvenanceEdge(fl.getId(), u.toString(), NonProvenanceEdge.NPE_TYPE_URI);
				//log.info("Wrote URL " + u); 
				Neo4JStorage.store(npe);
			} catch(Exception exc) {  
				System.err.println("Illegal URI: " + filename + " - " + exc.getMessage());
			}
						
			System.out.println("Wrote " + fl + " => " + fl.getId());
		} // End while
	} // End main
} // End FileHasher
