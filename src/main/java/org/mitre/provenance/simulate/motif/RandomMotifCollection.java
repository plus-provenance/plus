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
package org.mitre.provenance.simulate.motif;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.simulate.SyntheticGraph;
import org.mitre.provenance.simulate.SyntheticGraphProperties;

/**
 * This class mocks up a synthetic provenance DAG by assembling a random collection of various motifs.
 * @author DMALLEN
 */
public class RandomMotifCollection extends SyntheticGraph {	
	protected PLUSWorkflow wf = null;
	protected Class<?> [] motifs = new Class [] { 
		Tree.class, InvertedTree.class, Chain.class,
		Bipartite.class, Lattice.class, Diamond.class,
		Bottleneck.class
	}; 
	
	public RandomMotifCollection(SyntheticGraphProperties props) throws PLUSException { 
		super(props);
		
		wf = new PLUSWorkflow();
		wf.setName(props.getName());
		
		addNode(wf);
		
		if(props.getComponents() < 2) throw new PLUSException("Must have minimum of 2 components!");
		
		init();
	}
	
	public PLUSWorkflow getWorkflow() { return wf; } 
	
	/**
	 * Creates a series of random motifs, and then links them.
	 * The linkage is done by selecting a random node in one motif and linking it to a random node in the
	 * next downstream motif.
	 */
	protected void init() { 
		Motif [] list = new Motif [props.getComponents()];
		
		// Choose "count" random motifs, and put them into list[].
		for(int x=0; x<props.getComponents(); x++) { 
			try { 
				list[x] = chooseRandomMotif();
				if(props.getSGF() != null) list[x].setSGF(props.getSGF());
				list[x].setPrivilegeSet(props.getPrivilegeSet());
			} catch(Exception e) { 
				e.printStackTrace(); 
			}
		}				
				
		// Go through each motif in the list, and link it to eCount other motifs.
		// This makes the entire graph connected.   Notice that we can only add an
		// edge from something at list[x] to list[x+1] -- this ensures that we 
		// don't accidentally introduce cycles into the graph.
		// By connecting each component to at least 1 downstream, we know that all
		// components are interlinked (reachable to one another)
		for(int x=0; x<props.getComponents()-1; x++) { 
			// Pick a number between 1 and half the number of motifs we have.
			int eCount = (int)(Math.random() * 100 % (props.getComponents()/2));
			if(eCount <= 0) eCount *= -1; 
			if(eCount == 0) eCount = 1; 
						
			for(int y=0; y<eCount; y++) { 
				addEdge(new PLUSEdge(randomNode(list[x]), 
						             randomNode(list[x+1]), 
						             getWorkflow())); 						         
			} // End for
		} // End for				
		
		for(int x=0; x<props.getComponents(); x++) {
			// Add items from the underlying motif to this collection.
			addAll(list[x]);
		} // End for
	} // End init
	
	/**
	 * Select a random node from a given Motif.
	 * @param m the motif
	 * @return a random node within it
	 */
	protected PLUSObject randomNode(Motif m) {		
		while(true) { 
			int i = (int)(Math.random() * m.getNodes().size() * 1000 % m.getNodes().size());
			if(i < 0) i *= -1; 
			// System.out.println("Random node: chose " + i + " of " + m.getNodes().values().size());
			
			Object [] arr = m.getNodes().toArray();
			PLUSObject o = (PLUSObject)arr[i]; 
			if(!o.isWorkflow()) return o;   // Don't pick workflows.
		} // End while
	} // End randomNode
	
	/**
	 * Select a random motif style, and return a new instance of it.
	 * @return
	 * @throws Exception
	 */
	protected Motif chooseRandomMotif() throws Exception { 
		int i = (int)(Math.random() * (motifs.length*1000) % motifs.length);
		if(i < 0) i *= -1; 
		
		// System.out.println("Choosing to add a new " + motifs[i].getSimpleName());
		
		return (Motif)motifs[i].newInstance(); 
	} // End chooseRandomMotif
	
	public static void main(String [] args) throws Exception {
		SyntheticGraphProperties p = new SyntheticGraphProperties().setComponents(200).setConnectivity(0.55);
		RandomMotifCollection rmc = new RandomMotifCollection(p);
		
		System.out.println("Storing: " + rmc);
		new LocalProvenanceClient().report(rmc);
		System.out.println("Done.");
	}
} // End RandomMotifCollection
