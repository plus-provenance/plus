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
 * Uninteresting class.  This does nothing but group all data related objects under one common ancestor, rather than
 * having them mixed in with invocations, etc.  It also sets the object type to "data" all the time.  Ho hum.
 * 
 * <p>All constructors call super
 * @author moxious
 */
public abstract class PLUSDataObject extends PLUSObject {
	public PLUSDataObject() { 
		super();
		setObjectType(PLUS_TYPE_DATA);
	} 
	
	public PLUSDataObject(String name) {		
		super(name); 
		setObjectType(PLUS_TYPE_DATA);
	}
	
	public PLUSDataObject(PLUSObject other) { 
		super(other);		
		setObjectType(PLUS_TYPE_DATA);
	} // End PLUSObject
	
	/** Always returns true */ 
	public boolean isDataItem() { return true; } 	
} // End PLUSDataObject
