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
package org.mitre.provenance.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.db.neo4j.Neo4JPLUSObjectFactory;
import org.mitre.provenance.db.neo4j.Neo4JStorage;
import org.mitre.provenance.npe.NonProvenanceEdge;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.neo4j.graphdb.Node;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;

/**
 * Services in this class produce RSS/XML and JSON feeds of the latest objects reported to PLUS in various categories.
 * @author dmallen
 */
@Path("/feeds")
public class Feeds {
	protected static final Logger log = Logger.getLogger(Feeds.class.getName());
	
	@Context
	UriInfo uriInfo;
	
	public static Integer maxResults = 30;

	public Feeds() { ; } 

	@Path("/externalIdentifiers")
	@GET
	@Produces({"application/rss+xml", MediaType.APPLICATION_JSON})
	public Response externalIdentifiers(@Context HttpServletRequest req, @DefaultValue("30") @QueryParam("n") int maxItems,
			@DefaultValue("rss") @QueryParam("format") String format) throws PLUSException, FeedException {
		if(maxItems < 0) return ServiceUtility.BAD_REQUEST("Bad n value");
		if(maxItems > maxResults) maxItems = maxResults;
		
		if(!"rss".equals(format) && !"json".equals(format)) return ServiceUtility.BAD_REQUEST("Illegal format");
		
		ProvenanceCollection col = Neo4JPLUSObjectFactory.getNonProvenanceEdges(null, ServiceUtility.getUser(req), maxItems);
		
		if("json".equals(format)) { 
			return ServiceUtility.OK(col);
		} else { 
			String title = "Provenance Search Results";
			String description = "Feed of external identifiers";	
			// Set up the syndicated feed
			SyndFeed feed = new SyndFeedImpl();
			feed.setTitle(title);
			feed.setDescription(description); 
			feed.setUri(uriInfo.getAbsolutePath().toString());	  
			feed.setLink(uriInfo.getAbsolutePath().toString());
			feed.setLanguage("en-us" );
			feed.setPublishedDate(new Date(System.currentTimeMillis()));
			feed.setCopyright("(C) MITRE Corporation 2010"); 
			feed.setFeedType("rss_2.0");
	
			ArrayList<SyndEntry> entries = new ArrayList<SyndEntry>();
			
			for(NonProvenanceEdge npe : col.getNonProvenanceEdges())
				entries.add(FeedEntryFactory.getFeedEntry(npe, uriInfo.getAbsolutePath().getPath()));		
			
			feed.setEntries(entries);
	
			return ServiceUtility.OK(feed);
		} // End else
	} // End externalIdentifiers

	@Path("/objects/search/{query}")
	@GET
	@Produces("application/rss+xml")
	public Response objectsSearch(@Context HttpServletRequest req, @PathParam("query") String query) throws PLUSException, FeedException { 
		String title = "Trusting Composed Information: Federated Search Results";
		String description = "Feed of objects containing the keyword " + query;	
		// Set up the syndicated feed
		SyndFeed feed = new SyndFeedImpl();
		feed.setTitle(title);
		feed.setDescription(description); 
		feed.setUri(uriInfo.getAbsolutePath().toString());	  
		feed.setLink(uriInfo.getAbsolutePath().toString());
		feed.setLanguage("en-us" );
		feed.setPublishedDate(new Date(System.currentTimeMillis()));
		feed.setCopyright("(C) MITRE Corporation 2010"); 
		feed.setFeedType("rss_2.0");

		// Fetch the objects
		ProvenanceCollection objects = Neo4JPLUSObjectFactory.searchFor(query, ServiceUtility.getUser(req), maxResults);		

		// Cycle through the objects
		ArrayList<SyndEntry> entries = new ArrayList<SyndEntry>();
		for(PLUSObject object : objects.getNodesInOrderedList(ProvenanceCollection.SORT_BY_CREATION))
			entries.add(FeedEntryFactory.getFeedEntry(object, uriInfo.getAbsolutePath().getPath())); 
		feed.setEntries(entries);
		
		return ServiceUtility.OK(feed);
	}

	@Path("/objects/owners/")
	@GET
	@Produces({"application/rss+xml", MediaType.APPLICATION_JSON})
	public Response owners(@Context HttpServletRequest req, @DefaultValue("rss") @QueryParam("format") String format, @QueryParam("n") int maxItems) throws FeedException, PLUSException { 
		if(!"rss".equals(format) && !"json".equals(format))
			return ServiceUtility.BAD_REQUEST("Illegal format '" + format + "' specified.");
		
		String title = "Provenance Feed: Object Owners";
		String description = "Feed of object owners";

		if(maxItems <= 0) maxItems = 10;
		if(maxItems > maxResults) maxItems = maxResults;
		
		// Fetch the objects
		log.info("Getting " + maxResults + " actors.");
		ProvenanceCollection actors = Neo4JStorage.getActors(maxItems);
		log.info("Formatting actor results with " + actors.getActors().size() + " results.");
		
		// TODO consider incorporating permissions into actor reporting.
		// User user = ServiceUtility.getUser(req);	
		
		if("json".equals(format)) return ServiceUtility.OK(actors);
		else { 
			// Set up the syndicated feed
			SyndFeed feed = new SyndFeedImpl();
			feed.setTitle(title);
			feed.setDescription(description); 
			feed.setUri(uriInfo.getAbsolutePath().toString());	  
			feed.setLink(uriInfo.getAbsolutePath().toString());
			feed.setLanguage("en-us" );
			feed.setPublishedDate(new Date(System.currentTimeMillis()));
			feed.setCopyright("(C) MITRE Corporation 2010"); 
			feed.setFeedType("rss_2.0");
	
	
			// Cycle through the objects
			ArrayList<SyndEntry> entries = new ArrayList<SyndEntry>();
			for(PLUSActor actor : actors.getActors())
				entries.add(FeedEntryFactory.getFeedEntry(actor, uriInfo.getAbsolutePath().getPath()));
			
			feed.setEntries(entries);
	
			return ServiceUtility.OK(feed);
		} // End else
	} // End owners
	
