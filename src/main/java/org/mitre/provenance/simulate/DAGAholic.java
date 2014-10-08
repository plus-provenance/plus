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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSInvocation;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.PLUSWorkflow;

/**
 * Generate a random DAG.   This is the "DAGAholic" method of creating random graphs. Note this object is a 
 * ProvenanceCollection, and so can be easily stored/serialized/reported as such.
 * 
 * <p>One annoying implementation detail: we actually have to keep a second copy of nodes and edges; the superclass
 * tends to store nodes/edges in a map, but we need to be able to refer to them in an ordered list.  The super-class doesn't
 * support that, so we need to implement that for this class.
 * 
 * @author moxious
 */
public class DAGAholic extends SyntheticGraph {
	protected static Logger log = Logger.getLogger(DAGAholic.class.getName());
	
	/** The nodes in the graph. */
	protected List <PLUSObject> nodeList;
	
	/** The edges in the graph */
	protected List <PLUSEdge> edgeList;
	
	protected Map<String,PLUSEdge>edgeHash = new HashMap<String,PLUSEdge> ();
	
	/** The workflow that links everything */
	protected PLUSWorkflow wf = null;
	
	public static final int SET_SIZE = 100; 
	public static final int SET_NODE_COUNT = 200; 
	
	public DAGAholic() throws PLUSException {
		this(new SyntheticGraphProperties());
		generate();
	}
	
	/**
	 * Create a new DAGAholic generator
	 * @param wfName the name of the workflow to generate
	 * @param nodes the number of nodes the final graph should contain
	 * @param pctChance the percentage changes (between 0.0 and 1.0) that any two nodes will be connected.
	 * @param pctData this percentage of nodes will be data; the remainder will be invocations.
	 * @param sgf the SGF to associate with all of the objects in the graph.
	 * @param protect the number of objects to protect in the graph (randomly chosen)
	 * @throws Exception
	 */
	public DAGAholic(SyntheticGraphProperties props) throws PLUSException {
		super(props); 
		
		nodeList = new ArrayList <PLUSObject>();
		edgeList = new ArrayList <PLUSEdge> ();
		
		generate();
	} // End DAGAholic
	
	/**
	 * Actually generates the contents of the DAGAholic instance.
	 * @throws PLUSException
	 */
	protected void generate() throws PLUSException {
		wf = new PLUSWorkflow();
		wf.setName(props.getName()); 
		wf.setWhenStart(new Date().toString());
		wf.setWhenEnd(new Date().toString());
		
		addNode(wf);

		// Create a number of actors according to the square root of the number of nodes. 
		int aCount = (int)Math.sqrt(new Double(props.getComponents()));
		PLUSActor [] actors = new PLUSActor [aCount];
		for(int x=0; x<aCount; x++) { 
			actors[x] = new PLUSActor("Group Owner " + (x+1));
			addActor(actors[x]); 			
		} // End for

		for(int x=0; x<props.getComponents(); x++) {
			PLUSObject node = null;
	
			// Create nodes as either data or invocation, depending on a random roll, 
			// and a stated desired percentage of data/invocation.
			if(rand.nextDouble() <= props.getPercentageData()) {
				node = new PLUSString("Node " + (x+1));// + " with " + pctChance + " connectivity.");
				((PLUSString)node).setContent("The quick brown fox jumps over the lazy dog");
			} else { 
				node = new PLUSInvocation("Node " + (x+1)); 
				((PLUSInvocation)node).setWorkflow(wf);
			}
			
			if(props.getSGF() != null) node.useSurrogateComputation(props.getSGF());
			
			int r = Math.abs(rand.nextInt());			
			r = r % aCount;
			
			// Randomly assign the rth actor to this node.
			node.setOwner(actors[r]);

			// We need these nodes in an ordered list for later random access.
			nodeList.add(node);
			
			addNode(node);
		} // End for

		int curNode = 0;
		
		while(true) { 			
			int next = curNode + 1;

			// Don't refer to something past the end of the array.
			if(next >= props.getComponents()) break;  

			PLUSEdge edge = new PLUSEdge(nodeList.get(curNode), nodeList.get(next), wf);
			edgeList.add(edge); 
			
			addEdge(edge); 
			edgeHash.put(edge.toString(), edge); 

			double effectiveChance = props.getConnectivity();

			// Effective chance needs to be scaled back for things earlier in the graph.
			float pctThrough = (float)curNode / (float)props.getComponents();
			effectiveChance = (props.getConnectivity() * pctThrough); 
			if(effectiveChance <= 0) effectiveChance=(float)0.01;

			int connectionsMade = 0;
			while(rand.nextDouble() <= effectiveChance) { 
				next++;

				if(next >= props.getComponents()) break;
				//if(next > (curNode + (nodes * pctChance))) break;

				connectionsMade++;
				edge = new PLUSEdge(nodeList.get(curNode), nodeList.get(next), wf); 
				edgeList.add(edge); 
				edgeHash.put(edge.toString(), edge); 
				
				addEdge(edge);
			} // End while

			// log.info("connectivity=" + props.getConnectivity() + ", effective=" + effectiveChance + " on node " + curNode + " => " + connectionsMade + " additional connections."); 

			curNode = next; 
		} // End while

		// Randomly mark protectN nodes for surrogate magic later on.
		HashSet<Integer> protectedIdxs = new HashSet<Integer>();
		for(int x=0; x<props.getProtectN(); x++) {
			int randNodeIdx = 0;
			
			do {
				// Keep picking a random index until we're sure it wasn't one that
				// we already marked as protected.
				randNodeIdx = rand.nextInt(props.getComponents());
			} while(protectedIdxs.contains(randNodeIdx));		
			
			nodeList.get(randNodeIdx).setPrivileges(props.getPrivilegeSet());
			protectedIdxs.add(randNodeIdx);
		}

		// log.info("finished randomprotect");

		for(int x=(props.getComponents()-1); x>0; x--) { 
			int futilityIndex = 0; 
			while(rand.nextDouble() <= props.getConnectivity()) {
				int tries = 0;
				boolean added = false;
				futilityIndex++; 
				while(!added) {
					tries++;
					
					if(tries >= 10) {
						// log.info("Adding backwards edges isn't working.  I quit."); 
						break; 
					}
					
					// Keep looking for a valid edge to add until it's there.
					int fromIdx = rand.nextInt(x); 
					PLUSEdge edge = new PLUSEdge(nodeList.get(fromIdx), nodeList.get(x), wf);

					if(!edgeHash.containsKey(edge.toString())) {
						edgeList.add(edge);
						edgeHash.put(edge.toString(), edge); 
						// System.out.println("Backwards: " + fromIdx + " to " + x);
						added = true; 
					} // End if
				} // End while

				if(!added && futilityIndex > 2) { 
					//System.out.println("Backwards futility limit reached."); 
					break;
				} 
			} // End while
		} // End for		
	} // End generate

	public static void main(String [] args) throws Exception {
		DAGAholic d = new DAGAholic();		
		System.out.println(d); 
		
		Neo4JStorage.store(d); 
	}
} // End DAGAholic
