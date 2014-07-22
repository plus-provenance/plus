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
package org.mitre.provenance.plusobject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.PropertyCapable;
import org.mitre.provenance.PropertySet;
import org.mitre.provenance.client.ProvenanceClient;
import org.mitre.provenance.dag.DAGPath;
import org.mitre.provenance.dag.LineageDAG;
import org.mitre.provenance.mediator.Mediator;
import org.mitre.provenance.surrogate.SignPost;
import org.mitre.provenance.surrogate.SurrogateGeneratingFunction;
import org.mitre.provenance.surrogate.Surrogateable;
import org.mitre.provenance.tools.PLUSUtils;
import org.mitre.provenance.user.PrivilegeSet;
import org.mitre.provenance.user.User;

/**
 * A generic PLUSObject. While this object provides lots of functionality, you
 * never should instantiate *just* a PLUSObject, but you should use the
 * subtypes. See the load() method and the extentLoaderMappings for details on
 * how this works.
 * <p>
 * PLUSObjects may be surrogates. (Check via the isSurrogate() method).
 * Surrogates cannot be written to the database, or loaded from the database,
 * they must be computed. (Check computeSurrogates())
 * <p>
 * All PLUSObjects have "types" and "subtypes". The major type would be
 * something like "workflow", "invocation", or "data". The subtype is used by
 * some subclass of PLUSObject to be more specific about what kind of object it
 * is. So you could have a subtype of "table" under the type "data". And so on.
 * Types are used to determine which subclass is best suited to load the
 * PLUSObject. (Check extentLoaderMappings for more details)
 * 
 * @author DMALLEN
 * 
 */
