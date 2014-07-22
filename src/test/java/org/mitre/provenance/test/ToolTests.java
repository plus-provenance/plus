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
package org.mitre.provenance.test;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.junit.Before;
import org.junit.Test;
import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.client.ProvenanceClient;
import org.mitre.provenance.contenthash.ContentHasher;
import org.mitre.provenance.contenthash.MD5ContentHasher;
import org.mitre.provenance.contenthash.SHA256ContentHasher;

public class ToolTests {
    @Before
    public void setUp() {
        ProvenanceClient.instance = new LocalProvenanceClient();
    }
	
	@Test
	public void testHashers() throws NoSuchAlgorithmException, IOException {
		String a = "Hello, World!";
		
		String knownCorrectMD5 = "65a8e27d8879283831b664bd8b7f0ad4";
		String knownCorrectSHA256 = "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f";
		
		MD5ContentHasher md5 = new MD5ContentHasher();
		SHA256ContentHasher sha256 = new SHA256ContentHasher();
		
		String md5Hash = ContentHasher.formatAsHexString(md5.hash(new ByteArrayInputStream(a.getBytes())));
		String sha256Hash = ContentHasher.formatAsHexString(sha256.hash(new ByteArrayInputStream(a.getBytes())));
		
		System.out.println("MD5 expected, actual:\n" + knownCorrectMD5 + "\n" + md5Hash);
		System.out.println("SHA256 expected, actual:\n" + knownCorrectSHA256 + "\n" + sha256Hash);
		
		assertTrue("MD5 hashes correct", knownCorrectMD5.equals(md5Hash));
		assertTrue("SHA256 hashes correct", knownCorrectSHA256.equals(sha256Hash)); 
	}
}
