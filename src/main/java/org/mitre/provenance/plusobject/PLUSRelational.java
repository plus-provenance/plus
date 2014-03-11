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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

import org.mitre.provenance.PLUSException;
import org.neo4j.graphdb.PropertyContainer;

/**
 * A PLUSObject that is attached to a relational database.
 * @author DMALLEN
 */
public class PLUSRelational extends PLUSDataObject {
	protected static Logger log = Logger.getLogger(PLUSRelational.class.getName());
	
	/** Database connect string */
    protected String dbconnector;
    
    /** Table name identifier */
    protected String tableName; 
    
    /** Name of a key field */
    protected String keyFieldName; 
    
    /** The value of the key field which is indicated */
    protected String keyFieldValue; 
    
    /** SQL statement, which when evaluated, results in the data this object refers to */
    protected String resultOfSQL; 
    
    protected ArrayList <String> columnNames; 
    protected ArrayList <String> redacted;
    
    public static final String PLUS_SUBTYPE_RELATIONAL = "relational";
    	
    public PLUSRelational() { 
    	super(); 
    	setKeyFieldName("N/A");
    	setKeyFieldValue("N/A");
    	setDBConnector("unknown"); 
    	setSQL("none");
    	setObjectSubtype("relational");
    	redacted = new ArrayList <String> ();
    }
        
    public ArrayList <String> getColumnNames() { return columnNames; } 
    public ArrayList <String> getRedactedList() { return redacted; } 
    
    /**
     * Relational objects have the ability to mark particular attributes as "redacted".  Subclasses get to decide
     * what to do with this "redacted" information, but it should be used to remove fields from output reported
     * to users.  So for example by setting the column "foo" to redacted, when calling methods to access the 
     * object's underlying data, foo data will not be returned.
     * @param attribute the name of the attribute whose redaction status you want to change.
     * @param redactedStatus true if the attribute should be redacted, false if you want it removed from the redaction list.
     */
    public void setRedacted(String attribute, boolean redactedStatus) { 
    	if(redacted == null) redacted = new ArrayList <String> (); 
    	
    	if(redactedStatus) redacted.add(attribute.toLowerCase());
    	else {
    		int idx = redacted.indexOf(attribute.toLowerCase());
    		if(idx == -1) return;
    		else redacted.remove(idx); 
    	}
    } // End setRedacted
    
    /**
     * Determine whether a particular attribute is redacted or not.
     * @param attribute the name of the attribute
     * @return true if it should be redacted, false otherwise.
     */
    public boolean isRedacted(String attribute) {
    	if(redacted == null) redacted = new ArrayList <String> (); 
    	return redacted.contains(attribute.toLowerCase()); 
    }
    
    /**
     * Relational objects tend to require a lot of data following them around, and have high latency.
     * Call this method when you need to populate the object.  Subclasses of PLUSRelational should override
     * this method, since all this one does is throw an exception.
     * 
     * @throws SQLException
     */
    public void populate() throws SQLException { 
    	throw new SQLException("MUST OVERRIDE populate()!"); 
    }
    
    public String getDBConnector() { return dbconnector; } 
    public String getTableName() {
    	if(tableName == null && getSQL() != null) { 
    		String sql = getSQL();
    		log.info("PLUSRelational: trying to figure out table name.");
    		
    		try { 
	    		String marker = " from ";
	    		int idx = sql.toLowerCase().lastIndexOf(marker);	    		
	    		int end = sql.indexOf(" ", (idx + marker.length() + 1));
	    		tableName = sql.substring(idx, end);
	    		return tableName;
    		} catch(Exception e) {
    			log.severe("Failed to figure out table name from SQL"); 
    			return null; 
    		} 
    	} // End if
    	
    	return tableName; 
    } // End getTableName
    
    public String getKeyFieldName() { return keyFieldName; } 
    public String getKeyFieldValue() { return keyFieldValue; } 
    public String getSQL() { return resultOfSQL; } 
        
    public void setDBConnector(String dbconnector) { this.dbconnector = dbconnector; } 
    public void setTableName(String tableName) { this.tableName = tableName; } 
    public void setKeyFieldName(String keyFieldName) { this.keyFieldName = keyFieldName; } 
    public void setKeyFieldValue(String keyFieldValue) { this.keyFieldValue = keyFieldValue; } 
    public void setSQL(String resultOfSQL) { this.resultOfSQL = resultOfSQL; }
    
    protected void copy(PLUSRelational other) {
    	super.copy(other); 
    	setDBConnector(other.getDBConnector());
    	setTableName(other.getTableName());
    	setKeyFieldName(other.getKeyFieldName());
    	setKeyFieldValue(other.getKeyFieldValue());
    	setSQL(other.getSQL());
    	columnNames = other.columnNames;
    	redacted = other.getRedactedList();
    	setObjectSubtype(PLUS_SUBTYPE_RELATIONAL);
    	setObjectType(PLUS_TYPE_DATA); 
    } // End copy()
    
    public PLUSObject clone() { 
    	PLUSRelational r = new PLUSRelational();
    	r.copy(this);
    	return r;
    }
        
	public Map<String,Object> getStorableProperties() {		
		Map<String,Object> m = super.getStorableProperties();
		m.put("keyFieldName", getKeyFieldName());
		m.put("keyFieldValue", getKeyFieldValue());		
		m.put("tableName", getTableName());
		m.put("SQL", getSQL());		
		return m;
	}
	
	public PLUSObject setProperties(PropertyContainer props) throws PLUSException { 
		super.setProperties(props);
		setKeyFieldName(""+props.getProperty("keyFieldName", ""));
		setKeyFieldValue(""+props.getProperty("keyFieldValue", ""));
		setTableName("" + props.getProperty("tableName", ""));
		setSQL(""+ props.getProperty("SQL"));		
		return this;
	}
} // End PLUSRelational
