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


/**
 * Add a "styleTable" method to jQuery for consistent table styling.
 */
(function ($) {
	$.fn.styleTable = function (options) {
		var defaults = { css: 'styleTable tablesorter' };
		options = $.extend(defaults, options);

		return this.each(function () {
			input = $(this);
						
			input.addClass(options.css);

			input.find("tr").on('mouseover mouseout', function (event) {
				if (event.type == 'mouseover') {
					$(this).children("td").addClass("ui-state-hover");
				} else {
					$(this).children("td").removeClass("ui-state-hover");
				}
			});

			input.find("th").addClass("ui-state-default");
			input.find("td").addClass("ui-widget-content");

			input.find("tr").each(function () {
				$(this).children("td:not(:first)").addClass("first");
				$(this).children("th:not(:first)").addClass("first");
			});
			
			// Set up sortable tables.
			// $(this).tablesorter();
		});
	};
})(jQuery);


/**
 * Generic error function suitable as a standin for most AJAX calls.
 * Does console logging only.
 */
var genericError = function(msg) {
	return function(jqXHR, textStatus, errorThrown ) {
		console.log("failure: " + msg);
		console.log(errorThrown);
		console.log(jqXHR);
		console.log(textStatus);
	};
};

/**
 * Returns markup for an AJAX "loading" animation.
 */
function loadingMarkup() {
	return "<span style='float:center; vertical-align: middle;'><img src='/plus/media/img/loading.gif' alt='Loading...'/></span>";
}

/**
 * Inserts a generic application error message into the DOM
 * @param message the message
 * @param selector (optional) the selector where it will be placed, or D3-CANVAS otherwise.
 */
function applicationError(message, selector) {
	if(selector) {
		$("#D3-CANVAS").html("<h2>Error</h2>" + "<p>" + message + "</p>");	
	} else {
		$("#D3-CANVAS").html("<h2>Error</h2>" + "<p>" + message + "</p>");
	}	
}

function showOwnedObjects(aid, name, sel) {	
	$(sel).html(loadingMarkup());
	
	Provenance().getOwnedObjects({
		aid: aid,
		success: function(g) {
			tableInject(sel, generateObjectTable(g, name + ": Owned Objects", sel));
		},
		error: genericError("Show owned objects (" + aid + ") ")
	});
	
	return true;
} // End showOwnedObjects

function workflowDetail(oid, selector) {
	$(selector).html(loadingMarkup());
	
	Provenance().getWorkflowMembers({
		oid: oid,
		success: function(g) { 
			tableInject(selector, generateObjectTable(g, "Workflow Detail", selector));
		},
		error: function(){
			$(selector).html("<h2>Error</h2>" + "Error getting workflow members for " + oid);
		}
	});
} // End workflowDetail

function tableInject(selector, table) {
	$(selector).html(table);
	$(selector + " table").styleTable();
	$(selector + " table").tablesorter();
} // End tableInject

function generateActorTable(provGraph, title, sel) {
	var table = "<table id='actorTable' border='1' class='ui-widget tablesorter'>" + 
	            "<thead class='ui-widget-header'><tr>" +  
	            "<th>Name</th><th>Created</th>" + 
	            "</tr>" + 
	            "</thead><tbody class='ui-widget-content'>";
	
	provGraph.actors(function (actorJson) {
		// TODO - link to what exactly? 
		// Injecting this content back into the same page gets confusing.
		var actor = ProvenanceActor(actorJson);
		
		var link = "<a class='tree' href='#' onclick='showOwnedObjects(\"" + 
		           actor.getId() + "\", \"" + actor.getName() + 
		           "\", \"" + sel + "\")'>" + actor.getName() + "</a>";
		
		table = table + "<tr><td>" + actor.getName() + "</td><td>" + actor.getCreated() + "</td></tr>";
	});
	
	if(title) { 
		table = "<h2>" + title + "</h2>" + table;
	}
	
	return table;
} // End generateActorTable

function linkProvenanceObject(node) {	
	if(node.type == 'npid') {
		// Non-provenance objects don't have an oid
		return "<a href='/plus/view.jsp?" + node.id + "'>" + node.label + "</a>";
	} else {
		return "<a href='/plus/view.jsp?" + node.oid + "'>" + node.name + "</a>";
	}
}

function generateNPIDTable(provGraph, title, sel) {
	
}

/**
 * Generates HTML suitable for displaying a list of provenance objects.
 * @param provGraph the ProvenanceGraph object containing the list of objects.
 * @param title the title for the table
 * @param sel a selector for downstream links; content fetched from those links will be injected into this selector.
 * @returns {String}
 */
function generateObjectTable(provGraph, title, sel) {
	var preamble = "";
	
	if(title) { preamble = "<h2>" + title + "</h2>"; }
	
	if(provGraph.countNodes() <= 0) {
		var t = "<p>No objects are available in response to your request.  " + 
		        "This may indicate that you do not have the proper permissions to see the data.";		
		return preamble + t;
	}
	
	var table = "<table id='objectsTable' border='1' class='ui-widget tablesorter'>" +
	"<thead class='ui-widget-header'><tr>" + 
		"<th>Description</th>" +
		"<th>Object Type</th>"+          
		"<th>Created</th>"+
        "</tr></thead>"+
	"<tbody class='ui-widget-content'>";

	provGraph.nodes(function (node) {
		var name = node.label;
		var thisoid = node.oid;
		var type = node.type + "/" + node.subtype;
		var created = new Date(parseInt(node.created,10));
		
		link = linkProvenanceObject(node);
		
		// Don't browse straight to workflow elements, browse to a list of
		// their contents.
		if(node.type == 'workflow') { 
			link = '<a href="#" onclick="workflowDetail(\'' + thisoid + 
			       '\', \'' + sel + '\')">' + name + '</a>';
		} 
		
		table = table + "<tr><td>" + link + "</td>" + 
				"<td>" + type + "</td><td>" + created + "</td></tr>";
	});

	table = table + "</tbody></table>";				

	return preamble + table;
} // End generateObjectTable

//Perform a search for "searchTerm".  
//Generate an HTML table of results, and inject it into the provided jQuery selector.
function doSearch(searchTerm, injectResultsIntoSelector) {
	$(injectResultsIntoSelector).html(loadingMarkup());
	
	Provenance().search({
		searchTerm : searchTerm,
		success : function(g) { 
			tableInject(injectResultsIntoSelector, generateObjectTable(g, "Search:  " + searchTerm, injectResultsIntoSelector));
		},
		error: genericError("Search (" + searchTerm + ") ")
	});
	
	return true;
} // End doSearch

function submitSearch(searchTermSel, targetSel) {
	var term = $(searchTermSel).val();
	
	console.log("SubmitSearch " + searchTermSel + ", " + targetSel);
	// Display the loading animation while search proceeds.
	$(targetSel).html(loadingMarkup());

	// Perform the search, inject the results into D3-CANVAS
	doSearch(term, targetSel);
	return false;
} // End submitSearch
