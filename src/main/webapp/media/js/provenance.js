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


/*
 * Lightweight provenance library for javascript.
 * @author DMALLEN
 */
function ProvenanceGraph(d3json) { 
	this.d3json = d3json;
	
	this.getD3Json = function() { return this.d3json; };
	
	this.nodesUpstreamOf = function(oid, otherFunc) {
		var n = this.getNode(oid);
		if(!n) { throw "No such node " + oid; }
		
		return this.nodes(otherFunc, function(o) { return o.created < n.created; });
	};

	this.nodesDownstreamOf = function(oid, otherFunc) {		
		var n = this.getNode(oid);
		if(!n) { throw "No such node " + oid; }
		
		return this.nodes(otherFunc, function(o) { return o.created > n.created; }); 
	};
	
	this.countActors = function() { return this.d3json.actors.length; };
	this.countNodes = function() { return this.d3json.nodes.length; };
	this.countEdges = function() { return this.d3json.links.length; };
	
	/** Apply the first argument (a function) to all actors in the graph.
	 * If a second parameter is provided, it will be evaluated as a function with the actor as
	 * an argument.  If it returns true, the first function will fire. If it returns false, it won't.
	 */
	this.actors = function(otherFunc, conditionFunc) {
		for(var i in this.d3json.actors) {
			if(conditionFunc && conditionFunc(this.d3json.actors[i])) {
				otherFunc(this.d3json.actors[i]);
			} else if(!conditionFunc) {
				otherFunc(this.d3json.actors[i]);
			}
		}
	};
	
	this.nodes = function(otherFunc, conditionFunc) { 
		for(var i in this.d3json.nodes) { 
			if(conditionFunc && conditionFunc(this.d3json.nodes[i])) {
				otherFunc(this.d3json.nodes[i]);
			} else if(!conditionFunc) { 
				otherFunc(this.d3json.nodes[i]);
			}
		}
		
		return this;
	};
	
	this.getNode = function(oid) { 
		for(var x=0; x<this.d3json.nodes.length; x++) { 
			var obj = this.d3json.nodes[x];
			if(obj.oid == oid) { return obj; } 
		}
		
		return null;
	};
	
	this.getFingerprint = function() { return jQuery.extend(true, {}, this.d3json.fingerprint); };
	
	this.isFoot = function(oid) { 
		try { return this.d3json.nodeTags[oid].foot; }
		catch (e) { return false; }		
	};

	this.isHead = function(oid) { 
		try { return this.d3json.nodeTags[oid].head; }
		catch (e) { return false; }		
	};
	
	this.additionalInfoAvailable = function(oid) {
		// The structure will have the node marked with the tag "more" if more info
		// is available.
		try { return this.d3json.nodeTags[oid].more; }
		catch (e) { return false; }
	};
	
	return this;
} // End ProvenanceGraph

function ProvenanceActor(json) { 
	this.json = json;
	
	this.getCreated = function() { 
		var c = this.json.created;
		return new Date(parseInt(c,10));
	};
	this.getName = function() { return this.json.name; };
	this.getType = function() { return this.json.type;  };
	this.getId = function() { return this.json.aid; };
	
	return this;
} // End ProvenanceActor

GLOBAL_SETTINGS = {
	n: 50,   // Max number of nodes per graph to return.
	maxHops: 8, // Max distance from starting point to fetch
	includeNodes: true, // whether results should include nodes
	includeEdges: true, // whether results should include edges
	includeNPEs: true, // whether results include non-provenance IDs
	followNPIDs: false, // whether graph discovery goes through NPIDs or not.
	forward: true, // whether to look forward
	backward: true, // whether to look backward
	breadthFirst: true
};

DEFAULT_ACTIVITY_OID = "urn:uuid:implus:999990000000000000000000000000000000";

/*
 * The Provenance object is the top-level way of interacting with a remote
 * Provenance API.
 */
