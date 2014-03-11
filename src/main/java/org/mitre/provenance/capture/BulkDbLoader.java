package org.mitre.provenance.capture;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.neo4j.cypher.CypherExecutionException;
import org.neo4j.cypher.javacompat.ExecutionResult;

public class BulkDbLoader {
	
	
	
	/**
	 * Read in a cypher file one line at a time and execute each via the Neo4JStorage.execute() method.
	 * Note, typical cyphers created via "dump" may contain extraneous lines prior to the "begin"
	 * clause.  Also, the "begin" and "commit" clauses don't work via execute as each line execution
	 * already infers a begin and commit.
	 * 
	 * The target DB is that which is specified by the Neo4JStorage API class.
	 * @param cypherFile
	 */
	public void loadCypher(String cypherFile)
	{
		BufferedReader cypher;
		String line;
		int errorCount = 0;
		boolean started = false;
		
		System.out.println("loading from cypher file " + cypherFile);
		try {
			cypher = new BufferedReader(new FileReader(cypherFile));
			
			while (cypher.ready()) {
				
				line = cypher.readLine();
				
				// ignore begin, commit, and ; cypher lines
				if (line != null) {
					if (line.equalsIgnoreCase("begin")) { started = true; continue; }
					if (line.equalsIgnoreCase("commit")) { started = false; }
					if (line.equals(";")) { continue; }
				}
				
				if (! started) continue;
				
				// skip line if processLine returns false
				if (processLine(line) != true) { continue; }
				
				System.out.println("Running cypher: " + line);
				try {
					ExecutionResult res = Neo4JStorage.execute(line);
					
				}
				catch (CypherExecutionException e)
				{
					System.out.println("Ignoring failed execution: " + e.getMessage());
					errorCount++;
				}
				
			}
			
		} catch (FileNotFoundException e) {
			System.out.println("Failed to find cypher file: " + cypherFile);
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Cypher load complete! Error count: " + errorCount);
		
	}
	
	/**
	 * Do something to or use contents of line in some way.
	 * 
	 * @param line
	 * @return TRUE (use line), FALSE (skip line)
	 */
	private boolean processLine(String line) 
	{
		// Things to do with line:
		//	* extract info about the node being created (oid, meta)
		//  * look up existing node to see if a match exists
		//  * adjust mapping against OID
		//  * retool line for any adjustments needed
		//  * if line should be skipped return 'false'
		
		return true;
		
	}

	public static void main(String[] args)
	{
		String testCypher = args[0];  // "c:/users/phil/cypher";
		
		if (testCypher == null) {
			System.out.print("Usage: java -cp org.mitre.provenance.capture.BulkDbLoader  <cypherFile>\n");
		}
		BulkDbLoader loader = new BulkDbLoader();
		
		loader.loadCypher(testCypher);
		
	}

}
