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

import java.util.Date;

import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.user.User;

public abstract class Marking extends PLUSObject {
	/** When the marking was asserted.  Note that this is different than when the marking was
	 * created, which is stored by the PLUSObject super-class.
	 */
	protected Date whenAsserted; 
	/** Who made the marking, or who made the statement associated with the marking. */
	protected User claimant;
	
	/**
	 * Determines whether or not a marking is heritable.  "Heritable" means that if A has a marking
	 * M, and A -> B -> C in the lineage DAG, then both B and C also have marking M.
	 * @return true if the marking is heritable, false otherwise.
	 */
	public abstract boolean isHeritable();
	
	public User getClaimant() { return claimant; } 
	public void setClaimant(User user) { claimant = user; } 
	public Date getWhenAsserted() { return whenAsserted; } 
	public void setWhenAsserted(Date d) { whenAsserted = (Date) d.clone(); }
} // End Marking
