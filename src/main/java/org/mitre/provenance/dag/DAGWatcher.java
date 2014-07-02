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

import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSObject;

/**
 * A DAGWatcher is a class that receives signals about items being added to and removed from graphs.
 * @author moxious
 */
public abstract class DAGWatcher {
	/** Signal/method called when a node is added to the DAG */
	public abstract void nodeAdded(PLUSObject node);
	/** Signal/method called when a node is removed from the DAG */
	public abstract void nodeRemoved(PLUSObject node); 
	/** Signal/method called when an edge is removed from the DAG */
	public abstract void edgeRemoved(PLUSEdge edge); 
	/** Signal/method called when an edge is added to the DAG */
	public abstract void edgeAdded(PLUSEdge edge);
	/** Signal/method called when a timer begins */
	public abstract void startTimer(String timerName); 
	/** Signal/method called when a timer stops */
	public abstract void stopTimer(String timerName); 
	/** Signal/method called when the DAG is completed */
	public abstract void finished(LineageDAG dag); 
}
