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

package org.mitre.provenance.simulate.motif;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.client.ProvenanceClientException;
import org.mitre.provenance.client.RESTProvenanceClient;
import org.mitre.provenance.simulate.SyntheticGraphProperties;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Scanner;

/**
	* A looper tool to upload synthetic motif graphs to a Provenance node of your choosing.
	* A synthetic graph can be controlled by volume (node count) and density (connectivity) 
	* This was initially created to stress-test a Provenance node. 
	* @author Alexander Marr
*/

public class RemoteLoader {
	/*
		connectivity refers to the density of the graph while graphSize refers to its volume. 
		minTime is the minimum time a graph took to upload to the server while maxTime is the maximum.
		testtime is a placeholder value to later find the average time it took for all graphs to upload. 
	*/
	static int graphSize, numGraphs, actors, nodes, edges, npe;
	static float connectivity;
	static long testtime=0, count=1, minTime, maxTime, time;
	static FileWriter writer;
	
	public static void performTest(RESTProvenanceClient SynthLink, int graphSize, float connectivity, int numGraphs, String writeCSV) throws PLUSException, IOException { 		
		/* Initializing the RESTProvenanceclient and the graph we're going to be working with, respectively. 
		Feel free to change "prov04.mitre.org" to another server and "8080" to the listening port on that server.*/
		
		SyntheticGraphProperties Synth = new SyntheticGraphProperties().setComponents(graphSize).setConnectivity(connectivity);
	
		//Initialize CSV file-writing stream and write the CSV file-header. Default file location in {HOME}/user/UploadCSV.txt
		if (writeCSV.equals("y")){
			try{
				writer = new FileWriter(System.getProperty("user.home") + "\\RemoteUploadCSV.txt");
				writer.append("Upload Number(1), Time to Upload in seconds(2), actors(3), nodes(4), edges(5), npes(6), connectivity(7)" + String.format("%n"));
			}
			catch (Exception e)
			{
				System.out.println("Error with CSVWRITER. Printing Stack Trace: ");
				e.printStackTrace();
			}
		}
		
		switch (numGraphs) {
													//case for an infinite loop
		case 0: 								
			while (numGraphs == 0) { 
				System.out.printf("Generating Motif Graph number "  + count + "........");
				RandomMotifCollection RandomMotif = new RandomMotifCollection(Synth, "REST"); //construct REST graph
				System.out.printf( " done.%n");
				
				Instant start = Instant.now(); //Start timer to record how long Uploading takes
				System.out.println("Uploading graph number " + count + "......... ");
				SynthLink.report(RandomMotif, "quiet"); //Uploading to prov04.mitre.org
				Instant end = Instant.now();   //End timer 
				
				testtime = testtime + Duration.between(start,end).getSeconds();      //record total time
				if (maxTime < Duration.between(start,end).getSeconds()) {maxTime = Duration.between(start,end).getSeconds();} //record maximum time
				if (count == 1) {minTime = Duration.between(start,end).getSeconds();}
				if (minTime > Duration.between(start,end).getSeconds()) { minTime = Duration.between(start,end).getSeconds();} //record minimum time
				
				printTotalTimes("Completed run number " + count + " in " + Duration.between(start,end).getSeconds() + " seconds!");
				
				//get actors, nodes, edges, npe (non-provenance edge), and time
				actors = RandomMotif.countActors(); 
				nodes = RandomMotif.countNodes();
				edges = RandomMotif.countEdges();
				npe = RandomMotif.countNPEs();
				time = Duration.between(start,end).getSeconds();
				
				//appending info to the CSV file
				if (writeCSV.equals("y")) {
				writer.append(count + "," + time + "," + actors + "," + nodes + "," + edges + "," + npe + "," + connectivity + String.format("%n")); 
				writer.flush();}
				
				count++;
				
			}
			
													//case for finite loop
		default: 								
			for (int i=1; i<=numGraphs; i++) {
				System.out.printf("Generating Motif Graph number "  + i + "........");
				RandomMotifCollection RandomMotif = new RandomMotifCollection(Synth, "REST");
				System.out.printf(" done.%n");
					
				Instant start = Instant.now();
				System.out.printf("Uploading Graph number " + i + "......... ");
				SynthLink.report(RandomMotif, "quiet");
				Instant end = Instant.now();
				System.out.println("done.");
				
				testtime = testtime + Duration.between(start,end).getSeconds();
				if (maxTime < Duration.between(start,end).getSeconds()) {maxTime = Duration.between(start,end).getSeconds();} //record maximum time
				if (i == 1) {minTime = Duration.between(start,end).getSeconds();}
				if (minTime > Duration.between(start,end).getSeconds()) { minTime = Duration.between(start,end).getSeconds();} //record minimum time
				
				printTotalTimes("Completed run number " + i + " in " + Duration.between(start,end).getSeconds() + " seconds!");
				
				actors = RandomMotif.countActors();
				nodes = RandomMotif.countNodes();
				edges = RandomMotif.countEdges();
				npe = RandomMotif.countNPEs();
				time = Duration.between(start,end).getSeconds();
				
				if (writeCSV.equals("y")) {
					writer.append(count + "," + time + "," + actors + "," + nodes + "," + edges + "," + npe + "," + connectivity + String.format("%n")); 
					writer.flush();}
				
				count++;
				
			}
			
			
		}		
	}
	
