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
import org.mitre.provenance.plusobject.PLUSURL;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.surrogate.sgf.GenericSGF;
import org.mitre.provenance.user.PrivilegeClass;
import org.mitre.provenance.user.PrivilegeSet;

/**
 * Sample NGA human-in-the-loop analysis workflow, created to
 * support Chris Basel, July 2011
 * @author dmallen
 */
public class AnalysisWorkflow extends ProvenanceCollection {
	public AnalysisWorkflow() throws PLUSException { 
		super();

		PLUSWorkflow aw = new PLUSWorkflow();
		aw.setName("Analysis Workflow");
		aw.setWhenStart(new Date().toString());
		aw.setWhenEnd(new Date().toString()); 
                addNode(aw);
                
		PrivilegeSet TS = new PrivilegeSet();
		TS.addPrivilege(new PrivilegeClass(10)); 
		
		PrivilegeSet SEC = new PrivilegeSet();
		SEC.addPrivilege(new PrivilegeClass(5)); 
		
		PrivilegeSet PUBLIC = new PrivilegeSet();
		PUBLIC.addPrivilege(PrivilegeClass.PUBLIC);

                PLUSActor air = Neo4JPLUSObjectFactory.getActor("Air Force (Authoritative Source)", true); 
                PLUSActor ic = Neo4JPLUSObjectFactory.getActor("Navy", true); 
                PLUSActor washco = Neo4JPLUSObjectFactory.getActor("Washington Post Company", true); 
                PLUSActor ihs = Neo4JPLUSObjectFactory.getActor("IHS (Authoritative Source)", true); 
                PLUSActor tr = Neo4JPLUSObjectFactory.getActor("Thomas Reuters", true);
                PLUSActor forgov = Neo4JPLUSObjectFactory.getActor("Pakistan", true); 
                PLUSActor nyo = Neo4JPLUSObjectFactory.getActor("New York Times Company", true); 
                PLUSActor no = Neo4JPLUSObjectFactory.getActor("Intel Agency", true); 
                                
		PLUSString usRefDB = new PLUSString("US Reference Database 1");
		usRefDB.setPrivileges(SEC);
                usRefDB.setOwner(air);
                GregorianCalendar cref1 = new GregorianCalendar();
                cref1.set(2011, 6, 1);
                usRefDB.setCreated(cref1.getTimeInMillis());
		usRefDB.getMetadata().put("Source", "Authoritative");
                usRefDB.getMetadata().put("KPS Notes", "Source authoritative for airstrip information. - KPS");
                usRefDB.getMetadata().put("JNH Notes", "Shoddy coverage in subsahara Africa. See  - JNH"); 

                PLUSURL janes = new PLUSURL("Jane's Defence", "http://jdw.janes.com");
		janes.setPrivileges(PUBLIC);
                janes.setOwner(ihs);
                GregorianCalendar cjane = new GregorianCalendar();
                cjane.set(2011, 6, 16);
                janes.setCreated(cjane.getTimeInMillis());
                janes.getMetadata().put("URL", "http://jdw.janes.com"); 
		
		PLUSString personal = new PLUSString("Personal Communication", "(Contents hidden)");
		personal.setPrivileges(TS);
                GregorianCalendar cper = new GregorianCalendar();
                cper.set(2011, 7, 5);
                personal.setCreated(cper.getTimeInMillis());
                personal.getMetadata().put("AboutSource", "I trust this source for demographic information, not political information. - Joe"); 
                
		PLUSString reuters = new PLUSString("Reuters");
		reuters.setPrivileges(PUBLIC);
                GregorianCalendar creu = new GregorianCalendar();
                creu.set(2011, 7, 6, 11, 33);
                reuters.setCreated(creu.getTimeInMillis());
                reuters.setOwner(tr);
		
		PLUSURL nyTimes = new PLUSURL("New York Times Web", "http://web.nytimes.com/");
		nyTimes.setPrivileges(PUBLIC);
                GregorianCalendar cnyt = new GregorianCalendar();
                cnyt.set(2011, 7, 6, 14, 15);
                nyTimes.setCreated(cnyt.getTimeInMillis());
                nyTimes.setOwner(nyo);
 		
		addNode(reuters);
		addNode(nyTimes);
		addNode(usRefDB);
		addNode(janes);
		addNode(personal);
		
		addEdge(new PLUSEdge(reuters, nyTimes, aw)); 
		
		PLUSInvocation hilp1 = new PLUSInvocation("Human Process");
		hilp1.setPrivileges(TS); 
		hilp1.setWorkflow(aw);
                GregorianCalendar chil = new GregorianCalendar();
                chil.set(2011, 7, 6, 16, 00);
                hilp1.setCreated(chil.getTimeInMillis());
                hilp1.getMetadata().put("Analyst", "Joe");
                hilp1.setOwner(no);
                hilp1.getMetadata().put("Specialty", "WMDs"); 
                addNode(hilp1);
		
		addEdge(new PLUSEdge(nyTimes, hilp1, aw)); 
		addEdge(new PLUSEdge(usRefDB, hilp1, aw));
		addEdge(new PLUSEdge(janes, hilp1, aw)); 
		addEdge(new PLUSEdge(personal, hilp1, aw)); 
		
		PLUSString analysisProduct1 = new PLUSString("Analysis Product");
		analysisProduct1.setPrivileges(TS);
                analysisProduct1.setOwner(no);
                GregorianCalendar can1 = new GregorianCalendar();
                can1.set(2011, 7, 7, 16, 00);
                analysisProduct1.setCreated(can1.getTimeInMillis());
		addNode(analysisProduct1);
		
		addEdge(new PLUSEdge(hilp1, analysisProduct1, aw)); 
		
		PLUSURL washPo = new PLUSURL("Washington Post", "http://web.wapo.com/");
		washPo.setPrivileges(PUBLIC);
                GregorianCalendar cwas = new GregorianCalendar();
                cwas.set(2011, 7, 7, 17, 14);
                washPo.setCreated(cwas.getTimeInMillis());
                washPo.setOwner(washco);

                
		PLUSString foreignIntel = new PLUSString("Foreign Intel Report", "(Contents hidden)");
		foreignIntel.setPrivileges(TS);
		foreignIntel.setOwner(forgov);
                GregorianCalendar cfg = new GregorianCalendar();
                cfg.set(2011, 7, 4);                
                foreignIntel.setCreated(cfg.getTimeInMillis());
                foreignIntel.getMetadata().put("About Source", "This comes from a high level, friendly military official. - See Martha for details");

                
		PLUSString usRefDB2 = new PLUSString("US Reference Database 2");
		usRefDB2.setPrivileges(SEC);
                usRefDB2.setOwner(ic);
                GregorianCalendar cref2 = new GregorianCalendar();
                cref2.set(2011, 6, 15);
                usRefDB2.setCreated(cref2.getTimeInMillis());
		usRefDB2.getMetadata().put("Source", "Authoritative");
                usRefDB2.getMetadata().put("AC Notes", "Source authoritative for government structure. - AC");
                usRefDB2.getMetadata().put("RZ Notes", "Excellent source, especially for the Middle East  - RZ"); 

		
		PLUSInvocation hilp2 = new PLUSInvocation("Human Process");
		hilp2.setPrivileges(TS);
		hilp2.setWorkflow(aw);
                hilp2.setOwner(no);
                GregorianCalendar chp2 = new GregorianCalendar();
                chp2.set(2011, 7, 8, 9, 1);
                hilp2.setCreated(chp2.getTimeInMillis());
                hilp2.getMetadata().put("Analyst", "Sarah");
                hilp2.getMetadata().put("Specialty", "Middle East"); 
                
		addNode(washPo);
		addNode(foreignIntel);
		addNode(usRefDB2);
		addNode(hilp2);
		
		addEdge(new PLUSEdge(reuters, washPo, aw)); 
		addEdge(new PLUSEdge(analysisProduct1, hilp2, aw)); 
		addEdge(new PLUSEdge(usRefDB2, hilp2, aw)); 
		addEdge(new PLUSEdge(foreignIntel, hilp2, aw)); 
		addEdge(new PLUSEdge(washPo, hilp2, aw)); 
		
		PLUSString revised = new PLUSString("Revised Analysis Product", "(Contents hidden)");
		revised.setPrivileges(TS);
                revised.setOwner(no);
                GregorianCalendar crev = new GregorianCalendar();
                crev.set(2011, 7, 10, 16, 45);
                revised.setCreated(crev.getTimeInMillis());
                revised.getMetadata().put("Timestamp", "2370"); 
		addNode(revised);
		addEdge(new PLUSEdge(hilp2, revised, aw)); 
		
		PLUSInvocation downgrade = new PLUSInvocation("Review and Downgrade");
		downgrade.setPrivileges(TS);
                downgrade.setOwner(no);
                GregorianCalendar cdon = new GregorianCalendar();
                cdon.set(2011, 7, 9, 16, 45);
                downgrade.setCreated(cdon.getTimeInMillis());
		downgrade.setWorkflow(aw);
                
		PLUSString sanitized = new PLUSString("Sanitized Analysis Product", "(Contents hidden)");
		sanitized.setPrivileges(SEC);
                sanitized.setOwner(no);
                GregorianCalendar csan = new GregorianCalendar();
                csan.set(2011, 7, 9, 20, 45);
                sanitized.setCreated(csan.getTimeInMillis());
		addNode(downgrade);
		addNode(sanitized);
		
		addEdge(new PLUSEdge(analysisProduct1, downgrade, aw)); 
		addEdge(new PLUSEdge(downgrade, sanitized, aw)); 
		
		for(PLUSObject ob : getNodes()) { 
			ob.useSurrogateComputation(new GenericSGF());
		}
	} // End AnalysisWorkflow
	
	protected void finalize() { 
		
	} // End finalize
} // End AnalysisWorkflow
