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

import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSObject;

/**
 * A single step movement through a LineageDAG.  You could think of a LineageDAG as a pile of these, 
 * or a DAGPath as a list of sequential PathSteps.  
 * @author DMALLEN
 */
public class PathStep implements Cloneable {
	/** The node from which the step was taken */
	public PLUSObject node;
	/** The edge followed out of that node */
	public PLUSEdge outboundEdge;
	
	public PathStep(PLUSObject node, PLUSEdge outboundEdge) { 
		this.node = node;
		this.outboundEdge = outboundEdge;
	}
	
	public PathStep clone() { 
		PathStep p = new PathStep(node, outboundEdge);
		return p;
	}
	
	public PLUSObject getNode() { return node; } 
	public PLUSEdge getOutboundEdge() { return outboundEdge; }
	
	public String toString() { 
		return new String("[PathStep " + node.getName() + " -> " + outboundEdge.getTo() + "]"); 
	}
} // End PathStep
