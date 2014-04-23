package org.mitre.provenance.plusobject.prov;

import java.io.StringWriter;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.plusobject.PLUSActivity;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSDataObject;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSFile;
import org.mitre.provenance.plusobject.PLUSInvocation;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSURL;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.simulate.DAGAholic;
import org.mitre.provenance.simulate.SyntheticGraphProperties;
import org.mitre.provenance.user.PrivilegeClass;
import org.openprovenance.prov.model.Activity;
import org.openprovenance.prov.model.Agent;
import org.openprovenance.prov.model.Attribute;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.Entity;
import org.openprovenance.prov.model.HasOther;
import org.openprovenance.prov.model.Name;
import org.openprovenance.prov.model.Namespace;
import org.openprovenance.prov.model.Other;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.model.Statement;
import org.openprovenance.prov.xml.ProvFactory;

/**
 * A class that knows how to convert ProvenanceCollection objects into a PROV-DM representation. 
 * 
 * <p>This code follows mappings provided in the MAPPINGS-PLUS-TO-PROV-DM.txt file found in the source
 * distribution.  Without consulting that file, the mappings in this source code likely won't make much 
 * sense.
 * 
 * <p>This is an initial cut - it is still in need of substantial development, testing, and verification.
 * @author moxious
 */
public class PROVConverter {
	public static final String BASE_NAMESPACE = "http://github.com/plus-provenance/plus#";
	public static final String METADATA_NAMESPACE = BASE_NAMESPACE + "metadata";
	private static Logger log = Logger.getLogger(PROVConverter.class.getName());
	
	protected ProvFactory factory = null;
	protected Name name = null;
	
	// Store mappings from the PLUS OID of the original object, to its PROV-DM counterpart type.
	// This lets us look up later on what the connection is between two PLUSObjects and their related types.
	protected HashMap<String,Activity> provActivities = new HashMap<String,Activity>();
	protected HashMap<String,Entity> provEntities = new HashMap<String,Entity>();
	protected HashMap<String,Agent> provAgents = new HashMap<String,Agent>();
	protected HashMap<String,Statement> provStatements = new HashMap<String,Statement>();	
	
	protected static final String PLUSTYPE_PREF = "plustype";
	protected QualifiedName INVOCATION_TYPE = new org.openprovenance.prov.xml.QualifiedName(BASE_NAMESPACE + "PLUSInvocation", "invocation", PLUSTYPE_PREF);
	protected QualifiedName DATA_TYPE = new org.openprovenance.prov.xml.QualifiedName(BASE_NAMESPACE + "PLUSDataObject", "data", PLUSTYPE_PREF);
	
	/**
	 * Create a new converter object.
	 */
	public PROVConverter() { 
		factory = new ProvFactory();
		name = factory.getName();
	} 
	
	/**
	 * This is the main method that most callers should use.
	 * @param col a provenance collection
	 * @return a PROV-DM Document object, suitable for serialization and writing to multiple formats.
	 * @throws PLUSException
	 */
	public Document provenanceCollectionToPROV(ProvenanceCollection col) throws PLUSException {		
		for(PLUSActor a : col.getActors()) {
			provAgents.put(a.getId(), actorToAgent(a)); 
		}
		
		for(PLUSObject o : col.getNodes()) {
			HasOther item = null;
			
			if(o.isActivity()) {
				Activity a = activityToActivity(o);
				item = a;
				provActivities.put(o.getId(), a); 
			} else if(o.isWorkflow()) {
				Entity e = workflowToPlan(o);
				item = e;
				provEntities.put(o.getId(), e);
			} else if(o.isDataItem()) { 
				Entity e = dataObjectToEntity(o);
				item = e;
				provEntities.put(o.getId(), e); 
			} else if(o.isInvocation()) {
				Activity a = invocationToActivity(o);
				item = a;
				provActivities.put(o.getId(), a);
			} else { 
				log.warning("Don't know what this object is, skipping: " + o);
			}
			
			// Log properties common to all PLUSObjects.
			if(item != null) {
				convertMetadata(o, item);
				
				item.getOther().add(makeObjectProperty("name", o.getName()));
				item.getOther().add(makeObjectProperty("created", o.getCreatedAsDate()));
				item.getOther().add(makeObjectProperty("plus_type", o.getObjectType()));
				item.getOther().add(makeObjectProperty("plus_subtype", o.getObjectSubtype()));
				
				for(PrivilegeClass pc : o.getPrivileges().getPrivilegeSet()) { 
					item.getOther().add(makeObjectProperty("requires", pc.toString()));
				}
				
				for(String sgf : o.getSGFs()) {
					item.getOther().add(makeObjectProperty("hasSGF", sgf));
				}
			}
		}
		
		for(PLUSEdge e : col.getEdges()) {
			provStatements.put(e.getFrom().getId() + "/" + e.getTo().getId(), edgeToStatement(e)); 
		}
				
		if(col.countNPEs() > 0) 
			log.warning("Conversion of " + col + " - NPE conversion not yet supported.");
		
		// Assemble final document.
		Document d = factory.newDocument(provActivities.values(), provEntities.values(), provAgents.values(), provStatements.values());
		Namespace n = new Namespace();
		n.setDefaultNamespace(BASE_NAMESPACE);
		n.addKnownNamespaces();
		d.setNamespace(n);
		return d;
	} // End provenanceCollectionToPROV

