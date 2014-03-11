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

import java.util.Hashtable;

import org.mitre.provenance.plusobject.marking.Taint;

/**
 * Models the type of a PLUSObject.
 * @author DMALLEN
 */
public class PLUSObjectType
{
	/** Stores registered classes which have been associated with PLUS object types */
	private static Hashtable<PLUSObjectType,Class<? extends PLUSObject>> registeredClasses = new Hashtable<PLUSObjectType,Class<? extends PLUSObject>>();
	
	/** Register classes associated with PLUS object types */
	static {
		registeredClasses.put(new PLUSObjectType("heritable", "taint"), Taint.class);
		registeredClasses.put(new PLUSObjectType("workflow", "*"), PLUSWorkflow.class);
		registeredClasses.put(new PLUSObjectType("workflow", "execution"), PLUSWorkflow.class); 
		registeredClasses.put(new PLUSObjectType("activity", "*"), PLUSActivity.class);
		registeredClasses.put(new PLUSObjectType("activity", "registration"), PLUSActivity.class);
		//registeredClasses.put(new PLUSObjectType("data", "anontable"), PLUSAnonTable.class);
		registeredClasses.put(new PLUSObjectType("data", "file"), PLUSFile.class);
		registeredClasses.put(new PLUSObjectType("data", "file-image"), PLUSFileImage.class);
		registeredClasses.put(new PLUSObjectType("invocation", "*"), PLUSInvocation.class);
		registeredClasses.put(new PLUSObjectType("invocation", "invocation"), PLUSInvocation.class);
		registeredClasses.put(new PLUSObjectType("data", "string"), PLUSString.class);
		registeredClasses.put(new PLUSObjectType("data", "URL"), PLUSURL.class); 
		registeredClasses.put(new PLUSObjectType("generic", "generic"), PLUSGeneric.class);
	}
	
	/** Defines the PLUS object type */
	private String objectType;
	
	/** Defines the PLUS object subtype */
	private String objectSubtype;

	/** Construct the PLUS object type */
	public PLUSObjectType(String objectType, String objectSubtype)
		{ setObjectType(objectType); setObjectSubtype(objectSubtype); }

	// PLUS object type getters
	public String getObjectType() { return objectType; } 
	public String getObjectSubtype() { return objectSubtype; } 
	
	/** Set the PLUS object type */
	public void setObjectType(String objectType)
		{ this.objectType = objectType==null ? "" : objectType; } 
	
	/** Set the PLUS object subtype */
	public void setObjectSubtype(String objectSubtype)
		{ this.objectSubtype = objectSubtype==null ? "" : objectSubtype; } 
	
	/** Indicates if two PLUS objects are compatible with one another */
	public boolean compatibleWith(PLUSObjectType other)
		{ return equals(other); }
	
	/** Indicates if two PLUS objects are equal to one another */
	public boolean equals(Object object) {
		if(!(object instanceof PLUSObjectType)) return false;
		PLUSObjectType type = (PLUSObjectType)object;
		return objectType.equals(type.objectType) && objectSubtype.equals(type.objectSubtype);
	}
	
	/** Outputs the PLUS object type as a string */
	public String toString()
		{ return new String("<type=" + getObjectType() + " subtype=" + getObjectSubtype() + ">"); }
	
	/** Generates a hash code for the PLUS object */
	public int hashCode()
		{ return objectType.hashCode() + objectSubtype.hashCode(); }

	/** Return the class registered with the PLUS object type */
	public Class<? extends PLUSObject> getRegisteredClass()
		{ return registeredClasses.get(this); }
}