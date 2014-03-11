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
package org.mitre.provenance.workflows;

import java.util.Date;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSInvocation;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;

public class AMPPDemo extends ProvenanceCollection {
	protected int x = 1;
	protected PLUSWorkflow wf = null;
	
	public class Iteration { 
		public PLUSObject confusionMatrix = null;
		public PLUSObject fusedData = null;		
		public PLUSInvocation fusion = null;
	}
	
	public AMPPDemo() { 
		super();
		
		wf = new PLUSWorkflow();
		wf.setName("AMPP Demo"); 
		wf.setWhenStart((new Date()).toString()); 
		wf.setWhenEnd((new Date()).toString());
		
		addNode(wf);
	} // End AMPPDemo
	
	public int generate(String [] actors, int iterations) throws PLUSException {
		Iteration [][] its = new Iteration[iterations][actors.length];		
		
		for(int x=0; x<iterations; x++) { 
			for(int y=0; y<actors.length; y++) { 
				Iteration prev = new Iteration();				
				if((x-1) >= 0) prev = its[x-1][y];
				
				its[x][y] = createIteration(actors[y], prev.confusionMatrix, prev.fusedData);
				
				if((x-1) >= 0) flow(its[x-1][y], its[x][y]);  
			} // End for
			
		} // End for

		// From Barbara's slides.
		tie(its[0][2], its[0][0]); 
		tie(its[0][2], its[2][1]); 
		tie(its[0][0], its[4][1]);  
		tie(its[2][1], its[2][2]);
		tie(its[3][1], its[3][2]); 
		tie(its[4][1], its[4][2]); 
		
		return 0;
	}
	
	public void flow(Iteration one, Iteration two) {
		addEdge(new PLUSEdge(one.confusionMatrix, two.fusion, wf)); 
	}
	
	public void tie(Iteration one, Iteration two) throws PLUSException { 		
		PLUSInvocation transmit = new PLUSInvocation("Transmit Data"); 
		transmit.setOwner(one.confusionMatrix.getOwner());
		addNode(transmit);
		
		addEdge(new PLUSEdge(one.confusionMatrix, transmit, null));
		addEdge(new PLUSEdge(transmit, two.fusion, null)); 
	} // End tie
	
	public Iteration createIteration(String owner, PLUSObject confusionMatrix, PLUSObject fusedData) throws PLUSException { 
		PLUSActor a = Neo4JPLUSObjectFactory.getActor(owner, true);
				
		PLUSObject conf = confusionMatrix;
		PLUSObject fused = fusedData;
		
		if(conf == null) { conf = new PLUSString("Confusion Matrix " + (x++)); conf.setOwner(a);  }
		if(fused == null) { fused = new PLUSString("Fused Data " + (x++)); fused.setOwner(a); }  		
		
		PLUSObject obs = new PLUSString("Sensor Observation " + (x++)); obs.setOwner(a);		
		
		PLUSInvocation fusion = new PLUSInvocation("Fusion"); 
		fusion.setOwner(a); 
				
		if(!contains(conf)) addNode(conf);
		if(!contains(fused)) addNode(fused);
		
		addNode(fusion);
		addNode(obs);
		addEdge(new PLUSEdge(obs, fusion, wf)); 
		addEdge(new PLUSEdge(conf, fusion, wf));
		addEdge(new PLUSEdge(fused, fusion, wf)); 
		
		PLUSString newFused = new PLUSString("Fused Data " + (x++)); newFused.setOwner(a);
		PLUSString newConf = new PLUSString("Confusion Matrix " + (x++)); newConf.setOwner(a); 
		
		addNode(newFused);
		addNode(newConf);
		addEdge(new PLUSEdge(fusion, newFused, wf)); 
		addEdge(new PLUSEdge(fusion, newConf, wf)); 
		
		Iteration i = new Iteration();
		i.fusion = fusion;
		i.confusionMatrix = newConf;
		i.fusedData = newFused;
		
		return i;
	} // End createIteration
		
	protected void finalize() { ; }
}
