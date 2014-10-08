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

/**
 * This class exists just to create a separate type for activity exceptions, so that they can be handled
 * separately.
 * @author moxious
 *
 */
public class ActivityException extends Exception {
	private static final long serialVersionUID = 2821080249938657084L;
	public ActivityException() { super(); } 
	public ActivityException(String message) { super(message); } 
	public ActivityException(String message, Throwable t) { super(message, t); } 
}
