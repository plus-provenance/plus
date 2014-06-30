package org.mitre.provenance;

/**
 * This is a shim interface intended to unify functionality from several different libriaries (notably JSON and Neo4J libs).
 * This simply represents a bucket of properties.
 * The map interface wasn't appropriate because this is intended to be read-only.
 * Other objects with different functionality will get wrapped in an implementation of this interface so that code can create/save
 * objects as a bag of properties, without having to understand which library is being used to do that, and their different APIs.
 * @author moxious
 */
public interface PropertySet {
	public Object getProperty(String propName);
	public Object getProperty(String propName, Object defaultValue);
	public Iterable<String> getPropertyKeys();
	public boolean hasProperty(String property); 
}
