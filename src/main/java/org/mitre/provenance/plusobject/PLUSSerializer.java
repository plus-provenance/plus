package org.mitre.provenance.plusobject;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.plusobject.json.JSONConverter;
import org.mitre.provenance.plusobject.prov.PROVConverter;
import org.openprovenance.prov.model.Document;

/**
 * An object that takes provenance collections, and knows how to serialize them in a number of different ways.
 * @author moxious
 */
public class PLUSSerializer {
	public enum Format { D3_JSON, PROV_XML, PROV_RDF, PROV_TTL };
	
	public PLUSSerializer() { ; } 
	
	/**
	 * Serialize the provided collection in the specified format.
	 * @param col
	 * @param fmt
	 * @return
	 * @throws IOException
	 * @throws PLUSException
	 */
	public String serialize(ProvenanceCollection col, Format fmt) throws IOException, PLUSException {		
		Document doc = null;
		
		if(fmt == Format.PROV_XML || fmt == Format.PROV_RDF || fmt == Format.PROV_TTL) {
			doc = new PROVConverter().provenanceCollectionToPROV(col);
		}
		
		switch(fmt) {
		case PROV_XML:			
			return PROVConverter.consume(PROVConverter.formatAs(PROVConverter.Format.XML, doc));
		case PROV_RDF:
			return PROVConverter.consume(PROVConverter.formatAs(PROVConverter.Format.RDF, doc));
		case PROV_TTL:
			return PROVConverter.consume(PROVConverter.formatAs(PROVConverter.Format.TTL, doc));			
		case D3_JSON:
		default:
			return JSONConverter.provenanceCollectionToD3Json(col);			
		}
	}
}
