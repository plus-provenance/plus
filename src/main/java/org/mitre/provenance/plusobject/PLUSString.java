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

import java.util.Map;

import org.mitre.provenance.PLUSException;
import org.neo4j.graphdb.PropertyContainer;

/**
 * The simplest plus data item possible - just a string.
 * @author DMALLEN
 */
public class PLUSString extends PLUSDataObject {
	String content;
	public static final String PLUS_SUBTYPE_STRING = "string";	
	
	public PLUSString() { 
		super(); 
	}
	
	public PLUSString(String name, String content) { 
		super(name); 
		setContent(content); 
		setObjectType(PLUS_TYPE_DATA); 
		setObjectSubtype(PLUS_SUBTYPE_STRING); 
	}
	
	public PLUSString(String content) { 
		super(content);
		setContent(content); 
		setObjectType(PLUS_TYPE_DATA);
		setObjectSubtype(PLUS_SUBTYPE_STRING);
	}
	
	public PLUSString clone() { 
		PLUSString s = new PLUSString();
		s.copy(this); 
		return s;
	}
	
	public void copy(PLUSString other) { 
		super.copy(other);
		setContent(other.getContent());
	} // End copy
	
	public String getContent() { return ""+content; }  
	public void setContent(String content) { this.content = content; }  
	
	public Map<String,Object> getStorableProperties() {
		Map<String,Object> m = super.getStorableProperties();
		m.put("content", getContent());
		return m;
	}
	
	public PLUSObject setProperties(PropertyContainer props) throws PLUSException { 
		super.setProperties(props);
		setContent(""+props.getProperty("content"));
		return this;
	}
} // End PLUSString
