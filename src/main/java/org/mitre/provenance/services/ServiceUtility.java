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

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.mitre.provenance.plusobject.PLUSActor;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.plusobject.json.JSONConverter;
import org.mitre.provenance.user.User;

import com.google.gson.GsonBuilder;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * This class provides utility methods as helpers for other service implementations.
 * @author david
 */
public class ServiceUtility {
	public static User getUser(HttpServletRequest req) {
		Object o = req.getSession().getAttribute("plus_user");		
		if(o instanceof User) return (User)o;
		
		// TODO
		return User.DEFAULT_USER_GOD;
	} // End getUser
	
	public static Response OK(Map<String,Object> map) {
		return Response.ok(new GsonBuilder().setPrettyPrinting().create().toJson(map), 
				MediaType.APPLICATION_JSON).build();
	}
		
	public static Response OK(List<?> list) { 
		String json = new GsonBuilder().setPrettyPrinting().create().toJson(list);		
		return Response.ok(json, MediaType.APPLICATION_JSON).build();		
	}
	
	/**
	 * Convenience function for returning an HTTP OK response, with the D3 formatted JSON results from a 
	 * provenance collection.
	 * @param col
	 * @return
	 */
	public static Response OK(ProvenanceCollection col) {		
		return Response.ok(JSONConverter.provenanceCollectionToD3Json(col), MediaType.APPLICATION_JSON).build();
	}
	
	public static Response OK(SyndFeed feed) throws FeedException {
		return Response.ok(new SyndFeedOutput().outputString(feed), "application/rss+xml").build();
	}
	
	/**
	 * Convenience function for returning an internal server error response, with a message.
	 * @param msg
	 * @return
	 */
	public static Response ERROR(String msg) { 
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
	}
	
	public static Response FORBIDDEN(String msg) {
		return Response.status(Response.Status.FORBIDDEN).entity(msg).build();
	}
	
	/**
	 * Convenience function for returning an HTTP not found response, with a message.
	 * @param msg
	 * @return
	 */
	public static Response NOT_FOUND(String msg) { 
		return Response.status(Response.Status.NOT_FOUND).entity(msg).build();
	}
	
	/**
	 * Convenience function for returning an HTTP bad request response, with a message.
	 * @param msg
	 * @return
	 */
	public static Response BAD_REQUEST(String msg) { 
		return Response.status(Response.Status.BAD_REQUEST).entity(msg).build();
	}	
}
