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
package org.mitre.provenance.plusobject.builder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.mitre.provenance.Metadata;
import org.mitre.provenance.PLUSException;
import org.mitre.provenance.contenthash.ContentHasher;
import org.mitre.provenance.contenthash.SHA256ContentHasher;
import org.mitre.provenance.dag.ViewedCollection;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.npe.NonProvenanceEdge;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSEdge;
import org.mitre.provenance.plusobject.PLUSInvocation;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSString;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.user.User;

/**
 * A tool to help build provenance graphs more easily, and to make code to do so more readable.
 * For example, to create a simple two-node graph that shows a data item input to a process, this code can be used:
 * <p><pre>new ProvBuilder().link(db.dataNamed("Data input"), db.invocationNamed("Process Foo"))</pre>
 * <p>Most types return a ProvBuilder for easy method chaining.   Almost all methods do <strong>not</strong> modify the state
 * of the current object, they return a new object.
 * 
 *  <p>Below is some sample code that illustrates building a simple workflow.   Let's say that we want to model 
 *  two pieces of data ("X" and "Y" with values "3" and "4" respectively) being fed into an "addition" process, resulting in 
 *  a piece of data "Z" (value 7).  That code might look like this:
 *  <pre>
 *  ProvBuilder pb = new ProvBuilder(new PLUSWorkflow("Simple Addition"), null);
 *  
 *  ProvBuilder inputs = new ProvBuilder(
 *     pb.newNodeNamed("X", "value", "3"),
 *     pb.newNodeNamed("Y", "value", "4"));
 *     
 *  ProvBuilder process = pb.newInvocationNamed("Addition");
 *  ProvBuilder output = pb.newNodeNamed("Z", "value", "7");
 *  return pb.merge(pb.link(inputs, process), pb.link(process, output));
 * </pre>
 *
 *  A number of methods take an arbitrary number of Strings called "metadataKeyValuePairs".   Java doesn't permit
 *  static initialization of maps, so this is how some basic metadata can be specified in creating a new object.  For example,
 *  if you call newDataNamed("Foo", "X", "hello", "Y", "world") then you'll get an object named Foo, with two metadata pairs:
 *  X=hello, and Y=world.
 * @author moxious
 */
public class ProvBuilder extends ViewedCollection {
	protected PLUSWorkflow wf = PLUSWorkflow.DEFAULT_WORKFLOW;	
	protected ContentHasher hasher = null;

	/** Create a new builder object with the default workflow, viewed by User PUBLIC. */
	public ProvBuilder() {
		this(PLUSWorkflow.DEFAULT_WORKFLOW, User.PUBLIC);		
	}
		
	/**
	 * Create a new ProvBuilder object connected to a specified workflow and user "viewer".
	 * @param wf 
	 * @param viewer
	 * @throws PLUSException
	 */
	public ProvBuilder(PLUSWorkflow wf, User viewer, PLUSObject ...objs) {
		super(viewer, objs);
		
		if(wf == null) wf = PLUSWorkflow.DEFAULT_WORKFLOW;		
		
		this.wf = wf;		
		
		if(wf != PLUSWorkflow.DEFAULT_WORKFLOW) addNode(wf);
		
		try { hasher = new SHA256ContentHasher(); }
		catch(Exception exc) { ; } 
	} // End ProvBuilder
	
	/**
	 * Create a new builder as the union of all of the arguments.
	 * @param builders
	 */
	public ProvBuilder(ProvBuilder ... builders) { 
		this();		
		merge(builders);
	}
	
	/**
	 * Create a new builder containing all of the arguments.
	 * @param items
	 */
	public ProvBuilder(PLUSObject ... items) {
		this(null, null, items);		
	}
	
	/**
	 * Utility method to generate a new random SHA-256 hash 
	 * @return a String representing a hex-encoded, random SHA-256 hash.
	 * @throws IOException
	 */
	protected String generateHash() throws IOException { 
		String uuid = UUID.randomUUID().toString();		
		return ContentHasher.formatAsHexString(hasher.hash(new ByteArrayInputStream(uuid.getBytes())));
	}
		