function Provenance() {
	this.__settings = GLOBAL_SETTINGS;
	
	this.__genericAJAX = function(properties) { 
		$.ajax({
			url: properties.url,
			contentType:"application/x-javascript;",
			type: properties.type || "GET",
			success:function(datum) {				
				var data = jQuery.extend(true, {}, datum);
				
				if(properties.constructor) { 
					properties.success(properties.constructor(data));
				} else { 
					properties.success(data); 
				}
			},
			error: properties.error,			
			dataType:"json"
		});
		
		return this;
	};
	
	/**
	 * Get/fetch particular setting values.
	 */
	this.settings = function(settingName, newValue) { 
		if(!settingName) { return this.__settings; }
		if(settingName && !newValue) { return this.__settings[settingName]; }
		else { 
			this.__settings[settingName] = newValue;
			return newValue;
		}
		
		return this;
	};
	
	this.buildURLForGraph = function(oid) { 
		// First, ensure settings are correct or twiddle them if they're
		// not.   That way we don't have to trust the UI layer to get it
		// right.
		if(isNaN(parseInt(this.__settings.n))) { this.__settings.n = 20; } 
		if(isNaN(parseInt(this.__settings.maxHops))) { this.__settings.maxHops = 8; }
		
		if(this.__settings.n < 5) { this.__settings.n = 5; }
		if(this.__settings.n > 200) { this.__settings.n = 200; }
		if(this.__settings.maxHops > 200) { this.__settings.maxHops = 200; } 
		if(this.__settings.maxHops < 1) { this.__settings.maxHops = 1; } 
		
		return "/plus/api/graph/" + oid + "?" + 
		       "n=" + this.__settings.n + "&" + 
		       "maxHops=" + this.__settings.maxHops + "&" + 
		       "includeNodes=" + this.__settings.includeNodes + "&" +
		       "includeEdges=" + this.__settings.includeEdges + "&" + 
		       "includeNPEs=" + this.__settings.includeNPEs + "&" + 
		       "followNPIDs=" + this.__settings.followNPIDs + "&" +  
		       "forward=" + this.__settings.forward + "&" + 
		       "backward=" + this.__settings.backward + "&" + 
		       "breadthFirst=" + this.__settings.breadthFirst;
	};
	
	this.ensure = function(properties, requiredPropList) { 
		for (prop in requiredPropList) {
			if(!properties[requiredPropList[prop]]) {
				console.log(properties);
				throw "Missing " + requiredPropList[prop];
			}
		}
		
		return true;
	};
	
	this.getProvenanceGraph = function(properties) {
		this.ensure(properties, ['oid', 'success']);
		
		props = jQuery.extend(true, {}, properties); 
		
		props.type = "GET";
		props.constructor = ProvenanceGraph;
		props.url = this.buildURLForGraph(properties.oid);
		
		this.__genericAJAX(props);
        return this;
	}; // End getProvenanceGraph

	this.assertMarking = function(properties) {
		this.ensure(properties, ['oid', 'reason', 'type']);
		if(properties.type != "taint") throw "Unsupported type";
		
		$.ajax({
			type: "POST",
			data: properties.reason,
			url: "/plus/api/object/taint/" + properties.oid,
			contentType: "application/x-www-form-urlencoded; charset=UTF-8",
			datatype: "json",
			success: function(msg) {
				msg = jQuery.extend(true, {}, msg);
				properties.success(ProvenanceGraph(msg));
			},
			error: properties.error,
		});	

		return this;
	}; // End assertMarking

	this.search = function(properties) { 
		this.ensure(properties, ['searchTerm', 'success']);
		
		props = jQuery.extend(true, {}, properties);
		props.type = "POST";
		props.data = { searchTerm: properties.searchTerm };
		props.url = "/plus/api/object/search";
		props.contentType = "application/x-javascript";
		
		console.log("POSTING SEARCH");
		props.success = function(d) { 
			var data = jQuery.extend(true, {}, d);
			console.log("SEARCH RESULTS");
			console.log(data);
			return properties.success(ProvenanceGraph(data));
		};
		
		$.ajax(props);
	};
	
	this.getObject = function(properties) {
		this.ensure(properties, ['oid', 'success']);
		
		props = jQuery.extend(true, {}, properties); 
		
		props.type = "GET";
		props.constructor = ProvenanceGraph;
		props.url = "/plus/api/object/" + properties.oid; 
		
		this.__genericAJAX(props);		
		return this;
	};
	
	this.getWorkflowMembers = function(properties) { 
		this.ensure(properties, ['oid', 'success']);
		
		props = jQuery.extend(true, {}, properties);
		props.type = "GET";
		props.constructor = ProvenanceGraph;
		props.url = "/plus/api/workflow/" + properties.oid;
		
		this.__genericAJAX(props);		
		
		return this;
	};
	
	// Get a list of latest workflows.
	this.getWorkflows = function(properties) {
		this.ensure(properties, ['success']);
		
		props = jQuery.extend(true, {}, properties);
		props.type = "GET";
		props.constructor = ProvenanceGraph;
		props.url = "/plus/api/workflow/latest";
		
		this.__genericAJAX(props);		
		return this;
	};
	
	this.getObjectsByMetadata = function(properties) { 
		this.ensure(properties, ['key', 'value', 'success']);
		
		props = jQuery.extend(true, {}, properties);
		props.type = "GET";
		props.constructor = ProvenanceGraph;
		props.url = "/plus/api/object/metadata/" + properties.key + "/" + properties.value + "?format=json";
		
		this.__genericAJAX(props);
		return this;
	};
	
	// Get a list of objects owned by a particular actor ID
	this.getOwnedObjects = function(properties) { 
		this.ensure(properties, ['aid', 'success']);
		
		props = jQuery.extend(true, {}, properties);
		props.type = 'GET';
		props.constructor = ProvenanceGraph;
		props.url = "/plus/api/feeds/objects/owner/" + properties.aid + "?format=json";
		
		this.__genericAJAX(props);
		
		return this;
	};
	
	this.getOwners = function(properties) {
		this.ensure(properties, ['success']);
		
		props = jQuery.extend(true, {}, properties);
		props.type = 'GET';
		props.constructor = ProvenanceGraph;
		props.url = "/plus/api/feeds/objects/owners/?format=json";
		
		this.__genericAJAX(props);
		return this;		
	};
	
	this.getLatestNPIDs = function(properties) {
		this.ensure(properties, ['success']);
		
		props = jQuery.extend(true, {}, properties);
		props.type = 'GET';
		props.url = "/plus/api/feeds/externalIdentifiers?format=json";
		props.constructor = ProvenanceGraph;
		
		this.__genericAJAX(props);
		return this;
	};
	
	this.getLatestConnectedData = function(properties) {
		this.ensure(properties, ['success']);
		
		props = jQuery.extend(true, {}, properties);
		props.type = "GET";
		props.constructor = ProvenanceGraph;
		props.url = "/plus/api/feeds/connectedData?format=json&n=10";
		
		this.__genericAJAX(props);
		return this;
	};
	
	this.getLatestObjects = function(properties) {
		this.ensure(properties, ['success']);
		
		props = jQuery.extend(true, {}, properties);
		props.type = 'GET';
		props.constructor = ProvenanceGraph;
		props.url = "/plus/api/feeds/objects/latest?format=json";
		
		this.__genericAJAX(props);
		return this;
	}; 
	
	this.getActor = function(properties) { 
		this.ensure(properties, ['aid', 'success']);

		props = jQuery.extend(true, {}, properties); 
		
		props.type = "GET";
		props.constructor = ProvenanceActor;
		props.url = "/plus/api/actor/" + properties.aid; 
		
		this.__genericAJAX(props);		
		return this;
	};
	
	this.getTaint = function(properties) {
		this.ensure(properties, ['oid', 'success']);

		props = jQuery.extend(true, {}, properties);
		props.type = "GET";
		props.constructor = ProvenanceGraph;
		props.url = "/plus/api/object/taint/" + properties.oid;
		
		this.__genericAJAX(props);
		return this;
	}; // End getTaint
	
	this.cypherQuery = function(properties) { 
		this.ensure(properties, ['query', 'success']);
		
	    var props = jQuery.extend(true, {}, properties);
	    props.type = "POST";
	    props.url = "/plus/api/graph/search";
	    props.data = { "query" : properties.query };
	    props.success = function(data) { 
	    	console.log(data);
	    	properties.success(ProvenanceGraph(data));
	    };
	    	    
	    $.ajax(props);		
		return this;
	};
	
	this.deleteTaint = function(properties) { 
		this.ensure(properties, ['oid']);
		
		$.ajax({
			type: "DELETE",
			url: "/plus/api/object/taint/" + properties.oid,
			contentType: "application/x-www-form-urlencoded; charset=UTF-8",
			dataType: "json",
			success: function(msg) {
				var data = jQuery.extend(true, {}, msg);
				var pg = ProvenanceGraph(data);				
				properties.success(pg);
			},
			error: properties.error,			
		});		
		
		return this;
	}; // End deleteTaint
	
	this.getTimeSpan = function(properties) { 
		this.ensure(properties, ['oid', 'success']);

		props = jQuery.extend(true, {}, properties); 
		
		props.type = "GET";
		props.constructor = false;
		props.url = "/plus/api/fitness/" + properties.oid + "/timelag"; 
		
		this.__genericAJAX(props);		
		return this;
	};
		
	this.termFinder = function(properties) { 
		this.ensure(properties, ['oid', 'term', 'success']);
		
		props = jQuery.extend(true, {}, properties);
		props.type = "GET";
		props.constructor = ProvenanceGraph;
		props.url = "/plus/api/fitness/" + properties.oid + "/termFinder?term=" + properties.term;
		
		this.__genericAJAX(props);
		return this;
	};
	
	this.getSummary = function(properties) { 
		this.ensure(properties, ['oid', 'success']);
		
		props = jQuery.extend(true, {}, properties);
		props.type = "GET";
		props.constructor = false;
		props.url = "/plus/api/fitness/" + properties.oid + "/summary";
		
		this.__genericAJAX(props);
		return this;
	};
	
	this.getChainOfCustody = function(properties) { 
		this.ensure(properties, ['oid', 'success']);
		
		props = jQuery.extend(true, {}, properties);
		props.type = "GET";
		props.constructor = false;
		props.url = "/plus/api/fitness/" + properties.oid + "/custody";
		
		this.__genericAJAX(props);
		return this;
	};
	
	return this;
} // End Provenance

