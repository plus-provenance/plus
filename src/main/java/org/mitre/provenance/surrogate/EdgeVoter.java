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
package org.mitre.provenance.surrogate;
import org.mitre.provenance.EdgeMarking;
import org.mitre.provenance.plusobject.PLUSObject;

/**
 * A class that a surrogate can use to define a computation that votes on edges. 
 * @author moxious
 */
public interface EdgeVoter {
	/**
	 * Given an edge to another object, return which marking the edge should have.
	 * @param other the other participant in the edge
	 * @return the marking representing the surrogate's "vote" on the edge.
	 */
	public EdgeMarking getMarking(PLUSObject other); 
} // End EdgeVoter
