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

/**
 * This is a class that will mediate various objects.
 * I.e. given several different options for a data item,
 * the most preferred object will be returned.  This implies a total ordering.
 * @author MDMORSE
 */
public abstract class ObjectPreference {
	/**
	 * this procedure should be overridden by classes that inherit from this.
	 * It takes two data surrogates and returns a number less than or equal to 0 if
	 * <i>a</i> is preferable to <i>b</i> and a number greater than 0 if b is preferable
	 * to a.   The default (if 0 is returned) is to assume that <i>a</i> is preferrable.
	 * @param a a PLUSObject 
	 * @param b a PLUSObject
	 * @return a value less than 0 if a is preferrable to b, otherwise a number greater than 0 if b is 
	 * preferrable to a
	 */
	public int preference(PLUSObject a, PLUSObject b){
		//default is to take the one with the greater security.
		// But that's a little tricky with priv classes.  
		return 1;
	} // End preference
} // End ObjectPreference
