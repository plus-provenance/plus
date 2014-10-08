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

/**
 * This class encapsulates redirect information for a user when they get a result
 * to a query.  For example, a SignPost could be "contact bblaustein@mitre.org for access
 * permissions".  
 * 
 * <p>A SignPost is <b>never</b> actual data content.  It is just a sign pointing elsewhere for
 * more information about the data item.  
 * @author moxious
 */
public class SignPost {
	/** Default sign post indicating that an object was sourced locally, with no further information. */
	public static final SignPost SRC_HINTS_LOCAL = new SignPost("local");
	
	/** Stores the sign post information */
	private String post;
	
	/** Constructs the sign post */
	public SignPost(String post)
		{ this.post = post; }
	
	/** Displays the sign post as a string */
	public String toString() { return post; } 	
} // End SignPost
