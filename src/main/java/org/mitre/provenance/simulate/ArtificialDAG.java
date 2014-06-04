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

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.client.AbstractProvenanceClient;
import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.dag.LineageDAG;
import org.mitre.provenance.dag.TraversalSettings;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.surrogate.SurrogateGeneratingFunction;
import org.mitre.provenance.user.PrivilegeClass;

public class ArtificialDAG {
	DAGComponent [] comps; 
	int componentCount;
	float connectivity;
	PLUSWorkflow wf; 
	
	public static final float RANDOMIZE_SGF_ASSIGNMENT = (float)0.15;
	
	public static final int MAX_OUTPUTS_PER_COMPONENT = 5; 
	public static final int CONFIGURABLE_DEPTH = 3; 
	public static Random randGen = new Random();
	
	public SurrogateGeneratingFunction SGF = null;
	
	public AbstractProvenanceClient client = new LocalProvenanceClient();
	
	public ArtificialDAG(PLUSWorkflow wf, int components, float connectivity, SurrogateGeneratingFunction SGF) throws Exception { 
		componentCount = components;
		this.connectivity = connectivity;
		this.wf = wf; 
		this.SGF = SGF;
		generateComponents();
		connectComponents(); 
	} // End AritificialDAG
	
	private void connectComponents() throws Exception { 
		ArrayList <PLUSEdge> edges = new ArrayList <PLUSEdge> (); 
		
		for(int x=0; x<componentCount; x++) { 
			for(int y=0; y<comps[x].outputs.length; y++) { 
				DAGComponent c = comps[x];
				PLUSObject obj = c.outputs[y]; 
				
				if(x < (componentCount-1) && randGen.nextDouble() < connectivity) { 
					int choice = -1;
					while(choice <= 0 || (x+choice) >= componentCount) 
						choice = randGen.nextInt() % CONFIGURABLE_DEPTH;
					
					DAGComponent other = comps[x+choice];
					other.invocation.addInput(obj.getName(), obj.getId());
					
					System.out.println("CONNECTIVITY: C" + x + "/" + y + " => C" + (x+choice));  
							          
					PLUSEdge e = new PLUSEdge(obj, other.invocation, wf, "used by");
					edges.add(e); 				
				}
			} // End for			
		}
		
		// Guarantee full connectivity.
		for(int x=1; x<componentCount; x++) { 
			if(comps[x].invocation.getInputCount() <= 0) {
				int other = randGen.nextInt();
				if(other < 0) other *= -1;
				other = other % x; 
				
				DAGComponent connectMeTo = comps[other];
				
				int rint = randGen.nextInt() % connectMeTo.outputs.length;
				if(rint < 0) rint *= -1; 
				PLUSObject robj = connectMeTo.outputs[rint];
				if(SGF != null) robj.useSurrogateComputation(SGF);
				
				comps[x].invocation.addInput(robj.getName(), robj.getId());
				
				System.out.println("FULL CONNECTIVITY: C" + other + "/" + rint + " => C" + x); 
				PLUSEdge e = new PLUSEdge(robj, comps[x].invocation,  
						                  wf, "used by");
				edges.add(e); 				
			} // End if
		} // End for
		
		System.out.println("Writing invocations and edges..."); 
		for(int x=0; x<componentCount; x++) client.report(comps[x]);
		//for(int x=0; x<edges.size(); x++) client.report(ProvenanceCollection.collect(edges.get(x))); 		
		client.report(ProvenanceCollection.collect(edges.toArray(new PLUSEdge[]{})));
		System.out.println("Done!"); 
	} // End connectComponents
	
	public PLUSObject getEntryPoint() { 
		return comps[0].invocation; 
	}
	
	private void generateComponents() throws Exception { 
		comps = new DAGComponent[componentCount];
		
		int aCount = (int)Math.sqrt((double)componentCount);
		PLUSActor [] actors = new PLUSActor [aCount];
		for(int x=0; x<aCount; x++) { 
			actors[x] = new PLUSActor("Group " + (x+1));
			Neo4JStorage.store(actors[x]);			
		} // End for
		
		for(int x=0; x<componentCount; x++) {
			int oCount = randGen.nextInt() % MAX_OUTPUTS_PER_COMPONENT;
			if(oCount < 0) oCount *= -1; 
			oCount++;   // So it can't be 0.

			int r = randGen.nextInt();
			if(r < 0) r *= -1; 
			r = r % aCount;
			PLUSActor owner = actors[r];			
			comps[x] = new DAGComponent(wf, "Component " + x, 
					owner, 
					oCount, SGF, RANDOMIZE_SGF_ASSIGNMENT);			
		} // End for
	} // End generateComponents

