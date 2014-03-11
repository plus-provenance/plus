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
package org.mitre.provenance.plusobject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserPrincipal;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.contenthash.ContentHasher;
import org.mitre.provenance.contenthash.SHA256ContentHasher;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.neo4j.graphdb.PropertyContainer;

/**
 * A generic file existing in some abstract path.
 * @author DMALLEN
 */
public class PLUSFile extends PLUSDataObject {
	protected File file;
	protected String origPath;
	
	public static final String PLUS_SUBTYPE_FILE = "file";
		
	public PLUSFile() { 
		super(); 
	}
	
	public PLUSFile(File file) { 
		super(file.getName());
		try { origPath = file.getCanonicalPath(); } 
		catch(IOException exc) { 
			origPath = file.getAbsolutePath();
		}
		
		this.file = file;
		setObjectSubtype(PLUS_SUBTYPE_FILE);
		extractMetadata();
	}
	
	public PLUSFile(String filename) { 
		super(filename);
		origPath = filename; 
		file = new File(filename); 
		setObjectSubtype(PLUS_SUBTYPE_FILE);
		extractMetadata();
	}
	
	/**
	 * Hash the contents of the file, and place it into the object metadata.
	 * @return the hash of the file's content.
	 * @throws IOException 
	 */
	public String hash() throws PLUSException, IOException { 
		FileInputStream fis = new FileInputStream(file);
		try {
			SHA256ContentHasher hasher = new SHA256ContentHasher();
			byte [] hash = hasher.hash(fis);			
			String h = ContentHasher.formatAsHexString(hash);
			getMetadata().put(Metadata.CONTENT_HASH_SHA_256, h); 
			return h;
		} catch (NoSuchAlgorithmException e) {
			throw new PLUSException("Error hashing: " + e, e);
		} finally { fis.close(); } 
	} // End hash
	
	protected void extractMetadata() { 
		if(!file.exists() || !file.canRead()) return;
		
		// Try to figure out who owns this file, if it exists.
		if(getOwner() == null) {
			// This is a java-7 ism that permits us to access the file owner.
			try { 
				Path path = Paths.get(file.getAbsolutePath());
				UserPrincipal owner = Files.getOwner(path);
				String username = owner.getName();
				try {
					setOwner(Neo4JPLUSObjectFactory.getActor(username, true));
				} catch (PLUSException e) {
					log.warning(e.getMessage());
				}
			} catch(IOException exc) { 
				log.warning(exc.getMessage());
			}
		}
		
		try { getMetadata().put("isLink", ""+file.getCanonicalFile().equals(file.getAbsoluteFile())); } catch(Exception exc) { ; } 
		try { getMetadata().put("exists", ""+file.exists()); } catch(Exception exc) { ; }
		try { getMetadata().put("path", file.getAbsolutePath()); } catch(Exception exc){ ; }
		try { getMetadata().put("canonical", file.getCanonicalPath()); } catch(Exception exc) { ; } 
		try { getMetadata().put("directory", ""+file.isDirectory()); } catch(Exception exc) { ; } 
		try { getMetadata().put("lastmodified", ""+file.lastModified()); } catch(Exception exc) { ; }		
		try { getMetadata().put("size", ""+file.length()); } catch(Exception exc) { ; } 
	} // End extractMetadata
	
	public PLUSObject clone() { 
		PLUSFile f = new PLUSFile();
		f.copy(this);
		return f;
	}
	
	public void copy(PLUSFile other) { 
		super.copy(other);
		setOriginalPath(other.getOriginalPath()); 
		setFile(other.getFile());
		setObjectType(PLUS_TYPE_DATA);
		setObjectSubtype(PLUS_SUBTYPE_FILE); 
	} // End copy
	
	public File getFile() { return file; } 
	public void setFile(File file) { this.file = file; } 
	public String getOriginalPath() { return origPath; } 
	protected void setOriginalPath(String origPath) { this.origPath = origPath; } 
	
	public Map<String,Object> getStorableProperties() {
		Map<String,Object> m = super.getStorableProperties();
		m.put("originalPath", getOriginalPath());
		return m;
	}
	
	public PLUSObject setProperties(PropertyContainer props) throws PLUSException { 
		super.setProperties(props);
		setOriginalPath(""+props.getProperty("originalPath"));
		return this;
	}
} // End PLUSFile
