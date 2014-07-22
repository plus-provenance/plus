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
import org.mitre.provenance.surrogate.SurrogateGeneratingFunction;
import org.mitre.provenance.surrogate.sgf.GenericSGF;
import org.mitre.provenance.surrogate.sgf.RandomInferMarker;
import org.mitre.provenance.user.PrivilegeClass;
import org.mitre.provenance.user.User;

public class TestSerializeDeserialize {
	AbstractProvenanceClient client = new LocalProvenanceClient();
	
    @Before
    public void setUp() {
        ProvenanceClient.instance = new LocalProvenanceClient();
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
