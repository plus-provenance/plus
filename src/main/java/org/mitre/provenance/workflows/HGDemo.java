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
 * support Chris Basel, Jan 2012 
 * @author AGE
 */
public class HGDemo extends ProvenanceCollection {
	public HGDemo() throws Exception { 
		super();

		PLUSWorkflow aw = new PLUSWorkflow();
		aw.setName("HG Demo Workflow");
		aw.setWhenStart(new Date().toString());
		aw.setWhenEnd(new Date().toString()); 
        addNode(aw);

                //Privledge Sets
		PrivilegeSet TS = new PrivilegeSet();
		TS.addPrivilege(new PrivilegeClass(10)); 
                PrivilegeSet SEC = new PrivilegeSet();
		SEC.addPrivilege(new PrivilegeClass(5)); 
                PrivilegeSet PUBLIC = new PrivilegeSet();
		PUBLIC.addPrivilege(PrivilegeClass.PUBLIC);

                
                //Actors
		PLUSActor cia = Neo4JPLUSObjectFactory.getActor("CIA", true);
		PLUSActor DOD = Neo4JPLUSObjectFactory.getActor("DOD", true);
               PLUSActor hgnga = Neo4JPLUSObjectFactory.getActor("HG NGA", true);
                PLUSActor state = Neo4JPLUSObjectFactory.getActor("State Department", true); 
                PLUSActor rand = Neo4JPLUSObjectFactory.getActor("National Defense Research Institute (RAND)", true); 
                PLUSActor tr = Neo4JPLUSObjectFactory.getActor("Thomas Reuters", true);
                PLUSActor forgov = Neo4JPLUSObjectFactory.getActor("Yemen", true); 
                PLUSActor im = Neo4JPLUSObjectFactory.getActor("Index Mundi", true); 
                
                //Data and Invocations
                PLUSString ciafact = new PLUSString("CIA World Factbook");
		ciafact.setPrivileges(PUBLIC);
                ciafact.setOwner(cia);
                GregorianCalendar cal  = new GregorianCalendar();
                cal.set(2011, 6, 1);
                // ciafact.setCreated(cal.getTimeInMillis());
		ciafact.getMetadata().put("Source", "Tier 1");
                addNode(ciafact);
                
                PLUSInvocation createWebpage = new PLUSInvocation("Create Web Page");
		createWebpage.setPrivileges(PUBLIC);
		createWebpage.setWorkflow(aw);
                createWebpage.setOwner(im);
                cal.set(2011, 6, 2);
                // createWebpage.setCreated(cal.getTimeInMillis());
                addNode(createWebpage);
                
                PLUSString pepa = new PLUSString("Petrolium Exploration and Production Authority");
		pepa.setPrivileges(PUBLIC);
                pepa.setOwner(forgov);
                cal.set(2011, 7, 5);
                // pepa.setCreated(cal.getTimeInMillis());
                pepa.getMetadata().put("Source", "Foreign");
                pepa.getMetadata().put("AboutSource", "I trust this source for demographic information, not political information. - Joe"); 
                addNode(pepa);

                PLUSURL pepaweb = new PLUSURL("Index Mundi Yemen Oil Production", "http://www.indexmundi.com");
		pepaweb.setPrivileges(PUBLIC);
                pepaweb.setOwner(im);
                cal.set(2011, 6, 16);
                // pepaweb.setCreated(cal.getTimeInMillis());
                pepaweb.getMetadata().put("URL", "http://indexmundi.com"); 
                addNode(pepaweb);

                PLUSInvocation updateWebpage = new PLUSInvocation("Update Web Page");
		updateWebpage.setPrivileges(PUBLIC);
		updateWebpage.setWorkflow(aw);
                updateWebpage.setOwner(im);
                cal.set(2011, 7, 5);
                // updateWebpage.setCreated(cal.getTimeInMillis());
                addNode(updateWebpage);
                
                PLUSURL pepaweb2 = new PLUSURL("Index Mundi Yemen Oil Production", "http://www.indexmundi.com");
                pepaweb2.setPrivileges(PUBLIC);
                pepaweb2.setOwner(im);
                cal.set(2011, 6, 16);
                // pepaweb2.setCreated(cal.getTimeInMillis());
                pepaweb2.getMetadata().put("URL", "http://indexmundi.com"); 
                addNode(pepaweb2);

                PLUSInvocation registerSource = new PLUSInvocation("Register Source");
		registerSource.setPrivileges(SEC);
		registerSource.setWorkflow(aw);
                registerSource.setOwner(hgnga);
                cal.set(2011, 7, 6);
                // registerSource.setCreated(cal.getTimeInMillis());
                addNode(registerSource);

                PLUSInvocation annotateSource = new PLUSInvocation("AnnotationService");
		annotateSource.setPrivileges(SEC);
		annotateSource.setWorkflow(aw);
                annotateSource.setOwner(hgnga);
                cal.set(2011, 7, 6);
                // annotateSource.setCreated(cal.getTimeInMillis());
                addNode(annotateSource);

                PLUSInvocation geocodeSource = new PLUSInvocation("Geo Tag Service");
		geocodeSource.setPrivileges(SEC);
		geocodeSource.setWorkflow(aw);
                geocodeSource.setOwner(hgnga);
                cal.set(2011, 7, 6);
                // geocodeSource.setCreated(cal.getTimeInMillis());
                addNode(geocodeSource);
                
                addEdge(new PLUSEdge(ciafact, createWebpage, aw));
                addEdge(new PLUSEdge(createWebpage, pepaweb, aw));
                addEdge(new PLUSEdge(pepaweb, updateWebpage,aw));
                addEdge(new PLUSEdge(pepa, updateWebpage,aw));
                addEdge(new PLUSEdge(updateWebpage, pepaweb2, aw));
                addEdge(new PLUSEdge(pepaweb2, registerSource, aw));
                addEdge(new PLUSEdge(registerSource, annotateSource, aw));
                addEdge(new PLUSEdge(annotateSource, geocodeSource, aw)); 

                
                PLUSString randdoc = new PLUSString("Regime and Periphery in Northern Yemen");
                randdoc.setPrivileges(PUBLIC);
                randdoc.setOwner(rand);
                cal.set(2011, 6, 1);
                // randdoc.setCreated(cal.getTimeInMillis());
                addNode(randdoc);

                PLUSInvocation registerSource2 = new PLUSInvocation("Register Source");
		registerSource2.setPrivileges(SEC);
		registerSource2.setWorkflow(aw);
                registerSource2.setOwner(hgnga);
                cal.set(2011, 7, 6);
                // registerSource2.setCreated(cal.getTimeInMillis());
                addNode(registerSource2);

                PLUSInvocation annotateSource2 = new PLUSInvocation("AnnotationService");
		annotateSource2.setPrivileges(SEC);
		annotateSource2.setWorkflow(aw);
                annotateSource2.setOwner(hgnga);
                cal.set(2011, 7, 6);
                // annotateSource2.setCreated(cal.getTimeInMillis());
                addNode(annotateSource2);

                PLUSInvocation geocodeSource2 = new PLUSInvocation("Geo Tag Service");
		geocodeSource2.setPrivileges(SEC);
		geocodeSource2.setWorkflow(aw);
                geocodeSource2.setOwner(hgnga);
                cal.set(2011, 7, 6);
                // geocodeSource2.setCreated(cal.getTimeInMillis());
                addNode(geocodeSource2);
                

                addEdge(new PLUSEdge(randdoc, registerSource2, aw));
                addEdge(new PLUSEdge(registerSource2, annotateSource2, aw));
                addEdge(new PLUSEdge(annotateSource2, geocodeSource2, aw)); 

                PLUSInvocation registerSource4 = new PLUSInvocation("Register Source");
                registerSource4.setPrivileges(SEC);
		registerSource4.setWorkflow(aw);
                registerSource4.setOwner(hgnga);
                cal.set(3011, 7, 6);
                // registerSource4.setCreated(cal.getTimeInMillis());
                addNode(registerSource4);

                addEdge(new PLUSEdge(ciafact, registerSource4, aw));

                
                PLUSURL reuters = new PLUSURL("Chaos in Yemen", "http://www.reuters.com");
		reuters.setPrivileges(PUBLIC);
                reuters.setOwner(tr);
                cal.set(2011, 6, 17);
                // reuters.setCreated(cal.getTimeInMillis());
                reuters.getMetadata().put("URL", "http://reuters.com"); 
                addNode(reuters);

                
                PLUSInvocation registerSource3 = new PLUSInvocation("Register Source");
		registerSource3.setPrivileges(SEC);
		registerSource3.setWorkflow(aw);
                registerSource3.setOwner(hgnga);
                cal.set(3011, 7, 6);
                // registerSource3.setCreated(cal.getTimeInMillis());
                addNode(registerSource3);

                PLUSInvocation annotateSource3 = new PLUSInvocation("AnnotationService");
		annotateSource3.setPrivileges(SEC);
		annotateSource3.setWorkflow(aw);
                annotateSource3.setOwner(hgnga);
                cal.set(3011, 7, 6);
                // annotateSource3.setCreated(cal.getTimeInMillis());
                addNode(annotateSource3);

                PLUSInvocation geocodeSource3 = new PLUSInvocation("Geo Tag Service");
		geocodeSource3.setPrivileges(SEC);
		geocodeSource3.setWorkflow(aw);
                geocodeSource3.setOwner(hgnga);
                cal.set(3011, 7, 6);
                // geocodeSource3.setCreated(cal.getTimeInMillis());
                addNode(geocodeSource3);

                
                addEdge(new PLUSEdge(reuters, registerSource3, aw));
                addEdge(new PLUSEdge(registerSource3, annotateSource3, aw));
                addEdge(new PLUSEdge(annotateSource3, geocodeSource3, aw)); 


                PLUSString prodoc = new PLUSString("Events related to the pipeline bombing");
                prodoc.setPrivileges(SEC);
                prodoc.setOwner(hgnga);
                cal.set(2011, 7, 8);
                // prodoc.setCreated(cal.getTimeInMillis());
                addNode(prodoc);


                addEdge(new PLUSEdge(geocodeSource2, prodoc, aw));
                addEdge(new PLUSEdge(geocodeSource3, prodoc, aw));
                addEdge(new PLUSEdge(registerSource4, prodoc, aw)); 

                PLUSString dodrep = new PLUSString("Supply Line Management in Yemen");
                dodrep.setPrivileges(SEC);
                dodrep.setOwner(DOD);
                cal.set(2011, 7, 13);
                // dodrep.setCreated(cal.getTimeInMillis());
                addNode(dodrep);
                
                PLUSString staterep = new PLUSString("Warning to all State Department Employees");
                staterep.setPrivileges(SEC);
                staterep.setOwner(state);
                cal.set(2011, 7, 13);
                // staterep.setCreated(cal.getTimeInMillis());
                addNode(staterep);

                addEdge(new PLUSEdge( prodoc, dodrep, aw));
                addEdge(new PLUSEdge( prodoc, staterep, aw)); 
                
                
		for(PLUSObject ob : getNodes()) { 
			ob.useSurrogateComputation(new GenericSGF());
		}
	} // End HGDemo
	
	protected void finalize() { 
		
	} // End finalize
	
} // End HGDemo
