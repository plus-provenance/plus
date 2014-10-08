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

import java.util.Hashtable;

import org.mitre.provenance.plusobject.*;

/**
 * A generic activity is one that was pulled from the database in response to PLUS queries, but that
 * is not executable.  This is to create the differentiation between: <b>(a)</b> a java Activity object 
 * that the user created in order to execute as part of a workflow, and <b>(b)</b> an Activity object
 * created as a result of pulling data from the database. 
 * @author moxious
 *
 */
public class GenericActivity extends Activity {
	public GenericActivity(String name) { super(name); } 

	/**
	 * Override the execute method.  This always throws an ActivityException no matter what the inputs,
	 * because generic activities cannot be executed.
	 */
	public Hashtable <String, PLUSObject> execute(Hashtable <String, PLUSObject> inputVariableMappings) 
		throws ActivityException {
		throw new ActivityException("This object is a generic activity populated from the database, "+
				                    "and cannot be executed!!!  If you see this error message, you are " +
				                    "trying to do something really strange!");		
	} // End execute
} // End GenericActivity
