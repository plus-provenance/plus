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
 * 
 * Original author: M. David Allen
 * Updated by: Zack Panitzke
 */

function colorMarkup(verdict) { 
	var base = "/plus/media/img/fitness/";
	var image = "";
	
	switch (verdict) {
		case 'G':	image = "greenOnlySensor.png"; break;
		case 'Y':	image = "yellowOnlySensor.png"; break;
		case 'R':	image = "redOnlySensor.png"; break;
		default: 	image = "noneSensor.png"; break;
	}
	
	return "<img width='90' border='0' src='" + base + image + "'/>";
};

// define the Assessor base class.
var Assessor = function(name, configurable) {
	this.id = Math.round(Math.random()*10000) % 10000;
	this.verdict = "";   // must be one of R, G, Y, or ""
	this.assessedId = "";
	this.name = name;
	this.configurable = configurable;
	this.configured = false;
};
Assessor.prototype.isConfigurable = function() { return this.configurable; }
Assessor.prototype.isConfigured = function() { return this.configured; };
Assessor.prototype.configure = function() { return !this.configurable; }
Assessor.prototype.getName = function() { return this.name; };
Assessor.prototype.getId = function() { return this.id; };
Assessor.prototype.getVerdict =	function() { return this.verdict; };
/**
 * @param selector the selector where resulting elements should be inserted.
 * @callback a function to call once completed; will be passed true or false, depending on success/fail of the run.
 */
Assessor.prototype.run = function(selector, callback) { throw "Implementing classes must implement run()"; };		
Assessor.prototype.getInput = function(title, message, value, callback) {
	// Reset some aspects about the dialog.
	$("#assessmentsDialog").attr("title", title);
	$("#dialogmessage").html(message);
	$("#assessmentDialogInput").val(value !== null ? value : "");
	
	$("#assessmentsDialog").dialog({
		autoOpen: true,
		height: 300,
		width: 350,
		modal: true,
		buttons: {
			"OK" : function() {
				callback($("#assessmentDialogInput").val());
				$(this).dialog("close");
				return false;
			}
		}, 
		close: function() { 				
			$(this).dialog("close");
		}
	});		
	
	return this;
};

