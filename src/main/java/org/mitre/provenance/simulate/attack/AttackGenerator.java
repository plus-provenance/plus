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
package org.mitre.provenance.simulate.attack;

import org.mitre.provenance.db.neo4j.Neo4JStorage;

/**
 * Simple utility class for running all of the attacks and saving the results to the database.
 * This is an experiment performed to see if attacks can be detected on 
 * the basis of DAG fingerprints.
 * 
 * 
 * <p>These helper classes help generate instances of graphs that demonstrate attacks.
 * 
 * @author DMALLEN
 */
public class AttackGenerator {
	public static void main(String [] args) throws Exception { 
		Neo4JStorage.store(new DataModificationAttack().getCollection());
		Neo4JStorage.store(new DataStealingAttack().getCollection());
		Neo4JStorage.store(new DisruptionAttack().getCollection());
		Neo4JStorage.store(new ManInTheMiddleAttack().getCollection());
		Neo4JStorage.store(new SeverAttack().getCollection());		
	} // End main
} // End AttackGenerator
