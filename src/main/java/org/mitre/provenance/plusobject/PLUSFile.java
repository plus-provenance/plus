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
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.PropertySet;
import org.mitre.provenance.contenthash.ContentHasher;
import org.mitre.provenance.contenthash.SHA256ContentHasher;

/**
 * A generic file existing in some abstract path.
 * @author moxious
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
	}
	
	public PLUSFile(String filename) { 
		super(filename);
		origPath = filename; 
		file = new File(filename); 
		setObjectSubtype(PLUS_SUBTYPE_FILE);
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
	
	public PLUSObject setProperties(PropertySet props, ProvenanceCollection contextCollection) throws PLUSException { 
		super.setProperties(props, contextCollection);
		setOriginalPath(""+props.getProperty("originalPath"));
		return this;
	}
} // End PLUSFile
