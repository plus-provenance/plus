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
package org.mitre.provenance.dag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mitre.provenance.plusobject.PLUSObject;

/**
 * A catalog of paths through a DAG.  This is a map of keys to paths; keys correspond to the two OIDs of 
 * nodes in the graph, values correspond to a list of OIDs in sequence describing a path through the graph
 * to reach from one OID to the other.
 * @author dmallen
 */
public class PathCatalog extends HashMap<String,List<String>> {	
	private static final long serialVersionUID = 5985118072305278850L;

	protected int maxPathLength;	
	
	public PathCatalog() { 		
		maxPathLength = -1;
	}
	
	public Set<String> commonElements(List<List<String>> paths) { 
		HashMap<String,Integer> counts = new HashMap<String,Integer>();
		
		for(List<String> path : paths) { 
			for(String member : path) { 
				Integer count = counts.get(member);
				if(count == null) count = 1;
				else count = count + 1;
				counts.put(member, count);
			} // End for
		} // End for
		
		HashSet<String>results = new HashSet<String>();
		
		for(String k : counts.keySet()) { 
			if(counts.get(k).equals(paths.size())) results.add(k);
		}
		
		return results;
	} // End commonElements
	
	public List<List<String>> getPathsOfLength(int len) { 
		List<List<String>> paths = new ArrayList<List<String>>();
		
		if(len <= 1) return paths;
		
		for(String k : keySet()) { 
			List<String>path = get(k);
			if(path.size() == len) paths.add(path);
		}
		
		return paths;
	} // End getPathsOfLength
	
	public int getMaxPathLength() { 
		if(maxPathLength == -1) { 
			for(String k : keySet()) { 
				if(get(k).size() > maxPathLength) 
					maxPathLength = get(k).size();
			}
		}
		
		return maxPathLength;
	} // End getMaxPathLength
	
	public boolean containsKey(PLUSObject one, PLUSObject two) { 
		return containsKey(getKeyForUndirectedEdge(one, two));  
	}
	
	public void addPath(PLUSObject one, PLUSObject two, List<String>path) { 
		put(getKeyForUndirectedEdge(one, two), path); 
	}

	public void addPath(String oidOne, String oidTwo, List<String>path) { 
		put(getKeyForUndirectedEdge(oidOne, oidTwo), path); 
	}
	
	public static String getKeyForUndirectedEdge(PLUSObject one, PLUSObject two) { 
		return getKeyForUndirectedEdge(one.getId(), two.getId()); 
	}
	
	public static String getKeyForUndirectedEdge(String oid1, String oid2) { 
		ArrayList<String> l = new ArrayList<String>();
		l.add(oid1); l.add(oid2);  
		Collections.sort(l);		
		return l.get(0) + " " + l.get(1); 
	} // End getKeyForUndirectedEdge
} // End class PathCatalog
