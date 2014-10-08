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

import java.util.Date;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.surrogate.SurrogateGeneratingFunction;
import org.mitre.provenance.user.PrivilegeClass;
import org.mitre.provenance.user.PrivilegeSet;

/**
 * This object contains parameters for generating synthetic graphs.
 * @author moxious
 */
public class SyntheticGraphProperties {
	/** The name of the workflow which holds the synthetic graph. */
	protected String name = "Synthetic Graph";
	
	/** The chance that a given node in the graph will be connected to things downstream of it */
	protected double connectivity = 0.25;
	
	/** The number of nodes or components in the synthetic graph */
	protected int components = 100;
	
	/** The percentage of nodes that should be data; the complement will be invocations */
	protected double pctData = 0.75;
	
	/** The number of nodes in the graph that should be protected */
	protected int protectN = 0;
	
	/** The SGF used for generating different accounts of nodes */
	protected SurrogateGeneratingFunction sgf = null;
	
	/** The privilege class to apply to protected nodes */
	protected PrivilegeSet ps = new PrivilegeSet();
	
	public SyntheticGraphProperties() {
		connectivity = 0.25;
		components = 100;
		pctData = 0.75;
		protectN = 0;
		sgf = null;
		ps = new PrivilegeSet(); ps.addPrivilege(PrivilegeClass.PUBLIC);
		name = "Synthetic Graph " + (new Date()).toString();
	}
	
	public String toString() { 
		return "Synthetic Graph: " + getName() + " components=" + getComponents() + 
			   " connectivity=" + getConnectivity() + " protectN=" + getProtectN() + 
			   " SGF=" + getSGF() + " PS=" + getPrivilegeSet() + " pctData=" + getPercentageData();
	}
	
	public String getName() { return name; } 
	public Integer getComponents() { return components; } 
	public Integer getProtectN() { return protectN; }
	public SurrogateGeneratingFunction getSGF() { return sgf; } 
	public PrivilegeSet getPrivilegeSet() { return ps; } 
	public Double getPercentageData() { return pctData; } 
	public Double getConnectivity() { return connectivity; } 
	
	public SyntheticGraphProperties setName(String name) { 
		this.name = name;
		return this;
	}
	
	public SyntheticGraphProperties addPrivilege(PrivilegeClass pc) { 
		ps.addPrivilege(pc);
		return this;
	}
	
	public SyntheticGraphProperties setPrivilegeSet(PrivilegeSet ps) {
		this.ps = ps;
		return this;
	}
	
	public SyntheticGraphProperties setConnectivity(double connectivity) throws PLUSException {
		if(connectivity <= 0 || connectivity > 1) throw new PLUSException("Connectivity must be > 0 and <= 1");
		this.connectivity = connectivity;
		return this;
	}
	
	public SyntheticGraphProperties setSGF(SurrogateGeneratingFunction sgf) { 
		this.sgf = sgf;
		return this;
	}
	
	public SyntheticGraphProperties setComponents(int components) throws PLUSException {
		if(components <= 0) throw new PLUSException("Components must be > 0");
		this.components = components; 
		return this;
	}
	
	public SyntheticGraphProperties percentageData(double pctData) throws PLUSException {
		if(pctData > 1 || pctData < 0) throw new PLUSException("pctData must be <= 1 and >= 0");
		this.pctData = pctData;
		return this;
	}
	
	public SyntheticGraphProperties protectN(int protectN) { 
		if(protectN < 0) protectN = 0;
		if(protectN > components) protectN = components;
		this.protectN = protectN;
		return this;
	}
} // End SyntheticGraphProperties
