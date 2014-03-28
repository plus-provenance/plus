package org.mitre.provenance;

import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.user.User;

public class Stub {
	public static void main(String [] args) throws Exception { 
		ProvenanceCollection col = Neo4JPLUSObjectFactory.searchFor("chain", User.DEFAULT_USER_GOD);
		
		for(PLUSObject o : col.getNodes()) { 
			System.out.println(o);
		}
	}
}
