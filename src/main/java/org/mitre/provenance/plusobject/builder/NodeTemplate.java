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
package org.mitre.provenance.plusobject.builder;

/**
 * Helper class to repetitively generate many ProvBuilders based on a template.
 * The name template should contain variable names in curly braces, e.g. "Data item {x}".
 * With a template with that name, generate() can then be called with varName="x" and various values
 * to generate novel data items. 
 * 
 * <p>As an example, one might create a NodeTemplate such as:
 * <pre>NodeTemplate tmpl = new NodeTemplate(NodeType.DATA, "Communication {x}");</pre>
 * 
 * When generating objects based on that template, the results would be ProvBuidler objects containing a single node, always data,
 * with various substitutions of names based on the value of {x}.
 * 
 * @see ProvBuilder
 * @author moxious
 */
public class NodeTemplate {
	public enum NodeType { DATA, INVOCATION, WORKFLOW };
	
	public NodeType nt;
	public String name;
	
	public NodeTemplate(NodeType nt, String name) { 
		this.nt = nt;
		this.name = name;
	}
}
