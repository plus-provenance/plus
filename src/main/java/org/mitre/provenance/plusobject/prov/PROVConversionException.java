package org.mitre.provenance.plusobject.prov;

import org.mitre.provenance.PLUSException;

/**
 * Exception indicating an error in conversion of provenance to PROV-DM structures.
 * @author moxious
 */
public class PROVConversionException extends PLUSException {
	private static final long serialVersionUID = 1L;

	public PROVConversionException(String msg) { super(msg); } 
}
