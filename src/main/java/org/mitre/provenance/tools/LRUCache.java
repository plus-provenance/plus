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
package org.mitre.provenance.tools;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Least-recently used cache implementation.
 * @author moxious
 */
public class LRUCache<K,V> {
	public static int MINIMUM_CACHE_SIZE = 100;
	public static int MAXIMUM_CACHE_SIZE = 4500; 

	private LinkedHashMap<K,V>   map;
	private int                  cacheSize;
	int hits = 0;
	int misses = 0;

	/**
	 * Create an LRU cache that tracks some maximum number of elements.
	 * @param cacheSz the maximum number of elements to track.
	 */
	public LRUCache (int cacheSz) {
		if(cacheSz < MINIMUM_CACHE_SIZE)       this.cacheSize = MINIMUM_CACHE_SIZE; 		
		else if(cacheSz > MAXIMUM_CACHE_SIZE)  this.cacheSize = MAXIMUM_CACHE_SIZE; 
		else  			                       this.cacheSize = cacheSz;
		 
		map = new LinkedHashMap<K,V>() {
			// (an anonymous inner class)
			private static final long serialVersionUID = 1;
			protected boolean removeEldestEntry (Map.Entry<K,V> eldest) {
				return size() > LRUCache.this.cacheSize; 
			}}; 
	} // End LRUCache

	/** Return the number of times the cache has been asked for an object it actually had. */
	public int getHits() { return hits; } 
	/** Return the number of times the cache has been asked for an object it didn't have. */
	public int getMisses() { return misses; } 

	/**
	 * Get something out of the cache.
	 * @param key the key you used to put it in.
	 * @return the value the cache stores.
	 */
	public V get(K key) { 
		V val = map.get(key); 

		if(val != null) hits++; 
		else misses++; 

		return val; 
	}

	public boolean containsKey(K key) { 
		return map.containsKey(key);
	}

	/**
	 * Adds an entry to this cache.
	 * If the cache is full, the LRU (least recently used) entry is dropped.
	 * @param key    the key with which the specified value is to be associated.
	 * @param value  a value to be associated with the specified key.
	 */
	public void put(K key, V value) { map.put (key,value); }	
	public void remove(K key) { map.remove(key); } 

	public void clear() { map.clear(); }
	public int usedEntries() { return map.size(); }
} // End LRUCache
