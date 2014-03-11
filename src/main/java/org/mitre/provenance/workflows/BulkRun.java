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

import java.util.ArrayList;
import java.util.List;

import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.simulate.SyntheticGraphProperties;
import org.mitre.provenance.simulate.motif.RandomMotifCollection;
import org.mitre.provenance.surrogate.sgf.SurgicalInferAll;
import org.mitre.provenance.user.PrivilegeClass;
import org.mitre.provenance.user.PrivilegeSet;

/**
 * A quick utility to populate a provenance database with some test cases.
 * @author DMALLEN
 */
public class BulkRun {
	public BulkRun() { 
		
	}
	
	/**
	 * This method takes some time to run.  It's intended to populate the database with a sample of basic provenance workflows.
	 * As a result, it takes as much as 30 seconds while it runs them, and writes the contents to the database.
	 * @throws Exception
	 */
	public void run() throws Exception { 
		List<ProvenanceCollection> cols = new ArrayList<ProvenanceCollection>();
		
		PrivilegeSet ps = new PrivilegeSet();
		ps.addPrivilege(PrivilegeClass.PUBLIC);
		RandomMotifCollection rmc = new RandomMotifCollection(
				new SyntheticGraphProperties().setComponents(20).setSGF(new SurgicalInferAll()).setPrivilegeSet(ps));
		cols.add(rmc);
		
		HGDemo hgd = new HGDemo();
		cols.add(hgd);
		
		EvilCycle c =  new EvilCycle();
		cols.add(c);
		
		ImpliedShaper is = new ImpliedShaper();
		cols.add(is);
		
		IAWorkflow ia = new IAWorkflow();  ia.finalize(); 
		cols.add(ia);
		
		try { 
			TBMCSWorkflow t = new TBMCSWorkflow();
			cols.add(t.getCollection());
		} catch(Exception exc) { 
			exc.printStackTrace();
		}
		
		AMPPDemo a = new AMPPDemo(); a.finalize();
		cols.add(a); 
		
		AnalysisWorkflow af =  new AnalysisWorkflow();  af.finalize();
		cols.add(af);
		
		ContainmentDemo cd = new ContainmentDemo(); cd.finalize();
		cols.add(cd);
		
		SimpleWorkflow sf = new SimpleWorkflow();
		cols.add(sf);
				
		for(ProvenanceCollection col : cols) { 
			Neo4JStorage.store(col);			
		}		
	}
	
	public static void main(String [] args) throws Exception {
		new BulkRun().run();
	}
}
