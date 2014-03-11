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

/**
 * This is a type of ObjectPreference, but specific to surrogates. 
 * Preference functions that take into account specific details of surrogates should 
 * extend this class rather than ObjectPreference.
 * @see ObjectPreference
 * @author MDMORSE
 */
public abstract class SurrogatePreference extends ObjectPreference {
	
} // End SurrogatePreference