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
package org.mitre.provenance.db.neo4j;

import org.mitre.provenance.PLUSException;

/**
 * Exception used to indicate that a given object does not exist.
 * @author moxious
 */
public class DoesNotExistException extends PLUSException {
	private static final long serialVersionUID = 5680935251131876261L;
	
	public DoesNotExistException(String msg) { super(msg); } 	
}
