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
package org.mitre.provenance.simulate;

import java.util.Random;

import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSInvocation;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.surrogate.SurrogateGeneratingFunction;
import org.mitre.provenance.user.PrivilegeClass;

/**
 * Component of a DAG, consisting of a single invocation and a number of outputs.  Used
 * for building larger composite synthetic DAGs.
 * @author DMALLEN
 */
public class DAGComponent extends ProvenanceCollection {	
	public static Random randGen = new Random();
	
	public String basename;
	public PLUSInvocation invocation;
	public PLUSObject [] outputs;
	public SurrogateGeneratingFunction sgf;
	PLUSWorkflow wf;
	public float sgfPct;	
	
	public DAGComponent(PLUSWorkflow wf, String basename, PLUSActor owner,  
			            int outputCount, SurrogateGeneratingFunction sgf, float sgfPct) throws Exception {
		this.wf = wf; 
		this.sgf = sgf;
		this.basename = basename;
		addNode(wf);
		outputs = new PLUSObject [outputCount];
		this.sgfPct = sgfPct;		
		
		generate(owner);
		System.out.println("Created component with " + outputCount + " outputs."); 
	}
			
	private boolean shouldUseSurrogatesRandomly() { 
		if(sgfPct <= 0) return false;
		
		float guess = randGen.nextFloat();
		if(guess < 0) guess *= -1; 
		
		if(guess <= sgfPct) return true;
		return false;
	} // ENd shouldAssignPrivsRandomly
	
	private void generate(PLUSActor owner) throws Exception { 
		invocation = new PLUSInvocation(basename); 
		invocation.setOwner(owner); 
		invocation.setWorkflow(wf); 		
		
		invocation.getPrivileges().addPrivilege(new PrivilegeClass(10));
		
		if(shouldUseSurrogatesRandomly()) { 	
			if(sgf != null) invocation.useSurrogateComputation(sgf);
		} // End if
			
		addNode(invocation);
		
		for(int x=0; x<outputs.length; x++) { 
			outputs[x] = new PLUSString("O" + x + " of " + basename, 
						                "O" + x + " of " + basename);
			
			outputs[x].getPrivileges().addPrivilege(new PrivilegeClass(10));
			outputs[x].setOwner(owner); 
			if(shouldUseSurrogatesRandomly()) { 
				if(sgf != null) outputs[x].useSurrogateComputation(sgf);
			}
			
			Neo4JStorage.store(outputs[x]);			
			invocation.addOutput("" + x, outputs[x].getId());
			
			addNode(outputs[x]);
			PLUSEdge e = new PLUSEdge(invocation, outputs[x], wf, "generated");
			addEdge(e); 
		}		  
	} // End generate
} // End DAGComponent
