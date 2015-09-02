/* Copyright 2015 MITRE Corporation
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

package org.mitre.provenance.tools;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import java.time.Instant;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.client.RESTProvenanceClient;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.simulate.motif.Diamond;
import org.mitre.provenance.simulate.motif.Motif;
import org.mitre.provenance.tools.RemoteLoader;

public class SelectivityTest {
	/*
		A tool for testing selectivity when querying. This program runs in two main phases: First, it remotely 
		uploads a user-defined amount of graphs with a connectivity of 1. Second, after initializing the 
		Selectivity Constant, it queries the server for a certain amount of the uploaded graphs. If the query
		returns (SelectivityConstant * total graphs) amount of graphs then it is said that the test was 
		successful.
	*/
	
	static String graphName;
	static int graphSize, maxGraphs;
	static float selectivityConstant, scalabilityConstant;
	static ProvenanceCollection collection = null;
	public static void main(String[] args) throws PLUSException, IOException {
		/*
		  The following RESTProvenanceClient object uses Prov04.mitre.org as the default remote 
		  Plus server. If another is desired, just change it.  
		*/
		RESTProvenanceClient client = new RESTProvenanceClient("Prov04.mitre.org", "8080");
		
		//Running classes defined in this class
		initializeInfo();
		System.out.printf("Performing upload.... ");
		performSelectivityUpload(client, graphSize, maxGraphs, selectivityConstant);
		performQuery(client);

	}
	
	public static void initializeInfo()
	{
		//This method is used to initialize static class variables via user input 
		
		Scanner input = new Scanner(System.in);
		
		System.out.println("Enter graph sizes: ");
		graphSize = input.nextInt();
		
		System.out.println("Enter maximum number of graphs: ");
		maxGraphs = input.nextInt();

		//percent of maximumGraphs will have a unique workflow ID/name.
		//This is because, when querying, 
		System.out.println("Enter selectivity constant: ");
		selectivityConstant = input.nextFloat();
	}
	
	public static void performSelectivityUpload(RESTProvenanceClient client, int graphSize, int maxGraphs, 
												float selectivityConstant) throws PLUSException, IOException
	{
		/* This method performs the upload. It uses the performtest method from the class RemoteLoader.
		 * First it uploads (total graphs - unique graphs) then uploads the unique graphs. 
		*/
		int uniqueGraphs = (int) (maxGraphs * selectivityConstant);
		RemoteLoader.performTest(client, graphSize, 1, (maxGraphs - uniqueGraphs), "n");
		
		for(int i=1; i<=uniqueGraphs; i++) {
			//uploading unique graphs with the name "SelectivityTest number x" 
			//where x is the value of i in the loop
			client.report(new Diamond("SelectivityTest number " + i));
		}
		
	}
	
	public static void performQuery(RESTProvenanceClient client) throws IOException
	{
		/* The following code performs the actual query. It queries for every graph with the name
		 * of the form "SelectivityTEst number *" where " * " is the wildcard value. It also records the time
		 * it takes the query and calculates additional information. If the amount of nodes returned equals
		 * the amount of unique graphs you want (defined by SelectivityConstant * total graphs) then the 
		 * test was successful. 
		*/
		String query = "MATCH (n:Provenance) where n.name =~\"SelectivityTest number.*\" return n;";
		
		Instant start = Instant.now();
		collection = client.query(query);
		Instant end = Instant.now();
		int instantTime = (int) Duration.between(start,end).getSeconds();
		
		int actors = collection.countActors(); int nodes = collection.countNodes();
		int edges = collection.countEdges(); int npes = collection.countNPEs();
		
		System.out.println("Query ran for a total of " + instantTime + " seconds!");
		System.out.println("Actors = " + actors + "  Nodes = " + nodes);
		System.out.println("Edges = " + edges + "  NPEs = " + npes);
		
	}
} //end SelectivityTest class