	public static void performTest(LocalProvenanceClient SynthLink, int graphSize, float connectivity, int numGraphs, String writeCSV) throws PLUSException, IOException { 		
		/* Initializing the RESTProvenanceclient and the graph we're going to be working with, respectively. 
		Feel free to change "prov04.mitre.org" to another server and "8080" to the listening port on that server.*/
		
		SyntheticGraphProperties Synth = new SyntheticGraphProperties().setComponents(graphSize).setConnectivity(connectivity);
	
		//Initialize CSV file-writing stream and write the CSV file-header. Default file location in {HOME}/user/UploadCSV.txt
		if (writeCSV.equals("y")){
			try{
				writer = new FileWriter(System.getProperty("user.home") + "\\LocalUploadCSV.txt");
				writer.append("Upload Number(1), Time to Upload in seconds(2), actors(3), nodes(4), edges(5), npes(6), connectivity(7)" + String.format("%n"));
			}
			catch (Exception e)
			{
				System.out.println("Error with CSVWRITER. Printing Stack Trace: ");
				e.printStackTrace();
			}
		}
		
		switch (numGraphs) {
													//case for an infinite loop
		case 0: 								
			while (numGraphs == 0) { 
				System.out.printf("Generating Motif Graph number "  + count + "........");
				RandomMotifCollection RandomMotif = new RandomMotifCollection(Synth, "REST"); //construct REST graph
				System.out.printf( " done.%n");
				
				Instant start = Instant.now(); //Start timer to record how long Uploading takes
				System.out.println("Uploading graph number " + count + "......... ");
				SynthLink.report(RandomMotif); //Uploading to prov04.mitre.org
				Instant end = Instant.now();   //End timer 
				
				testtime = testtime + Duration.between(start,end).getSeconds();      //record total time
				if (maxTime < Duration.between(start,end).getSeconds()) {maxTime = Duration.between(start,end).getSeconds();} //record maximum time
				if (count == 1) {minTime = Duration.between(start,end).getSeconds();}
				if (minTime > Duration.between(start,end).getSeconds()) { minTime = Duration.between(start,end).getSeconds();} //record minimum time
				
				printTotalTimes("Completed run number " + count + " in " + Duration.between(start,end).getSeconds() + " seconds!");
				
				//get actors, nodes, edges, npe (non-provenance edge), and time
				actors = RandomMotif.countActors(); 
				nodes = RandomMotif.countNodes();
				edges = RandomMotif.countEdges();
				npe = RandomMotif.countNPEs();
				time = Duration.between(start,end).getSeconds();
				
				//appending info to the CSV file
				if (writeCSV.equals("y")) {
				writer.append(count + "," + time + "," + actors + "," + nodes + "," + edges + "," + npe + "," + connectivity + String.format("%n")); 
				writer.flush();}
				
				count++;
				
			}
			
													//case for finite loop
		default: 								
			for (int i=1; i<=numGraphs; i++) {
				System.out.printf("Generating Motif Graph number "  + i + "........");
				RandomMotifCollection RandomMotif = new RandomMotifCollection(Synth, "REST");
				System.out.printf(" done.%n");
					
				Instant start = Instant.now();
				System.out.printf("Uploading Graph number " + i + "......... ");
				SynthLink.report(RandomMotif);
				Instant end = Instant.now();
				System.out.println("done.");
				
				testtime = testtime + Duration.between(start,end).getSeconds();
				if (maxTime < Duration.between(start,end).getSeconds()) {maxTime = Duration.between(start,end).getSeconds();} //record maximum time
				if (i == 1) {minTime = Duration.between(start,end).getSeconds();}
				if (minTime > Duration.between(start,end).getSeconds()) { minTime = Duration.between(start,end).getSeconds();} //record minimum time
				
				printTotalTimes("Completed run number " + i + " in " + Duration.between(start,end).getSeconds() + " seconds!");
				
				actors = RandomMotif.countActors();
				nodes = RandomMotif.countNodes();
				edges = RandomMotif.countEdges();
				npe = RandomMotif.countNPEs();
				time = Duration.between(start,end).getSeconds();
				
				if (writeCSV.equals("y")) {
					writer.append(count + "," + time + "," + actors + "," + nodes + "," + edges + "," + npe + "," + connectivity + String.format("%n")); 
					writer.flush();}
				
				count++;
				
			}
			
			
		}		
	}
	
