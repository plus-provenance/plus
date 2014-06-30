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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.PropertyCapable;
import org.mitre.provenance.PropertySet;
import org.mitre.provenance.StopWatch;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.tools.PLUSUtils;

/**
 * A FingerPrint is a statistical profile of a LineageDAG that stores basic information
 * about the nature of the information in the DAG.  This is a first-class object which can be
 * written, loaded, and so on.
 * @see LineageDAG#getFingerPrint()
 * @author DMALLEN
 */
public class FingerPrint extends DAGWatcher implements PropertyCapable {
	private static final Logger log = Logger.getLogger(FingerPrint.class.getName());
	
	protected String id = null;
	protected boolean finished = false;
	protected boolean written = false; 
	protected int nodes = 0;
	protected int edges = 0; 
	protected int owners = 0;
	protected int invocations = 0;
	protected int data = 0; 
	protected String dagId = null;
	protected String startId = null;
	protected long created; 
	protected long runTime = 0; 
	
	protected long minNodeCreated = Long.MAX_VALUE-1;
	protected long maxNodeCreated = 0; 
	
	protected HashMap<String,Boolean> seenOwners = new HashMap<String,Boolean>();
	protected HashMap<String, StopWatch> timers = new HashMap<String,StopWatch>();
	protected HashMap<String,NodeProfile> nodeProfiles = new HashMap<String,NodeProfile> ();	
	protected PathCollection pathCollection = null;

	/** Data/invocation ratio */
	public static final String DI_RATIO = "DIRatio";
	/** Data/total ratio */
	public static final String DATA_RATIO = "DataRatio";
	/** Invocation/total ratio */
	public static final String INVOCATION_RATIO = "InvRatio";
	/** Maximum amount of inbound edges to any node */
	public static final String MAX_IN_EDGES = "MaxInEdges";
	/** Maximum  amount of outbound edges to any node */
	public static final String MAX_OUT_EDGES = "MaxOutEdges";
	/** Node/Edge Ratio */
	public static final String NODE_EDGE_RATIO = "NodeEdgeRatio";
	/** Average total number of edges per node */
	public static final String AVG_TOTAL_EDGES = "AverageTotalEdges";
	/** Run time of entire graph (newest node creation time minus oldest node creation time) */
	public static final String RUN_TIME = "RunTime"; 
	
	protected long init = 0;
	protected long end = 0;
	
	public FingerPrint() { 
		id = PLUSUtils.generateID();
		init = System.currentTimeMillis();
	} 
	
	public String getId() { return id; } 
	public void setStartId(String startId) { this.startId = startId; } 
	protected void setCreated(long creationTime) { created = creationTime; } 
	
	/**
	 * Sets the object's created timestamp to this moment in milliseconds since the epoch, UTC
	 */
	public void setCreated() { 
		Calendar calInitial = Calendar.getInstance();  
        int offsetInitial = calInitial.get(Calendar.ZONE_OFFSET)  
                + calInitial.get(Calendar.DST_OFFSET);  
  
        long current = System.currentTimeMillis();  
          
        // Check right time  
        created = current - offsetInitial;
	}
	
	public String toString() {
		StringBuffer b = new StringBuffer("");
		Metadata m = asMetadata();
		
		for(String k : m.keySet()) { 
			b.append(k + " = " + m.get(k) + "\n");
		}
		
		return b.toString();
	}
	
	public long getCreated() { return created; } 
	public void setDagId(String dagId) { this.dagId = dagId; } 
	public String getDagId() { return dagId; } 
	
	/**
	 * Return the FingerPrint object as a metadata table
	 * @return a table mapping field names (such as INVOCATION_RATIO) to values
	 */
	public Metadata asMetadata() { 
		Metadata m = new Metadata();
				
		m.put("TotalTime", (end-init));
		m.put("DataItems", ""+data);
		m.put("Invocations", ""+invocations); 		
		m.put("Nodes", ""+nodes); 
		m.put("Edges", ""+edges);
		m.put(RUN_TIME, ""+(maxNodeCreated-minNodeCreated));
		m.put(NODE_EDGE_RATIO, ""+(edges <= 0 ? 0 : ((double)nodes/(double)edges)));
		m.put("Owners", ""+owners); 
		
		for(String key : timers.keySet()) 
			m.put("timer:"+key, timers.get(key).toString());
		
		int nps = 0;
		int inMax = 0;
		int outMax = 0; 
		int totTot = 0; 
		for(String key : nodeProfiles.keySet()) { 
			nps++; 
			NodeProfile p = nodeProfiles.get(key);
			
			if(p.getEdgesIn() > inMax) inMax = p.getEdgesIn();
			if(p.getEdgesOut() > outMax) outMax = p.getEdgesOut(); 
			
			totTot += p.getTotalEdges(); 
		} // End for
			
		m.put(MAX_IN_EDGES, ""+inMax); 
		m.put(MAX_OUT_EDGES, ""+outMax); 
		
		m.put(INVOCATION_RATIO, ""+(invocations <= 0 ? 0 : ((double)invocations/(double)nodes))); 
		m.put(DATA_RATIO, ""+(data <= 0 ? 0 : ((double)data/(double)nodes))); 
		m.put(DI_RATIO, ""+(invocations <= 0 ? 0 : ((double)data/(double)invocations)));
		
		if(nps > 0) m.put(AVG_TOTAL_EDGES, ""+((double)totTot/(double)nps)); 
		else        m.put(AVG_TOTAL_EDGES, "0");		
				
		return m;
	} // End asMetadata
		
