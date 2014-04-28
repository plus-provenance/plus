package org.mitre.provenance.simulate;

/**
 * Helper class to repetitively generate many DeclarativeBuilders based on a template.
 * The name template should contain variable names in curly braces, e.g. "Data item {x}".
 * With a template with that name, generate() can then be called with varName="x" and various values
 * to generate novel data items. 
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
