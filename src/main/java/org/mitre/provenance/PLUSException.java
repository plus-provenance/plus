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
package org.mitre.provenance;

/**
 * A standard exception.  Nothing special, but very frequently used in the PLUS classes.
 * @author moxious
 */
public class PLUSException extends Exception {
	static final long serialVersionUID = 1130010014L;
	public PLUSException() { ; } 
	public PLUSException(String message) { super(message); } 
	public PLUSException(Throwable t) { super(t); } 
	public PLUSException(String message, Throwable t) { super(message, t); } 
} // End PLUSException 
