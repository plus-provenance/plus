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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserPrincipal;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.mitre.provenance.Metadata;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.client.AbstractProvenanceClient;
import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.client.ProvenanceClient;
import org.mitre.provenance.contenthash.ContentHasher;
import org.mitre.provenance.contenthash.SHA256ContentHasher;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.npe.NonProvenanceEdge;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSFile;
import org.mitre.provenance.plusobject.PLUSInvocation;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.tools.LRUCache;
import org.mitre.provenance.user.User;

/**
 * This class is an operating system monitoring class for UNIX-based operating systems which support the proc filesystem.
 * For more information about procfs, see http://en.wikipedia.org/wiki/Procfs
 * 
 * <p>Basically, this polls available OS information about processes that are running, and then saves that information as provenance.
 * The OS will tell us for example which process IDs (PIDs) have which files open for read and write, and what the command line is
 * of the application that executed.
 * r
 * <p>We have to apply a few basic fingerprinting techniques to avoid logging duplicates.
 * 
 * <p>This code could doubtless see many improvements, but it's a basic proof of concept for how to collect provenance in real systems.
 * For many users, this kind of provenance would be seen as too granular, but it can produce some very interesting findings; in 
 * particular, because we use content-bound identifiers on everything that we encounter, this can establish linkages between 
 * different processes that read and use the same files.
 * 
 * <p>A major weakness of this capture approach is that you can never know when in the process lifecycle to scan a particular PID.
 * Which assets the process is using vary dramatically (particularly for long-lived processes) depending on when you hit it in 
 * the lifecycle.  Improvements should focus around appending in subsequent polls. 
 * 
 * @author moxious
 */
public class PROCtor {
	protected static final Logger log = Logger.getLogger(PROCtor.class.getName());
	protected String myPID = null;
	public static final LRUCache<String,PLUSObject> cache = new LRUCache<String,PLUSObject>(1000); 
	
	protected HashSet<String> pollPIDs = new HashSet<String>();
	protected static AbstractProvenanceClient client = new LocalProvenanceClient();
	protected SHA256ContentHasher hasher = new SHA256ContentHasher();
	
	public static final String UUID_KEY = "file_uuid";
	
	/**
	 * Signals that an object already exists.
	 * @author david
	 */
	public static class ExistsException extends PLUSException { 
		private static final long serialVersionUID = 11233123L;
		protected PLUSObject o; 
		public ExistsException(PLUSObject obj) { this.o = obj; }
		public PLUSObject getObject() { return o; } 
	}
	
	public void addPID(String pid) { 
		pollPIDs.add(pid);
	}
	
	//HashMap<String,PLUSObject> cache = new HashMap<String,PLUSObject>();	
	protected static File PROC = new File("/proc"); 
		
	public PROCtor() throws Exception { 
		myPID = PROCtor.getMyPID();
	}
	
	public void run(long pollTimeoutMs, int times) throws Exception { 
		int x=0;
		
		while(true) { 
			if(times > 0 && x >= times) break;
			
			poll();			
			Thread.sleep(pollTimeoutMs);
			x++;
		}		
	}
	
	protected List<String> slurpLines(File f) { 
		BufferedReader br = null;
		ArrayList<String> lines = new ArrayList<String>();
		
		try { 
			br = new BufferedReader(new FileReader(f)); 
			String line = null;
			while((line = br.readLine()) != null) lines.add(line);
			return lines;
		} catch(IOException exc) { return null; } 
		finally { try { br.close(); } catch(IOException e) { ; } }
	} // End slurpLines
	
	/**
	 * Read the complete contents of a file and return them as a string.   Simple utility for tiny files.
	 * @param f file to read.
	 * @return the complete text contents
	 */
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
	
	/**
	 * Computes a special identifier for files based on their path and when they were last modified.  This is not a content-bound identifier,
	 * but can be used in case a duplicate file has been seen on the same system.
	 * @param f the file to use
	 * @return a string identifier
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	protected String getIDForFile(File f) throws NoSuchAlgorithmException, IOException {
		// Unique ID for a file based on its absolute pathname, and last modified date.
		// When this hash value changes, you know it's a different file.
		String stamp = f.getCanonicalPath() + "-" + f.lastModified();				
		return ContentHasher.formatAsHexString(hasher.hash(new ByteArrayInputStream(stamp.getBytes())));
	}
	
	/**
	 * Polls through all available items in the proc fs, and processes them individually.
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws PLUSException
	 */
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
			
