package org.mitre.provenance.db.neo4j;

import org.mitre.provenance.PropertySet;
import org.neo4j.graphdb.PropertyContainer;

/**
 * Shim class to turn any Neo4J PropertyContainer object into a PropertySet.
 * @author moxious
 */
public class Neo4JPropertySet implements PropertySet {
	protected PropertyContainer c = null;
	
	public Neo4JPropertySet(PropertyContainer c) {
		this.c = c;
	}

	public Object getProperty(String propName) {
		return c.getProperty(propName);
	}

	public Object getProperty(String propName, Object defaultValue) {
		return c.getProperty(propName, defaultValue);
	}
	
	public Iterable<String> getPropertyKeys() {
		return c.getPropertyKeys();
	}
	
	public boolean hasProperty(String property) {
		return c.hasProperty(property);
	}
}
