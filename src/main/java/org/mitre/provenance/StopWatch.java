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
package org.mitre.provenance;

import java.util.logging.Logger;

/**
 * A stop watch is a simple timer that can be started and stopped to help with in-application profiling of various sorts.
 * @author DMALLEN
 */
public class StopWatch {
	protected static final Logger log = Logger.getLogger(StopWatch.class.getName());
	
	protected long sumTime = 0; 
	protected long start = -1;
	protected long end = -1; 
	protected long count = 0;	
	protected long min = Long.MAX_VALUE;
	protected long max = -1;
	
	public StopWatch() { start=-1; end=-1; sumTime=0; count=0; } 
	
	public void start() {
		if(isStarted()) log.severe("Already started!"); 
		start = System.currentTimeMillis();
	} // End start
	
	public void stop() { 
		end = System.currentTimeMillis();
		if(isStarted()) {			
			long elapsed = (end - start);
			sumTime += elapsed;
			if(sumTime > max) max = elapsed;
			if(sumTime < min) min = elapsed; 
			count++; 
		}
		else { log.severe("Not started!"); } 
		start = -1; 
		end = -1; 
	} // End stop
	
	public double getAverage() {
		if(count >= 0) return (double)sumTime / (double) count;
		return (double)0; 
	}
	
	public long getCount() { return count; } 
	public boolean isStarted() { return start != -1; } 
	public boolean isComplete() { return start != -1 && end != -1; } 
	public void setStart(long start) { this.start = start; } 
	public void setEnd(long end) { this.end = end; } 
	
	public long getPresentElapsedTime() { 
		if(isStarted()) return System.currentTimeMillis()-start;
		else return -1; 		
	}
	
	public long getSumElapsedTime() {
		if(isStarted()) return sumTime + (System.currentTimeMillis()-start);
		else return sumTime;
	} // End getElapsedTime
	
	public String toString() { return ""+getSumElapsedTime();  }
	
	/**
	 * Generate simple statistics about a stop watch's number of times started, min, max, and avg durations.
	 * @return a string describing the statistics.
	 */
	public String summarize() { 
		StringBuffer b = new StringBuffer("");		
		b.append("count=" + getCount() +  " tot=" + sumTime);
		if(count > 0) b.append(" avg=" + getAverage() + " min=" + min + " max=" + max); 		
		return b.toString();
	}
} // End StopWatch