	/**
	 * @return the user that is the "viewer" of this builder
	 */
	public User getUser() { return viewer; } 
	
	/**
	 * Modify the user that is the viewer of this builder.
	 * @param viewer
	 * @return the same builder
	 */
	public ProvBuilder setUser(User viewer) { this.viewer = viewer; return this; } 
	
	/**
	 * Combine all the provided builders into the current one, modifying the current one.
	 * @param builders
	 * @return the same builder
	 */
	public ProvBuilder merge(ProvBuilder ... builders) {
		for(ProvBuilder db : builders) addAll(db);
		return this;
	}
	
	/**
	 * Link the first object found under headName with the first object found under tailName
	 * @param headName
	 * @param tailName
	 * @return a new ProvBuilder containing the linkage
	 * @throws PLUSException
	 */
	public ProvBuilder link(String headName, String tailName) throws PLUSException { 
		ProvBuilder head = nodeNamed(headName);
		ProvBuilder tail = nodeNamed(tailName);
		
		return link(head, tail); 
	}
		
	/**
	 * Return the first node in this collection.  This is primarily useful when the collection is a holder of a singleton;
	 * for more fine-grained control over which node to get, use other methods inherited from ProvenanceCollection.
	 * @return the very first node in the collection.
	 */
	public PLUSObject singleNode() {
		if(countNodes() > 1) log.warning("Collection has more than one node!");
		return getNodes().iterator().next();
	}
	
	/**
	 * Same as excise(items, false)
	 * @see ProvBuilder#excise(ProvBuilder, boolean)
	 * @param items the items you want removed
	 * @return a new builder
	 */
	public ProvBuilder excise(ProvBuilder items) { 
		return excise(items, false);
	}
	
	/**
	 * Remove all items found in one builder from this, and returns a new builder.  Removes all edges incident to nodes
	 * being removed.
	 * @param items the list of items you want removed
	 * @param removeWorkflows if true, workflow nodes will be removed as well.  If false, they won't be.
	 * @return new declarative builder
	 */
	public ProvBuilder excise(ProvBuilder items, boolean removeWorkflows) {
		ProvBuilder db = new ProvBuilder(this);
			
		for(PLUSEdge e : items.getEdges()) {
			db.removeEdge(e);
		}
		
		for(PLUSObject o : items.getNodes()) {
			if(o.isWorkflow() && !removeWorkflows) continue;
			
			// System.out.println("Removing " + o); 
			db.removeNode(o, true);  // Remove incident edges too.
		}
	
		for(PLUSActor a : items.getActors()) {
			db.removeActor(a);
		}
		
		for(NonProvenanceEdge npe : items.getNonProvenanceEdges()) {
			db.removeNonProvenanceEdge(npe);
		} 
		
		return db;
	} // End excise
	
	/**
	 * Returns a new builder containing only those edges that go between nodes named headName, and nodes named tailName.
	 * @param headName
	 * @param tailName
	 * @return a new builder containing the results
	 * @throws PLUSException
	 */
	public ProvBuilder findLinks(String headName, String tailName) throws PLUSException { 
		ProvBuilder db = new ProvBuilder(wf, viewer);
		
		PLUSObject head = null;
		PLUSObject tail = null;
		
		for(PLUSObject o : getNodes()) {
			if(o.getName().equals(headName)) head = o;
			else if(o.getName().equals(tailName)) tail = o;
			
			if(head != null && tail != null) break;
		}
		
		PLUSEdge e = getEdge(head, tail);
		
		db.addEdge(e); 
		return db;
	} // End findLinks
	
	/**
	 * Create new PLUSEdges between all nodes of heads and all nodes of tails.
	 * @param heads
	 * @param tails
	 * @return this
	 */
	public ProvBuilder link(ProvenanceCollection heads, ProvenanceCollection tails) throws PLUSException {
		if(heads == null || heads.countNodes() <= 0) throw new PLUSException("Heads must be non-empty and non-null.");
		if(tails == null || tails.countNodes() <= 0) throw new PLUSException("Tails must be non-empty and non-null."); 
		
		ProvBuilder db = new ProvBuilder(getWorkflow(), this.viewer);
		
		for(PLUSObject head : heads.getNodes()) {			
			db.addNode(head);
			
			if(head.isWorkflow()) continue;
			
			for(PLUSObject tail : tails.getNodes()) {
				db.addNode(tail);
				
				if(tail.isWorkflow()) continue;
				
				db.addEdge(new PLUSEdge(head, tail, wf));
			}
		}
			
		return db;
	} // End link
	
