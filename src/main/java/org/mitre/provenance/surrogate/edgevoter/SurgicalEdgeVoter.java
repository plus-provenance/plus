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
package org.mitre.provenance.surrogate.edgevoter;

import org.mitre.provenance.EdgeMarking;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.surrogate.EdgeVoter;

/**
 * Make one particular vote on one particular edge.
 * @author DMALLEN
 */
public class SurgicalEdgeVoter implements EdgeVoter {
	String markerID; 
	EdgeMarking marking; 

	/**
	 * Create a new surgical edge marker.  Vote only "marking" on only edges to "other".
	 * Otherwise always vote null. 
	 * @param otherID ID of the other participant node to the edge
	 * @param marking create a voter for edges with this type of marking
	 */
	public SurgicalEdgeVoter(String otherID, EdgeMarking marking) {
		markerID = otherID; 
		this.marking = marking;
	} 
	
	public void setMarking(EdgeMarking marking) { 
		this.marking = marking; 
	} // End setMarking
	
	public EdgeMarking getMarking(PLUSObject other) {
		if(markerID.equals(other.getId())) return marking;
		return null;
	} // End getMarking
} // End SurgicalEdgeVoter
