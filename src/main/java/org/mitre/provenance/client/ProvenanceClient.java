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
package org.mitre.provenance.client;

/**
 * This is a placeholder for a global provenance client, something that can be used in many different threads.
 * Modify the instance below 
 * @author moxious
 *
 */
public class ProvenanceClient {
	/**
	 * The provenance client to use in the course of provenance operations.  By default, this is the dummy provenance client which always
	 * throws RuntimeExceptions.  In your code, you should specify the appropriate client for your use case. 
	 */
	public static AbstractProvenanceClient instance = new DummyProvenanceClient();
}