public class PLUSObject extends Surrogateable implements Comparable<Object>,
		PropertyCapable, Cloneable, SourcedObject {
	protected static Logger log = Logger.getLogger(PLUSObject.class.getName());
	
	public static final String PLUS_TYPE_WORKFLOW = "workflow";
	public static final String PLUS_TYPE_DATA = "data";
	public static final String PLUS_TYPE_ACTIVITY = "activity";
	public static final String PLUS_TYPE_HERITABLE = "heritable";
	public static final String PLUS_TYPE_INVOCATION = "invocation";
	public static final String PLUS_TYPE_GENERIC = "generic";

	/** The location of the running PLUS prototype. */
	public static final String PLUS_NAMESPACE = "http://denim.mitre.org/plus/";

	/** Unique identifier for a provenance object */	
	protected String id;
	
	/** Object's name */
	protected String name;
	
	/** Milliseconds since the epoch measurement of when the object was created */
	protected long created;
		
	/** Optional uncertainty (default 1.0) */
	protected Float uncertainty = (float) 1.0;
	
	/** A type identifier for what kind of provenance object this is */
	protected PLUSObjectType type;

	/** Source hints for this object */
	protected SignPost srcHints = SignPost.SRC_HINTS_LOCAL;
	
	/** Link to the PLUSActor who owns this */
	protected PLUSActor owner;

	/** List of privileges required to see this object */
	protected PrivilegeSet privileges;

	/** Metadata fields associated with the object */
	protected Metadata metadata;

	/** Create an empty blank PLUSObject. An ID will be automatically generated */
	public PLUSObject() {
		super();

		setName("Unnamed"); 
		setId(PLUSUtils.generateID());
		setCreated();
		metadata = new Metadata();
		type = new PLUSObjectType("object", "object");
		privileges = new PrivilegeSet();
	}

	/** Create a plus object with a particular name */
	public PLUSObject(String name) {
		this();
		type = new PLUSObjectType("unknown", "unknown");
		setName(name);
		privileges = new PrivilegeSet();
	} // End constructor

	/** Create a PLUSObject that is a copy of the other */
	public PLUSObject(PLUSObject other) {
		this();
		copy(other);
	}

	/**
	 * Copies all information from the specified PLUS object to this PLUS object, with
	 * one exception: type/subtype information is copied ONLY IF this object is lacking that
	 * information.
	 */
	public void copy(PLUSObject other) {
		setMetadata(other.getMetadata());
		setName(other.getName());
		setSGFs(other.getSGFs());
		setCreated(other.getCreated());
		setId(other.getId());
		setPrivileges(other.getPrivileges());
		setUncertainty(other.getUncertainty());
		setOwner(other.getOwner());		
		setSourceHints(other.getSourceHints());
		
		// TODO
		// WARN: Sometimes it's desirable to copy these fields, but remember that
		// it fundamentally changes what the object is.  So this has been changed
		// to copy type/subtype only when they're empty.
		if(getObjectType() == null) setObjectType(other.getObjectType());
		if(getObjectSubtype() == null) setObjectSubtype(other.getObjectSubtype());		
	} // End copy

	public String getId() { return id;	}
	public String getName() { return name;	}
	public Date getCreatedAsDate() { return new java.util.Date(created); }

	/** 
	 * Get the date/time when this object was created.
	 * @return Creation time, in milliseconds since the epoch. (midnight, January 1, 1970 UTC)
	 * @see PLUSObject#getCreatedAsDate()
	 */
	public long getCreated() { return created; }
	public Float getUncertainty() {	return uncertainty; }
	public String getObjectType() {	return type.getObjectType(); }
	public String getObjectSubtype() { return type.getObjectSubtype(); }
	
	/** @see SourcedObject#getSourceHints() */
	public SignPost getSourceHints() { return srcHints; } 
	
	/**
	 * Get the privilege set associated with this object, i.e. the privileges users must have in order to 
	 * access/see this object.
	 */
	public PrivilegeSet getPrivileges() {
		return privileges;
	}

	public Metadata getMetadata() { return metadata; } 
	public PLUSObjectType getType() { return type; 	}

	/** Returns the certainty of the PLUS object */
	public String getCertainty() {
		NumberFormat df = NumberFormat.getNumberInstance();
		df.setMaximumFractionDigits(2);
		return df.format(100.0 * uncertainty) + "%";
	}

	public void setMetadata(Metadata metadata) { this.metadata = metadata; }
	public void setName(String name) {	this.name = name; }
		
	public void setPrivileges(PrivilegeSet ps) { privileges = ps;	}
	public void setId(String id) {	this.id = id; }
	/** @see SourcedObject#setSourceHints(SignPost) */
	public void setSourceHints(SignPost sp) { srcHints = sp; }  	
	
	/** Set the object's created time, in milliseconds since the epoch */
	public void setCreated(long d) { created = d; }
	
	/**
	 * Sets the object's created timestamp to this moment in milliseconds since the epoch, UTC
	 */
	public void setCreated() { 
		Calendar calInitial = Calendar.getInstance();  
        int offsetInitial = calInitial.get(Calendar.ZONE_OFFSET)  
                + calInitial.get(Calendar.DST_OFFSET);  
  
        long current = System.currentTimeMillis();  
          
        // Check right time  
        created = current - offsetInitial;
	}

	public void setOwner(PLUSActor owner) {
		this.owner = owner;
	}

	protected void setType(PLUSObjectType type) { this.type = type; }
	protected void setObjectSubtype(String objectSubtype) { this.type.setObjectSubtype(objectSubtype); }
	protected void setObjectType(String objectType) { this.type.setObjectType(objectType);	}

	/** Set the uncertainty for the PLUS object */
	public void setUncertainty(Float uncertainty) {
		if(uncertainty == null) this.uncertainty = (float)1;
		else this.uncertainty = uncertainty > 1 ? 1 : uncertainty < 0 ? 0
				: uncertainty;
	}

	/**
	 * Determines whether the property or meaning of this node is "heritable" to
	 * others via FLING. Almost always, the answer is false.
	 */
	public boolean isHeritable() {
		return type.getObjectType().equals("heritable");
	}

	public boolean isDataItem() {
		return type.getObjectType().equals("data");
	}

	public boolean isActivity() {
		return type.getObjectType().equals("activity");
	}

	public boolean isInvocation() {
		return type.getObjectType().equals("invocation");
	}

	public boolean isWorkflow() {
		return type.getObjectType().equals("workflow");
	}

	/**
	 * Get the PLUSActor who is considered the owner of this object.
	 * @return the PLUSActor, or null if there is no known/identified actor.
	 */
	public PLUSActor getOwner() {
		return owner;
		/*
		String aid = getOwnerId();
		if (aid == null || "".equals(aid))
			return null;

		log.finest("PLUSObject#getOwner: Loading actor " + getOwnerId() + " from database");

		try { 
			Node n = Neo4JStorage.exists(this);
			if(n == null) throw new PLUSException(toString() + " hasn't been written yet");
			
			// Assumption: only one actor owns each relationship.  This will throw an exception
			// if that turns out to be wrong. 
			Relationship r = n.getSingleRelationship(Neo4JStorage.OWNS, Direction.INCOMING);
						
			if(r == null) { 
				log.warning("Can't load PLUSActor because relationship is null, but aid=" + aid);
				return null;
			} else if(r.getStartNode() == null) { 
				log.warning("Can't load PLUSActor because OWNS relationship start node is null");
				return null;
			}
			
			PLUSActor owner = Neo4JPLUSObjectFactory.newActor(r.getStartNode());
			
			if(owner == null) { 
				log.warning("After loading node " + r.getStartNode() + " PLUSActor is null");
				return null;
			}
			
			// Added for integrity checking, but this should never happen.  Paranoid coding.
			if(!aid.equals(owner.getId())) 
				throw new PLUSException("Found actor " + owner + " doesn't match claimed " + aid);
			
			return owner;						
		} catch (Exception e) {
			log.severe("Failed to load actor: " + e.getMessage());
			return null;
		}
		*/
	} // End getOwner

	/**
	 * Given a class, return a new instance object using the reflection API
	 * 
	 * @param c the fully qualified name of the SGF class (i.e. org.mitre.provenance.surrogate.BlahBlahBlah)
	 * @return a new instance of that class.
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 * @throws Exception
	 */
	private SurrogateGeneratingFunction getSGFInstance(String c) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		// log.info("getSGFInstance:  " + c);
		Class<?> clazz = Class.forName(c);
		
		if(!SurrogateGeneratingFunction.class.isAssignableFrom(clazz)) {
			log.severe("SGF isn't assignable from " + c + "!");
		}
		
		SurrogateGeneratingFunction f = (SurrogateGeneratingFunction) clazz.newInstance();

		return f;
	} // End getSGFInstance

	public PLUSObject clone() {
		PLUSObject c = new PLUSObject(this);
		return c;
	} // End clone

	/**
	 * Gets a version of this object that is suitable for a given user.
	 * The mediator may be invoked if more than one SGF is present, to choose the best available choice.
	 * @see org.mitre.provenance.mediator.Mediator
	 * @param user the user asking to see the object.
	 * @return the object itself (if the user is permitted to see it) a suitable surrogate (if applicable) 
	 * or null (if the user is not permitted to see this object)
	 * @throws PLUSException if user is null
	 */
	public PLUSObject getVersionSuitableFor(User user) throws PLUSException { 
		if(user == null) throw new PLUSException("Must specify user!");
		
		if(ProvenanceClient.instance.canSee(user, this)) return this;
		List<PLUSObject> surrogates = computeSurrogates(user);
		
		if(surrogates == null || surrogates.size() == 0) {
			log.info("No suitable surrogates of " + getId() + " for " + user.getName());
			return null;
		}
		
		Mediator m = new Mediator();
		return m.getMostPreferable(surrogates);
	}
	
	/**
	 * Use the SGFs attached to this object and generate relevant surrogates.
	 * 
	 * @return an array of PLUSObjects that are suitable surrogates for u.
	 * @throws PLUSException
	 */
	public ArrayList<PLUSObject> computeSurrogates(User u) throws PLUSException {
		ArrayList<PLUSObject> surrogates = new ArrayList<PLUSObject>();

		// log.debug("***** Computing surrogates for " + this);
		System.out.flush();

		if (sgfs == null) { 
			log.severe("computeSurrogates: No SGFs!");
			sgfs = new ArrayList<String>();
		}

		for (int x = 0; x < sgfs.size(); x++) {
			String c = sgfs.get(x);
			try {
				// log.debug("Computing surrogate for " + getName() +
				// " with " + c); System.out.flush();
				SurrogateGeneratingFunction f = getSGFInstance(c);

				if (!f.isRunnable(this)) {
					log.warning("Skipping SGF " + f + " because it isn't ready.");
					continue;
				}

				PLUSObject surrogate = f.generateSurrogate(this, u);

				if (surrogate == null) {
					log.warning("SGF returned null!  Skipping...");
					continue;
				}

				// It's important that the surrogate has the same ID as the
				// object that it's
				// standing in for. Otherwise BLING and FLING on a surrogate
				// won't work.
				if (!getId().equals(surrogate.getId())) {
					log.warning("Surrogate function " + c + " did not properly set surrogate ID - force fixing.");
					surrogate.setId(getId());
				} // End if

				if (!surrogate.isSurrogate()) {
					System.err.println("Surrogate function "
									+ c
									+ " did not properly mark its result as a surrogate "
									+ "on item " + getName() + "/" + getId());
					throw new PLUSException("SGF " + f
							+ " returned a non-surrogate!");
				} // End if

				if (!this.getType().compatibleWith(surrogate.getType())) {					
					// log.warning("WARNING: Surrogate function " + c
					//		+ " returned an incompatible type on item "
					//		+ getName() + "/" + getId() + "!  Original type was " + this.getType());
					// TODO Should this throw an exception or not??? Sample SGF
					// functions actually do
					// this, so be prepared to go fix them...
					// throw new PLUSException("SGF " + f +
					// " returned incompatible type!");
				} // End if

				surrogates.add(surrogate);
			} catch (Exception e) {
				log.severe("PLUSObject#computeSurrogate: surrogate function threw: " + e.getMessage());				
				throw new PLUSException("Error computing surrogate: " + e, e);
			} // End catch
		} // End for

		return surrogates;
	} // End getSurrogates

	/**
	 * Determine whether this PLUSObject is reachable from another PLUSObject
	 * within a particular DAG.
	 * 
	 * @param otherNode
	 *            the other object
	 * @param dag
	 *            the DAG to use as context
	 * @return true if there is a FLING path from otherNode to this object
	 *         within the given DAG. False otherwise.
	 * @throws PLUSException
	 */
	public boolean reachableFrom(PLUSObject otherNode, LineageDAG dag) {
		return DAGPath.pathExists(dag, otherNode, this);
	} // End reachableFrom

	/**
	 * Determine whether this PLUSObject can reach another PLUSObject within a
	 * particular DAG.
	 * 
	 * @param otherNode
	 *            the other object
	 * @param dag
	 *            the DAG to use as context
	 * @return true if there is a FLING path from this node to otherNode within
	 *         a given DAG. False otherwise.
	 */
	public boolean canReach(PLUSObject otherNode, LineageDAG dag) {
		return DAGPath.pathExists(dag, this, otherNode);
	}
	
	public boolean createdBefore(PLUSObject other) {
		return getCreated() < other.getCreated();
	}

	public boolean createdAfter(PLUSObject other) {
		return getCreated() > other.getCreated();
	}

	public String toString() {
		String foo = new String(getName() + "/" + getUncertainty() + "/"
				+ getObjectType() + "/"
				+ getObjectSubtype());
		if (isSurrogate())
			return "SURROGATE: " + foo;
		else
			return foo;
	} // End toString

	/**
	 * Get a URI string that uniquely identifies this object.
	 * 
	 * @return a URI
	 */
	public String getURI(String contextPath) {
		return PLUSObject.oidToURI(getId(), contextPath);
	}

	public int compareTo(Object arg0) {
		return toString().compareTo("" + toString());
	} // End compareTo

	/**
	 * Given a unique ID, return the URL where it can be located. This is
	 * install-specific.
	 * 
	 * @param oid
	 *            unique ID of a PLUSObject
	 * @return a URL where it can be found.
	 * @see PLUSUtils#NAMESPACE
	 */
	public static String oidToURI(String oid, String contextPath) {
		return contextPath + "/graph/graphPage.jsp?oid=" + oid;
	}

	public Map<String,Object> getStorableProperties() {
		HashMap<String,Object> m = new HashMap<String,Object>();
		m.put("oid", getId());
		m.put("name", getName());
		m.put("created", getCreated());
		
		m.put("ownerid", null);
		
		if(getOwner() != null) {
			m.put("ownerid", getOwner().getId());
		}
				
		m.put("type", getObjectType());
		m.put("subtype", getObjectSubtype());
		m.put("certainty", getCertainty());
				
		if(sgfs != null && sgfs.size() > 0) {
			String [] sgfClassNames = new String[sgfs.size()];
			
			for(int x=0; x<sgfs.size(); x++) { 
				sgfClassNames[x] = sgfs.get(x);
			}
			
			m.put("SGFs", sgfClassNames);
		}
		
		return m;
	}
	
	public PLUSObject setProperties(PropertySet props, ProvenanceCollection contextCollection) throws PLUSException {
		String [] requiredProps = new String [] { "oid", "name", "type", "subtype" };
		
		for(int x=0; x<requiredProps.length; x++) {
			if(props.getProperty(requiredProps[x], null) == null) 
				throw new PLUSException("Required property " + requiredProps[x] + " missing");
		}
		
		setId(""+props.getProperty("oid"));
		setName(""+props.getProperty("name"));
		setCreated((Long)props.getProperty("created"));
		
		String aid = (String)props.getProperty("ownerid", null);
		if(aid != null && !"".equals(aid) && !"null".equals(aid)) {
			if(contextCollection != null && contextCollection.containsActorID(aid))
				setOwner(contextCollection.getActor(aid));
			else {
				log.severe("Cannot set owner for object: aid " + aid + " isn't in the context collection.");
				setOwner(null);
			}
		} else setOwner(null);
				
		setObjectType((String)props.getProperty("type", null));
		setObjectSubtype((String)props.getProperty("subtype", null));
		
		Object unc = props.getProperty("uncertainty", null);
		String sunc = ""+unc;
		if(unc == null || "null".equals(sunc) || "".equals(sunc)) setUncertainty(null);
		else setUncertainty(Float.parseFloat(sunc));
				
		String[] sgfs = (String[])props.getProperty("SGFs", null);
		if(sgfs != null) { 
			for(String className : sgfs) { 
				try { 
					SurrogateGeneratingFunction sgf = getSGFInstance(className);
					useSurrogateComputation(sgf);
				} catch(ClassNotFoundException cnfe) {
					log.severe("No such class " + className);
					throw new PLUSException("Missing/bad SGF " + className, cnfe);
				} catch(Exception exc) { 
					log.severe("Couldn't instantiate SGF " + className + ": " + exc.getMessage());
				}						 
			}
		}

		Metadata m = new Metadata();		
		String tok = "metadata:";
		
		for(String k : props.getPropertyKeys()) { 
			if(k.indexOf(tok) >= 0)
				m.put(k.substring(tok.length()), props.getProperty(k));
		} // End for
		
		setMetadata(m);
		return this;
	}
} // End PLUSObject
