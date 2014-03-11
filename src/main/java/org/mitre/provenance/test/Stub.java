package org.mitre.provenance.test;

import java.io.InputStream;
import java.util.List;

import org.mitre.provenance.dag.LineageDAG;
import org.mitre.provenance.dag.TraversalSettings;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.user.User;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.ResourceIterator;

public class Stub {
	public static void main(String [] args) throws Exception {
		String cypher = "match (nullin)-[rin]->(source:Provenance)-[r:contributed|generated|triggered|`input to`]->(sink:Provenance)-[rout]->(nulllout) " +  
         "where sink.type = 'data' " + 
         //"and not(type(rin)=~ 'contributed|generated|triggered|input.*to) " + 
         //"and not(type(rout)=~ 'contributed|generated|triggered|input.*to) " +        
         "return source.name,source.oid,sink.name,sink.oid;";

			ExecutionResult er = Neo4JStorage.execute(cypher);
			
			ResourceIterator name = er.columnAs("source.name");
			ResourceIterator oid = er.columnAs("source.oid");
			ResourceIterator sinkname = er.columnAs("sink.name");
			ResourceIterator sink = er.columnAs("sink.oid");
			
			while(name.hasNext()) {
				System.out.println("SOURCE: " + name.next() + " oid=" + oid.next() + " SINK: " + sinkname.next() + " oid=" + sink.next());
			}
			
	}
	
	public static void __main(String [] args) throws Exception { 
		System.out.println("Showing something strange...");

		InputStream in = Stub.class.getClassLoader()
                .getResourceAsStream("logback.xml");
		
		System.out.println("Input stream: " + in);
		
		List<PLUSWorkflow> wfs = Neo4JStorage.listWorkflows(User.DEFAULT_USER_GOD, 100);
		for(PLUSWorkflow wf : wfs) {
			ProvenanceCollection pc = Neo4JStorage.getMembers(wf, User.DEFAULT_USER_GOD, 100);
			for(PLUSObject o : pc.getNodes()) {
				System.out.println(o);
				LineageDAG dag = Neo4JPLUSObjectFactory.newDAG(o.getId(), User.DEFAULT_USER_GOD, new TraversalSettings());
				System.out.println(dag);
			}
		}
	}
}
