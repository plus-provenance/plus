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
package org.mitre.provenance.simulate;

import java.util.Random;

import org.mitre.provenance.plusobject.ProvenanceCollection;

/**
 * Base class for synthetically generated graphs, that provide various convenience methods.
 * @author moxious
 *
 */
public abstract class SyntheticGraph extends ProvenanceCollection {
	protected Random rand = new Random();
	protected SyntheticGraphProperties props = null;
	
	public SyntheticGraph(SyntheticGraphProperties props) {
		super();
		this.props = props;
	}
	
	public SyntheticGraphProperties getProperties() { return props; } 
	
	public String toString() { 
		String s = super.toString();
		return props.getName() + " containing " + s;
	}
} // End SyntheticGraph
