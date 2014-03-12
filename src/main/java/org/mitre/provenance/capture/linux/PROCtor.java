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
package org.mitre.provenance.capture.linux;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.logging.Logger;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.contenthash.ContentHasher;
import org.mitre.provenance.contenthash.SHA256ContentHasher;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.npe.NonProvenanceEdge;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSFile;
import org.mitre.provenance.plusobject.PLUSInvocation;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.tools.LRUCache;
import org.mitre.provenance.user.User;

/**
 * This class is an operating system monitoring class for UNIX-based operating systems which support the proc filesystem.
 * For more information about procfs, see http://en.wikipedia.org/wiki/Procfs
 * 
 * Basically, this polls available OS information about processes that are running, and then saves that information as provenance.
 * The OS will tell us for example which process IDs (PIDs) have which files open for read and write, and what the command line is
 * of the application that executed.
 * 
 * We have to apply a few basic fingerprinting techniques to avoid logging duplicates.
 * 
 * At present, this program polls procfs exactly once.  This might be appropriate for setting up as a daemon process in later code.
 */
public class PROCtor {
	protected static final Logger log = Logger.getLogger(PROCtor.class.getName());
	protected String myPID = null;
	public static final LRUCache<String,PLUSObject> cache = new LRUCache<String,PLUSObject>(1000); 
	
	protected SHA256ContentHasher hasher = new SHA256ContentHasher();
	
	public static final String UUID_KEY = "file_uuid";
	
	//HashMap<String,PLUSObject> cache = new HashMap<String,PLUSObject>();	
	protected static File PROC = new File("/proc"); 
		
	public PROCtor() throws Exception { 
		myPID = PROCtor.getMyPID();
	}
	
	public void run(long pollTimeoutMs) throws Exception { 
		while(true) { 
			poll();
			Thread.sleep(pollTimeoutMs);
		}		
	}
	
	protected String slurp(File f) { 
		BufferedReader br = null;
		try { 
			br = new BufferedReader(new FileReader(f)); 
			StringBuffer b = new StringBuffer("");		
			String line = null;		
			while((line = br.readLine()) != null) b.append(line);
			return b.toString();
		} catch(IOException ioe) { 
			return null;
		} finally { 
			try { br.close(); } catch (IOException e) { ; } 
		}
	}
	
	protected String getIDForFile(File f) throws NoSuchAlgorithmException, IOException {
		// Unique ID for a file based on its absolute pathname, and last modified date.
		// When this hash value changes, you know it's a different file.
		String stamp = f.getCanonicalPath() + "-" + f.lastModified();				
		return ContentHasher.formatAsHexString(hasher.hash(new ByteArrayInputStream(stamp.getBytes())));
	}
	
