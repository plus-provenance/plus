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

import java.util.Date;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.npe.NonProvenanceEdge;
import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSObjectType;
import org.mitre.provenance.surrogate.SignPost;
import org.mitre.provenance.tools.PLUSUtils;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;

/** Constructs the specified feed entry */
public class FeedEntryFactory
{	
	public static String linkURL = "/plus/view.jsp";
	public static final String AUTHOR = "PLUS";
	
	/** Constructs a feed entry for the specified object */
	public static SyndEntry getFeedEntry(PLUSObject object, String contextPath) throws PLUSException { 		
		// Get object information
		PLUSObjectType type = object.getType();
		String url = (String)object.getMetadata().get("plus:url");
		PLUSActor source = object.getOwner();
		SignPost sp = object.getSourceHints();
		
		// Generate the description
		StringBuffer description = new StringBuffer();
		description.append("This object is a " + type.getObjectType() + "/" + type.getObjectSubtype() + ".");		
		if(url!=null) description.append("<p>The object refers to a URL at <a href='" + url + "'>" + url + "</a></p>");		
		if(source!=null) description.append("<p>The owner of this object is " + source.getName() + "</p>");
		if(sp != null) description.append("<p>This object originally came from " + sp + "</p>"); 		
		
		// Set the entry content
		SyndContent content = new SyndContentImpl();
		content.setType("text/html");
		content.setValue(description.toString()); 
		
		// Generate the entry
		SyndEntry entry = new SyndEntryImpl();
		entry.setTitle(object.getName());
		
		entry.setPublishedDate(new Date(object.getCreated()));
		entry.setLink(linkURL + "?oid=" + object.getId() + "&id=" + object.getId());			
		entry.setUri(linkURL + "?oid=" + object.getId() + "&id=" + object.getId());
		entry.setAuthor(AUTHOR); 
		entry.setDescription(content);
		return entry; 
	}
	
	public static SyndEntry getFeedEntry(NonProvenanceEdge npe, String contextPath) { 
		SyndContent content = new SyndContentImpl();
		content.setType("text/html");
		
		String fromPart = "";
		String toPart = "";
		
		if(PLUSUtils.isPLUSOID(npe.getFrom())) {
			fromPart = "<a target='_tw' href='" + linkURL + "?oid=" + 
					   npe.getFrom() + "&id=" + npe.getFrom() + "'>Provenance Object</a>";
		} else { 
			if(npe.getType().equals(NonProvenanceEdge.NPE_TYPE_URI) || "URL".equals(npe.getType()))
				fromPart = "<a href='" + npe.getFrom() + "' target='_tw'>" + npe.getFrom() + "</a>";
			else fromPart = npe.getFrom();
		}
		
		if(PLUSUtils.isPLUSOID(npe.getTo())) {
			toPart = "<a target='_tw' href='" + linkURL + "?oid=" + 
				     npe.getTo() + "&id=" + npe.getTo() + "'>Provenance Object</a>";
		} else {
			if(npe.getType().equals(NonProvenanceEdge.NPE_TYPE_URI) || "URL".equals(npe.getType())) 
				toPart = "<a href='" + npe.getTo() + "' target='_tw'>" + npe.getTo() + "</a>";
			else toPart = npe.getTo();
		}
		
		content.setValue(fromPart + " <b>" + npe.getType() + "</b> " + toPart); 
		
		SyndEntry entry = new SyndEntryImpl();
		entry.setTitle("Exernal Identifier: " + npe.getCreatedAsDate());
		
		String newLink = linkURL;
		newLink = newLink.replace(contextPath + "/.*", contextPath + "/widgets/trust/trustpanel.jsp");
		entry.setLink(newLink + "?oid=" + npe.getFrom() + "&id=" + npe.getFrom());		
		entry.setPublishedDate(new Date(System.currentTimeMillis())); 	
		entry.setAuthor(AUTHOR);		
		entry.setDescription(content);

		return entry;
	} // End getFeedEntry
	
	/** Constructs a feed entry for the specified source */
	public static SyndEntry getFeedEntry(PLUSActor source, String contextPath)
	{ 
		// Set the entry content
		SyndContent content = new SyndContentImpl();
		content.setType("text/html");
		content.setValue("Owner name " + source.getName()); 
		
		// Generate the entry
		SyndEntry entry = new SyndEntryImpl();
		entry.setTitle(source.getName());
		entry.setPublishedDate(new Date(System.currentTimeMillis())); 	
		entry.setLink(linkURL + "?owner=" + source.getId());	
		entry.setAuthor(AUTHOR);		
		entry.setDescription(content);
		return entry;
	}
}