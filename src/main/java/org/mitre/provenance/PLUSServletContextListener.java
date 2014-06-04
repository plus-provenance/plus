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
package org.mitre.provenance;

import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.mitre.provenance.db.neo4j.Neo4JStorage;

/**
 * This class works with the servlet application deployment of PLUS, and permits the application to know when it
 * is started and stopped.  This is typically only called by the container (tomcat, jetty, etc)
 * 
 * @author moxious
 */
public class PLUSServletContextListener implements ServletContextListener {
	private static Logger log = Logger.getLogger(PLUSServletContextListener.class.getName());
	
	/** Called when the web application is shutting down */
	public void contextDestroyed(ServletContextEvent arg0) {
		log.info("Shutting down.");
		Neo4JStorage.shutdown();
	}

	/** Called when the web application is started */
	public void contextInitialized(ServletContextEvent arg0) {
		log.info("PLUS context initialized: doing database setup.");
		try { Neo4JStorage.initialize(); } 
		catch(Exception exc) { 
			System.err.println("Initialization failed; but continuing"); 
		}
	}
} // End PLUSServletContextListener
