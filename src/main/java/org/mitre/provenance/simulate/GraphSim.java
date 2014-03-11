package org.mitre.provenance.simulate;

import java.util.HashSet;
import java.util.List;

import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSInvocation;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSString;

public class GraphSim extends SyntheticGraph {
	public GraphSim(SyntheticGraphProperties p) { 
		super(p);
		
		generateNodes();
		generateEdges();
		guaranteeConnectivity();
		protectAtRandom();
	}
	
	protected void protectAtRandom() {
		HashSet<String> prots = new HashSet<String>();
		List<PLUSObject> l = getNodesInOrderedList();
		
		for(int x=0; x<props.protectN; x++) { 
			String id = null;
			
			while(id == null || prots.contains(id)) { 
				id = l.get(Math.abs(rand.nextInt(l.size()))).getId();
			}
			
			prots.add(id);
			
			if(props.getSGF() != null) getNode(id).useSurrogateComputation(props.getSGF());
			if(props.getPrivilegeSet() != null)
				getNode(id).setPrivileges(props.getPrivilegeSet());
		}		
	}
	
	protected void guaranteeConnectivity() {
		List<PLUSObject> l = getNodesInOrderedList();
		
		for(int x=0; x<l.size()-1; x++) { 
			if(getOutboundEdgesByNode(l.get(x).getId()).size() == 0) {
				addEdge(new PLUSEdge(l.get(x), l.get(x+1)));
			}
		}
	}
	
	protected void generateNodes() { 
		for(int x=0; x<props.getComponents(); x++) {
			PLUSObject o = null;
			
			if(Math.abs(rand.nextDouble()) < props.pctData) { 
				o = new PLUSString("Node " + x);
			} else {
				o = new PLUSInvocation("Inv " + x);
			}
			
			try { Thread.sleep(2); }
			catch (InterruptedException e) { ; } 

			addNode(o);
		}
	}
	
	protected PLUSEdge makeRandomEdge(List<PLUSObject> l, int idx) {
		int otherIdx = -1;
		PLUSEdge e = null;
		
		int futility = 0;
		
		while(e == null || contains(e)) {
			if(idx > 0 && rand.nextBoolean()) {
				otherIdx = Math.abs(rand.nextInt(idx));
				e = new PLUSEdge(l.get(otherIdx), l.get(idx));
			} else if(idx < (l.size() - 1)) {
				otherIdx = idx + Math.abs(rand.nextInt(l.size() - idx));
				e = new PLUSEdge(l.get(idx), l.get(otherIdx));
			} 
			
			futility++;
			if(futility > 5) break;
		}
		
		addEdge(e); 
		
		return e;
	}
	
	protected void generateEdges() {
		List<PLUSObject> l = getNodesInOrderedList();
		
		for(int x=0; x<l.size(); x++) { 
			while(rand.nextDouble() <= props.getConnectivity()) {
				makeRandomEdge(l, x);
			}
		}
	} // End generateEdges
}