	/**
	 * Find nodes whose names match a patter, and return that as a new builder.   
	 * @param pattern the name pattern.
	 * @return a ProvBuilder with zero or more nodes
	 */
	public ProvBuilder nodesMatching(String pattern) { 
		ProvBuilder db = new ProvBuilder();
		
		for(PLUSObject o : getNodes()) { 
			if(o.getName().matches(pattern)) { db.addNode(o); } 
		}
		
		return db;
	} // End nodesMatching
	
	/**
	 * @return a new provenance builder with the same settings as this one, but empty.  This does not modify the object it operates on.
	 */
	public ProvBuilder newEmpty() { 
		return new ProvBuilder(getWorkflow(), getViewer());
	}
	
	/**
	 * Find the most recent node (by creation date) and return a new builder containing only that node.
	 * If there are no nodes, the result will be empty.
	 * @return a ProvBuilder
	 */
	public ProvBuilder mostRecent() {
		List<PLUSObject> l = getNodesInOrderedList();
		if(l.size() > 0) return new ProvBuilder(null, getViewer(), l.get(0));
		return new ProvBuilder(null, getViewer());
	}
	
	/**
	 * Find a node with the given name in the current ProvBuilder, and return a new builder containing only that node.
	 * If the node cannot be found, the result will be empty.   This will return the first result only.  So if there is more than
	 * one node with the same name, only the first will be returned.
	 * @param name the name of the node you're looking for.
	 * @return a ProvBuilder containing one or zero nodes.
	 * @see ProvBuilder#nodesMatching(String)
	 */
	public ProvBuilder nodeNamed(String name) {		
		ProvBuilder db = new ProvBuilder(null, getViewer());
		
		for(PLUSObject o : getNodes()) {
			if(o.getName().equals(name)) { db.addNode(o); break; } 
		}
		
		return db;
	}	
	
	/**
	 * Create a new actor in the local database, and return a builder containing that actor.
	 * @param name name of the actor.
	 * @return a new builder containing the actor
	 * @throws PLUSException
	 */
	public ProvBuilder newActorNamed(String name) throws PLUSException {
		ProvBuilder db = newEmpty();
		PLUSActor a = Neo4JPLUSObjectFactory.getActor(name, true);
		db.addActor(a);
		return db;
	}
	
	/**
	 * Returns the workflow associated with this builder, which may be null if it was not needed or specified in the constructor.
	 * @return a PLUSWorkflow or null.
	 */
	public PLUSWorkflow getWorkflow() { return wf; } 
	
	public ProvBuilder newWorkflowNamed(String name) { 
		return newWorkflowNamed(name, null); 
	}

	public ProvBuilder newWorkflowNamed(String name, PLUSActor owner) { 
		ProvBuilder db = newEmpty();
		PLUSWorkflow w = new PLUSWorkflow();
		w.setName(name);
		if(owner != null) { w.setOwner(owner); db.addActor(owner); } 
		db.addNode(w);
		return db;
	}
	
	/**
	 * Create a new data item with the given name.
	 * @param name
	 * @return a new builder containing the data.
	 */
	public ProvBuilder newDataNamed(String name, String ...metadataKeyValuePairs) { 
		return newDataNamed(name, null, metadataKeyValuePairs); 
	}
	