/*
SAMPLE NODE JSON:
	{
	"certainty": "100%",
	"privileges": [],
	"label": "NTUSER.DAT{016888bd-6c6f-11de-8d1d-001e0bcde3ec}.TM.blf",
	"ownerid": "",
	"type": "data",
	"id": "urn:uuid:mitre:plus:74f6880b-c512-4747-99ee-b39b1c5ac8aa",
	"sourceHints": "local",
	"created": 1376453531059,
	"oid": "urn:uuid:mitre:plus:74f6880b-c512-4747-99ee-b39b1c5ac8aa",
	"name": "NTUSER.DAT{016888bd-6c6f-11de-8d1d-001e0bcde3ec}.TM.blf",
	"subtype": "file",
	"originalPath": "C:\\Users\\dmallen\\NTUSER.DAT{016888bd-6c6f-11de-8d1d-001e0bcde3ec}.TM.blf",
	"metadata": {
	  "plus:reportTime": "1376439131575",
	  "plus:reporter": "unknown@127.0.0.1 ProvenanceClient 0.5"
	}
	}
	
SAMPLE EDGE JSON:  (source and target are indices into the node array)
    {
      "to": "1AEBFE94010863F5A543AAE940FFE6785AB1C3A5F0F0E7A8D4C695DF2B39E27F",
      "source": 0,
      "sourceHints": "local",
      "created": 1376439131059,
      "target": 32,
      "left": false,
      "label": "npe",
      "from": "urn:uuid:mitre:plus:74f6880b-c512-4747-99ee-b39b1c5ac8aa",
      "type": "npe",
      "right": false
    }
*/