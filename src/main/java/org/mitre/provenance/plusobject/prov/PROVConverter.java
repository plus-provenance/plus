package org.mitre.provenance.plusobject.prov;

import java.io.StringWriter;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.dag.TraversalSettings;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.npe.NonProvenanceEdge;
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
import org.mitre.provenance.tools.PLUSUtils;
import org.mitre.provenance.user.PrivilegeClass;
import org.mitre.provenance.user.User;
import org.openprovenance.prov.model.Activity;
import org.openprovenance.prov.model.Agent;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.Entity;
import org.openprovenance.prov.model.HasOther;
import org.openprovenance.prov.model.Name;
import org.openprovenance.prov.model.Namespace;
import org.openprovenance.prov.model.Other;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.model.Statement;
import org.openprovenance.prov.model.Used;
import org.openprovenance.prov.model.WasDerivedFrom;
import org.openprovenance.prov.model.WasGeneratedBy;
import org.openprovenance.prov.model.WasInformedBy;
import org.openprovenance.prov.xml.ProvFactory;

/**
 * A class that knows how to convert ProvenanceCollection objects into a PROV-DM representation. 
 * 
 * <p>This code follows mappings provided in the MAPPINGS-PLUS-TO-PROV-DM.txt file found in the source
 * distribution.  Without consulting that file, the mappings in this source code likely won't make much 
 * sense.
 * 
 * <p>This is an initial cut - it is still in need of substantial development, testing, and verification.
 * 
 * <p><strong>Warning</strong>: the PROV API doesn't do good input checking when you use the factory to 
 * create object instances.  Because the model objects need to get serialized several different ways, 
 * this creates situations where the model is created just fine, but when you go to serialize, you get
 * various exceptions due to invalid data.   PLUS data objects don't exist in the W3C/XML space, and so
 * when we do this translation we're creating artifacts like QNames that don't natively exist in PLUS.
 * Beware situations where data in PLUS translates into invalid XML NCNames, anyURI, and so on.  This
 * won't be caught by the PROV API, but will cause failure to serialize.  
 * 
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
			// System.out.println(a);
			Agent agent = actorToAgent(a);
			agent.getOther().add(makeObjectProperty("created", a.getCreatedAsDate()));
			provAgents.put(a.getId(), agent); 			
		}
		
		for(PLUSObject o : col.getNodes()) {
			HasOther item = null;
			// System.out.println(o);
			
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
				convertOwnership(o);
								
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
				
				// System.out.println(item);
			}
		}
		
		for(PLUSEdge e : col.getEdges()) {
			// System.out.println(e);
			Statement stmt = edgeToStatement(e);
			provStatements.put(e.getFrom().getId() + "/" + e.getTo().getId(), stmt); 
		}
				
		for(NonProvenanceEdge npe : col.getNonProvenanceEdges()) {
			// System.out.println(npe);
			String oid = npe.getIncidentOID();
			String npid = npe.getIncidentForeignID();
			String type = npe.getType();
			
			if(PLUSUtils.isPLUSOID(npid)) {
				log.warning("NPEs connecting two PLUSObjects are not yet supported: " + npe);
				continue;
			}
			
			PLUSObject node = col.getNode(oid);
			if(node == null) {
				log.warning("NPE " + npe + " references OID which isn't in collection; skipping");
				continue;
			}
			
			HasOther o = getHasOther(node);
			if(o == null) {
				log.warning("NPE " + npe + " references HasOther which wasn't created; skipping");
				continue;
			}
			
			makeObjectProperty(type, npid, "npe");
		} // End for
		
		// Assemble final document.
		Document d = factory.newDocument(provActivities.values(), provEntities.values(), provAgents.values(), provStatements.values());		
		Namespace n = Namespace.gatherNamespaces(d);
		n.addKnownNamespaces();
		n.setDefaultNamespace(BASE_NAMESPACE);
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
	
	public HasOther getHasOther(PLUSObject obj) { 
		if(provEntities.containsKey(obj.getId())) return provEntities.get(obj.getId());
		if(provActivities.containsKey(obj.getId())) return provActivities.get(obj.getId());
		return null;
	}
	
	public QualifiedName findEntityOrActivity(PLUSObject obj) { 
		if(provEntities.containsKey(obj.getId())) return provEntities.get(obj.getId()).getId();
		if(provActivities.containsKey(obj.getId())) return provActivities.get(obj.getId()).getId();
		return null;
	}
	
	protected Statement edgeToStatement(PLUSEdge e) throws PROVConversionException { 
		String edgeType = e.getType();
		
		if(PLUSEdge.EDGE_TYPE_INPUT_TO.equals(edgeType)) {
			Activity act = provActivities.get(e.getTo().getId());
			Entity ent = provEntities.get(e.getFrom().getId());
			
			if(!canConvert(e, ent, act)) return null;
			
			Used u = factory.newUsed(getQualifiedName(e), act.getId(), ent.getId());
			// System.out.println(u);
			return u;
		} else if(PLUSEdge.EDGE_TYPE_TRIGGERED.equals(edgeType)) {
			Activity act1 = provActivities.get(e.getFrom().getId());
			Activity act2 = provActivities.get(e.getTo().getId());
			
			if(!canConvert(e, act1, act2)) return null;
			
			WasInformedBy wib = factory.newWasInformedBy(getQualifiedName(e), act2.getId(), act1.getId());
			// System.out.println(wib);
			return wib;
		} else if(PLUSEdge.EDGE_TYPE_GENERATED.equals(edgeType)) {
			Entity ent = provEntities.get(e.getTo().getId());
			Activity act = provActivities.get(e.getFrom().getId());

			if(!canConvert(e, act, ent)) return null;
						
			WasGeneratedBy wgb = factory.newWasGeneratedBy(getQualifiedName(e), ent.getId(), act.getId());
			//wgb.setTime(factory.newTime(e.getTo().getCreatedAsDate()));
			//System.out.println(wgb);
			return wgb;
		} else if(PLUSEdge.EDGE_TYPE_MARKS.equals(edgeType) || PLUSEdge.EDGE_TYPE_UNSPECIFIED.equals(edgeType)) {
			QualifiedName q1 = findEntityOrActivity(e.getFrom());
			QualifiedName q2 = findEntityOrActivity(e.getTo());
			
			if(!canConvert(e, q1, q2)) return null;
			
			WasInformedBy wib = factory.newWasInformedBy(getQualifiedName(e), q2, q1);
			//System.out.println(wib);
			return wib;
		} else if(PLUSEdge.EDGE_TYPE_CONTRIBUTED.equals(edgeType)) {
			Entity e1 = provEntities.get(e.getFrom().getId());
			Entity e2 = provEntities.get(e.getTo().getId());

			if(!canConvert(e, e1, e2)) return null;
			
			WasDerivedFrom wdf = factory.newWasDerivedFrom(getQualifiedName(e), e2.getId(), e1.getId());
			//System.out.println(wdf);
			return wdf;
		} else { 
			log.warning("Don't understand edge " + e + " : skipping.");
			return null;
		}		
	} // End edgeToStatement
	
	/**
	 * If applicable, convert the ownership relationship between an object and its actor into a "wasAssociatedWith" statement,
	 * or a "wasAttributedTo" statement, depending on the type of object.
	 * @param o
	 */
	protected void convertOwnership(PLUSObject o) { 
		// Ownership gets mapped onto a "wasAssociatedWith" or "wasAttributedTo" relationship
		if(o.getOwner() != null) {
			PLUSActor a = o.getOwner();
			if(provAgents.containsKey(a.getId())) {
				Agent agent = provAgents.get(a.getId());
				QualifiedName ownership = new org.openprovenance.prov.xml.QualifiedName(BASE_NAMESPACE, 
						a.getId() + "/" + o.getId(), "owns");						
				
				QualifiedName other = findEntityOrActivity(o);
				
				if(other != null && provEntities.containsKey(o.getId())) {
					// Entities are related to agents via "wasAttributedTo"
					provStatements.put(a.getId(), factory.newWasAttributedTo(ownership, other, agent.getId()));
				} else if(other != null && provActivities.containsKey(o.getId())) {
					provStatements.put(a.getId(), factory.newWasAssociatedWith(ownership, other, agent.getId()));					
				} else { 
					log.warning("Could not find appropriate owner for other " + other + " on " + o);
				}
			} else { 
				log.warning("Owner of " + o + " not in list of converted agents."); 
			}
		}
	} // End convertOwnership
	
	protected void convertMetadata(PLUSObject obj, HasOther convertedObj) throws PROVConversionException { 
		Metadata md = obj.getMetadata();
				
		for(String key : md.keySet()) {
			String val = ""+md.get(key);
						
			// Sometimes metadata key names can contain invalid XML characters that cause syntax errors, because
			// the PROV library doesn't check for this.
			String local = key.replaceAll("[^A-Za-z0-9]", "_");
			if(!key.equals(local)) { 
				// System.out.println("METADATA: '" + key + "' '" + val + "'" + " local '" + local + "'");
			}
			
			Other o = factory.newOther(BASE_NAMESPACE, local, "metadata", val, name.XSD_STRING);		
			convertedObj.getOther().add(o);
		}
	}
	
	protected Other makeObjectProperty(String name, Object value) { 
		return makeObjectProperty(name, value, "prop");
	}
	
	/**
	 * Make a single object property into an "Other" statement.
	 * @param name property name
	 * @param value property value
	 * @param prefix ns prefix
	 * @return
	 */
	protected Other makeObjectProperty(String name, Object value, String prefix) {
		QualifiedName nameType = this.name.XSD_STRING;		
		
		if(value instanceof Date) {
			nameType = this.name.XSD_DATETIME;
			
			// Needs to be valid XML date format.
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			value = sdf.format((Date)value);
		} else if(value instanceof Integer) {
			nameType = this.name.XSD_INTEGER;
		} else if(value instanceof Double) {
			nameType = this.name.XSD_DOUBLE;
		} else {
			value = ""+value;						
		}
		
		// if("npe".equals(prefix)) System.out.println("OBJECT PROPERTY: " + name + " " + value + " " + prefix + " " + nameType);
		return factory.newOther(BASE_NAMESPACE, name, prefix, value, nameType);
	} // End makeObjectProperty
		
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
	} // End dataObjectToEntity
	
	public Agent actorToAgent(PLUSActor actor) throws PROVConversionException { 
		// TODO attributes
		Agent a = factory.newAgent(getQualifiedName(actor), actor.getName());
		return a;
	}

	public QualifiedName getQualifiedName(PLUSEdge e) { 
		return new org.openprovenance.prov.xml.QualifiedName(BASE_NAMESPACE + e.getClass().getSimpleName(), 
				e.getFrom().getId() + ":" + e.getTo().getId(), 
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
		String oid = "urn:uuid:mitre:plus:bf894a51-3f5e-4134-ba62-bff5b24cd19a";
		
		ProvenanceCollection col = null;
		
		if(oid != null) { 
			col = Neo4JPLUSObjectFactory.newDAG(oid, 
				User.DEFAULT_USER_GOD, new TraversalSettings());
		} else {
			SyntheticGraphProperties props = new SyntheticGraphProperties().setConnectivity(0.5).setComponents(10).protectN(0).percentageData(0.5);
			col = new DAGAholic(props);			
		}
		
		Document d = new PROVConverter().provenanceCollectionToPROV(col);
		Namespace.withThreadNamespace(d.getNamespace());
		
		org.openprovenance.prov.xml.ProvSerialiser serializer = new org.openprovenance.prov.xml.ProvSerialiser();
		serializer.serialiseDocument(System.out, d, true);
	}
	
	/**
	 * Serialize a document as XML, and then turn it into one large string.
	 * @param d a document
	 * @return an XML serialized form of the document.
	 * @throws JAXBException
	 */
	public static String asXMLString(Document d) throws JAXBException { 
		Namespace.withThreadNamespace(d.getNamespace());
		org.openprovenance.prov.xml.ProvSerialiser serializer = new org.openprovenance.prov.xml.ProvSerialiser();
		StringWriter sw = new StringWriter();
		serializer.serialiseDocument(sw, d, true);
		return sw.toString();
	}
} // End PROVConverter