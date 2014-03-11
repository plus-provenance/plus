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
import java.util.HashMap;
import java.util.List;

import org.mitre.provenance.plusobject.PLUSObject;

/**
 * This object holds an entire collection of paths, keyed by their source/destination.
 * Because more than one distinct path can exist from A -> B, the values are lists of DAGPaths, rather than a
 * single DAGPath.
 * @author DMALLEN
 */
public class PathCollection extends HashMap<String,List<DAGPath>> {
	private static final long serialVersionUID = 3645381305648440636L;

	public PathCollection() { super(); } 
	public String getKey(PLUSObject one, PLUSObject two) { 
		return getKey(one.getId(), two.getId()); 
	}

	/**
	 * A sort of functional-style interface; implement this interface and pass it as an argument to PathCollection in
	 * order to filter a collection of paths down to a matching list.
	 * @see PathCollection#findMatchingCriteria(PathCriteria)
	 */
	public interface PathCriteria { 
		/**
		 * Return true if the given path is of interest, false otherwise.
		 */
		public boolean matches(DAGPath dp);
	}
	
	/**
	 * Return a list of paths that match a given criteria block.
	 */
	public List<DAGPath> findMatchingCriteria(PathCriteria crit) {
		ArrayList<DAGPath> bigList = new ArrayList<DAGPath>();
		
		for(List<DAGPath> paths : values()) { 
			for(DAGPath dp : paths) { 
				if(crit.matches(dp)) bigList.add(dp); 
			}
		}
		
		return bigList;
	}
	
	/**
	 * Return all DAGPaths this object knows about where the path terminates at obj.
	 */
	public List<DAGPath> endingWith(PLUSObject obj) { 
		List<DAGPath> bigList = new ArrayList<DAGPath>();
		
		String sought = "-" + obj.getId();
		
		for(String k : keySet()) { 
			if(k.endsWith(sought)) {  
				for(DAGPath dp : get(k)) bigList.add(dp);
			}
		}
		
		return bigList;		
	}
	
	/**
	 * Return the total number of paths the collection knows about.
	 */
	public int size() {
		int s = 0; 
		
		for(String k : keySet()) { 
			s += get(k).size();
		}
		
		return s;
	}

	/**
	 * Return all DAGPaths this object knows about where the path originates at obj.
	 */
	public List<DAGPath> beginningWith(PLUSObject obj) { 
		List<DAGPath> bigList = new ArrayList<DAGPath>();
		
		String sought = obj.getId() + "-";
		
		for(String k : keySet()) { 
			if(k.startsWith(sought)) { 
				for(DAGPath dp : get(k)) bigList.add(dp);
			}
		}
		
		return bigList;
	}
	
	/** 
	 * Generate an appropriate key for a given set of source and destination node IDs.
	 */
	public String getKey(String sourceID, String destID) {  
		return sourceID + "-" + destID;
	}
	
	/**
	 * Utility function for conveting a single DAGPath into a list of 1 DAGPath for easy insertion into this 
	 * data structure.
	 */
	public List<DAGPath> list(DAGPath p) { 
		ArrayList<DAGPath> list = new ArrayList<DAGPath>();
		list.add(p);
		return list;
	}
} // End PathCollection
