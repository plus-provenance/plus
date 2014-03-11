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
import java.util.GregorianCalendar;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSInvocation;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.surrogate.sgf.GenericSGF;
import org.mitre.provenance.user.PrivilegeClass;
import org.mitre.provenance.user.PrivilegeSet;

/**
 * Sample NGA human-in-the-loop analysis workflow, created to
 * support Chris Basel, Jan 2012 
 * @author AGE
 */
public class IAWorkflow extends ProvenanceCollection {
	public IAWorkflow() throws PLUSException { 
		super();

		PLUSWorkflow aw = new PLUSWorkflow();
		aw.setName("IA Workflow");
		aw.setWhenStart(new Date().toString());
		aw.setWhenEnd(new Date().toString()); 
                addNode(aw);
                
		PrivilegeSet TS = new PrivilegeSet();
		TS.addPrivilege(new PrivilegeClass(10)); 
		
		PrivilegeSet SEC = new PrivilegeSet();
		SEC.addPrivilege(new PrivilegeClass(5)); 
		
		PrivilegeSet PUBLIC = new PrivilegeSet();
		PUBLIC.addPrivilege(PrivilegeClass.PUBLIC);


                PLUSActor DOD = Neo4JPLUSObjectFactory.getActor("DOD", true); 
                PLUSActor intel = Neo4JPLUSObjectFactory.getActor("Intel Community", true); 

                PLUSString waasFile1 = new PLUSString("Air Surveillance Data", "Air Surveillance Data");
		waasFile1.setOwner(DOD);
                GregorianCalendar cref1 = new GregorianCalendar();
                cref1.set(2011, 6, 1);
                waasFile1.setCreated(cref1.getTimeInMillis());
		waasFile1.getMetadata().put("Location", "Baghdad");
                waasFile1.getMetadata().put("Latitude", "+33.348026");
                waasFile1.getMetadata().put("Longitude", "+44.362584");

		PLUSInvocation palan1 = new PLUSInvocation("Palantir Analyzer");
		palan1.setPrivileges(TS); 
		palan1.setWorkflow(aw);
                GregorianCalendar chil = new GregorianCalendar();
                chil.set(2011, 7, 6, 16, 00);
                palan1.setCreated(chil.getTimeInMillis());
                palan1.getMetadata().put("Analyst", "Joe");
                palan1.setOwner(intel);
                palan1.getMetadata().put("Specialty", "WMDs"); 
                addNode(palan1);

		addNode(waasFile1);
		addEdge(new PLUSEdge(waasFile1, palan1, aw)); 

                PLUSString palModel = new PLUSString("Palantir Model", "Palantir Model");
		palModel.setOwner(intel); 
		
                addNode(palModel);
                addEdge(new PLUSEdge(palan1, palModel, aw)); 


                PLUSInvocation etling = new PLUSInvocation("ETL Tool");
		etling.setPrivileges(TS); 
		etling.setWorkflow(aw);
                GregorianCalendar chil2 = new GregorianCalendar();
                chil.set(2011, 7, 8, 16, 00);
                etling.setCreated(chil2.getTimeInMillis());
                etling.getMetadata().put("Analyst", "Joe");
                etling.setOwner(intel);
                etling.getMetadata().put("Specialty", "WMDs"); 
                addNode(etling);

                addNode(etling);
                addEdge(new PLUSEdge(palModel, etling, aw)); 
    

                PLUSString doka = new PLUSString("Person C");
		doka.setOwner(DOD); 
		PLUSString  fatik = new PLUSString("Person D");
		fatik.setOwner(DOD);
                addNode(doka);
		addNode(fatik);
		
                addEdge(new PLUSEdge(etling, doka, aw));
                addEdge(new PLUSEdge(etling, fatik, aw)); 
                
                PLUSString waasFile2 = new PLUSString("Air Surveillance Data 2", "Air Surveillance Data 2");
		waasFile2.setOwner(intel);
		PLUSString iedThreat = new PLUSString("IED-threat intelligence", "IED-threat intelligence");
		iedThreat.setOwner(intel); 

                addNode(waasFile2);
		

                PLUSInvocation meting = new PLUSInvocation("GeoTagger");
		meting.setPrivileges(TS); 
		meting.setWorkflow(aw);
                GregorianCalendar chil3 = new GregorianCalendar();
                chil3.set(2011, 7, 7, 16, 00);
                meting.setCreated(chil3.getTimeInMillis());
                meting.getMetadata().put("Analyst", "Joe");
                meting.setOwner(intel);
                meting.getMetadata().put("Specialty", "WMDs"); 
                addNode(meting);

                addNode(meting);
                //choose 1 of these, based on what you're trying to say in the script
                addEdge(new PLUSEdge(waasFile1, meting, aw)); 
                //addEdge(new PLUSEdge(waasFile2, meting, aw));
                

                
                PLUSString anisah = new PLUSString("Person A");
		anisah.setOwner(DOD); 
                PLUSString baahir = new PLUSString("Person B");
		baahir.setOwner(DOD); 		
		
		addNode(anisah);
		addNode(baahir);
		
                addEdge(new PLUSEdge(meting,anisah, aw)); 
                addEdge(new PLUSEdge(meting, baahir, aw)); 
                addEdge(new PLUSEdge(etling, anisah, aw)); 

                
		for(PLUSObject ob : getNodes()) { 
			ob.useSurrogateComputation(new GenericSGF());
		}
	} // End AnalystWorkflow
	
	protected void finalize() { 
		
	} // End finalize
} // End AnalystWorkflow