	/*********************************************************************************/
	
	private static ArrayList <String> results = new ArrayList <String> (); 
	
	public static final void runLiftExperiment(int nodes, float connectivity, SurrogateGeneratingFunction SGF) throws Exception { 
		PLUSWorkflow wf = new PLUSWorkflow();
		wf.setName("Artificial DAG: " + new Date());
		wf.setWhenStart(new Date().toString()); 
		wf.setWhenEnd(new Date().toString());
		Neo4JStorage.store(wf);
		
		ArtificialDAG ad = new ArtificialDAG(wf, nodes, connectivity, SGF);
		
		Neo4JPLUSObjectFactory.newDAG(ad.getEntryPoint().getId(), org.mitre.provenance.user.User.DEFAULT_USER_GOD, TraversalSettings.UNLIMITED);		
	} // End runLiftExperiment
	
	public static final ArtificialDAG runTest(int nodes, float connectivity, SurrogateGeneratingFunction SGF, boolean surrogateTimings) throws Exception {
		PLUSWorkflow wf = new PLUSWorkflow();
		wf.setName("Artificial DAG: " + new Date());
		wf.setWhenStart(new Date().toString()); 
		wf.setWhenEnd(new Date().toString());
		
		wf.getPrivileges().addPrivilege(new PrivilegeClass(10));
		Neo4JStorage.store(wf);
		
		ArtificialDAG ad = new ArtificialDAG(wf, nodes, connectivity, SGF);
		LineageDAG dag = null;
		
		org.mitre.provenance.user.User u = null;
		
		if(!surrogateTimings) 
			u = org.mitre.provenance.user.User.DEFAULT_USER_GOD;
		else 
			u = org.mitre.provenance.user.User.PUBLIC;
					
		dag = Neo4JPLUSObjectFactory.newDAG(ad.getEntryPoint().getId(), u, TraversalSettings.UNLIMITED);		
		
		Metadata md = dag.getMetadata();

		results.add((surrogateTimings ? "Surrogates" : "Base") + "," + 
				dag.countNodes() + "," + 
				dag.countEdges() + "," + 
				nodes + "," + connectivity + "," + SGF + "," +
				md.get("BuildDAG") + "," + 
				md.get("EdgeVoting") + "," + 
				md.get("NewEdgeComputing") + "," + 
				md.get("AddComputedEdges") + "," + 
				md.get("inferredEdges") + "," + 
				md.get("Votes-Infer") + "," + 
				md.get("Votes-Hide") + "," + 
				md.get("Votes-Show") + "," + 
				md.get("EstDBAccessTime") + "," + 
				md.get("PrunedDisconnectedNodes"));
				
		System.out.println("Finished.");	
		return ad;
	} // End runTest
	
	public static void main(String [] args) throws Exception {
		int components = 0;
		float connectivity = 0;
		SurrogateGeneratingFunction SGF = null;
		
		try { 
			components = Integer.parseInt(args[0]);
			connectivity = Float.parseFloat(args[1]);
			
			SGF = null;
			
			if(args.length > 2)
				SGF = (SurrogateGeneratingFunction)Class.forName(args[2]).newInstance(); 
		} catch(Exception e) { 
			//PLUSUtils.log.error("Exception: " + e);
			//e.printStackTrace(); 
			//PLUSUtils.log.error("Usage: foo components connectivity SGF"); 
			//System.exit(0);
			components = 50;
			connectivity = (float)0.50;
		}
		
		int runXTimes = 1;
		
		try {
			for(int x=0; x<runXTimes; x++) {
				boolean val = false;
				if(SGF != null) val = true;
				runTest(components, connectivity, SGF, val);
			}
		} catch(Exception e) { 
			System.out.println("Exception: " + e);
			e.printStackTrace(); 
		} 
		
		System.out.println("Type,Nodes,Edges,Components,Connectivity,SGF,BuildDAG," + 
				"EdgeVoting,NewEdgeComputing,AddComputedEdges,inferredEdges,Votes-Infer," + 
				"Votes-Hide,Votes-Show,EstDBAccessTime,PrunedDisconnectedNodes"); 
		for(int x=0; x<results.size(); x++) { 
			System.out.println(results.get(x)); 
		}
		
		System.exit(0); 
	} // End main
} // End ArtificialDAG