var assessorTypes = [];
function makeAssessor(assessorBase, displayName) {
	assessorBase.prototype = Object.create(Assessor.prototype);
	assessorBase.prototype.constructor = assessorBase;
	
	// ugly hack until ES6 is supported
	var reg = /function ([^\(]*)/;
	var constructorName = reg.exec(assessorBase.toString())[1];
	assessorTypes.push({ "display": displayName, "constructor": constructorName })
}

function TaintAssessor() {
	Assessor.call(this, "Taint Assessor", false);
}
makeAssessor(TaintAssessor, "Taint Assessor");
/**
 * @param selector the selector where resulting elements should be inserted.
 * @callback a function to call once completed; will be passed true or false, depending on success/fail of the run.
 */
TaintAssessor.prototype.run = function (callback) {
	var me = this;

	if (getSelectedOID() == this.assessedId) {
		// Don't need to re-run.
		if(callback) { callback(true); }
	}
	
	this.assessedId = getSelectedOID();

	Provenance().getTaint({
		oid: getSelectedOID(),
		success: function(provGraph) {
			me.verdict = (provGraph.countNodes() == 0 ? "Y" : "R");
			console.log("TaintAssessor verdict: " + me.verdict);
			if(callback) { callback(true); }
		},
		error: function(jqXHR, textStatus, errorThrown) {
			console.log("Error fetching taint");
			me.verdict = "R";
			if(callback) { callback(false); }
		}
	});
};

function TermFinder() {
	Assessor.call(this, "Term Finder", true);
	this.term = "";
}
makeAssessor(TermFinder, "Term Finder");
/**
 * @param selector the selector where resulting elements should be inserted.
 * @callback a function to call once completed; will be passed true or false, depending on success/fail of the run.
 */
TermFinder.prototype.run = function(callback) {		
	var oid = getSelectedOID();
	var me = this;

	if (getSelectedOID() == this.assessedId) {
		// Don't need to re-run.
		if (callback) { callback(true); }
		return this;
	}

	this.assessedId = getSelectedOID();
	console.log("Running termFinder on term=" + this.term);
		
	Provenance().termFinder({
		oid: oid,
		term: this.term,
		success: function(provGraph) {
			var goodTermFound = provGraph.countNodes() > 0;
			me.verdict = (goodTermFound ? "G" : "Y");
			console.log("TermFinder verdict: " + me.verdict);
			if(callback) { callback(true); }
		},
		error: function(jqXHR, textStatus, errorThrown) {
			console.log("Error fetching TermFinder: " + textStatus + " " + errorThrown);
			me.verdict = "R";
			if(callback) { callback(false); }
		}
	});
};
TermFinder.prototype.configure = function (callback) {
	var me = this;
	this.getInput("Enter a term", "Please enter a term you expect in the provenance:", this.term,
		function (term) { 
			if(!(me.term === term)) {
				// Reset this value so when term changes, we'll recompute.
				me.assessedId = "";  
			}

			console.log("TermFinder reconfigured with term: " + me.term);
			me.term = term;
			me.name = "Term Finder: " + me.term;

			me.configured = true;
			if (callback) { callback(true); }
		}
	);
};

function BadTermFinder() {
	Assessor.call(this, "Bad Term Finder", true);
	this.term = "";
}
makeAssessor(BadTermFinder, "Bad Term Finder");
/**
 * @param selector the selector where resulting elements should be inserted.
 * @callback a function to call once completed; will be passed true or false, depending on success/fail of the run.
 */
BadTermFinder.prototype.run = function(callback) {
	var oid = getSelectedOID();
	var me = this;

	if(getSelectedOID() == this.assessedId) {
		// Don't need to re-run.
		if(callback) { callback(true); }
		return this;
	}
	// Note that we're assessing this one so we don't redo.
	this.assessedId = getSelectedOID();

	console.log("Running bad term finder on " + this.term);

	Provenance().termFinder({
		oid: oid,
		term: this.term,
		success: function(provGraph) {
			var badTermFound = provGraph.countNodes() > 0;
			me.verdict = (badTermFound ? "R" : "G");
			console.log("BadTermFinder verdict: " + me.verdict);
			if(callback) { callback(true); }
		},
		error: function(jqXHR, textStatus, errorThrown) {
			console.log("Error fetching BadTermFinder: " + textStatus + " " + errorThrown);
			me.verdict = "R";
			if(callback) { callback(false); }
		}
	});		
};
BadTermFinder.prototype.configure = function (callback) {
	var me = this;
	this.getInput("Enter a term", "Please enter a term you want to avoid in the provenance:", this.term,
		function(term) {
			if (!(me.term === term)) {
				// Reset this value so when term changes, we'll recompute.
				me.assessedId = "";  
			}
			
			console.log("BadTermFinder got: " + me.term);
			me.term = term;
			me.name = "Bad Term Finder: " + me.term;
			
			me.configured = true;
			if (callback) { callback(true); }
		}
	);
};

/*************************************************************************/

var Assessors = function() {
	this.assessorList = {};
	
	this.getAssessorByName = function(assessorName) {
		var func = window[assessorName];
		return new func();
	}
	
	this.add = function(assessor) {
		console.log("Adding Assessor " + assessor.getId());
		this.assessorList[assessor.getId()] = assessor;
		
		if (assessor.isConfigurable()) {
			var me = this;
			assessor.configure(function(configSuccess) {
				me.refresh(assessor.getId());
			});
		} else {
			this.refresh(assessor.getId());
		}
	};
	
	this.summarize = function() {
		return Object.keys(this.assessorList).join(" ");
	};
	
	this.remove = function(id) {
		var strippedId = String(id).replace("assessor-","");
		console.log("Removing Assessor " + strippedId);
		
		delete this.assessorList[strippedId];
		$("#" + id).remove();
		
		this.updateSummary();
	};
	
	this.configure = function(id) {
		var strippedId = String(id).replace("assessor-","");
		console.log("Configuring Assessor " + strippedId);
		var thisAssessor = this.assessorList[strippedId];
		
		if (thisAssessor.isConfigurable()) {
			var me = this;
			thisAssessor.configure(function(configSuccess) {
				me.refresh(strippedId);
			});
		}
	};
	
	this.refresh = function(id) {
		var me = this;
		console.log("Refreshing Assessor " + id);
		
		this.assessorList[id].run(function(runSuccess) {
			var rendered = me.render(id);
			if ($("#assessor-" + id).length > 0) {
				$("#assessor-" + id).replaceWith(rendered);
			} else {
				rendered.insertBefore("#blankAssessorRow");
			}
			me.updateSummary();
		});
	};
	
	this.refreshAll = function() {
		console.log("Refreshing all Assessors.");
		
		for (var key in this.assessorList) {
			if (!this.assessorList.hasOwnProperty(key)) { continue; }
			this.refresh(key);
		}
	};
	
	this.render = function(id) {
		console.log("Rendering assessor row with id: " + id);
		var assessor = this.assessorList[String(id).replace("assessor-","")];
		
		var newRow = $("#blankAssessorRow").clone();
		newRow.attr("id", "assessor-" + assessor.getId());
		newRow.find(".imgCell").html(colorMarkup(assessor.getVerdict()));
		newRow.find(".nameCell").html(assessor.getName());
		newRow.find(".configureLink").css("display", assessor.isConfigurable() ? "" : "none");
		newRow.css("display", "");
		
		return newRow;
	};
	
	this.getSummaryVerdict = function() {
		var verdicts = {G: 0, Y: 0, R: 0};
		var overall = 'Y';
		console.log("Updating summary verdict.");
		
		for (var key in this.assessorList) {
			if (!this.assessorList.hasOwnProperty(key)) { continue; }
			
			var verdict = this.assessorList[key].getVerdict();
			console.log(">>>> ID: " + key + " Verdict: " + verdict);
			if (verdict) { 
				verdicts[verdict] = verdicts[verdict] + 1;
			}
		}
		
		console.log ("Overall verdicts:");
		for (var key in verdicts) {
			console.log(">>>> VERDICT " + key + ": " + verdicts[key]);
		}
		
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
		if (verdicts.G && !verdicts.Y && !verdicts.R) { 
			// Clean "green" assessment -- all lights were green, or off.
			overall = 'G';
		} else if (!verdicts.G && verdicts.R) { 
			// Clean "red" assessment.  If you had reds, you get red, whether
			// or not there were any yellows.
			overall = 'R';
		} else if (verdicts.G && verdicts.R) {
			// Mixed reds and greens yields yellow.
			overall = 'Y';
		} else {
			// Default is no real information.
			overall = 'Y';
		}
		
		return overall;
	};
	
	this.updateSummary = function() {
		var verdict = this.getSummaryVerdict();
		$("#overallAssessmentCell").html(colorMarkup(verdict));
		$('#assessmentsDialog').dialog({autoOpen: false});
	};
	
	return this;
};
var assessors = new Assessors;

/**
 * Function to submit a custom cypher query and update results.
 */
function submitCustomQuery(q) {	
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