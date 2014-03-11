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
package org.mitre.provenance.capture;

import java.io.File;
import java.io.FileInputStream;

import org.mitre.provenance.client.RESTProvenanceClient;
import org.mitre.provenance.contenthash.ContentHasher;
import org.mitre.provenance.contenthash.SHA256ContentHasher;
import org.mitre.provenance.npe.NonProvenanceEdge;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSFile;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.ProvenanceCollection;

public class CaptureTest {
	public static void main(String [] args) throws Exception { 
		File homeDir = new File(System.getProperty("user.home")); 
		
		ProvenanceCollection col = new ProvenanceCollection();
		SHA256ContentHasher hasher = new SHA256ContentHasher();
		
		PLUSObject last = null;
		
		for(File file : homeDir.listFiles()) {
			if(file.isDirectory()) continue;
			
			PLUSFile f = new PLUSFile(file);
			FileInputStream fis = null;
			
			try { 
				fis = new FileInputStream(file);
				String hash = ContentHasher.formatAsHexString(hasher.hash(fis));			
			
				System.out.println(file.getAbsolutePath() + " => " + hash);
				col.addNode(f);
			
				col.addNonProvenanceEdge(new NonProvenanceEdge(f, hash, "sha256"));
				
				if(last != null) 
					col.addEdge(new PLUSEdge(last, f, null));
				
				last = f;
			} catch(Exception exc) { 
				System.err.println("Skipping " + file.getAbsolutePath() + ": " + exc.getMessage());
				continue;
			} finally { fis.close(); } 
		}

		RESTProvenanceClient client = new RESTProvenanceClient("localhost", "8080");
		client.report(col);
	}
}