	/**
	 * Create a new data item with the given name and owner.
	 * @param name
	 * @param owner
	 * @return a new builder containing the data specified.
	 */
	public ProvBuilder newDataNamed(String name, PLUSActor owner, String ... metadataKeyValuePairs) { 
		PLUSString s = new PLUSString(name);
		ProvBuilder db = newEmpty();
		
		try {
			s.getMetadata().put(Metadata.CONTENT_HASH_SHA_256, generateHash());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		tagMetadata(s, metadataKeyValuePairs);
		
		if(owner != null) { s.setOwner(owner); db.addActor(owner); } 
		db.addNode(s);
		return db;
	}
	
	/**
	 * Create a new invocation with the specified name and metadata.
	 * @param name the name of the invocation
	 * @param metadataKeyValuePairs
	 * @return a ProvBuilder containing a single PLUSInvocation.
	 * @throws PLUSException
	 */
	public ProvBuilder newInvocationNamed(String name, String ... metadataKeyValuePairs) throws PLUSException { 
		return newInvocationNamed(name, null, metadataKeyValuePairs); 
	}
	
	/**
	 * Create a new invocation with the specified name, owner, and metadata.
	 * @param name the name of the invocation
	 * @param owner the owner of the invocation
	 * @param metadataKeyValuePairs
	 * @return a ProvBuilder with a single PLUSInvocation.
	 */
	public ProvBuilder newInvocationNamed(String name, PLUSActor owner, String ... metadataKeyValuePairs) {
		ProvBuilder db = newEmpty();
		PLUSInvocation inv = new PLUSInvocation();
		inv.setName(name);		
		
		if(owner != null) { inv.setOwner(owner); db.addActor(owner); } 
		
		tagMetadata(inv, metadataKeyValuePairs);
		db.addNode(inv);
		return db;
	}
	
	/**
	 * Convenience method to associate a set of metadata key/value pairs with a specified PLUSObject.
	 * @param obj the object to tag
	 * @param metadataKeyValuePairs
	 * @return the object being tagged.
	 */
	private PLUSObject tagMetadata(PLUSObject obj, String ... metadataKeyValuePairs) {
		for(int x=0; x<metadataKeyValuePairs.length; x+=2) {
			String key = metadataKeyValuePairs[x];
			String val = metadataKeyValuePairs[x+1];
			
			obj.getMetadata().put(key, val); 
		}		
		
		return obj;
	}
	
	public String toString() {
		StringBuffer b = new StringBuffer("");
		
		b.append("ProvBuilder " + super.toString() + " containing [\n");
		
		for(PLUSObject o : getNodes()) { 
			b.append(o.toString() + "\n");
		}
		
		for(PLUSEdge e : getEdges()) { 
			b.append(e.toString() + "\n");
		}
		
		b.append("]\n");
		return b.toString();
	}
	
	/**
	 * Generates an instance of a ProvBuilder, by substituting varValue for a variable named varName in the specified template.
	 * @param tmpl
	 * @param varName
	 * @param varValue
	 * @return a ProvBuilder object.
	 * @see NodeTemplate
	 */
	public ProvBuilder generate(NodeTemplate tmpl, String varName, String varValue) {
		return generate(tmpl, varName, varValue, null); 
	}
	
	/**
	 * Generates an instance of a ProvBuilder, by substituting varValue for a variable named varName in the specified template.  Ensures
	 * that the resulting objects are owned by the specified owner.
	 * @param tmpl
	 * @param varName
	 * @param varValue
	 * @param owner
	 * @return a ProvBuilder containing the result of the generation.
	 * @see NodeTemplate
	 */
	public ProvBuilder generate(NodeTemplate tmpl, String varName, String varValue, PLUSActor owner) { 
		ProvBuilder db = newEmpty();
		
		String instanceName = tmpl.name.replaceAll("\\{" + varName + "\\}", varValue);
		
		if(tmpl.nt == NodeTemplate.NodeType.DATA) {
			return db.newDataNamed(instanceName, owner);
		} else if(tmpl.nt == NodeTemplate.NodeType.INVOCATION) { 
			return db.newInvocationNamed(instanceName, owner);
		} else {
			return db.newWorkflowNamed(instanceName, owner);
		}
	}
	
	/**
	 * Sample main method to demonstrate employment; meant only for instruction.
	 * @param args
	 * @throws Exception
	 */
	public static void main(String [] args) throws Exception { 
		ProvBuilder db = new ProvBuilder();
		System.out.println(new ProvBuilder().link(db.newDataNamed("Data input"), db.newInvocationNamed("Foo")));
	}
} // End ProvBuilder