	protected void poll() throws IOException, NoSuchAlgorithmException, PLUSException { 
		String[] PIDs = PROC.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				// Match only filenames that are entirely numeric.
				// These filenames correspond to system PIDs (process IDs)
				return name.matches("^[0-9]+$");   
			}
		});
		
		for(String pid : PIDs) { 
			if(pid.equals(myPID)) continue;  // Don't process myself.
			processPID(new File(PROC, pid)); 	
		}		
	}
	
	protected void processPID(File procPID) throws IOException, NoSuchAlgorithmException, PLUSException {
		if(!procPID.exists()) {
			log.warning("PID " + procPID + " doesn't exist.");
			return;
		}
		
		PLUSInvocation inv = createOrRetrieveInvocation(procPID);
		if(inv == null) return;		
						
		String [] fileDescriptors = null;
		File fds = new File(procPID, "fd"); 
		fileDescriptors = fds.list();
		
		if(fileDescriptors == null) { return; } // No permissions here.

		ProvenanceCollection pcol = new ProvenanceCollection();
		
		System.out.println("\n");
		
		boolean revisiting = false;
		
		if(Neo4JStorage.exists(inv) != null) 
			revisiting = true;
		else pcol.addNode(inv);
		
		HashSet<String> inputs = new HashSet<String>();
		HashSet<String> outputs = new HashSet<String>();

		for(String fdName : fileDescriptors) {
		    File fdFile = new File(fds, fdName); 
		    
		    // We get the canonical file to resolve the procfs symlink, so that 
		    // we're gathering metadata about the file, and not a symlink to the file.
		    // If we're revisiting this PID and the file has already been logged, this will return null
		    // so we don't re-log the same things.
		    PLUSObject fdObj = createOnlyIfNew(fdFile.getCanonicalFile()); 
		    
		    if(fdObj == null) { 
		    	// System.out.println("Error creating obj for " + fdFile);
		    	continue;
		    }
		    
		    fdObj.getMetadata().put("unix:fd", fdName); 
		    pcol.addNode(fdObj); 
		    
		    // TRICKY: is this correct?
		    // If the fd was open for writing, that means the program is either creating or appending to it.
		    // Hence, it's an output.
		    // If the file is open for read only, it's an input.
		    if(fdFile.canWrite()) outputs.add(""+fdObj.getMetadata().get(UUID_KEY));		    
		    else inputs.add(""+fdObj.getMetadata().get(UUID_KEY));
		    
		    String file_uuid = ""+fdObj.getMetadata().get(UUID_KEY);
		    
		    if(Neo4JStorage.exists(fdObj) != null) pcol.addNonProvenanceEdge(new NonProvenanceEdge(fdObj, file_uuid, UUID_KEY)); 
		}
		
		for(String id : inputs) { 
		    PLUSObject o = (PLUSObject)cache.get(id); 		    
		    if(o != null) pcol.addEdge(new PLUSEdge(o, inv)); 
		} 

		for(String id : outputs) { 
		    PLUSObject o = (PLUSObject)cache.get(id); 
		    if(o != null) pcol.addEdge(new PLUSEdge(inv, o));
		} 
							
		int written = Neo4JStorage.store(pcol);
		
		log.info((revisiting ? "REVISITED" : "NEW") + ": " + inv.getMetadata().get("cmdline") + 
				 " PID " + inv.getMetadata().get("pid") + " => " + 
				 inputs.size() + " inputs, " + outputs.size() + " outputs.  Total written=" + written);
	}
	
	public boolean isSymlink(File file) throws IOException {
		if(file == null) return false; 
		
		File canon;
		if (file.getParent() == null) canon = file;
		else {
			File canonDir = file.getParentFile().getCanonicalFile();
			canon = new File(canonDir, file.getName());
		}
		
		return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
	}
	
	public static String getMyPID() { 
		String pidStr = ManagementFactory.getRuntimeMXBean().getName();

		int idx = pidStr.indexOf("@");
		
		if(idx == -1) return pidStr;
		else return pidStr.substring(0, idx); 
	}
	
	/**
	 * Get or create a new PLUSInvocation on the basis of a proc PID file, e.g. /proc/56 (pid 56)
	 * Returns null for insufficient permissions, or when  you shouldn't log a particular pid.  (For 
	 * example, this program will not log its own run)
	 */
	public PLUSInvocation createOrRetrieveInvocation(File procPID) throws NoSuchAlgorithmException, IOException {
		String procFileID = getIDForFile(procPID);		
		if(procFileID == null) return null;
		
		String pid = procPID.getName();
		if(pid.equals(myPID)) return null;   // Don't log myself.

		String [] children = procPID.list();  		
		if(children == null) return null;   // No permissions.
	
		if(cache.containsKey(procFileID)) return (PLUSInvocation)cache.get(procFileID); 
		
		try { 
			ProvenanceCollection results = Neo4JPLUSObjectFactory.loadBySingleMetadataField(User.DEFAULT_USER_GOD, UUID_KEY, procFileID);
			if(results != null && results.countNodes() > 0) {
				PLUSInvocation i = (PLUSInvocation)results.getNodes().toArray()[0];
				cache.put(procFileID, i); 
				return i; 
			}
		} catch(PLUSException exc) { 
			exc.printStackTrace();
		}
					
		long lmod = procPID.lastModified();
		
		String cmdline = slurp(new File(procPID, "cmdline"));		
		File exe = new File(procPID, "exe").getCanonicalFile();
		File cwd = new File(procPID, "cwd").getCanonicalFile(); 
		
		PLUSInvocation inv = new PLUSInvocation(exe.getCanonicalPath());
		inv.getMetadata().put("pid", pid); 
		inv.getMetadata().put("cwd", cwd.getCanonicalPath()); 
		inv.getMetadata().put("cmdline", cmdline); 
		inv.getMetadata().put("started", ""+lmod); 
		inv.getMetadata().put(UUID_KEY, procFileID);
		inv.getMetadata().put(Metadata.CONTENT_HASH_SHA_256, procFileID);
		
		cache.put(procFileID, inv);  // Cache this so we don't go back over it.
		
		return inv;
	}
	
	public PLUSObject createOnlyIfNew(File f) throws NoSuchAlgorithmException, IOException {
		if(f == null || !f.exists()) return null;
		
		if(!f.isFile()) return null;   // Don't log things like sockets right now.
		String id = getIDForFile(f); 
		
		if(id == null) { 
			log.warning("Couldn't compute file id for " + f);
			return null;
		}
				
		if(cache.containsKey(id)) return null; 
		
		try { 
			ProvenanceCollection results = Neo4JPLUSObjectFactory.loadBySingleMetadataField(User.DEFAULT_USER_GOD, UUID_KEY, id);
			if(results != null && results.countNodes() > 0) {
				PLUSObject o = (PLUSObject) results.getNodes().toArray()[0];
				cache.put(id, o); 
				return null;
			}
		} catch(PLUSException exc) { 
			exc.printStackTrace(); 
		}
			
		PLUSFile pf = new PLUSFile(f);
		pf.getMetadata().put(UUID_KEY, id); 		
		
		if(id != null) cache.put(id, pf);
		
		if(f.isFile()) {
			long fileSize = 0;
			try { fileSize = f.length(); } 
			catch(Exception exc) { 
				exc.printStackTrace();
				return pf;
			}

			// Best effort to hash the content.
			if(fileSize < 1024 * 1024) {
				FileInputStream fis = null;
				try { 
					fis = new FileInputStream(f);
					String sha256hash = ContentHasher.formatAsHexString(hasher.hash(fis));
					fis.close();
					pf.getMetadata().put(Metadata.CONTENT_HASH_SHA_256, sha256hash);					
				} 
				catch(IOException exc) { ; } 
				finally { 
					if(fis != null) try { fis.close(); } catch(Exception e) { ; } 
				}
			}			
		}
		
		return pf;
	}
	
	public static void main(String [] args) throws Exception {
		PROCtor p = new PROCtor();
		
		if(!PROC.exists()) {
			log.severe("This utility is intended to run on Linux systems with a PROC filesystem. You do not appear to have one (or it is not readable)");
			System.exit(1);
		}
		
		if(args.length > 0) { 
			for(int x=0; x<args.length; x++)
				p.processPID(new File(PROC, args[x]));
		} else {
			log.info("Starting PROCtor in polling mode.  Hit Ctrl-C to end logging.");
			p.run(5000);		
		}
	}
}
