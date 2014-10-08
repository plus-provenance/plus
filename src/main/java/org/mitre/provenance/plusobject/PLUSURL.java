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

import java.net.MalformedURLException;
import java.net.URL;

/**
 * A web URL with a name.
 * @author moxious
 */
public class PLUSURL extends PLUSString {
	protected URL url;
	public static final String PLUS_SUBTYPE_URL = "URL";
	
	public PLUSURL() { 
		super(); 
	}
	
	public PLUSURL(String name, String urlLocation) { 
		super(name); 
		setContent(urlLocation); 
		setObjectType(PLUS_TYPE_DATA); 
		setObjectSubtype(PLUS_SUBTYPE_URL); 
	}
	
	public PLUSURL(String urlLocation) { 
		super(urlLocation);
		setContent(urlLocation); 
		setObjectType(PLUS_TYPE_DATA);
		setObjectSubtype(PLUS_SUBTYPE_URL);
	}
	
	public PLUSString clone() { 
		PLUSURL s = new PLUSURL();
		s.copy(this); 
		return s;
	}
	
	public URL getURL() throws MalformedURLException { 
		url = new URL(content);
		return url;
	}
	
	public void copy(PLUSString other) { 
		super.copy(other);
		setContent(other.getContent());
		setObjectType(PLUS_TYPE_DATA);
		setObjectSubtype(PLUS_SUBTYPE_URL); 
	} // End copy
	
	public String getContent() { return content; }  
	public void setContent(String content) { this.content = content; }  	
} // End PLUSURL