	private boolean canConvert(PLUSEdge e, Object f, Object t) { 
		if(f == null || t == null) { 
			log.warning("Will not convert dangling edge " + e + 
					" with PROV-from " + (f == null ? "null" : f.getClass().getSimpleName()) + 
					" PROV-to " + (t == null ? "null" : t.getClass().getSimpleName()));
			return false;
		}
		
		return true;
	} // End canConvert
	
	public QualifiedName findEntityOrActivity(PLUSObject obj) { 
		if(provEntities.containsKey(obj.getId())) return provEntities.get(obj.getId()).getId();
		if(provActivities.containsKey(obj.getId())) return provActivities.get(obj.getId()).getId();
		return null;
	}
	
	public Statement edgeToStatement(PLUSEdge e) throws PROVConversionException { 
		String edgeType = e.getType();
		
		if(PLUSEdge.EDGE_TYPE_INPUT_TO.equals(edgeType)) {
			Activity act = provActivities.get(e.getTo().getId());
			Entity ent = provEntities.get(e.getFrom().getId());
			
			if(!canConvert(e, ent, act)) return null;
			
			return factory.newUsed(getQualifiedName(e), act.getId(), ent.getId());  
		} else if(PLUSEdge.EDGE_TYPE_TRIGGERED.equals(edgeType)) {
			Activity act1 = provActivities.get(e.getFrom().getId());
			Activity act2 = provActivities.get(e.getTo().getId());
			
			if(!canConvert(e, act1, act2)) return null;
			
			return factory.newWasInformedBy(getQualifiedName(e), act2.getId(), act1.getId());
		} else if(PLUSEdge.EDGE_TYPE_GENERATED.equals(edgeType)) {
			Entity ent = provEntities.get(e.getTo().getId());
			Activity act = provActivities.get(e.getFrom().getId());

			if(!canConvert(e, act, ent)) return null;
			
			return factory.newWasGeneratedBy(getQualifiedName(e), ent.getId(), act.getId());
		} else if(PLUSEdge.EDGE_TYPE_MARKS.equals(edgeType) || PLUSEdge.EDGE_TYPE_UNSPECIFIED.equals(edgeType)) {
			QualifiedName q1 = findEntityOrActivity(e.getFrom());
			QualifiedName q2 = findEntityOrActivity(e.getTo());
			
			if(!canConvert(e, q1, q2)) return null;
			
			return factory.newWasInformedBy(getQualifiedName(e), q2, q1);
		} else if(PLUSEdge.EDGE_TYPE_CONTRIBUTED.equals(edgeType)) {
			Entity e1 = provEntities.get(e.getFrom().getId());
			Entity e2 = provEntities.get(e.getTo().getId());

			if(!canConvert(e, e1, e2)) return null;
			
			return factory.newWasDerivedFrom(getQualifiedName(e), e2.getId(), e1.getId());
		} else { 
			log.warning("Don't understand edge " + e + " : skipping.");
			return null;
		}		
	} // End edgeToStatement
	
	public void convertMetadata(PLUSObject obj, HasOther convertedObj) throws PROVConversionException { 
		Metadata md = obj.getMetadata();
		
		// Ownership gets mapped onto a "wasAssociatedWith" relationship
		if(obj.getOwner() != null) {
			PLUSActor a = obj.getOwner();
			if(provAgents.containsKey(a.getId())) {
				Agent agent = provAgents.get(a.getId());
				QualifiedName ownership = new org.openprovenance.prov.xml.QualifiedName(BASE_NAMESPACE, 
						a.getId() + "/" + obj.getId(), "owns");						
				
				QualifiedName other = findEntityOrActivity(obj);
				
				if(other != null)
					provStatements.put(a.getId(), factory.newWasAssociatedWith(ownership, other, agent.getId()));
			} else { 
				log.warning("Owner of " + obj + " not in list of converted agents."); 
			}
		}
		
		for(String key : md.keySet()) { 
			convertedObj.getOther().add(
				factory.newOther(BASE_NAMESPACE, key, "metadata", md.get(key), name.XSD_STRING)
			);
		}
	}
	
