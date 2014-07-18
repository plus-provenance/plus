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

function colorMarkup(imgFilename) { 
	var base = "/plus/media/img/fitness/";		
	return "<img width='90' border='0' src='" + base + imgFilename + "'/>";
};

var Assessor = function () { 	
	// Set a unique ID once on create.
	this.id = Math.round(Math.random()*10000) % 10000;
	this.verdict = "";   // must be one of R, G, B, or ""
	this.assessedId = "";
	
	this.isConfigured = function() { return false; };
	
	this.getName = function() { return "Generic Assessor"; };
	this.getId = function() { return this.id; };
	this.getVerdict = function() { return this.verdict; };
	
	this.configureLink = function() { 
		return "<a onclick='configureAssessor(" + this.getId() + "); return false' href='#'>configure</a>";
	};	
	
	this.removeLink = function() { 
		return "<a onclick='removeAssessor(" + this.getId() + "); return false' href='#'>remove</a>";
	};

	/**
	 * @param selector the selector where resulting elements should be inserted.
	 * @callback a function to call once completed; will be passed true or false, depending on success/fail of the run.
	 */
	this.run = function(selector, callback) { throw "Implementing classes must implement run()"; };
	
	this.render = function() { 
		return "<tr><td>" + colorMarkup("greenOnlyOffSensor.png") + "</td><td>Blankety Blank</td><td></td></tr>";
	};
		
	this.getInput = function(title, message, callback) {
		// Reset some aspects about the dialog.
		$("#assessmentsDialog").attr("title", title);
		$("#dialogmessage").html(message);
		$("#assessmentDialogInput").val("");
		
		$("#assessmentsDialog").dialog({
			autoOpen: true,
			height: 300,
			width: 350,
			modal: true,
			buttons: {
				"OK" : function() {
					callback($("#assessmentDialogInput").val());
					$(this).dialog("close");
				}
			}, 
			close: function() { 				
				$(this).dialog("close");
				refreshAssessors();
			}
		});		
		
		return this;
	};
	
	return this;
};

var TaintAssessor = function() { 
	this.configure = function () { 
		return true;
	};

	this.getName = function() { return "Taint Assessor"; };
	
	/**
	 * @param selector the selector where resulting elements should be inserted.
	 * @callback a function to call once completed; will be passed true or false, depending on success/fail of the run.
	 */
	this.run = function (selector, callback) {
		var me = this;
		
		if(getSelectedOID() == this.assessedId) {
			// Don't need to re-run.
			$(selector + " tr:last").after(this.render());
			if(callback) { callback(true); } 
			return this;
		} else { 
			// Note that we're assessing this one so we don't redo.
			this.assessedId = getSelectedOID();
		}
		
		Provenance().getTaint({
			oid: getSelectedOID(),
			success: function(provGraph) {
				this.verdict = (provGraph.countNodes() == 0 ? "" : "R");
				console.log("TaintAssessor: verdict => " + this.verdict);
				$(selector + ' tr:last').after(me.render());
				if(callback) { callback(true); } 
			},
			error: function(jqXHR, textStatus, errorThrown) {
				console.log("Error fetching taint");
				this.verdict = "R";
				$(selector + ' tr:last').after(me.render());
				if(callback) { callback(false); }
			}
		});
	};
	
	this.render = function() { 				
		return '<tr>' +
		'	<td><img width="90" border="0" src="/plus/media/img/fitness/redOnlyOffSensor.png"/></td>' +          
	    '   <td style="white-space:nowrap; padding-left:4px">Taint Assessor</td>' + 
	    '<td>' + this.removeLink() + "</td>" + 		 
		'</tr>';		
	};
	
	this.configure = function() { ; };
	this.isConfigured = function() { return true; };
	return this;
};
TaintAssessor.prototype = new Assessor;