	@Path("/objects/owner/{owner}")
	@GET
	@Produces({"application/rss+xml", MediaType.APPLICATION_JSON})
	public Response objectsOwner(@Context HttpServletRequest req, @PathParam("owner") String ownerID,
			                     @DefaultValue("rss") @QueryParam("format") String format) throws FeedException, PLUSException { 
		PLUSActor owner = null;

		if(!"rss".equals(format) && !"json".equals(format))
			return ServiceUtility.BAD_REQUEST("Illegal format '" + format + "' specified.");
		
		Node n = Neo4JStorage.actorExists(ownerID);
		if(n == null) return ServiceUtility.NOT_FOUND("No such owner " + ownerID);
		owner = Neo4JPLUSObjectFactory.newActor(n);
		
		// Fetch the objects
		ProvenanceCollection objects = null;
		objects = Neo4JStorage.getOwnedObjects(owner, ServiceUtility.getUser(req), maxResults);

		if("rss".equals(format)) { 
			if(ownerID != null) 
				owner = Neo4JPLUSObjectFactory.newActor(Neo4JStorage.actorExists(ownerID)); 
	
			String title = "Provenance Feed: "+owner.getName()+" Provenance Objects";
			String description = "Feed of objects reported by " + owner.getName();
	
			// Set up the syndicated feed
			SyndFeed feed = new SyndFeedImpl();
			feed.setTitle(title);
			feed.setDescription(description); 
			feed.setUri(uriInfo.getAbsolutePath().toString());	  
			feed.setLink(uriInfo.getAbsolutePath().toString());
			feed.setLanguage("en-us" );
			feed.setPublishedDate(new Date(System.currentTimeMillis()));
			feed.setCopyright("(C) MITRE Corporation 2010"); 
			feed.setFeedType("rss_2.0");
	
			// Cycle through the objects
			ArrayList<SyndEntry> entries = new ArrayList<SyndEntry>();
			for(PLUSObject object : objects.getNodesInOrderedList(ProvenanceCollection.SORT_BY_CREATION))
				entries.add(FeedEntryFactory.getFeedEntry(object, uriInfo.getAbsolutePath().getPath()));
			
			feed.setEntries(entries);
	
			return ServiceUtility.OK(feed);
		} else { 
			return ServiceUtility.OK(objects);
		}
	}
		
	@Path("/objects/latest")
	@GET
	@Produces({"application/rss+xml", MediaType.APPLICATION_JSON})
	public Response latest(@Context HttpServletRequest req, 
			               @DefaultValue("rss") @QueryParam("format") String format) throws PLUSException, FeedException {
		if(!"rss".equals(format) && !"json".equals(format)) 
			return ServiceUtility.BAD_REQUEST("Invalid format");
		
		// Fetch the objects
		ProvenanceCollection objects = Neo4JPLUSObjectFactory.getRecentlyCreated(ServiceUtility.getUser(req), maxResults);	  

		log.info("latest objects: " + objects.countNodes());
		
		if("json".equals(format)) { 
			return ServiceUtility.OK(objects);
		} else { 
			String title = "Trusting Composed Information Provenance Feed: Latest Objects";
			String description = "Feed of the most recently created lineage objects reported to IM-PLUS";
	
			// Set up the syndicated feed
			SyndFeed feed = new SyndFeedImpl();
			feed.setTitle(title);
			feed.setDescription(description); 
			feed.setUri(uriInfo.getAbsolutePath().toString());	  
			feed.setLink(uriInfo.getAbsolutePath().toString());
			feed.setLanguage("en-us" );
			feed.setPublishedDate(new Date(System.currentTimeMillis()));
			feed.setCopyright("(C) MITRE Corporation 2010"); 
			feed.setFeedType("rss_2.0");
		
			// Cycle through the objects
			ArrayList<SyndEntry> entries = new ArrayList<SyndEntry>();
			for(PLUSObject object : objects.getNodesInOrderedList(ProvenanceCollection.SORT_BY_CREATION))
				entries.add(FeedEntryFactory.getFeedEntry(object, uriInfo.getAbsolutePath().getPath())); 
			feed.setEntries(entries);
	
			return ServiceUtility.OK(feed);
		} // End else
	}
}
