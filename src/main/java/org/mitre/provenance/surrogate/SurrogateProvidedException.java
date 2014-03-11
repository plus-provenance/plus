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
import org.mitre.provenance.plusobject.*;

/**
 * When a user asks for data out of the PLUS system, there are basically two answers they
 * can get.  The first is an object with the real underlying data.  The second answer is that
 * they could get a surrogate.
 * 
 * <p>This exception is thrown by the PLUS system when it can't answer your query directly,
 * (i.e. it's not giving you what you asked for) but it can answer with a surrogate.  This isn't
 * an error like most exceptions thrown would be, but a signaling mechanism that you're getting
 * something different than what you expected, but a valid answer.
 * 
 * <p>The exception itself is just a wrapper for a SurrogateDetail object, which has the
 * details of the surrogate response to the query.
 * @author DMALLEN
 *
 */
public class SurrogateProvidedException extends Exception {	
	private static final long serialVersionUID = 1L;
	
	protected PLUSObject surrogate; 
		
	public SurrogateProvidedException(PLUSObject surrogate) { 
		this.surrogate = surrogate;  
	}
	
	public PLUSObject getProvidedSurrogate() { return surrogate; } 
} // End SurrogateProvidedException
