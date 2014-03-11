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
import java.io.IOException;
import java.util.logging.Logger;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.npe.NonProvenanceEdge;
import org.mitre.provenance.plusobject.PLUSFile;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.ProvenanceCollection;

/** 
 * Path harvester creates a series of provenance objects from files on a path.   This is useful for pre-creating objects
 * when you know they will be referenced, and capturing hashes from a large set of files.
 * @author DMALLEN
 */
public class PathHarvester {
	protected static final Logger log = Logger.getLogger(PathHarvester.class.getName());
	
	protected boolean processHidden = false;
	protected File path = null;
	
	public PathHarvester(File path) { this(path, false); } 
	
	/**
	 * Create a new PathHarvester.
	 * @param path the starting point path you wish to process.
	 * @param processHidden if true, hidden files will be processed.  If false, they will be skipped.
	 */
	public PathHarvester(File path, boolean processHidden) {
		this.path = path;
		this.processHidden = processHidden;
	}
	
	/**
	 * Harvest a collection of provenance objects from the path used to create the object.
	 * @param recursive if true, directories will be processed recursively.  If false, only this one path will be processed.
	 * @return a provenance collection containing the results.
	 * @throws PLUSException
	 */
	public ProvenanceCollection harvest(boolean recursive) throws PLUSException {
		ProvenanceCollection pc = new ProvenanceCollection();
		
		if(!path.exists()) return pc;
		if(path.isHidden() && !processHidden) return  pc;		
		
		PLUSFile pf = new PLUSFile(path);
		pc.addNode(pf);
		
		try { 
			if(path.exists() && path.isFile() && path.canRead()) {
				String hash = pf.hash();
				log.info(path.getAbsolutePath() + " => " + hash);
			}
		} catch(IOException exc) { 
			log.warning(path.getAbsolutePath() + " => " + exc.getMessage());
		}
			
		if(recursive && path.exists() && path.isDirectory() && path.canRead()) {
			for(File child : path.listFiles()) {
				PathHarvester ph = new PathHarvester(child);
				
				ProvenanceCollection children = ph.harvest(recursive);
				pc.addAll(children);
				
				for(PLUSObject co : children.getNodes()) {
					pc.addNonProvenanceEdge(new NonProvenanceEdge(pf, co, NonProvenanceEdge.NPE_TYPE_CONTAINMENT));
				}
			}
		}
		
		return pc;
	} // End harvest
	
	public static void main(String [] args) throws Exception {
		PathHarvester ph = new PathHarvester(new File("c:\\users\\dmallen\\desktop\\task planning"));
		ProvenanceCollection col = ph.harvest(true);
		
		Neo4JStorage.store(col);
		
		for(PLUSObject obj : col.getNodes()) { 
			System.out.println(obj);
		}
	}
}
