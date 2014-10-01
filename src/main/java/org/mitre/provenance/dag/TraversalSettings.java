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

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Controls the settings that the graph traverser uses to determine
 * what to return in response to a given query.
 * 
 * <p>This object provides relatively fine-grained control over what kind of
 * provenance graph to discover from the database.
 * 
 * <p>Creating default instances of this object will traverse the graph in both directions.
 * 
 * <P>Specific objects can be created idiomatically by using the chaining methods specified below, such as:
 * 
 * <pre>new TraversalSettings().onlyForward().setMaxDepth(1).excludeEdges().includeNodes();</pre>
 * 
 * <p>For example, that will create a TraversalSettings object that instructs the code to fetch all nodes forward of
 * a starting point, only one hop away, returning a set of nodes but not edges.
 * @author DMALLEN
 */
public class TraversalSettings {
	public static final TraversalSettings UNLIMITED = new TraversalSettings(-1, -1, true, true, true, true, true, true, true);
	public static final TraversalSettings UNLIMITED_PROVENANCE_ONLY = new TraversalSettings(-1, -1, true, true, true, false, false, true, true);
	
	/** The maximum number of nodes to return */
	public int n = 200;
	
	/** The maximum depth away from the starting point to visit.
	 * A value of less than or equal to zero means no limit. 
	 */
	public int maxDepth = 10;
	
	/** If true, breadth first will be used.  If false, depth first */
	public boolean breadthFirst = true; 
	
	/** Whether the result should include nodes */
	public boolean includeNodes = true;
	
	/** Whether the result should include edges */
	public boolean includeEdges = true;
	
	/** Whether the result should include non-provenance edges */
	public boolean includeNPEs = true;
	
	/** Whether or not NPIDs should be followed when 
	 * discovering new nodes in the graph.
	 */
	public boolean followNPIDs = true;
	
	/** Whether or not to traverse forward in the DAG */
	public boolean forward = true;
	
	/** Whether or not to traverse backwards in the DAG */
	public boolean backward = true;
	
	/** Whether or not to expand workflows. */
	public boolean expandWorkflows = true; 
	
	private TraversalSettings(int n, int maxDepth, boolean breadthFirst, boolean includeNodes, boolean includeEdges, 
			boolean includeNPEs, boolean followNPIDs, boolean forward, boolean backward) {
		this.n = n;
		this.maxDepth = maxDepth;
		this.breadthFirst = breadthFirst;
		this.includeNodes = includeNodes;
		this.includeEdges = includeEdges;
		this.includeNPEs = includeNPEs;
		this.followNPIDs = followNPIDs;
		this.forward = forward;
		this.backward = backward;
	}
	
	public TraversalSettings() {
		this(200, 10, true, true, true, true, true, true, true);
	}
	
	/** Traversal will not expand workflows (that is, will keep them individual nodes) */
	public TraversalSettings dontExpandWorkflows() { expandWorkflows = false; return this; } 
	
	/** Traversal will expand workflows (that is, expand their members and traverse them) */
	public TraversalSettings expandWorkflows() { expandWorkflows = true; return this; } 
	
	/** Traversal will not follow non-provenance edges/IDs */
	public TraversalSettings ignoreNPIDs() { followNPIDs = false; return this; }
	
	/** Traversal will follow non-provenance edges/IDs. */
	public TraversalSettings followNPIDs() { followNPIDs = true; return this; }
	
	/** Traversal will go both forwards and backwards (upstream/downstream) */
	public TraversalSettings bothWays() { forward = true; backward = true; return this; } 
	
	/** Set the maximum number of nodes to return */
	public TraversalSettings setN(int n) { this.n = n; return this; } 
	
	/** Traversal will go forward only */
	public TraversalSettings onlyForward() { forward = true; backward = false; return this; }
	
	/* Traversal will go backward only */
	public TraversalSettings onlyBackward() { forward = false; backward = true; return this; }
	
	/** Results will not include NonProvenanceEdge objects */
	public TraversalSettings excludeNPEs() { includeNPEs = false; return this; }
	
	/** Results will include NonProvenanceEdge objects as appropriate */
	public TraversalSettings includeNPEs() { includeNPEs = true; return this; }
	
	/** Results will not include edge objects */
	public TraversalSettings excludeEdges() { includeEdges = false; return this; }
	
	/** Results will include edge objects */
	public TraversalSettings includeEdges() { includeEdges = true; return this; }
	
	/** Results will exclude nodes */
	public TraversalSettings excludeNodes() { includeNodes = false; return this; }
	
	/** Results will include nodes */
	public TraversalSettings includeNodes() { includeNodes = true; return this; }
	
	/** Traversal will occur depth first */
	public TraversalSettings depthFirst() { breadthFirst = false; return this; }
	
	/** Traversal will occur breadth first */
	public TraversalSettings breadthFirst() { breadthFirst = true; return this; }
	
	/** Traversal will go to a maximum depth of the parameter specified */
	public TraversalSettings setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; return this; }
	
	/**
	 * Utility method to return the properties of this object as a map.
	 * @return
	 */
	public MultivaluedMap<String,String> asMultivaluedMap() {
		MultivaluedMap<String,String> m = new MultivaluedHashMap<String,String>();
		
		m.add("n", ""+n);
		m.add("maxDepth", ""+maxDepth);
		m.add("breadthFirst", ""+breadthFirst);
		m.add("includeNodes", ""+includeNodes);
		m.add("includeEdges", ""+includeEdges);
		m.add("includeNPEs", ""+includeNPEs);
		m.add("followNPIDs", ""+followNPIDs);
		m.add("forward", ""+forward);
		m.add("backward", ""+backward);
		
		return m;
	}
	
	public String toString() { 
		return (breadthFirst ? "Breadth first" : "Depth first") + ": " + 
	           n + " nodes to depth " + maxDepth + " including (" + 
			   (includeNodes ? "nodes, " : "") + 
			   (includeEdges ? "edges, " : "") + 
			   (includeNPEs ? "NPEs" : "") + 
			   ") forward=" + forward + " backward=" + backward + 
			   (followNPIDs ? "following NPIDs" : "");
	}
} // End TraversalSettings
