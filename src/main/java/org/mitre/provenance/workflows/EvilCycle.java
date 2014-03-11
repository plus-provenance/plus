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
import java.util.Date;
import java.util.Random;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;

public class EvilCycle extends ProvenanceCollection {
	public EvilCycle() throws PLUSException { 
		super();
		PLUSWorkflow wf = new PLUSWorkflow();
		wf.setName("Test Cycle"); 
		wf.setWhenStart((new Date()).toString());
		wf.setWhenEnd((new Date()).toString());
		addNode(wf);
		
		PLUSString one = new PLUSString("One", "One"); addNode(one);
		PLUSString two = new PLUSString("Two", "Two"); addNode(two);
		PLUSString three = new PLUSString("Three", "Three"); addNode(three); 
		
		PLUSEdge o_t = new PLUSEdge(one, two, wf, PLUSEdge.EDGE_TYPE_CONTRIBUTED);
		PLUSEdge t_t = new PLUSEdge(two, three, wf, PLUSEdge.EDGE_TYPE_CONTRIBUTED);
		PLUSEdge t_o = new PLUSEdge(three, one, wf, PLUSEdge.EDGE_TYPE_CONTRIBUTED);
		
		addEdge(o_t);
		addEdge(t_t);
		addEdge(t_o);
				
		PLUSActor [] actors = new PLUSActor [] {
			new PLUSActor("Group 1"), 
			new PLUSActor("Group 2"),
			new PLUSActor("Group 3"), 
			new PLUSActor("Group 4"),
			new PLUSActor("Group 5"), 
		};
		
		for(int x=0; x<actors.length; x++) Neo4JStorage.store(actors[x]); 
		
		int jangleCount = 20;
		
		Random randGen = new Random();
		ArrayList <PLUSObject> objs = new ArrayList <PLUSObject> (); 
		for(int x=0; x<jangleCount; x++) { 
			PLUSString j = new PLUSString("Jangle " + (x+1), "Jangle " + (x+1));
			int i = randGen.nextInt();
			if(i < 0) i *= -1; 
			i = i % actors.length; 
			j.setOwner(actors[i]); 
			addNode(j);
			objs.add(j);
		}
		
		for(int x=0; x<jangleCount; x++) { 
			for(int y=(x+1); y<jangleCount; y++) { 
				PLUSEdge edge = new PLUSEdge(objs.get(x), objs.get(y), wf, 
							                 PLUSEdge.EDGE_TYPE_CONTRIBUTED); 
				addEdge(edge);
			}
		} // End for		
	} // End EvilCycle
} // End EvilCycle