var TermFinder = function() {
	this.super = Assessor();
	this.term = "";
	
	this.isConfigured = function() { return term && term != ''; };
	
	this.getName = function() { return "Term Finder"; };
	
	this.render = function() {
		var color = (this.verdict ==="G" ? colorMarkup("greenOnlySensor.png") : colorMarkup("greenOnlyOffSensor.png"));
		console.log("TermFinder verdict " + this.verdict + " => " + color);
		return "<tr><td>" + color + "</td><td>Term Finder: " + this.term + "</td><td>" + 
			   this.configureLink() + "; " + this.removeLink() + 
		       "</td></tr>";
	};
	
	/**
	 * @param selector the selector where resulting elements should be inserted.
	 * @callback a function to call once completed; will be passed true or false, depending on success/fail of the run.
	 */
	this.run = function(selector, callback) {		
		var oid = getSelectedOID();
		var me = this;
		
		if(getSelectedOID() == this.assessedId) {
			// Don't need to re-run.
			$(selector + " tr:last").after(this.render());
			if(callback) { callback(true); }
			
			return this;
		} else { 
			// Note that we're assessing this one so we don't redo.
			this.assessedId = getSelectedOID();
		}
		
		console.log("Running termFinder on term=" + me.term);
				
		Provenance().termFinder({
			oid: oid,
			term: me.term,
			success: function(provGraph) {
				var goodTermFound = provGraph.countNodes() > 0;
				me.verdict = (goodTermFound ? "G" : "");
				console.log("TermFinder: verdict => " + me.verdict);
				$(selector + ' tr:last').after(me.render());
				if(callback) { callback(true); }
			},
			error: function(jqXHR, textStatus, errorThrown) {
				console.log("Error fetching termFinder: " + textStatus);
				this.verdict = "R";
				$(selector + ' tr:last').after(me.render());
				if(callback) { callback(false); }
			}
		});
	};
	
	this.configure = function () {
		var me = this;
		this.getInput("Enter a term", "Please enter a term you expect in the provenance:",
				function(term) { 
					if(!(me.term === term)) {
						// Reset this value so when term changes, we'll
						// recompute.
						me.assessedId = "";  
					}
			
					me.term = term;					
					console.log("TermFinder:  got " + me.term);
										
					return term;
				});
	};
	
	return this;
};
TermFinder.prototype = new Assessor;

var BadTermFinder = function() { 
	this.super = Assessor();
	this.term = "";

	this.getName = function() { return "Bad Term Finder"; };
	
	this.render = function() {
		var color = (this.verdict === "R" ? colorMarkup("redOnlySensor.png") : colorMarkup("redOnlyOffSensor.png"));
		console.log("BadTermFinder verdict " + this.verdict + " => " + color);
		return "<tr><td>" + color + "</td><td>Bad Term Finder: " + this.term + "</td>" + 
		       "<td>" + 
		       this.configureLink() + "; " + this.removeLink() + "</td></tr>";		
	};
	
	/**
	 * @param selector the selector where resulting elements should be inserted.
	 * @callback a function to call once completed; will be passed true or false, depending on success/fail of the run.
	 */
	this.run = function(selector, callback) {
		var oid = getSelectedOID();
		var me = this;
		
		if(getSelectedOID() == this.assessedId) {
			// Don't need to re-run.
			$(selector + " tr:last").after(this.render());
			if(callback) { callback(true); }
			return this;
		} else { 
			// Note that we're assessing this one so we don't redo.
			this.assessedId = getSelectedOID();
		}
		
		console.log("Running bad term finder on " + me.term);
		
		Provenance().termFinder({
			oid: oid,
			term: me.term,
			success: function(provGraph) {
				var badTermFound = provGraph.countNodes() > 0;
				me.verdict = (badTermFound ? "R" : "");
				console.log("BadTermFinder: verdict => " + me.verdict);
				$(selector + ' tr:last').after(me.render());
				if(callback) { callback(true); }
			},
			error: function(jqXHR, textStatus, errorThrown) {
				console.log("Error fetching termFinder: " + textStatus);
				this.verdict = "R";
				$(selector + ' tr:last').after(me.render());
				if(callback) { callback(false); }
			}
		});		
	};
	
	this.isConfigured = function() { return term && term != ''; };
	
	this.configure = function () {
		var me = this;
		
		this.getInput("Enter a term", "Please enter a term you want to avoid in the provenance:",
				function(term) {
					if(!(me.term === term)) {
						// Reset this value so when term changes, we'll
						// recompute.
						me.assessedId = "";  
					}

					me.term = term;
					console.log("BadTermFinder:  got " + me.term);	
					return term;
				});
				
		return this; 
	};
	
	return this;
};
BadTermFinder.prototype = new Assessor;

/*************************************************************************/

var assessors = [new TaintAssessor()];

function summarize() { 
	var v = "";
	for(var i in assessors) { 
		v = v + assessors[i].getId() + " ";		
	}
	return v;
}

function configureAssessor(id) { 
	for (var i in assessors) { 
		var a = assessors[i];
		
		if(a.getId() == id) {
			a.configure();
		}
	}
	
	// One of them changed, so we re-render all of them.
	// This is easier than trying to surgically remove one
	// row and change it.
	refreshAssessors();
}

function removeAssessor(id) { 
	var newlist = [];
	
	for(var i in assessors) { 
		var a = assessors[i];
		
		if(a.getId() == id) { continue; } 
		else { newlist.push(a); }
	}
	
	assessors = newlist;
	refreshAssessors();
} // End removeAssessor

