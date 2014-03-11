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
package org.mitre.provenance.plusobject.marking;

import org.mitre.provenance.plusobject.PLUSObject;

/**
 * A kind of marking that can be inherited down the lineage tree.  E.g. in the lineage DAG
 * A -> B -> C, if A has a particular heritable marking, then B and C can be considered to have
 * the same marking.
 * @author DMALLEN
 */
public abstract class HeritableMarking extends Marking {	
	public HeritableMarking() { 
		super();
		setName("HeritableMarking"); 
		setObjectType(PLUSObject.PLUS_TYPE_HERITABLE); 
		setObjectSubtype(PLUSObject.PLUS_TYPE_HERITABLE);
	} // End HeritableMarking
	
	public boolean isHeritable() { return true; }
} // End HeritableMarking
