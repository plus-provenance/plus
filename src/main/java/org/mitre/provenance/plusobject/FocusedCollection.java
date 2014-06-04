package org.mitre.provenance.plusobject;

/**
 * This represents a provenance collection that has a particular focus (i.e. a node of interest for a user)
 * @author moxious
 */
public interface FocusedCollection {
	/** Returns the "focus" of the DAG.   This corresponds to the "node of interest" for a user, if appropriate.
	 * @return the focus of the DAG
	 */
	public PLUSObject getFocus();
}