	public Other makeObjectProperty(String name, Object value) {
		QualifiedName nameType = this.name.XSD_STRING;		
		
		if(value instanceof Date) {
			nameType = this.name.XSD_DATETIME;
			
			// Needs to be valid XML date format.
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			value = sdf.format((Date)value);
		} // End if
		
		if(value instanceof Integer) nameType = this.name.XSD_INTEGER;
		
		return factory.newOther(BASE_NAMESPACE, name, "prop", value, nameType);
	}
	
	public Entity workflowToPlan(PLUSObject obj) throws PROVConversionException { 		
		if(!obj.isWorkflow()) throw new PROVConversionException("Object is not a workflow: " + obj);
		PLUSWorkflow w = (PLUSWorkflow)obj;
		
		Entity e = factory.newEntity(getQualifiedName(w), obj.getName());		
		
		e.getOther().add(makeObjectProperty("when_start", w.getWhenStart()));
		e.getOther().add(makeObjectProperty("when_end", w.getWhenEnd()));
		
		return e;
	}
	
	public Activity activityToActivity(PLUSObject obj) throws PROVConversionException { 
		if(!obj.isActivity()) throw new PROVConversionException("Object is not an activity: " + obj);
		PLUSActivity act = (PLUSActivity)obj;
		Activity a = factory.newActivity(getQualifiedName(act), obj.getName());
		
		a.getOther().add(makeObjectProperty("inputs", act.getInputs()));
		a.getOther().add(makeObjectProperty("outputs", act.getOutputs()));

		return a;
	}
	
	public Activity invocationToActivity(PLUSObject obj) throws PROVConversionException { 
		if(!obj.isInvocation()) throw new PROVConversionException("Object is not an invocation: " + obj);
		PLUSInvocation inv = (PLUSInvocation)obj;
		Activity a = factory.newActivity(getQualifiedName(inv), obj.getName());		
		
		return a;
	}
	
	public Entity dataObjectToEntity(PLUSObject obj) throws PROVConversionException { 
		if(!obj.isDataItem()) throw new PROVConversionException("Object is not a data item: " + obj);		
		Entity e = factory.newEntity(getQualifiedName((PLUSDataObject)obj), obj.getName());
		// factory.addType(e, DATA_TYPE, name.XSD_QNAME);
		
		if(obj instanceof PLUSFile) { 
			PLUSFile f = (PLUSFile)obj;
			e.getOther().add(makeObjectProperty("path", f.getFile().getAbsolutePath()));
		} else if(obj instanceof PLUSURL) { 
			PLUSURL u = (PLUSURL) obj;
			try {
				e.getOther().add(makeObjectProperty("url", u.getURL()));
			} catch (MalformedURLException e1) {
				log.severe(e1.getMessage());
			}
		}
		
		return e;
	}
	
	public Agent actorToAgent(PLUSActor actor) throws PROVConversionException { 
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		// TODO attributes
		Agent a = factory.newAgent(getQualifiedName(actor), actor.getName());
		return a;
	}

	public QualifiedName getQualifiedName(PLUSEdge e) { 
		return new org.openprovenance.prov.xml.QualifiedName(BASE_NAMESPACE + e.getClass().getSimpleName(), 
				e.getFrom().getId() + "/" + e.getTo().getId(), 
				e.getType().replaceAll(" ", "_"));
	}
	
	public QualifiedName getQualifiedName(PLUSObject obj) { 
		String className = obj.getClass().getSimpleName();
		return new org.openprovenance.prov.xml.QualifiedName(BASE_NAMESPACE + className, 
				obj.getId(), 
				className.replaceAll("PLUS", "").toLowerCase());
	}
				
	public QualifiedName getQualifiedName(PLUSActor actor) { 
		return new org.openprovenance.prov.xml.QualifiedName(BASE_NAMESPACE + actor.getClass().getSimpleName(), actor.getId(), "actor");
	}
	
	public static void main(String [] args) throws Exception { 
		SyntheticGraphProperties props = new SyntheticGraphProperties().setConnectivity(0.5).setComponents(10).protectN(0).percentageData(0.5);
		DAGAholic col = new DAGAholic(props);
		
		Document d = new PROVConverter().provenanceCollectionToPROV(col);
		Namespace.withThreadNamespace(d.getNamespace());
		
		org.openprovenance.prov.xml.ProvSerialiser serializer = new org.openprovenance.prov.xml.ProvSerialiser();
		serializer.serialiseDocument(System.out, d, true);		
	}
	
	public static String asString(Document d) throws JAXBException { 
		org.openprovenance.prov.xml.ProvSerialiser serializer = new org.openprovenance.prov.xml.ProvSerialiser();
		StringWriter sw = new StringWriter();
		serializer.serialiseDocument(sw, d, true);
		return sw.toString();
	}
}
