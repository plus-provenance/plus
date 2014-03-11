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
package org.mitre.provenance.workflowengine.activity;

import org.mitre.provenance.plusobject.*;

import java.util.*;

/**
 * Another special bogus activity.  This one emits specified objects in its output.
 * The execute() method doesn't even do anything except parrot the tuples you gave it earlier.
 * @author DMALLEN
 */
public class PLUSObjectEmittingActivity extends Activity {
	Hashtable <String,PLUSObject> emitted;
	
	public PLUSObjectEmittingActivity(String activityName) {
		super(activityName);
		emitted = new Hashtable <String,PLUSObject> ();
	} // End DBTupleEmittingActivity
	
	public void addEmittedObject(String varName, PLUSObject obj) { 
		emitted.put(varName, obj);
	} // End addEmittedObject
	
	/**
	 * @see Activity#execute(Hashtable)
	 */
	public Hashtable <String, PLUSObject> execute(Hashtable <String, PLUSObject> inputVariableMappings) 
		throws ActivityException { 		
		// Dead simple.
		return emitted;
	} // End execute	
} // End DBTupleEmittingActivity
