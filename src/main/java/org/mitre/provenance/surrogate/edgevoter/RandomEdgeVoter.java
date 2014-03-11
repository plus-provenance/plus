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
package org.mitre.provenance.surrogate.edgevoter;

import org.mitre.provenance.EdgeMarking;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.plusobject.*;
import org.mitre.provenance.surrogate.EdgeVoter;

import java.util.Random;

/**
 * An edge voter that can be used for test purposes and in experimental code.  It votes at random,
 * or according to assigned probabilities for votes given in the constructor.
 * @author DMALLEN
 */
public class RandomEdgeVoter implements EdgeVoter {
	protected double visible = 0.33;
	protected double hidden = 0.33; 
	protected double inferred = 0.33; 
	
	public static Random randGen = new Random(); 
	
	public RandomEdgeVoter(double pctVisible, double pctHidden, double pctInferred) throws PLUSException { 
		visible = pctVisible;
		hidden = pctHidden;
		inferred = pctInferred;
		
		double tot = visible + hidden + inferred; 
		if(tot != (double)1) throw new PLUSException("percentages must total 1!"); 		
	} // End RandomEdgeVoter
	
	public EdgeMarking getMarking(PLUSObject other) { 
		double rand = randGen.nextDouble(); 
		
		EdgeMarking r = null;
		
		if(rand <= hidden) r = EdgeMarking.HIDE;
		else if(rand <= (hidden+visible)) r = EdgeMarking.SHOW;
		else r = EdgeMarking.INFER;
		
		//PLUSUtils.log.info("V/H/I: " + visible + ", " + hidden + ", " + inferred + " RAND=" + rand + " verdict " + r); 
		return r;
	} // End getMarking
} // End RandomEdgeVoter
