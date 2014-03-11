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
package org.mitre.provenance.simulate.motif;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.dag.LineageDAG;
import org.mitre.provenance.dag.TraversalSettings;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.surrogate.SurrogateGeneratingFunction;
import org.mitre.provenance.user.PrivilegeClass;
import org.mitre.provenance.user.PrivilegeSet;

/**
 * <p>A motif is a general "shape" of a lineage DAG.  It should embody the general shape without
 * getting too fancy.  Necessary for doing various research related tests on different shapes 
 * of DAGs.
 * 
 * <p>This base class does all of the house-keeping.  Extensions of this class actually specify the
 * shapes that are studied.
 * @author DMALLEN
 */
public class Motif extends ProvenanceCollection {
	protected String wfName; 
	protected PLUSWorkflow workflow; 
	protected HashMap <String,String> standardMetadata;
	protected SurrogateGeneratingFunction sgf;
	protected PrivilegeSet ps = null;
	
	public Motif() { 
		this("Motif Workflow"); 
	} // End Motif
	
	public Motif(String wfType) { 
		this(wfType, null); 
	}
	
	/**
	 * Create a new motif, defined by a given type; associate the given SGF with all objects in the motif.
	 * @param wfType a descriptive string naming the motif
	 * @param sgf the SGF to apply.
	 */
	public Motif(String wfType, SurrogateGeneratingFunction sgf) {
		this.sgf = sgf;
		ps = new PrivilegeSet();
		
		standardMetadata = new HashMap<String,String> (); 
		standardMetadata.put("implus:motif", "true"); 
		wfName = wfType + " " + (new Date()).toString(); 
		
		workflow = new PLUSWorkflow();
		
		workflow.setName(wfName);
		workflow.setWhenStart((new Date()).toString());
		workflow.setWhenEnd((new Date()).toString());
		add(workflow); 
	} // End Motif

	/**
	 * Associate the given PrivilegeSet with all objects in the motif.
	 * @param ps
	 */
	public void setPrivilegeSet(PrivilegeSet ps) { 
		this.ps = ps;
		
		for(PLUSObject o : getNodes()) {
			o.setPrivileges(ps);
		}		
	}
	
	/**
	 * Apply the provided SGF to all objects in the motif.
	 * @param sgf
	 */
	public void setSGF(SurrogateGeneratingFunction sgf) { 
		this.sgf = sgf;
		
		for(PLUSObject o : getNodes()) { 
			if(!o.getSGFs().contains(sgf.getClass().getName())) {
				o.getPrivileges().addPrivilege(new PrivilegeClass(5)); 
				o.useSurrogateComputation(sgf);
			} // End if
		} // End for
	} // End setSGF
		
	protected void init() { 
		;
	} // End init

	protected void addStandardMetadata(String name, String value) { 
		standardMetadata.put(name, value); 
	} // End addStandardMetadata
	
	public PLUSWorkflow getWorkflow() { return workflow; }
	
	protected void add(PLUSObject o) {
		for(String field : standardMetadata.keySet()) {
			if(!o.getMetadata().containsKey(field)) o.getMetadata().put(field, standardMetadata.get(field)); 
		}
				
		if(sgf != null && !o.getSGFs().contains(sgf)) o.useSurrogateComputation(sgf); 		
		addNode(o); 
	} // End add
	
	public LineageDAG getDAG(org.mitre.provenance.user.User user) throws PLUSException {
		PLUSObject startingPoint = null;
		
		for(PLUSObject o : getNodes()) { 
			if(!o.isWorkflow()) { 
				startingPoint = o;
				break;
			}
		}
				
		if(startingPoint == null) throw new PLUSException("Cannot find suitable graph starting point.");
		
		return Neo4JPLUSObjectFactory.newDAG(startingPoint.getId(), user, TraversalSettings.UNLIMITED);
	} // End getDAG
	
	public static void main(String [] args) throws Exception { 
		List <Motif> motifs = new ArrayList <Motif> ();
		
		motifs.add(new Diamond()); 
		motifs.add(new Lattice()); 
		motifs.add(new Chain()); 
		motifs.add(new Bottleneck()); 
		motifs.add(new Bipartite()); 
		motifs.add(new InvertedTree()); 
		motifs.add(new Tree()); 
		
		for(Motif m : motifs) {
			System.out.println("Writing " + m.getWorkflow().getName()); 
			Neo4JStorage.store(m);
		} // End for
		
		System.out.println("Done"); 
	} // End main
} // End Motif
