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

/**
 * Surrogate functions use edge markings to indicate which edges in the DAG should be hidden, inferred, shown, 
 * etc.  This class represents a surrogate's "vote" on what should be done with an edge in a LineageDAG
 * @author DMALLEN
 */
public class EdgeMarking {
	/** Default marking to show an edge */
	public static final EdgeMarking SHOW = new EdgeMarking("show");
	/** Default marking to infer an edge */
	public static final EdgeMarking INFER = new EdgeMarking("infer");
	/** Default marking to hide an edge */
	public static final EdgeMarking HIDE = new EdgeMarking("hide");
	
	protected String marking; 
	
	/** Create a new edge marking identified by a particular type.  */
	public EdgeMarking(String type) { marking = type; }
	
	public String toString() { return marking; }  

	public boolean equals(Object other) {
		if(!(other instanceof EdgeMarking)) return false;
		return toString().equals(""+((EdgeMarking)other).toString()); 
	} 
	
	/** Return true if the marking is "show", false otherwise */
	public boolean isShow() { return "show".equals(marking); }
	
	/** Return true if the marking is "hide", false otherwise */
	public boolean isHide() { return "hide".equals(marking); } 
	
	/** Return true if the marking is "infer", false otherwise. */
	public boolean isInfer() { return "infer".equals(marking); } 	
} // End EdgeMarking