/**
 * Remove all assessment rows from the table.  Re-run all assessors as needed.  Once all
 * assessors have been run again, re-compute the summary assessment.
 */
function refreshAssessors() { 
	// Remove all but first row.
	console.log("Refreshing " + assessors.length + " total assessors");
	$("#assessmentTable").find("tr:gt(0)").remove();
			
	function runAssessorIdx(idx, assessors) {
		if(idx >= assessors.length || idx < 0) { throw "Invalid index"; }
		
		// Run the idx'th assessor, then wire a callback to daisy-chain the methods together.
		// This is necessary because we're single-threaded here.
		console.log("runAssessorIdx(" + idx + ")");
		assessors[idx].run("#assessmentTable", 
				function (succeeded) {					
					if(!succeeded) { console.log("Assessor #" + idx + " failed to complete"); }
					else { console.log("Assessor #" + idx + " succeeded."); }
					
					if(idx == (assessors.length - 1)) {
						// The final assessor has finished running.   Now we can update the verdict table.
						updateSummaryVerdict();
						return;						
					}
					
					runAssessorIdx((idx + 1), assessors); // Go to the next assessor.
				});		
	} // End runAssessorIdx
	
	$("#fitnessWidgetsLoadingAnim").show();
	runAssessorIdx(0, assessors); // Run this just-defined thing on the first item, starting the daisy chain.		
} // End refreshAssessors

/**
 * Looks through all of the assessors, queries them for a final verdict, creates a summary
 * verdict, and updates the DOM to display that.
 */
function updateSummaryVerdict() {
	var verdicts = {G: 0, Y: 0, R: 0};
	console.log("Update summary verdict");
	
	$("#fitnessWidgetsLoadingAnim").hide();
	
	for(var idx in assessors) {
		// Increment the number of times we've seen each verdict.
		var verdict = assessors[idx].getVerdict();
		if(verdict) { 
			verdicts[verdict] = verdicts[verdict] + 1; 
		}
	} // End for
			
	for(var key in verdicts) {
		console.log("VERDICT " + key + ": " + verdicts[key]);
	}
	
	var overall = "yellowOnlySensor.png";
	
	// At this point, using the contents of verdicts, we have to determine
	// what the overall verdict is.  A value for a flag (red/green/yellow)
	// is false if we didn't see any.
	//
	// There are 8 possibilities, corresponding to G, Y, R (on/off)
	// They are:
	// GYR => Y
	// GY  => Y
	// G   => G
	// G R => Y
	//  YR => R
	//   R => R
	//  Y  => Y
	//     => Y
	if(verdicts.G && !verdicts.Y && !verdicts.R) { 
		// Clean "green" assessment -- all lights were green, or off.
		overall = "greenOnlySensor.png";
	} else if(!verdicts.G && verdicts.R) { 
		// Clean "red" assessment.  If you had reds, you get red, whether
		// or not there were any yellows.
		overall = "redOnlySensor.png";
	} else if(verdicts.G && verdicts.R) {
		// Mixed reds and greens yields yellow.
		overall = "yellowOnlySensor.png";
	} else {
		// Default is no real information.
		overall = "yellowOnlySensor.png";
	}	
	
	// Set the overall assessment cell to the right stoplight.
	$("#overallAssessmentCell").html(colorMarkup(overall));
	// $("#assessmentTable").styleTable();	
} // End refreshAssessors

/**
 * Adds a new assessor to the table, according to the assessor type the user has chosen.
 */
function addAssessor() { 
	var assessorType = $("#assessorType").val();
	
	console.log("Pre add: " + summarize());
	// Each assessor type has a corresponding function.
	var func = window[assessorType];
		
	var instance = new func();
	console.log("Adding (+) instance " + instance.getId() + " to " + summarize());
	assessors.push(instance);
		
	// Call whatever instance-specific configuration is necessary.
	console.log("Pre configure: " + summarize());
	instance.configure();
	
	// Refresh runs them as needed and updates display.
	refreshAssessors();
} // End addAssessor