	public void edgeRemoved(PLUSEdge edge) {
		if(finished) log.severe("Removing edge " + edge + " after graph was finished!"); 
		edges--; 
	} // End edgeRemoved
	
	public void nodeRemoved(PLUSObject obj) { 
		if(finished) log.severe("Removing node " + obj.getName() + " after graph was finished!"); 
		nodes--; 
		nodeProfiles.remove(obj.getId()); 
	}
	
	public void edgeAdded(PLUSEdge edge) {
		if(finished) log.severe("Adding edge " + edge + " after graph was finished!"); 
		edges++;
		
		String from = edge.getFrom().getId();
		String to = edge.getTo().getId(); 

		NodeProfile fp = getProfile(from);
		NodeProfile tp = getProfile(to); 
		
		fp.edgeOut();
		tp.edgeIn(); 
	} // End edgeAdded

	protected NodeProfile getProfile(String nodeId) { 
		if(nodeId == null) nodeId = "null";
		
		if(!nodeProfiles.containsKey(nodeId))
			nodeProfiles.put(nodeId, new NodeProfile()); 
		
		return nodeProfiles.get(nodeId);  
	} // End getProfile
	
	/**
	 * Indicate that the dag in question is finished, and will not be subsequently changed.  
	 * This permits any fingerprinting that requires presence of the entire DAG.
	 * <p><b>As a side-effect of this call, after it has been called, certain other functions will
	 * issue error messages</b>.  Examples would include adding nodes and edges after the graph was
	 * finished.
	 * @param dag the DAG that has been finished.
	 */
	public void finished(LineageDAG dag) {
		end = System.currentTimeMillis();
		
		nodes = dag.countNodes();
		edges = dag.countEdges();
		
		data = 0;
		invocations = 0;
		for(PLUSObject o : dag.getNodes()) {
			if(o.isDataItem()) data++;
			if(o.isInvocation()) invocations++;
		}
		
		finished = true; 
	}
	
	public void nodeAdded(PLUSObject node) {
		if(finished) log.severe("Adding node " + node.getName() + " after graph was finished!"); 
		nodes++; 
		
		if(startId == null) setStartId(node.getId());  
		
		long c = node.getCreated();
		if(c > maxNodeCreated) maxNodeCreated = c;
		if(c < minNodeCreated) minNodeCreated = c; 
		
		if(node.isDataItem()) data++;
		if(node.isInvocation()) invocations++; 
		
		try {
			PLUSActor a = node.getOwner();
			String name = "unknown";
			if(a != null) name = a.getName(); 
			
			if(!seenOwners.containsKey(name)) {
				owners++;
				seenOwners.put(name, true); 
			} // End if
		} catch(Exception e) { 
			log.severe("FingerPrint#nodeAdded: " + e); 
		} // End catch
	} // End nodeAdded

	public void startTimer(String timerName) {
		StopWatch t = timers.get(timerName);
		if(t == null) { t = new StopWatch(); timers.put(timerName, t); } 
		t.start(); 
	} // End startTimer

	public void stopTimer(String timerName) {
		StopWatch t = timers.get(timerName);
		if(t == null) {
			log.severe("Stopping non-existant timer " + timerName + "!"); 
			t = new StopWatch(); timers.put(timerName, t); 
		} 
		
		if(!t.isStarted()) {
			log.warning("Stopping not-started timer " + timerName);
		}
		
		t.stop(); 
	} // End stopTimer	
		
	public Map<String, Object> getStorableProperties() {
		Metadata m = asMetadata();

		m.put("dagId", dagId);
		m.put("startId", startId);
		m.put("created", created);
		m.put(RUN_TIME, Long.parseLong(""+m.get(RUN_TIME)));
		m.put("nodes", nodes);
		m.put("edges", edges);

		return m;
	}

	public PLUSObject setProperties(PropertySet props) throws PLUSException {
		// TODO Auto-generated method stub
		throw new PLUSException("Implement me"); 
	}
	
	/***************************************************************************************/
	
	private class NodeProfile {
		int in = 0;
		int out = 0; 
		public NodeProfile() { in=0; out=0; } 
		public void edgeIn() { in++; } 
		public void edgeOut() { out++; } 
		public int getEdgesIn() { return in; } 
		public int getEdgesOut() { return out; }
		public int getTotalEdges() { return in + out; } 
	} // end class NodeProfile
} // End FingerPrint