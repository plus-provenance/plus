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

/**
 * A content hasher which uses the JRE's built-in implementation of SHA-256.
 * See http://download.oracle.com/javase/1.4.2/docs/api/java/security/MessageDigest.html for more information.
 * @author DMALLEN
 */
public class SHA256ContentHasher extends ContentHasher {
	MessageDigest digester; 
	
	public SHA256ContentHasher() throws NoSuchAlgorithmException { 
		digester = MessageDigest.getInstance("SHA-256");
	}
	
	public MessageDigest getDigest() { return digester; } 
} // End ContentHasher
