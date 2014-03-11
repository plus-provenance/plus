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
package org.mitre.provenance.capture.linux;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.npe.NonProvenanceEdge;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSInvocation;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;

/**
 * Utility program which parses the output of the linux strace utility, and saves the results as a provenance graph.
 * To use this program, pipe the standard error output of the strace utility directly to this application on the command line,
 * or run it with an argument of a file which contains an strace dump.
 * @author dmallen
 */
public class STraceLogger extends ProvenanceCollection {
	private static Logger log = Logger.getLogger(STraceLogger.class.getName());
	protected BufferedReader br = null;
	protected Pattern linePattern = Pattern.compile("(.*?)\\((.*)\\)\\s*=\\s*(.*)$");
	protected HashMap<String,PLUSObject> mappings = new HashMap<String,PLUSObject>();
	protected PLUSInvocation last = null;
	
	protected class Syscall { 
		public String function = null;
		public ArrayList<String> arguments = new ArrayList<String>();
		public String result = null;
	}
	
	public STraceLogger(InputStream is) throws IOException { 
		super();
		br = new BufferedReader(new InputStreamReader(is));		
	}
	
	protected Syscall parseLine(String line) { 
		Syscall call = new Syscall();
		
		Matcher matcher = linePattern.matcher(line);
		
		if(!matcher.matches()) return null;
		
		call.function = matcher.group(1);
		call.arguments = parseArgs(matcher.group(2));
		call.result = matcher.group(3); 
		
		System.out.println(line);
		System.out.println("FUNCTION " + call.function + " RESULT " + call.result);
		for(String arg : call.arguments) { 
			System.out.println("ARG: '" + arg + "'"); 
		}
		System.out.println();
		
		return call;
	}
	
	protected ArrayList<String> parseArgs(String args) { 
		ArrayList<String> list = new ArrayList<String>();
		
		ArrayList<Integer[]> cutPoints = new ArrayList<Integer[]>();
		ArrayList<String> stack = new ArrayList<String>();
		
		int start = 0 ;
		int end = -1;
		int x=0; 

		while(x < args.length()) {
			char c = args.charAt(x); 
			
			if(c == ',' && stack.size() == 0) {
				end = x;
				cutPoints.add(new Integer[] { start, end });
				start = x + 1;
				end = start;
			} 
			else if(c == '[') stack.add("]");
			else if(c == '{') stack.add("}");
			else if(c == '(') stack.add(")");
			else if(stack.size() > 0 && stack.get(stack.size()-1).equals(""+c)) { 
				stack.remove(stack.size()-1); 
			}
			
			x++;
		} // End while

		end = args.length();
		cutPoints.add(new Integer[] { start, end});
		
		for(Integer[]points : cutPoints) { 
			list.add(args.substring(points[0], points[1]).trim());
		}
		
		return list;
	} // End parseArgs
	
	public void parse() throws IOException, PLUSException {
		String line = null;
		
		while((line = br.readLine()) != null) {
			Syscall call = parseLine(line);
			
			if(call != null) { 
				PLUSInvocation inv = getInvocation(call); 
				addNode(inv);
				
				for(String arg : call.arguments) {  
					addEdge(new PLUSEdge(getOrCreate(arg, true), inv, PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_INPUT_TO)); 
				}
				
				addEdge(new PLUSEdge(inv, getOrCreate(call.result, true), PLUSWorkflow.DEFAULT_WORKFLOW, PLUSEdge.EDGE_TYPE_GENERATED));
				
				if(last != null) { 
					addNonProvenanceEdge(new NonProvenanceEdge(last, inv.getId(), NonProvenanceEdge.NPE_TYPE_SEQUENCE_STEP));
				}
				
				last = inv;
			} else continue;
		} // End while
		
		br.close();
	} // End parse
	
	protected PLUSInvocation getInvocation(Syscall call) { 
		PLUSInvocation inv = new PLUSInvocation();		
		inv.setName(call.function);		
		
		//for(int x=0; x<call.arguments.size(); x++) { 
		//	inv.addInput(""+(x+1), getOrCreate(call.arguments.get(x)).getId());
		//}			

		return inv;
	}
	
	protected PLUSObject getOrCreate(String data) { return getOrCreate(data, false); } 
	
	protected PLUSObject getOrCreate(String data, boolean forceCreate) { 
		if(!forceCreate && mappings.containsKey(data)) return mappings.get(data);
		
		PLUSString string = new PLUSString(data, data);
		addNode(string);
		mappings.put(data, string);
		
		return string;
	} // End getOrCreate
	
	public static void main(String [] args) throws Exception {
		STraceLogger slog = null;
		
		if(args.length > 0) { 
			slog = new STraceLogger(new FileInputStream(new File(args[0])));
		} else { 
			slog = new STraceLogger(System.in);
		}
		
		slog.parse();
		
		log.info("Writing provenance collection: " + slog); 
		Neo4JStorage.store(slog);		
	} // End main
} // End STraceLogger
