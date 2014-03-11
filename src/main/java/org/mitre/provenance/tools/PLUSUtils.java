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
package org.mitre.provenance.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * This is a garbage can class, with utility methods relevant to different places in the code.
 * @author DMALLEN
 */
public class PLUSUtils {
	private static final String PLUS_OID_PREFIX = "urn:uuid:mitre:plus:";
	
	/** Namespace used for XML-related junk */
	public static final String NAMESPACE = "http://confluence.mitre.org/display/PLUS/";
			
	private static Logger log = Logger.getLogger(PLUSUtils.class.getName());
		
	/**
	 * Generate a raw identifier used in PLUS object IDs
	 * @return a String unique identifier (UUID, time-based random number)
	 */
	public static String generateRawUUID() { 
		UUID u = UUID.randomUUID();
		return u.toString();
	}
	
	/**
	 * Generate a new random UUID suitable for identifying data items and invocations.
	 * @return a guaranteed unique string UUID
	 */
	public static String generateID() { 
		return PLUS_OID_PREFIX + PLUSUtils.generateRawUUID();
	} // End generateID
	
	/**
	 * Determines whether a given identifier is a valid PLUS object ID (OID).  Valid PLUS OIDs are generated
	 * by this class, and always contain PLUS_OID_PREFIX
	 * @param identifier the identifier to check
	 * @return true if the identifier is a PLUS OID, false otherwise.
	 * @see PLUSUtils#PLUS_OID_PREFIX
	 * @see PLUSUtils#generateID()
	 */
	public static boolean isPLUSOID(String identifier) { 
		return (identifier != null &&
				identifier.startsWith(PLUS_OID_PREFIX));
	}
			
	/**
	 * Read the data available from a process, and print it to stdout.
	 * @param p the process to read from.
	 * @throws IOException
	 */
	public static void printOutput(Process p) throws IOException {
		InputStream is = p.getInputStream();
		
		int av = is.available();
		byte [] data = new byte [av];
		for(int x=0; x<av; x++) { 
			data[x] = (byte)is.read();
		}

		String s = new String(data);
		PLUSUtils.log.fine("PROCESS OUTPUT:\n" + s); 
	}
	
	/**
	 * Takes a query string (in HTTP GET style) and returns a HashMap of its contents.  Performs URLDecoding on 
	 * all values (but not keys).  So for example, if passed x=1&y=2&z=3, it will return a HashMap with 3 keys
	 * (x, y, and z) with corresponding values.
	 * @param queryString an HTTP GET-style query string.
	 * @return a HashMap of its contents
	 */
	public static HashMap<String,String> unpackQueryString(String queryString) { 
		HashMap<String,String> results = new HashMap<String,String>();
		String [] pieces = queryString.split("&"); 
		
		for(int x=0; x<pieces.length; x++) { 
			String [] vals = pieces[x].split("=", 2);
			if(vals.length != 2) continue;
			try { 
				String key = vals[0];
				String val = URLDecoder.decode(vals[1], "UTF-8");	
				results.put(key, val); 
			} catch(UnsupportedEncodingException exc) { 
				PLUSUtils.log.warning("PLUSUtils#unpackQueryString: Can't decode '" + pieces[x] + "': " + exc.getMessage()); 
			} // End catch
		} // End for
		
		return results; 
	} // End unpackQueryString
	
	/**
	 * @deprecated
	 */
	public static String shortNameForURL(String url) {
		if(url == null || "".equals(url)) return "(None)";
		
		try {
			URL u = new URL(url);
			String rsrcName = u.getPath();
			String query = u.getQuery();
			boolean hasParams = (query != null && !"".equals(query)); 
				
			if(hasParams) return rsrcName + " (with parameters)";
			else return rsrcName; 
		} catch(Exception e) { ; } 			

		if(url.indexOf("&") > 0) return url.substring(0, url.indexOf("&")); 
		
		return url; 
	} // End shortNameForURL
	
	/**
	 * Utility method for helping with JXTA messages.  Take an input stream, and return its entire contents as a string.
	 * Don't do this for very large input streams, because it's all in memory.
	 * @deprecated
	 */
	public static String inputStreamToString(InputStream is) throws IOException { 
		final byte[] buffer = new byte[0x10000];
		StringBuilder out = new StringBuilder();

		int read;
		do {			
			read = is.read(buffer, 0, buffer.length);
			if (read>0) {
				out.append(new String(buffer, 0, read));
			}
		} while (read>=0);
		
		return out.toString();
	} // End inputStreamToString
	
