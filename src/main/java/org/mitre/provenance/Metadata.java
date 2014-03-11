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
package org.mitre.provenance;

import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

/**
 * A metadata object is a simple lookup table of key/value pairs.
 * Metadata objects are tagged to all PLUSObjects, allowing you to associate arbitrary extra information
 * with an object regardless of type.
 *
 * <p>Provenance objects contain base metadata which applies to all sorts of things you might want to log
 * provenance about, but Metadata provides an "escape hatch" of sorts where you can apply any kind of 
 * arbitrary system-specific metadata to a provenance object.  This class implements that feature.
 * @author DMALLEN
 */
public class Metadata extends java.util.Hashtable <String,Object> {	
	public static final long serialVersionUID = 131324134;	
	
	/** The maximum size of a key permitted.  This must be limited to prevent storage-layer problems */
	public static final int MAX_KEY_SIZE = 128;
	
	/** The maximum size of a value permitted.  This must be limited to prevent storage-layer problems. */
	public static final int MAX_VALUE_SIZE = 16 * 1024;  // 16KB.
	
	/**
	 * This key is to be used to store SHA-256 content hashes of the data the provenance refers to.
	 * Normally, we'll expect that the value behind this key is something that came from PLUS content
	 * hashing tools.   That is, the value should be UTF-8 text containing LOWERCASE base64 text describing the
	 * hash.   For example, the correct SHA256 hash of the string "Hello, World!" is: 
	 * dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f
	 * 
	 * @see org.mitre.provenance.contenthash.ContentHasher#formatAsHexString(byte[]) 
	 */
	public static final String CONTENT_HASH_SHA_256 = "sha256hash";
	
	/** Value that tracks which PLUSObject owns this metadata.  This value is only set if/when writeToDB()
	 * is called.
	 */
	String ownerOID = null;
	
	public Metadata() { 
		super();
		setOwnerOID(null); 
	} 
	
	/** 
	 * Copy the contents of another map into this metadata object.
	 * @param other a map containing keys and values to be copied.
	 */
	public void copy(Map <String, Object> other) { 
		if(other == null) return; 
		Iterator <String> it = other.keySet().iterator();
		while(it.hasNext()) {
			String key = it.next();
			put(""+key, other.get(key));
		} // End while
	} // End copy
		
	public Object put(String key, Number val) { return put(key, ""+val); }
	
	public boolean equals(Object other) { 
		if(!(other instanceof Metadata)) return false;
		return super.equals(other);
	}
	
	/**
	 * Put a key and value into the Metadata object.
	 * @param key the key
	 * @param val the value
	 * @see java.util.Hashtable#put(Object, Object)
	 */
	public Object put(String key, String val) {
		// Append blank text to val, so that if it's null,
		// you'll get the string null.  Otherwise, null val will
		// cause an exception, which isn't desirable here.				
		Object result = super.put(key, ""+val);		
		return result;
	} // End put
	
	public String getOwnerOID() { return ownerOID; } 
	public void setOwnerOID(String ownerOID) { this.ownerOID = ownerOID; } 
	
	public void dump(PrintStream output) throws Exception { 
		output.println("METADATA:");
		Enumeration <String> keys = keys();
		while(keys.hasMoreElements()) { 
			String e = keys.nextElement();
			output.println(e + " = " + get(e)); 
		} // End while
	} // End dump
} // End Metadata
