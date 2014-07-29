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
package org.mitre.provenance.test;

import org.junit.Before;
import org.junit.Test;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.client.AbstractProvenanceClient;
import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.client.ProvenanceClient;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.plusobject.json.JSONConverter;
import org.mitre.provenance.plusobject.json.ProvenanceCollectionDeserializer;
import org.mitre.provenance.simulate.SyntheticGraphProperties;
import org.mitre.provenance.simulate.motif.RandomMotifCollection;
import org.mitre.provenance.surrogate.SurrogateGeneratingFunction;
import org.mitre.provenance.surrogate.sgf.GenericSGF;
import org.mitre.provenance.surrogate.sgf.RandomInferMarker;
import org.mitre.provenance.test.util.TestUtils;
import org.mitre.provenance.user.PrivilegeClass;
import org.mitre.provenance.user.PrivilegeSet;
import org.mitre.provenance.user.User;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TestSerializeDeserialize {
	AbstractProvenanceClient client = new LocalProvenanceClient();
	
    @Before
    public void setUp() {
        ProvenanceClient.instance = new LocalProvenanceClient();
    }
	
    @Test
    public void testJSON() throws PLUSException { 
    	PrivilegeSet ps = new PrivilegeSet();
    	ps.addPrivilege(PrivilegeClass.PRIVATE_MEDICAL);
    	
    	SyntheticGraphProperties p = new SyntheticGraphProperties()
    	 		.setComponents(250)
    	 		.setConnectivity(0.5)
    	 		.setName("Test set for JSON Serialization")
    	 		.setPrivilegeSet(ps);
    	RandomMotifCollection r = new RandomMotifCollection(p);
    	
    	String json = JSONConverter.provenanceCollectionToD3Json(r);
		Gson g = new GsonBuilder().registerTypeAdapter(ProvenanceCollection.class, new ProvenanceCollectionDeserializer()).create();
		ProvenanceCollection col = g.fromJson(json, ProvenanceCollection.class);
		
		System.out.println("Took collection " + r + " round-tripped through JSON to " + col);
		
		TestUtils.equivalent(r, col);    	
    }
    
	@Test
	public void testPrivilegeSetsAndSGFs() throws PLUSException {
		PLUSString s = new PLUSString("Foo", "Bar");
				
		SurrogateGeneratingFunction [] sgfs = 
				new SurrogateGeneratingFunction[] { new GenericSGF(), new RandomInferMarker() };
		
		s.getPrivileges().addPrivilege(PrivilegeClass.ADMIN);
		s.getPrivileges().addPrivilege(PrivilegeClass.NATIONAL_SECURITY);
		
		for(int x=0; x<sgfs.length; x++) {
			SurrogateGeneratingFunction c = sgfs[x];
			System.out.println("Using SGF " + c.getClass().getName());
			s.useSurrogateComputation(c);
		}
				
		client.report(ProvenanceCollection.collect(s));
		
		PLUSString o = (PLUSString)Neo4JPLUSObjectFactory.load(s.getId(), User.DEFAULT_USER_GOD);
		
		TestUtils.equivalent(s, o);		
	}
	
	public static void foo(Class<? extends SurrogateGeneratingFunction> c) {
		System.out.println(c.getName());
	}
	
	public static void main(String [] args) throws Exception {
		Class <? extends SurrogateGeneratingFunction> c = GenericSGF.class;
		foo(c);
	}
}