			if(pollPIDs.isEmpty() || pollPIDs.contains(pid))
				processPID(new File(PROC, pid)); 	
		}		
	}
	
	protected ProcFDInfo getFDInfo(File procPID, String fd) {
		File fdInfoFile = new File(new File(procPID, "fdinfo"), fd);
		
		if(!fdInfoFile.exists()) return null;
		
		List<String> lines = slurpLines(fdInfoFile);
		
		String flags = null;
		String pos = null;
		
		for(String line : lines) { 			
			if(line.indexOf(':') != -1) {
				String [] toks = line.split("[ \\t]+");
				if(toks[0].contains("pos")) pos = toks[1]; 
				else if(toks[0].contains("flags")) flags = toks[1];
				else 
					log.warning("Unexpected line '" + line + "' in " + fdInfoFile.getAbsolutePath());				
			} else 
				// Ignore other lines, (inotify, tfd, eventfd-count, others)
				continue;
			
			if(flags != null && pos != null) break;
		}
		
		// Shouldn't happen...
		if(pos == null || flags == null) return null;
		
		return new ProcFDInfo(pos, flags); 
	}
	
	/**
	 * Processes a PID identified by a particular /proc filesystem path, and creates the necessary provenance objects.
	 * @param procPID
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws PLUSException
	 */
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
		
		boolean revisiting = false;
		
		if(client.exists(inv) != null) revisiting = true;
		else pcol.addNode(inv);
		
		List<String> inputs = new ArrayList<String>();
		List<String> outputs = new ArrayList<String>();
		List<String> related = new ArrayList<String>();

		for(String fdName : fileDescriptors) {
		    File fdFile = new File(fds, fdName); 
		    
		    // We get the canonical file to resolve the procfs symlink, so that 
		    // we're gathering metadata about the file, and not a symlink to the file.
		    File canonical = fdFile.getCanonicalFile();
		    
		    boolean previouslyWritten = false;
		    PLUSObject fdObj = null;

		    // This is what will let us know whether the file was open for input/output, or whatever.
		    ProcFDInfo fdInfo = getFDInfo(procPID, fdName);
		    if(fdInfo == null) { log.warning("Couldn't get fdInfo for " + procPID + "/fdinfo/" + fdName); continue; } 
		    
		    try { fdObj = createOnlyIfNew(canonical); }
		    catch(ExistsException e) {
		    	// There is a valid file here, but we've already seen it.  That means don't add it
		    	// to the collection or try to re-write it.
		    	previouslyWritten = true;
		    	fdObj = e.getObject();
		    }
		    
		    if(fdObj == null) continue; 
		    		    
		    if(!previouslyWritten) { 
		    	fdObj.getMetadata().put("unix:fd", fdName); 
		    	pcol.addNode(fdObj);
		    }
		    
		    // It's an output if we're appending to it, creating it, writing only to it, or truncating it.
		    if(fdInfo.O_APPEND() || fdInfo.O_CREAT() || fdInfo.O_WRONLY() || fdInfo.O_TRUNC())
		    	outputs.add(""+fdObj.getMetadata().get(UUID_KEY));		    
		    // It's an input if we're read only.
		    else if(fdInfo.O_RDONLY())
		    	inputs.add(""+fdObj.getMetadata().get(UUID_KEY));		    
		    else if(fdInfo.O_RDWR()) 
		    	related.add(""+fdObj.getMetadata().get(UUID_KEY));
		    else { 
		    	log.warning("Ambiguous mode for " + procPID + "/fdinfo/" + fdName + ": " + fdInfo.getFlags());
		    }
		    
		    if(fdFile.canWrite()) outputs.add(""+fdObj.getMetadata().get(UUID_KEY));		    
		    else inputs.add(""+fdObj.getMetadata().get(UUID_KEY));
		    
		    String file_uuid = ""+fdObj.getMetadata().get(UUID_KEY);
		    
		    if(previouslyWritten) pcol.addNonProvenanceEdge(new NonProvenanceEdge(fdObj, file_uuid, UUID_KEY)); 
		}
		
		for(String id : inputs) { 
		    PLUSObject o = (PLUSObject)cache.get(id); 		    
		    if(o != null) pcol.addEdge(new PLUSEdge(o, inv)); 
		} 

		for(String id : outputs) { 
		    PLUSObject o = (PLUSObject)cache.get(id); 
		    if(o != null) pcol.addEdge(new PLUSEdge(inv, o));
		} 
		
		for(String id : related) { 
			// Just mark these as "contributing".
			PLUSObject o = (PLUSObject)cache.get(id); 
			if(o != null) pcol.addEdge(new PLUSEdge(o, inv, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_CONTRIBUTED));
		}
							
		boolean written = false; 
		
		if(pcol.countNodes() > 0)
			written = client.report(pcol);
		
		if(written)
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
	
	/**
	 * Return the PID of the process that PROCtor is running underneath.
	 * @return
	 */
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
		
		Path path = Paths.get(procPID.getAbsolutePath());
		UserPrincipal owner = Files.getOwner(path);
		String username = owner.getName();
		try {
			inv.setOwner(Neo4JPLUSObjectFactory.getActor(username, true));
		} catch(PLUSException exc) { 
			log.warning("Failed to set owner for " + inv + ": " + exc.getMessage());
		}
		
		cache.put(procFileID, inv);  // Cache this so we don't go back over it.
		
		return inv;
	}
	
	/**
	 * Create a PLUSObject corresponding to a given file, only if that file is new.  Note that throwing an
	 * ExistsException is not an error condition, to signal to the caller that provenance already exists.
	 * @param f the file to inspect.
	 * @return a PLUSObject if it is new.
	 * @throws ExistsException if provenance already exists for that object, this will be thrown.
	 * @throws NoSuchAlgorithmException on error
	 * @throws IOException on error.
	 */
	public PLUSObject createOnlyIfNew(File f) throws ExistsException, NoSuchAlgorithmException, IOException {
		if(f == null || !f.exists()) return null;
		
		if(!f.isFile()) return null;   // Don't log things like sockets right now.
		String id = getIDForFile(f); 
		
		if(id == null) { 
			log.warning("Couldn't compute file id for " + f);
			return null;
		}
				
		if(cache.containsKey(id)) throw new ExistsException(cache.get(id)); 
		
		ProvenanceCollection results = null;
		
		try { 
			results = Neo4JPLUSObjectFactory.loadBySingleMetadataField(User.DEFAULT_USER_GOD, UUID_KEY, id, 1);
		} catch(PLUSException exc) { 
			exc.printStackTrace();
			throw new RuntimeException(exc);
		}
		
		if(results != null && results.countNodes() > 0) {
			PLUSObject o = (PLUSObject) results.getNodes().toArray()[0];
			cache.put(id, o); 
			throw new ExistsException(o);
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
	
	public static Options makeCLIOptions() { 
		Options options = new Options();
		
		options.addOption(OptionBuilder.withArgName("pid")
				          .hasArg()
				          .isRequired(false)
				          .withDescription("If specified, capture only provenance for this single PID and its children.")
				          .create("pid"));
		
		options.addOption(OptionBuilder.withArgName("once")
						  .hasArg(false)
						  .isRequired(false)
						  .withDescription("Poll the PID fs once, and then quit")
						  .create("once"));
		
		options.addOption(OptionBuilder.withArgName("poll")
						  .hasArg(false)
						  .isRequired(false)
						  .withDescription("Poll continuously until user interrupts.")
						  .create("poll"));
		
		return options;
	}
	
	public static void usage() { 
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("PROCtor", makeCLIOptions());
	}
	
	/**
	 * If provided with arguments, the program processes only those PIDs. If given no arguments, it starts in polling mode.
	 */
	public static void main(String [] args) throws Exception {
		ProvenanceClient.instance = client;
		
		CommandLineParser parser = new GnuParser();
		
		if(!PROC.exists()) {
			log.severe("This utility is intended to run on Linux systems with a PROC filesystem. You do not appear to have one (or it is not readable)");
			System.exit(1);
		}
		
		try { 
			CommandLine line = parser.parse(makeCLIOptions(), args);
			String pidArg = line.getOptionValue("pid");
			
			
			boolean once = line.hasOption("once");
			boolean poll = line.hasOption("poll");
			
			System.out.println("Once " + once + " poll " + poll);
			
			PROCtor p = new PROCtor();
			
			if(once && poll) { 
				System.err.println("You can't specify both to run once and to poll.");
				usage();
				System.exit(1);
			}
			
			// Default is to poll if user hasn't otherwise specified.
			if(!poll && !once) poll = true;
			
			if(pidArg != null) { 
				System.out.println("PID=" + pidArg);
				String[] pids = pidArg.split(" +");
				
				for(String pid : pids) { 
					p.addPID(pid);
				}
			} 
		
			if(poll)
				p.run(5000, -1);
			else p.run(5000, 1);
		} catch(ParseException exc) {
			usage();
			System.exit(1);
		}
	}
} // End PROCtor
