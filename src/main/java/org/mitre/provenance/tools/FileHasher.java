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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserPrincipal;
import java.util.logging.Logger;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.client.AbstractProvenanceClient;
import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.npe.NonProvenanceEdge;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSFile;
import org.mitre.provenance.plusobject.ProvenanceCollection;

/**
 * A simple tool to ease hashing/extraction of metadata of local files.
 * @author DMALLEN
 */
public class FileHasher {		
	protected static final Logger log = Logger.getLogger(FileHasher.class.getName());
	
	/**
	 * Given a file object, this creates a PLUSFile representation filled with appropriate metadata, file owner, and so on.
	 * @param file
	 * @return
	 */
	public static PLUSFile extractMetadata(File file, AbstractProvenanceClient client) { 
		if(!file.exists() || !file.canRead()) return null;
		
		PLUSFile f = new PLUSFile(file);
		
		// Try to figure out who owns this file, if it exists.
		if(f.getOwner() == null) {
			// This is a java-7 ism that permits us to access the file owner.
			try { 
				Path path = Paths.get(file.getAbsolutePath());
				UserPrincipal owner = Files.getOwner(path);
				String username = owner.getName();
				
				try {
					PLUSActor a = client.actorExistsByName(username);
					if(a == null) a = new PLUSActor(username);
					
					f.setOwner(a);
				} catch (PLUSException e) {
					log.warning(e.getMessage());
				}
			} catch(IOException exc) { 
				log.warning(exc.getMessage());
			}
		}
		
		try { f.getMetadata().put("isLink", ""+file.getCanonicalFile().equals(file.getAbsoluteFile())); } catch(Exception exc) { ; } 
		try { f.getMetadata().put("exists", ""+file.exists()); } catch(Exception exc) { ; }
		try { f.getMetadata().put("path", file.getAbsolutePath()); } catch(Exception exc){ ; }
		try { f.getMetadata().put("canonical", file.getCanonicalPath()); } catch(Exception exc) { ; } 
		try { f.getMetadata().put("directory", ""+file.isDirectory()); } catch(Exception exc) { ; } 
		try { f.getMetadata().put("lastmodified", ""+file.lastModified()); } catch(Exception exc) { ; }		
		try { f.getMetadata().put("size", ""+file.length()); } catch(Exception exc) { ; } 
		
		return f;
	} // End extractMetadata

	
	/**
	 * Takes a list of files as arguments; hashes each file, and creates a corresponding PLUSFile provenance object
	 * in the local repository.  Creates the corresponding hash metadata entry and an NPE to the hash value. 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String [] args) throws Exception { 
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		String line = null;
		
		LocalProvenanceClient client = new LocalProvenanceClient();
		
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
						
			ProvenanceCollection c = ProvenanceCollection.collect(fl);
			c.addNonProvenanceEdge(npe);

			client.report(c);
			
			try { 
				URI u = new URI("http", "//" + filename, null);	
				npe = new NonProvenanceEdge(fl.getId(), u.toString(), NonProvenanceEdge.NPE_TYPE_URI);
				//log.info("Wrote URL " + u); 
				client.report(ProvenanceCollection.collect(npe)); 
			} catch(Exception exc) {  
				System.err.println("Illegal URI: " + filename + " - " + exc.getMessage());
			}
						
			System.out.println("Wrote " + fl + " => " + fl.getId());
		} // End while
	} // End main
} // End FileHasher
