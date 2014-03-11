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
 * @author DMALLEN
 */
public abstract class DAGWatcher {
	public abstract void nodeAdded(PLUSObject node);
	public abstract void nodeRemoved(PLUSObject node); 
	public abstract void edgeRemoved(PLUSEdge edge); 
	public abstract void edgeAdded(PLUSEdge edge);
	public abstract void startTimer(String timerName); 
	public abstract void stopTimer(String timerName); 
	public abstract void finished(LineageDAG dag); 
}
