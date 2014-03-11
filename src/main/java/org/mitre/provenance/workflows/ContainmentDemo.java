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

import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSGeneric;
import org.mitre.provenance.plusobject.PLUSInvocation;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;

public class ContainmentDemo extends ProvenanceCollection {
	protected PLUSWorkflow wf = null;
	
	public ContainmentDemo() { 
		wf = genWorkflow("Containment Demo"); 
		addNode(wf);
		generate();
	}
	
	protected PLUSWorkflow genWorkflow(String name) { 
		PLUSWorkflow w = new PLUSWorkflow();
		w.setName(name);
		w.setWhenStart((new Date()).toString()); 
		w.setWhenEnd((new Date()).toString()); 
		return w;
	}
	
	public PLUSWorkflow generateFib(int n) {
		PLUSWorkflow w = genWorkflow("Fibonacci " + n);
		addNode(w); 
			
		if(n < 2) { 
			PLUSString s = new PLUSString(""+n, ""+n); 
			PLUSInvocation ret = new PLUSInvocation();
			ret.setName("Return");
			ret.setWorkflow(w); 
			
			addNode(w);
			addNode(s);
			addNode(ret);
			addEdge(new PLUSEdge(s, ret, w, PLUSEdge.EDGE_TYPE_TRIGGERED));			
		} else { 
			PLUSInvocation add = new PLUSInvocation();
			
			PLUSWorkflow recurse1 = generateFib(n-1);
			PLUSWorkflow recurse2 = generateFib(n-2); 
						
			add.setName("Add"); 
			PLUSString result = new PLUSString("Result");
			
			addNode(add);
			addNode(result);
			addEdge(new PLUSEdge(recurse1, add, w, PLUSEdge.EDGE_TYPE_TRIGGERED));
			addEdge(new PLUSEdge(recurse2, add, w, PLUSEdge.EDGE_TYPE_TRIGGERED)); 
			addEdge(new PLUSEdge(add, result, w, PLUSEdge.EDGE_TYPE_TRIGGERED));
		}
		
		return w;
	}
	
	public void generate() { 
		PLUSGeneric start = new PLUSGeneric("START");
		addNode(start);
				
		PLUSWorkflow fib = generateFib(5); 		
		addEdge(new PLUSEdge(start, fib, wf, PLUSEdge.EDGE_TYPE_TRIGGERED)); 
	}
	
	protected void finalize() { ; } 
}