	public static void testRemote() throws ProvenanceClientException, PLUSException, IOException { 
		
		//Initialize input scanner
		Scanner input = new Scanner(System.in);
		
		//initializing the number of nodes we want in our generated graph
		System.out.println("Enter Number of Nodes: ");
		graphSize = input.nextInt();
		
		//Initializing connectivity of our generated graph
		System.out.println("Enter connectivity of nodes (0 < x < 1): "); 
		connectivity = input.nextFloat();
		
		//How many times do we want to send unique graphs? Entering 0 loops infinitely. 
		System.out.println("Enter number of times to loop (enter 0 for infinite loop): ");
		numGraphs = input.nextInt();
		
		//Decide whether to write a csv file
		System.out.println("Write to CSV file (y/n)?");
		String writeCSV = input.next();
		
		input.close();

		performTest(new RESTProvenanceClient("prov04.mitre.org", "8080"), graphSize, connectivity, numGraphs, writeCSV);

	}
	
	
	public static void testLocal() throws PLUSException, IOException { 
		
		//Initialize input scanner
		Scanner input = new Scanner(System.in);
		
		//initializing the number of nodes we want in our generated graph
		System.out.println("Enter Number of Nodes: ");
		graphSize = input.nextInt();
		
		//Initializing connectivity of our generated graph
		System.out.println("Enter connectivity of nodes (0 < x < 1): "); 
		connectivity = input.nextFloat();
		
		//How many times do we want to send unique graphs? Entering 0 loops infinitely. 
		System.out.println("Enter number of times to loop (enter 0 for infinite loop): ");
		numGraphs = input.nextInt();
		
		//Decide whether to write a csv file
		System.out.println("Write to CSV file (y/n)?");
		String writeCSV = input.next();
		
		input.close();

		performTest(new LocalProvenanceClient(), graphSize, connectivity, numGraphs, writeCSV);

	}
	
	public static void printTotalTimes(String s)
	{
		for(int i=0; i<=50; i++)
		{
			System.out.printf("-");
		}
		System.out.printf("%n");
		System.out.println(s);
		
		//Total Times recorded in seconds
		System.out.println("\n......... TOTAL UPLOAD TIME =  " + testtime);
		System.out.println("......... AVERAGE UPLOAD TIME = " + (testtime/count));
		System.out.println("......... MINIMUM UPLOAD TIME = " + minTime);
		System.out.println("......... MAXIMUM UPLOAD TIME = " + maxTime);
		for(int i=0; i<=50; i++)
		{
			System.out.printf("-");
		}
		System.out.printf("%n");
	}

	public static void main(String[] args) throws PLUSException, IOException {
		
				Scanner main_input = new Scanner(System.in);
				System.out.println("Test Local or Remote (local/remote)?");
				String LocalorRemote = main_input.next();
				
				if (LocalorRemote.equals("local")) { testLocal(); }
				
				else {testRemote();}
				
				main_input.close();
	}

}