	/** @deprecated */
	public static String join(String delim, Object [] col) { 
		if(col == null || col.length == 0) return "";
		StringBuffer b = new StringBuffer("");
		
		for(int x=0; x<col.length; x++) { 
			b.append(""+col[x]);
			if(x < (col.length - 1)) b.append(delim);
		}
		return b.toString();
	}
	
	/**
	 * PLUS sometimes takes identifiers passed by other systems, which may be in any number
	 * of encoding schemes, including URL, URI, and PLUS OID.  This method normalizes the 
	 * identifier into something that is most likely to be found by the database.
	 * @param id the ID to normalize
	 * @return the best PLUS representation of the same ID.
	 */
	public static String normalizeExternalIdentifier(String id) {
		if(PLUSUtils.isPLUSOID(id)) return id;
		
		if(id.indexOf("?oid=") != -1) { 
			String subid = id.substring(id.indexOf("?oid=")+5);
			if(PLUSUtils.isPLUSOID(subid)) return subid;
		}
		
		try { 
			URI u = new URI(id); 
			if(u.getScheme() != null && u.getHost() != null && "".equals(u.getPath()))
				return u + "/";
			else return u.normalize().toString();
		} catch(URISyntaxException exc) { ; } 
		
		return id;
	} // End normalizeExternalIdentifier
	
	/**
	 * Take a number of elapsed milliseconds, and describe it as a string.
	 * @param elapsedMS a number of milliseconds elapsed.
	 * @return a string of the form "X years, Y months, Z days, (etc)"
	 */
	public static String describeTimeSpan(long elapsedMS) { 
		StringBuffer b = new StringBuffer("");
		
		long days_ms = 1000 * 60 * 60 * 24;
		int days = 0;

		long years_ms = days_ms * 365;
		int years = 0;
		
		long hours_ms = 1000 * 60 * 60;
		int hours = 0;
		
		long minutes_ms = 1000 * 60;
		int minutes = 0;
		
		long seconds_ms = 1000;
		int seconds = 0;
		
		while(elapsedMS > years_ms) { 
			years++;
			elapsedMS -= years_ms;
		}
		
		while(elapsedMS > days_ms) { 
			days++;
			elapsedMS -= days_ms;
		}
		
		while(elapsedMS > hours_ms) { 
			hours++;
			elapsedMS -= hours_ms;
		}
		
		while(elapsedMS > minutes_ms) { 
			minutes++;
			elapsedMS -= minutes_ms;
		}
		
		while(elapsedMS > seconds_ms) { 
			seconds++;
			elapsedMS -= seconds_ms;
		}
		
		if(years > 0) { b.append(years + " years "); } 
		if(days > 0) { b.append(days + " days "); }
		if(hours > 0) { b.append(hours + " hours "); } 
		if(minutes > 0) { b.append(minutes + " minutes "); }
		if(seconds > 0) { b.append(seconds + " seconds "); } 
		if(elapsedMS > 0) { b.append(elapsedMS + "ms."); } 
				
		return b.toString();
	} // End describeTimeSpan
	
	/**
	 * Normalizes a URI as specified in section 6.2.2 of RFC 3986
	 * @param uri a URI 
	 * @return an RFC 3986 URI normalized according to section 6.2.2.
	 * @throws URISyntaxException
	 * @throws UnsupportedEncodingException 
	 */
	public static URI normalizeURI(String uri) throws URISyntaxException, UnsupportedEncodingException {
		return normalizeURI(new URI(uri)); 
	}
	
