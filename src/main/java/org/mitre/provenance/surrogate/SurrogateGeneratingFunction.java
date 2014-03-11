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

import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.user.User;

/**
 * This function describes the interface for building a surrogate generating function.
 * @see SurrogateGeneratingFunction#generateSurrogate(PLUSObject, User)
 * @author DMALLEN
 *
 */
public abstract class SurrogateGeneratingFunction {
	/**
	 * This function will do nothing but throw an exception.  Please subclass it!
	 * <p>All Surrogate Generating Functions (SGFs) take two arguments - the object for which they will generate
	 * a surrogate, and the user who is requesting access to the data.  The object itself will always contain
	 * the underlying full-access data, equivalent to what is visible to the data owner, or the super-user.
	 * <p>The surrogate generating function takes the information about the user and uses it to compute and
	 * return a suitable surrogate for that user.  PLUS guarantees that the surrogate will be shown to that user,
	 * but no other user with privileges dominated by this user's privileges.
	 * <p>Note that in the return PLUSObject value, you must use the setSurrogateDetail method.  Objects that come
	 * back without surrogate details will not be considered valid.
	 * @param object the object for which a surrogate is needed.
	 * @param user the user who is requesting the object.
	 * @return a PLUSObject corresponding to the surrogate, with surrogate detail information present.
	 * @throws SurrogateException if there is an error computing the surrogate
	 * @see Surrogateable#setSurrogateDetail(SurrogateDetail)
	 */
	public PLUSObject generateSurrogate(PLUSObject object, User user) 
		throws SurrogateException { 
		throw new SurrogateException("You must override me!");
	} // End generateSurrogate
	
	/**
	 * Indicate whether the SurrogateGeneratingFunction is currently runnable.  
	 * Some SGFs require configuration parameters, or may involve processes on foreign hosts.  If your SGF
	 * needs to check on those kinds of situations, override this method and put that checking code in here.
	 * PLUS guarantees that it will call this method *before* calling generateSurrogate.
	 * <p>If none of this makes any sense, you don't have to do anything; the default here is to always return true.
	 * <p>If this method returns false, PLUS will <b>not</b> call generateSurrogate() until such time as 
	 * isRunnable() has been called again.
	 * <p>PLUS makes no promise about the amount of time between calling this function and when it will
	 * generate a surrogate.
	 * @param object the object for which PLUS will generate a surrogate.
	 * @return true if the SGF can currently generate a surrogate, false otherwise.
	 */
	public boolean isRunnable(PLUSObject object) { 
		return true;
	}
	
	public String toString() { 
		return new String("SGF: " + this.getClass().getCanonicalName()); 
	}
	
	/**
	 * @return always returns the fully-qualified name of the class, e.g. org.mitre.provenance.surrogate.BlahBlah
	 */
	public String getName() { 
		return getClass().getName();
	}
} // End SurrogateGeneratingFunction
