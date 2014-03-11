package org.mitre.provenance.workflows;

import java.util.Date;

import org.mitre.provenance.plusobject.PLUSActivity;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSInvocation;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.surrogate.sgf.GenericSGF;
import org.mitre.provenance.user.PrivilegeClass;

public class ImpliedShaper extends ProvenanceCollection {
	protected PLUSWorkflow wf = null;
	
	public ImpliedShaper() {
		wf = new PLUSWorkflow();
		wf.setName("Implied Shaper");
		wf.setWhenStart((new Date()).toString());		
		
		addNode(wf);
		
		PLUSActivity generic = new PLUSActivity("Generic activity");
		generic.setInputs(10);
		generic.setOutputs(10); 
		
		PLUSInvocation [] invokes = new PLUSInvocation [12];
		
		for(int x=0; x<12; x++) { 
			PLUSInvocation i = new PLUSInvocation("" + (x+1));
			i.setActivity(generic);
			i.setWorkflow(wf);	
			invokes[x] = i; 
		} // End if
		
		addEdge(new PLUSEdge(invokes[0], invokes[3], wf));
		addEdge(new PLUSEdge(invokes[1], invokes[3], wf));
		addEdge(new PLUSEdge(invokes[1], invokes[4], wf));
		addEdge(new PLUSEdge(invokes[2], invokes[4], wf));		
		addEdge(new PLUSEdge(invokes[3], invokes[5], wf));
		addEdge(new PLUSEdge(invokes[4], invokes[5], wf)); 
		addEdge(new PLUSEdge(invokes[5], invokes[6], wf)); 
		addEdge(new PLUSEdge(invokes[6], invokes[7], wf)); 
		addEdge(new PLUSEdge(invokes[6], invokes[8], wf)); 
		addEdge(new PLUSEdge(invokes[7], invokes[9], wf)); 
		addEdge(new PLUSEdge(invokes[7], invokes[10], wf)); 
		addEdge(new PLUSEdge(invokes[8], invokes[10], wf)); 
		addEdge(new PLUSEdge(invokes[8], invokes[11], wf)); 
				
		System.out.println("Writing...");
		
		wf.setWhenEnd((new Date()).toString()); 		
		addNode(wf);
		addNode(generic);
		
		for(int x=0; x<12; x++) {
			invokes[x].useSurrogateComputation(new GenericSGF());
			invokes[x].getPrivileges().addPrivilege(PrivilegeClass.NATIONAL_SECURITY);
			addNode(invokes[x]);
		}
	}
}
