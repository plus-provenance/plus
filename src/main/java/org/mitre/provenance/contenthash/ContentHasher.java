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
package org.mitre.provenance.contenthash;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * In certain contexts, it may be useful to pair a PLUSObject with a hash of the contents of the data asset that it represents.
 * This abstract class provides methods and an interface useful for various content hashing algorithms.
 * @author DMALLEN
 */
public abstract class ContentHasher {
	protected byte[] hashWith(InputStream is, MessageDigest digester) throws IOException { 
		int length = 1024 * 8;
		byte[] bytes = new byte[length];
	    
        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (numRead >= 0 &&
        	   ((bytes.length-offset) > 0) &&
               (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            digester.update(bytes, offset, numRead);
        	offset += numRead;
        }
        
        byte [] result = digester.digest();
        digester.reset();
        return result;		
	}
	
	public abstract MessageDigest getDigest();
	
	/**
	 * Take the byte array that results from a content hasher, and format it as a text string.
	 * Typically, byte arrays will contain non-printable binary data, this will clean it up to a hex
	 * value suitable for use in Strings.
	 * @param hashValue the byte array that came from a content hashing algorithm
	 * @return a String value of the same, hex packed.
	 */
	public static String formatAsHexString(byte [] hashValue) {
		BigInteger bi = new BigInteger(1, hashValue);
	    return String.format("%0" + (hashValue.length << 1) + "X", bi).toLowerCase();		
	}	
	
	public byte[] hash(InputStream is) throws IOException {
		return hashWith(is, getDigest());
	} // End hash
} // End ContentHasher
