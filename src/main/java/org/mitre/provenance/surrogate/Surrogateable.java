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

import java.util.ArrayList;
import java.util.List;

import java.util.logging.Logger;

/**
 * Represents and object that can either be a surrogate, or have surrogates generated for it.
 * A surrogate is an alternate version of a provenance object, usually with less information, that is
 * releasable to someone with fewer permissions than are required to see the full object.
 * @author moxious
 */
public abstract class Surrogateable {
	protected static Logger log = Logger.getLogger(Surrogateable.class.getName());
	/** Stores whether or not this item is a surrogate */
	protected SurrogateDetail detail = null;	
	
	/** Stores the list of classes that can be used to compute a surrogate for this object. */
	protected List <String> sgfs;
	
	public Surrogateable() { 		
		detail = null;
		sgfs = new ArrayList <String> (); 
	}
	
	/**
	 * Use this method to determine whether a particular SGF is attached to this class.
	 * @param computationClass the fully qualified class name of the SGF you want to check for.
	 * @return true if this class has that SGF, false otherwise.
	 */
	public boolean hasSurrogateComputation(String computationClass) { 
		int idx = sgfs.indexOf(computationClass);
		return idx != -1;
	}
	
	public boolean hasSurrogateComputationClass(Class<?> c) { 
		int idx = sgfs.indexOf(c.getName());
		return idx != -1;
	}	
	
	/**
	 * Use this method to "attach" a surrogate computation function to a data item.
	 * When surrogates are needed, this computation function will be used.  It should 
	 * be an instance of class SurrogateGeneratingFunction.
	 * <p>More than one SGF can be used for a class, so call this as many times as you want, but
	 * with different computation classes.
	 * @see org.mitre.provenance.surrogate.SurrogateGeneratingFunction
	 * @param sgf the function to use to calculate.  
	 */	
	public void useSurrogateComputation(SurrogateGeneratingFunction sgf) {				
		if(sgf == null) { 			
			log.severe("Cannot useSurrogateComputation null!");
			return;
		}

		/*
		 * In the past, we used to take a class as an argument here, and this logic was here
		 * to support that.  Because generics aren't reified in java, this created problems in 
		 * some environments.  The argument to this method was Class<? extends SurrogateGeneratingFunction>
		 * but when the caller specified something like MySGF.class as the argument, at compile time it's the
		 * right class, but at runtime it would appear to be java.lang.Class, which effectively  meant the
		 * class information we needed got destroyed.
		 * 
		 * So now we just take an SGF instance as an argument, and get its class information as necessary.
		 * We're keeping this logic around for now in case we find a way to go back to the old approach.
		 * 
		// Check to make sure that computationClass adheres to the required
		// interface of SurrogateGeneratingFunction.  The user has to specify
		// a function that meets that interface, otherwise we won't be able to use it.
		Class<? extends SurrogateGeneratingFunction> cls = org.mitre.provenance.surrogate.SurrogateGeneratingFunction.class;		
		//if(computationClass.getSuperclass() != cls) {
		if(!cls.isAssignableFrom(computationClass)) { 
			log.warning("****** WARNING (" + this.getClass().getName() +" / " + this.toString() + "*********");
			log.warning("useSurrogateComputation invalid class type: " + computationClass.getCanonicalName());			
			log.warning("Cannot be determined to be of type " + cls);
			return;
		}  // End if
		
		try { sgfs.add((Class<? extends SurrogateGeneratingFunction>)computationClass); } 
		catch(Exception e) { 
			log.severe("useSurrogateComputation: Something went horribly wrong: " + e.getMessage());
		}
		*/
		
		sgfs.add(sgf.getClass().getName());
	} // End useSurrogateComputation
	
	/**
	 * True if the item has surrogates, false otherwise.
	 */
	public boolean hasSurrogates() { return sgfs.size() > 0; } 
	
	/**
	 * Get a list of surrogate computation functions used for building surrogate accounts of this object.
	 * @return a list of Class objects that can be used to compute a surrogate of this function.
	 */
	public List <String> getSGFs() {
		if(sgfs == null) sgfs = new ArrayList <String>(); 
		return sgfs; 
	} // End getSGFs() 
	
	/**
	 * Set the group of SGFs used to compute surrogates.
	 * @param sgfs a list of Classes
	 */
	protected void setSGFs(List <String> sgfs) { 
		if(sgfs == null) 
			this.sgfs = new ArrayList <String> (); 
		else this.sgfs = sgfs; 
	} 
	
	/**
	 * Set the surrogate detail information for this item.  By doing this, you automatically mark this
	 * item as a surrogate. 
	 * @param detail the detail for this surrogate.
	 */
	public void setSurrogateDetail(SurrogateDetail detail) { this.detail = detail; } 
		
	/**
	 * Get surrogate details attached to this object. 
	 * @return a SurrogateDetail object, or null if this object is not a surrogate.
	 */
	public SurrogateDetail getSurrogateDetail() { return detail; } 
	
	/**
	 * Find out whether this object is a surrogate or not.
	 * @return true if the item is a surrogate, false otherwise.
	 */
	public boolean isSurrogate() { return detail != null; }  
} // End Surrogateable
