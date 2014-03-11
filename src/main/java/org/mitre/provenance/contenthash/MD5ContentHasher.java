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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5ContentHasher extends ContentHasher {
	protected static MessageDigest digester = null;
	
	static { 
		try { digester = MessageDigest.getInstance("MD5"); } 
		catch(NoSuchAlgorithmException exc) { 
			exc.printStackTrace();
		}
	}
	
	public MD5ContentHasher() throws NoSuchAlgorithmException { 
		if(digester == null) throw new NoSuchAlgorithmException();
	}
	
	public MessageDigest getDigest() { return digester; } 
} // End MD5ContentHasher
