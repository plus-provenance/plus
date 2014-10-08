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

/**
 * This exception is used by surrogacy functions to indicate that something bad has happened.
 * @author moxious
 *
 */
public class SurrogateException extends Exception {
	private static final long serialVersionUID = -375008079823503305L;

	public SurrogateException() {
		super();
	}

	public SurrogateException(String arg0) {
		super(arg0);		
	}
	
	public SurrogateException(String arg0, Throwable arg1) { 
		super(arg0, arg1);
	}
} // End SurrogateException
