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
package org.mitre.provenance.plusobject.json;

import java.util.HashSet;
import java.util.Map.Entry;

import org.mitre.provenance.PropertySet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * This is a wrapper class that allows a JsonObject from google GSON to behave like a PropertyContainer.
 * This makes it a lot easier to get/set properties out of a simple container into provenance objects.
 * @author moxious
 */
public class JsonObjectPropertyWrapper implements PropertySet {
	JsonObject obj = null;
	
	/** Create a new wrapper object that is entirely informed by its contained JsonObject */
	public JsonObjectPropertyWrapper(JsonObject o) { obj = o; } 
	
	public boolean hasProperty(String key) { return obj.has(key); }
	
	public Object getProperty(String key) {
		if(!obj.has(key)) return null;
		//JsonPrimitive p = obj.get(key).getAsJsonPrimitive();
		JsonElement elem = obj.get(key);
		
		if(elem.isJsonPrimitive() && ((JsonPrimitive)elem).isString()) return elem.getAsString();
		if(elem.isJsonPrimitive() && ((JsonPrimitive)elem).isNumber()) return elem.getAsLong();
		
		if(elem.isJsonArray()) {
			JsonArray arr = (JsonArray)elem;

			String[] r = new String [arr.size()];
			
			for(int x=0; x<arr.size(); x++) {
				r[x] = arr.get(x).getAsString();
			}
			
			return r;
		}
		
		// Default case.
		return elem.getAsString(); 
	} 
	
	public Object getProperty(String key, Object defaultValue) {
		if(!hasProperty(key)) return defaultValue;
		return getProperty(key); 
	}

	public Iterable<String> getPropertyKeys() {
		HashSet<String> keys = new HashSet<String>();
		for(Entry<String,JsonElement> e : obj.entrySet()) 
			keys.add(e.getKey());
		
		return keys;
	}
}
