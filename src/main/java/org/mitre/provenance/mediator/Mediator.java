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

import java.util.Iterator;
import java.util.List;

import org.mitre.provenance.plusobject.PLUSObject;

/**
 * This is a class that will mediate various surrogates.
 * I.e. given several different (surrogate) options for a data item,
 * the most preferred surrogate will be returned.  This implies a total ordering.
 * @author MDMORSE
 */
public class Mediator {	
	ObjectPreference objPref;	
	
	/**
	 * Create a mediator using the GenericObjectPreference.
	 * @see GenericObjectPreference
	 */
	public Mediator() { 
		this(new GenericObjectPreference()); 
	}
	
	/**
	 * Create a mediator using a given ObjectPreference.
	 * @param sp the preference function implementation.
	 */
	public Mediator(ObjectPreference sp) {
		objPref = sp;
	}
	
	/**
	 * Method that takes a list of Surrogates, and returns the most preferred
	 * in the total order.
	 * @param surrogates an arraylist of data surrogates
	 * @return DataSurrogateDetail - the most preferred surrogate.
	 */
	public PLUSObject getMostPreferable(List<PLUSObject> surrogates){
		Iterator<PLUSObject> surrogateIterator = surrogates.iterator();
		PLUSObject mostPreferred = surrogateIterator.next();
		
		while(surrogateIterator.hasNext()){
			PLUSObject detail = surrogateIterator.next();
			if(objPref.preference(mostPreferred, detail) > 0){
				mostPreferred = detail;
			}
		}
		return mostPreferred;
	} // End getMostPrefeeable
	
	public ObjectPreference getPreferenceFunction() { 
		return objPref;  
	}
	
	public void setPreferenceFunction(ObjectPreference pref) { 
		objPref = pref; 
	}
} // End Mediator