function updateAssessorTab(selector) {
	var html = ('<table id="assessmentTable" width="100%">' + 
	            '   <tbody>' + 
		        '    <tr>' + 
						'<td style="padding-left: 2px" id="overallAssessmentCell">' +
						'	<img width="90" border="0" src="/plus/media/img/fitness/yellowSensor.png"/>' +
						'</td>' +
						'<td style="white-space: nowrap; padding-left: 3px">Overall Assessment</td>' +
						'<td>' +
						'	<select id="assessorType" name="assessorType">' +
						'		<option value="TaintAssessor">Taint Assessor</option>' +
						'		<option value="TermFinder">Term Finder</option>' +
						'		<option value="BadTermFinder">Bad Term Finder</option>' +
						'	</select>' +
						'	<input onclick="addAssessor(); return false" ' +
						'          alt="Add Assessor" ' + 
						'          type="image" border="0" src="/plus/media/img/fitness/add.gif"></input>' +
						'   <img id="fitnessWidgetsLoadingAnim" style="visibility: hidden" src="/plus/media/img/loading.gif" border="0" alt=""/>' + 
						'</td>' +
					'</tr>' +
					'</tbody>' +
				'</table>');
	
	var dialog = "<div id='assessmentsDialog' title=''>" +
    				"<p id='dialogmessage'></p>" +  
    				"<form>" + 
    					"<input type='text' name='assessmentDialogInput' id='assessmentDialogInput' class='text ui-widget-content ui-corner-all' />" + 
    				"</form>" + 
    			"</div>";
	
	$(selector).html(html + dialog);
	// $("#assessmentTable").styleTable();
	refreshAssessors();
	
	$('#assessmentsDialog').dialog({
		autoOpen:  false
	});
	return false;
} // End updateAssessorTab

/**
 * Function to submit a custom cypher query and update results.
 */
function submitCustomQuery() { 
	var q = $("#query").val();
	
	Provenance().cypherQuery({
		query : q,
		success: function(provGraph) {
			var html = "<p><strong>Results:</strong></p><ul>";
			
			provGraph.nodes(function (n) { 
				html = html + "<li>" + n.name + "</li>";
			});
			
			html = html + "</ul>";
			
			$("#customQueryResults").html(html);
		},
		error: function(jqXHR, textStatus, errorThrown) {			
			$("#customQueryResults").html(
					"Error executing custom query: <pre>" +
					jqXHR.responseText + "</pre>"
			);
		} 
	});
}

function updateQueryTab(selector) { 
	var oid = getSelectedOID();
	
	var html = ("<p>Query:</p>" + 
			    "<form onsubmit='submitCustomQuery(); return false' name='customQuery'>" +
			    "<textarea id='query'  name='query' cols='70' rows='5'>" +
			    "MATCH (n:Provenance {oid: \"" + oid + "\"})\n" + 
			    "return n;" + 
			    "</textarea><br/>" + 
			    "<input type='submit' name='submit' value='Submit'/>" + 
			    "</form>" + 			    
			    "<div id='customQueryResults'></div>"
			    );
	
	$(selector).html(html);
} // End updateQueryTab

function updateCustodyTab(selector) {
	var oid = getSelectedOID();
	$(selector).html(loadingMarkup());

	Provenance().getChainOfCustody({
		oid: oid,
		success: function(data) {					
			$(selector).attr("oid", oid);
			
			// Keep track of the last ID we saw to avoid duplicates,
			// when objects go through a long chain of the same owner.
			var lastId = '';
			
			if(data && data.length > 0) { 			
				var html = "<ul>";
	
				for(var x in data) {
					// console.log(data[x]);
					var actor = ProvenanceActor(data[x]);
					var id = actor.getId();
					
					if(id == lastId) { continue; } 
					else { 
						lastId = id; 
						html = html + "<li>" + actor.getName() + "/" + actor.getType() + "</li>";
					} 
				}
	
				html = html + "</ul>";
				$(selector).html(html);
			} else { 
				$(selector).html("<p>No chain of custody or owner information available.  Data " + data);
			}
		},
		error: function(jqXHR, textStatus, errorThrown) {
			$(selector).html("Unable to fetch chain of custody information: " + textStatus);
		}					
	});
}

function updateSummaryTab(selector) { 
	var oid = getSelectedOID();
	$(selector).html(loadingMarkup());
	Provenance().getSummary({
		oid: oid,
		success: function(data) { 
			$(selector).attr("oid", oid);  
			$(selector).html(data.summary);
		},
		error: function(jqXHR, textStatus, errorThrown) {
			$(selector).html("Unable to fetch summary information: " + textStatus);
		}		
	});					
}

function updateTimespanTab(selector) {
	var oid = getSelectedOID();
	$(selector).html(loadingMarkup());

	Provenance().getTimeSpan({
		oid: oid,
		success: function(data) {
			$(selector).attr("oid", oid);

			var oldest = data.oldest;
			var newest = data.newest;
			var timespan = data.timespan;

			var html = "<p>" + 
			"<a href='?" + oldest + "'>Oldest</a><br/>" + 
			"<a href='?" + newest + "'>Newest</a><br/>" + 
			"Time span: " + timespan + "</p>";
			$(selector).html(html);
		},
		error: function(jqXHR, textStatus, errorThrown) {
			$(selector).html("Unable to fetch time span information: " + textStatus);
		}			
	});
}
