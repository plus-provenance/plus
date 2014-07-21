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

import java.util.Map;

import org.mitre.provenance.plusobject.ProvenanceCollection;

/**
 * Interface that describes an object that can be written to/restored from a store that can handle key/value pairs.
 * @author moxious
 * @see org.mitre.provenance.db.neo4j.Neo4JStorage
 */
public interface PropertyCapable {
	/** All PropertyCapable objects must be identifiable by a particular string */
	public String getId();
	
	/**
	 * Get a map of the fields that need to be stored and their values in order to store this object in Neo4J.
	 * This is only a list of simple properties and values, and does not include any relationships the object
	 * might have to other complex objects.
	 * @return a map
	 */
	public Map<String,Object> getStorableProperties();
	
	/**
	 * Set the internal properties of this object to reflect the properties provided by Neo4J
	 * @param props properties read from a Neo4J node
	 * @param contextCollection optional (can be null) collection which contains the context of other objects being created.
	 * Because some objects refer to others by reference IDs, this is used to look up those other objects as needed.
	 * @return the returned object.
	 * @throws PLUSException
	 */
	public Object setProperties(PropertySet props, ProvenanceCollection contextCollection) throws PLUSException;
} // End Neo4JCapable
