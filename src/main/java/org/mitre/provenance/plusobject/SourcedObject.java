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

import org.mitre.provenance.surrogate.SignPost;

/** 
 * A sourced object is an object that can carry around with it information about where it is stored, or where it 
 * came from.  This is not the same thing as provenance, only an identification of storage location.
 * <p>Provenance objects need this, for example on the JXTA network to identify which peer they came from.  Most
 * sourced objects will be "local" by default. 
 */
public interface SourcedObject {
	/**
	 * Get the source hints information about this object.  This is typically not stored in a database, but is meta-provenance
	 * describing who stores the object, such as in the case of distributed objects.
	 */
	public SignPost getSourceHints();
	
	/**
	 * Set the source hint information about this object.  This is typically not stored in a database, but is meta-provenance
	 * describing who stores the object, such as in the case of distributed objects.
	 * @param sp a SignPost object.
	 */
	public void setSourceHints(SignPost sp);
} // End ProvenanceObject
