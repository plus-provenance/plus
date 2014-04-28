package org.mitre.provenance.plusobject.prov;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class that can be used in place of org.openprovenance.xml.QualifiedName, but performs validation.
 * 
 * <p>Note that some PLUS objects may use urns (urn:uuid:plus:*) as localnames, which technically aren't legal
 * NCNames according to the spec.  This doesn't seem to matter in PROV-XML though, since all of the QualifiedNames
 * are only used inside of id attributes (as strings).</p> 
 * 
 * @author moxious
 *
 */
public class VerifiedXMLQualifiedName extends org.openprovenance.prov.xml.QualifiedName {
	private static Pattern ncName = Pattern.compile("^[A-Za-z_][A-Za-z0-9_\\-\\.]*");
	
	public VerifiedXMLQualifiedName(String namespaceURI, String localPart, String prefix) {
		super(namespaceURI, localPart, prefix);

		if(!validURI(namespaceURI)) throw new IllegalArgumentException("Invalid namespaceURI '" + namespaceURI + "'"); 
		if(!validNCName(localPart)) throw new IllegalArgumentException("Invalid localpart '" + localPart + "'");
		if(!validNCName(prefix)) throw new IllegalArgumentException("Invalid prefix '" + prefix + "'");		
	}
	
	private boolean validURI(String uri) { 
		try {
			URI u = new URI(uri);
		} catch (URISyntaxException e) {
			return false;
		}
		
		return true;
	} // End validURI
	
	private boolean validNCName(String tok) { 
		if(tok == null) return false;
		Matcher m = ncName.matcher(tok);
		if(m == null) return false;
		if(!m.matches()) return false;
		return true;
	} // End validNCName
} // End VerifiedXMLQualifiedName