	/**
	 * Normalizes a URI as specified in section 6.2.2 of RFC 3986.
	 * At present, this does nothing for opaque URIs (such as URNs, and mailto:foo@bar.com).  For non-opaque
	 * URIs, it standardizes the case of escaped octets, hostname, fixes port references, alphebetizes and
	 * properly encodes query string parameters, and resolves relative paths.
	 * @param uri a URI 
	 * @return an RFC 3986 URI normalized according to section 6.2.2.
	 * @throws URISyntaxException
	 * @throws UnsupportedEncodingException 
	 */
	public static URI normalizeURI(URI uri) throws URISyntaxException, UnsupportedEncodingException {
		if(uri.isOpaque()) return uri;
		
		uri = uri.normalize();
		
		String scheme   = uri.getScheme();
		String userInfo = uri.getUserInfo();
		String host     = uri.getHost();
		String path     = uri.getPath();
		String query    = uri.getQuery();
		String fragment = uri.getFragment();
		Integer port    = uri.getPort();
	
		if(path == null || "".equals(path)) path = "/";
		if(scheme != null) scheme = scheme.toLowerCase();
		if(host != null) host = host.toLowerCase();
		if(port != null && port.equals(getPortForScheme(scheme))) port = null;
		
		if(port != null) 
			return new URI(scheme, userInfo, host, port, 					
					URLEncoder.encode(path, "UTF-8").replaceAll("%2F", "/"),
					normalizeQueryString(query),
					(fragment == null ? null : URLEncoder.encode(fragment, "UTF-8")));
		else {
			String authority = host;
			if(userInfo != null) authority = userInfo + "@" + host;
			return new URI(scheme, authority, 
					URLEncoder.encode(path, "UTF-8").replaceAll("%2F", "/"),
					normalizeQueryString(query),
					(fragment == null ? null : URLEncoder.encode(fragment, "UTF-8")));
		} // End else
	} // End normalizeURI

	/**
	 * Given an un-encoded URI query string, this will return a normalized, properly encoded URI query string.
	 * <b>Important:</b> This method uses java's URLEncoder, which returns things that are 
	 * application/x-www-form-urlencoded, instead of things that are properly octet-esacped as the URI spec
	 * requires.  As a result, some substitutions are made to properly translate space characters to meet the
	 * URI spec.
	 * @param queryString
	 * @return
	 */
	private static String normalizeQueryString(String queryString) throws UnsupportedEncodingException { 
		if("".equals(queryString) || queryString == null) return queryString;
		
		String [] pieces = queryString.split("&");
		HashMap<String,String>kvp = new HashMap<String,String>();
		StringBuffer builder = new StringBuffer(""); 
		
		for(int x=0; x<pieces.length; x++) { 			
			String [] bs = pieces[x].split("=", 2); 
			bs[0] = URLEncoder.encode(bs[0], "UTF-8"); 
			if(bs.length == 1) kvp.put(bs[0], null); 
			else { 
				kvp.put(bs[0], URLEncoder.encode(bs[1], "UTF-8").replaceAll("\\+", "%20"));
			}
		}
		
		// Sort the keys alphabetically, ignoring case.
		ArrayList<String>keys = new ArrayList<String>(kvp.keySet());
		Collections.sort(keys, new Comparator<String>() {
			public int compare(String o1, String o2) {
				return o1.compareToIgnoreCase(o2);
			}			
		});
		
		// With the alphabetic list of parameter names, re-build the query string.
		for(int x=0; x<keys.size(); x++) {
			// Some parameters have no value, and are simply present.  If so, we put null in kvp,
			// and we just put the parameter name, no "=value".
			if(kvp.get(keys.get(x)) == null) builder.append(keys.get(x));
			else builder.append(keys.get(x) + "=" + kvp.get(keys.get(x)));
			
			if(x < (keys.size() -1)) builder.append("&");			
		}
		
		return builder.toString();
	} // End normalizeQueryString
		
	/**
	 * See http://www.iana.org/assignments/port-numbers.  This is a partial list of only the most common.
	 * @param scheme a scheme within a URI (such as http, ftp, ssh, etc)
	 * @return the standard port number for that scheme.
	 */
	private static Integer getPortForScheme(String scheme) {
		scheme = scheme.toLowerCase();

		if("http".equals(scheme)) return 80;
		if("ftp".equals(scheme)) return 21;
		if("ssh".equals(scheme)) return 22;
		if("telnet".equals(scheme)) return 23;
		if("gopher".equals(scheme)) return 70;
		if("http-alt".equals(scheme)) return 8080;
		if("radan-http".equals(scheme)) return 8088;
		if("dnsix".equals(scheme)) return 90;
		if("echo".equals(scheme)) return 7;
		if("daytime".equals(scheme)) return 13;
		if("smtp".equals(scheme)) return 25;
		if("time".equals(scheme)) return 37;
		
		return null;
	}
} // End PLUSUtils
