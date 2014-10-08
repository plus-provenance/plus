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
package org.mitre.provenance.surrogate.sgf;

import org.mitre.provenance.plusobject.*;
import org.mitre.provenance.surrogate.SignPost;
import org.mitre.provenance.surrogate.SurrogateDetail;
import org.mitre.provenance.surrogate.SurrogateException;
import org.mitre.provenance.surrogate.SurrogateGeneratingFunction;
import org.mitre.provenance.surrogate.SurrogateQuality;
import org.mitre.provenance.user.User;

import java.util.Date;

/**
 * Sample surrogate function.  Given a simple string, replace all instances of "banned words"
 * with the string "(REDACTED)"
 * @author moxious
 */
public class StringRedactionSurrogateFunction extends SurrogateGeneratingFunction {
	/** The specific list of words that are banned. */
	public String [] bannedWords = new String [] { "alien", "roswell", "CIA", 
			"Alien", "Roswell"
	};
	
	public StringRedactionSurrogateFunction() { 
		super();
	} // End StringRedactionSurrogateFunction
	
	/*
	 * Generate a redacted string as a surrogate.
	 * This method ignores the security priveleges of the user and does its thing every time
	 * regardless of what the user is allowed to see.  
	 * @see org.mitre.provenance.surrogate.SurrogateGeneratingFunction#generateSurrogate(org.mitre.provenance.PLUSEngine, java.lang.String, org.mitre.provenance.user.User)
	 */
	public PLUSObject generateSurrogate(PLUSObject obj, User user) 
		throws SurrogateException { 
		PLUSString ss = (PLUSString)obj;
		String content = ss.getContent();
			
		// Replace the words.
		String result = content;			
		for(int x=0; x<bannedWords.length; x++) { 				
			result = result.replaceAll(bannedWords[x], "(REDACTED)");
		} // End for
			
		// Create the surrogate.
		PLUSString surrogate = new PLUSString(result);
		surrogate.setName("Redaction of " + obj.getName());		
		
		SurrogateQuality q = new SurrogateQuality();
		q.put("security", "0");
		q.put("generator", "StringRedactionSurrogateFunction");
		q.put("generated", (new Date()).toString());
		q.put("complete", "No Guarantee");
		
		SurrogateDetail det = new SurrogateDetail(q, 
					new SignPost("Contact ReplaceMe@template.com for access permissions."));
		det.setEdgePolicy(SurrogateDetail.EDGE_POLICY_HIDE_ALL);
		surrogate.setSurrogateDetail(det);
		
		return surrogate; 
	} // End generateSurrogate()
} // End DataSurrogateDetail()
