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

import org.mitre.provenance.Metadata;
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
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * Services in this class produce RSS/XML and JSON feeds of the latest objects reported to PLUS in various categories.
 * @author moxious
 */
@Path("/feeds")
@Api(value = "/feeds", description = "Feeds of latest reported provenance information")
public class Feeds {
	protected static final Logger log = Logger.getLogger(Feeds.class.getName());
	
	protected @Context UriInfo uriInfo;
	
	public static Integer maxResults = 30;

	public Feeds() { ; } 
	
	/**
	 * Returns the latest items reported to the database that have hashed content.  That is, their metadata
	 * contains a reference to the field below.
	 * @param req
	 * @param maxItems
	 * @return D3-JSON formatted response
	 * @see Metadata#CONTENT_HASH_SHA_256
	 */
	@Path("/hashedContent")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get latest hashed content", notes="Content identified by MD5, SHA hashes", response=ProvenanceCollection.class)
	@ApiResponses(value = {
	  @ApiResponse(code = 400, message = "Error loading content"),	 
	  @ApiResponse(code = 400, message = "Bad n value"),
	})		
	public Response hashedContent(@Context HttpServletRequest req,
			@ApiParam(value = "Maximum number of items to return", required = false)
			@DefaultValue("10") @QueryParam("n") int maxItems) {
		if(maxItems <= 0 || maxItems > maxResults) return ServiceUtility.BAD_REQUEST("Bad n value");

		String propName = Neo4JStorage.getMetadataPropertyName(Metadata.CONTENT_HASH_SHA_256);
		
		String query = "match (n:Provenance) " + 
	               	   "where has(n.`" + propName + "`) " +  
	               	   "return n " +
	               	   "order by n.created desc " + 
	               	   "limit " + maxItems;
	
		try { 
			return ServiceUtility.OK_ExecuteQuery(req, query, "n");
		} catch(PLUSException exc) { 
			exc.printStackTrace();
			return ServiceUtility.ERROR(exc.getMessage());
		}
	}
	
	@Path("/connectedData")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get latest connected data", 
	              notes="Connected data are data items with both incoming and outgoing provenance links.", 
	              response=ProvenanceCollection.class)
	@ApiResponses(value = {
	  @ApiResponse(code = 400, message = "Error loading content"),	 
	  @ApiResponse(code = 400, message = "Bad n value"),
	})			
	public Response connectedData(@Context HttpServletRequest req, 
			@ApiParam(value = "Maximum number of items to return", required = false)
			@DefaultValue("10") @QueryParam("n") int maxItems) {
		
		if(maxItems <= 0 || maxItems > maxResults) return ServiceUtility.BAD_REQUEST("Bad n value");
		
		String query = "match (a:Provenance)-->(n:Provenance)-->(b:Provenance) " + 
		               "where n.type = 'data' " + 
				       "return n " +
		               "order by n.created desc " + 
		               "limit " + maxItems;
		
		try { 
			return ServiceUtility.OK_ExecuteQuery(req, query, "n");
		} catch(PLUSException exc) { 
			exc.printStackTrace();
			return ServiceUtility.ERROR(exc.getMessage());
		}
	} // End connectedData
	
	@Path("/externalIdentifiers")
	@GET
	@Produces({"application/rss+xml", MediaType.APPLICATION_JSON})
	@ApiOperation(value = "Get latest external identifiers (non-provenance IDs)", 
    notes="Any non-provenance identifiers associated with reported data", 
    response=ProvenanceCollection.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "Error loading content"),	 
			@ApiResponse(code = 400, message = "Bad n value"),
	})				
	public Response externalIdentifiers(@Context HttpServletRequest req, 
			@ApiParam(value = "Maximum number of items to return", required = false)
			@DefaultValue("30") @QueryParam("n") int maxItems,
			@ApiParam(value = "Format of response: rdf or json", required = false)
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
	@ApiOperation(value = "Search for provenance objects by name or description", 
    notes="Simple keyword or phrase", 
    response=ProvenanceCollection.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "Error loading content"),	 
	})					
	public Response objectsSearch(@Context HttpServletRequest req, 
			@ApiParam(value = "user-supplied query string", required = true)
			@PathParam("query") String query) throws PLUSException, FeedException { 
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
	@ApiOperation(value = "Get latest owners of provenance objects", 
                  notes=" ", response=ProvenanceCollection.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "Error loading content"),	 
			@ApiResponse(code = 400, message = "Bad n value"),
	})						
	public Response owners(@Context HttpServletRequest req, 
			@ApiParam(value = "representation format; json or rss", required = true)
			@DefaultValue("rss") @QueryParam("format") String format, 
			@ApiParam(value = "total items to return", required = true)
			@QueryParam("n") int maxItems) throws FeedException, PLUSException { 
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
	@ApiOperation(value = "Get objects owned by a particular actor", notes=" ", response=ProvenanceCollection.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "Error loading content"),	 
			@ApiResponse(code = 400, message = "Bad n value"),
	})
	public Response objectsOwner(@Context HttpServletRequest req, 
			@ApiParam(value = "The actor ID of the owner whose objects you want", required=true)
			@PathParam("owner") String ownerID,
			@ApiParam(value = "format to return; json or rss", required=true)
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
	@ApiOperation(value = "Get latest reported objects", notes=" ", response=ProvenanceCollection.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "Error loading content"),	 			
	})	
	public Response latest(@Context HttpServletRequest req, 
						   @ApiParam(value = "format of response; rss or json", required=true)
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
} // End Feeds
