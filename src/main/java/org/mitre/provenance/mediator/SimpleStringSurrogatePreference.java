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
package org.mitre.provenance.mediator;

import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSString;

/**
 * This is a class that will mediate various surrogates.
 * I.e. given several different (surrogate) options for a data item,
 * the most preferred surrogate will be returned.  This implies a total ordering.
 * @author MDMORSE
 */
public class SimpleStringSurrogatePreference extends SurrogatePreference {	
	public SimpleStringSurrogatePreference() { ; }
	
	/**
	 * this procedure takes two simple string data items, comparing
	 * them lexicographically. 
	 * @param a the first argument is a data surrogate
	 * @param b the second argument is a data surrogate that is compared to the first
	 * @return int value less than 0 if a is preferable to b, otherwise b is preferable
	 */
	public int preference(PLUSObject a, PLUSObject b){
		PLUSString aString = (PLUSString) a;
		PLUSString bString = (PLUSString) b;
		
		return aString.getContent().compareTo(bString.getContent());
	}
} // End SimpleStringSurrogatePreference
