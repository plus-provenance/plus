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
package org.mitre.provenance.dag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSObject;

/**
 * A DAGPath is a specific thread running through an existing LineageDAG.  Because LineageDAG encapsulates all
 * of the complexity of edge voting and surrogates, DAGPaths can't exist independent of a particular DAG.
 * @author DMALLEN
 */
public class DAGPath implements Cloneable {
	protected List <PathStep> steps;
	protected LineageDAG sourceDAG;
	protected PLUSObject head;
	protected PLUSObject tail;
	boolean directed = true;

	public DAGPath(LineageDAG dag, PLUSObject from, PLUSObject to, Collection<PathStep>steps) { 
		this.sourceDAG = dag;
		this.head = from;
		this.tail = to;
		this.steps = new ArrayList<PathStep>(steps);
		if(steps == null) this.steps = new ArrayList<PathStep>();
		directed = false;
	} // End DAGPath
	
	/**
	 * Find a new DAGPath from one point to another within a particular dag.  This will use depth-first search to locate the
	 * path for you.  No guarantee that this is the shortest or best path in the DAG.
	 * @param dag the source DAG to use for finding the path.
	 * @param from the starting point.
	 * @param to the ending point
	 * @throws PLUSException if the path does not exist in the DAG
	 */
	public DAGPath(LineageDAG dag, PLUSObject from, PLUSObject to) throws PLUSException {		
		this.sourceDAG = dag;
		this.head = from;
		this.tail = to; 		
		steps = findPath(head, tail);
	} // End DAGPath

	/** Return true if the path contains a given edge, false otherwise */
	public boolean contains(PLUSEdge edge) {
		String f = edge.getFrom().getId();
		String t = edge.getTo().getId();
		
		for(PathStep ps : steps) {
			if(ps.getOutboundEdge().getFrom().equals(f) && 
			   ps.getOutboundEdge().getTo().equals(t)) return true; 
		}
		
		return false;
	}
	
	public List<PathStep> getSteps() { return steps; }	
	
	/** Return the item at the head of the path */
	public PLUSObject getHead() { return head; } 
	
	/** Return the item at the tail of the path */
	public PLUSObject getTail() { return tail; } 
	
	public DAGPath clone() { 
		ArrayList<PathStep>ns = new ArrayList<PathStep>();
		for(PathStep ps : steps) { ns.add(ps.clone()); }
		return new DAGPath(sourceDAG, head, tail, ns);
	}
	
	/**
	 * Add a path step to the path.  This method checks for coherence - the step must be contiguous with the last step, otherwise
	 * an exception will result.
	 * @param step a new step to add to the chain.
	 */
	public void addStep(PathStep step) { 
		if(steps.size() > 0) 
			assert(step.getNode().getId().equals(steps.get(steps.size()-1).getOutboundEdge().getTo()));
		else assert step.getNode().getId().equals(head.getId());
		
		steps.add(step); 		
		tail = sourceDAG.getNode(step.getOutboundEdge().getTo().getId());  
	} // End addStep	
	
	public boolean exists() { 
		return steps != null && steps.size() > 0; 
	}
	
	public int getLength() { 
	    if (steps == null)
		return 0;
	    return steps.size(); 
	} 
	
	/** 
	 * Does deep comparison of a path to make sure that one path is equal to another.
	 */
	public boolean equals(Object o) {
		if(!(o instanceof DAGPath)) return false;
		DAGPath other = (DAGPath)o;
		
		try { 
			if(!sourceDAG.getId().equals(other.sourceDAG.getId())) return false;
			if(!head.getId().equals(other.head.getId())) return false;
			if(!tail.getId().equals(other.tail.getId())) return false;
			if(steps.size() != other.steps.size()) return false;
			
			for(int x=0; x<steps.size(); x++) { 
				PathStep ps = steps.get(x);
				PathStep os = other.steps.get(x);
				
				if(!ps.node.getId().equals(os.node.getId())) return false;
				if(!ps.outboundEdge.getTo().equals(os.outboundEdge.getTo())) return false;
			}
		} catch(NullPointerException exc) { 
			exc.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public boolean contains(String oid) { 
		for(int x=0; x<steps.size(); x++) { 
			if(steps.get(x).getNode().getId().equals(oid)) return true;
		}
		
		return false;
	} // End contains

	/**
	 * Determine whether the DAGPath is directed.  Directed paths will have a sequence of steps that respect edge ordering,
	 * and the source will be strictly before the target in the graph.  
	 * @return true if the path is directed, false otherwise.
	 */
	public boolean isDirected() { 
		return directed;
	}
	
	public PathStep getLastStep() throws PLUSException { 
		return getStep(steps.size()-1); 
	}
	
	public PathStep getStep(int idx) throws PLUSException { 
		if(idx < 0 || idx >= getLength()) throw new PLUSException("Step index number out of range: " + idx);
		return steps.get(idx); 
	}
	
	public boolean contains(PLUSObject obj) { 
		return contains(obj.getId()); 
	}
	
	private List <PathStep> findPath(PLUSObject from, PLUSObject to) throws PLUSException { 		
		if(sourceDAG.getNode(from.getId()) == null) throw new PLUSException("DAGPath: Node " + from.getName() + " missing from DAG");		
		if(sourceDAG.getNode(to.getId()) == null) throw new PLUSException("DAGPath: Node " + to.getName() + " missing from DAG"); 

		List <PathStep> result = new ArrayList <PathStep> ();
		List <PLUSEdge> edges = sourceDAG.getOutboundEdgesByNode(from.getId());
		
		for(PLUSEdge e : edges) { 
			if(e.getTo().equals(to.getId())) {
				result.add(new PathStep(from, e));
				result.add(new PathStep(to, null));
				return result;
			} // End if
		} // End for
		
		for(PLUSEdge e : edges) {  
			PLUSObject nextNode = sourceDAG.getNode(e.getTo().getId());
			if(nextNode == null) continue; 
			
			List <PathStep> recursed = findPath(nextNode, to);
			if(recursed == null || recursed.size() == 0) continue;
			else { 
				for(PathStep ps : recursed) result.add(ps);  
				return result;
			}
		}
		
		return null;
	} // End findPath
	
	public String toString() { 
		StringBuffer buf = new StringBuffer("");
		
		if(!exists()) return "(No such path)"; 
		
		for(PathStep ps : steps) { 
			buf.append(ps.getNode().getName() + " => ");
		} // End for
		
		buf.append(tail.getName()); 
		
		buf.append(" Length " + steps.size()); 
		
		return buf.toString(); 
	} // End toString()
	
	/** 
	 * Determine whether or not a path exists between two nodes in a LineageDAG
	 * @param dag the dag to check
	 * @param from the starting node
	 * @param to the ending node
	 * @return true if a path exists from the starting node to the ending node under the specified DAG.  False otherwise.
	 */
	public static boolean pathExists(LineageDAG dag, PLUSObject from, PLUSObject to) { 
		try { 
			DAGPath path = new DAGPath(dag, from, to);
			return path.getLength() > 0; 
		} catch(Exception e) { 
			e.printStackTrace();
		}
		
		return false;
	} // End pathExists	
} // End 
