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

/**
 * A generic plus object.  Contains no data other than what is in the default PLUSObject base type.
 * Default plus objects can't be instantiated and written to the DB though, so we have PLUSGeneric.
 * @author moxious
 */
public class PLUSGeneric extends PLUSObject {
	public static final String PLUS_SUBTYPE_GENERIC = "generic";
	
	public PLUSGeneric() { 
		super();
		setObjectType(PLUS_TYPE_GENERIC);
		setObjectSubtype(PLUS_SUBTYPE_GENERIC); 		
	}
	
	public PLUSGeneric(String customType, String customSubtype) { 
		super();
		setObjectType(customType);
		setObjectSubtype(customSubtype);
	}
	
	public PLUSGeneric(String name) { 
		this();
		setName(name); 		
	}
	
	public PLUSGeneric(PLUSObject other) {
		super(other);
		copy(other); 
	}
} // End PLUSGeneric
