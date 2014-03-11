<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@page import="org.mitre.provenance.user.OpenIDUser"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<% OpenIDUser user = (OpenIDUser)session.getAttribute("plus_user"); %>
<head>
	<title>Provenance</title>

	<link rel="stylesheet" type="text/css" href="media/css/app.css" />
	
	<script src="//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
	<script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.10.3/jquery-ui.min.js"></script>
    <script src="//cdnjs.cloudflare.com/ajax/libs/jquery-layout/1.3.0-rc-30.79/jquery.layout.min.js"></script>
	
	<!-- link rel="stylesheet" type="text/css" href="//ajax.googleapis.com/ajax/libs/jqueryui/1.10.3/themes/ui-lightness/jquery-ui.css" /-->
		
	<script src="media/js/d3.v3.min.js"></script>
	<script type="text/javascript" src="media/js/dagre.js"></script>
	
	<script src="media/js/jquery.tablesorter.js"></script>
	<script src="media/js/ui.js"></script>
	<script src="media/js/provenance.js"></script>
	<script src="media/js/fitness.js"></script>
	<script src="media/js/provenanceVis.app.js"></script>

	<link rel="stylesheet" type="text/css" href="//ajax.googleapis.com/ajax/libs/jqueryui/1.10.3/themes/pepper-grinder/jquery-ui.css" />
	<link rel="stylesheet" type="text/css" href="media/js/tablesorter/style.css"/>

	<script>	
	var myLayout = null; // a var is required because this page utilizes: myLayout.allowOverflow() method

	function pageOidSubmit(value) { 
		// Display the loading animation while search proceeds.
		$("#D3-CANVAS").html(loadingMarkup());
		oidSubmit(value);
	}
		
	function configure() {
		$("#maxNodes").val(GLOBAL_SETTINGS.n);
		$("#maxHops").val(GLOBAL_SETTINGS.maxHops);
		
		// Various boolean settings...
		var boolSettings = ["includeNPEs", "forward", "backward", "followNPIDs", "breadthFirst"];
		
		for(var i in boolSettings) { 
			var settingName = boolSettings[i];
			if(GLOBAL_SETTINGS[settingName]) {
				$("#" + settingName).attr("checked", "true");
			} else { 
				$("#" + settingName).attr("checked", "false");
			}
		}
		
		$("#configurationDialog").dialog({
			autoOpen: true,
			width: "auto",
			height: "auto",
			buttons: {
				"OK" : function() {					
					for(var i in boolSettings) {
						var settingName = boolSettings[i];
					
						if($("#" + settingName).is(":checked")) {							
							GLOBAL_SETTINGS[settingName] = true;
						} else { GLOBAL_SETTINGS[settingName] = false; }
						
						console.log("Set " + settingName + " => " + GLOBAL_SETTINGS[settingName]);
					}
					
					GLOBAL_SETTINGS.n = $("#maxNodes").val();
					GLOBAL_SETTINGS.maxHops = $("#maxHops").val();
				
					console.log("Set n=" + GLOBAL_SETTINGS.n + ", maxHops=" + GLOBAL_SETTINGS.maxHops);
					
					// Resubmit the selected OID to force a reload from
					// the server with the new settings.
					pageOidSubmit(getSelectedOID());
					$(this).dialog("close");
				},
			   "Cancel" : function() { 
				   // FINE!  I didn't want to change settings anyway!
				   $(this).dialog("close");   
			   }	
			}, 
			close: function() { 				
				$(this).dialog("close");
				refreshAssessors();
			}
		});
	} // End configure
			
	function checkPopulateFitnessWidget(selector) {
		var oid = getSelectedOID();
		
		// Some widgets are sensitive to changes in OID.
		// Those only need to be reloaded/updated when the 
		// OID changes.
		if($(selector).attr("oid") == oid) { 
			console.log("No update needed");
			return;
		} 
		
		if(selector == '#summaryTab') { updateSummaryTab(selector); } 
		else if(selector == '#queryTab') { updateQueryTab(selector); }  
		else if(selector == '#assessorTab') { updateAssessorTab(selector); } 
		else if(selector == '#timeSpanTab') { updateTimespanTab(selector); } 
        else if(selector == '#custodyTab') { updateCustodyTab(selector); } 
		else { 
			console.log("Error: undefined selector " + selector);
		}		
	} // End checkPopulateFitnessWidget
	
	$(document).ready(function () {
		myLayout = $('body').layout({
			// enable showOverflow on west-pane so popups will overlap north pane
			west__showOverflowOnHover: false
		});
		
		// $(".ui-layout-west").width("500px");
		myLayout.sizePane("west", (screen.width/5 - 20));
		
		$("#tabs").tabs();
		$("#fitnessWidgetTabs").tabs({
			activate: function( event, ui ) {
				console.log(event);
				console.log(ui);			
				checkPopulateFitnessWidget(ui.newPanel.selector);
			}			
		});		
				
		// Style buttons
		$( "input[type=submit]").button();		
		
		$("#configurationDialog").dialog({
			autoOpen: false
		});
			
		// This can control whether south starts out hidden or not.
		// myLayout.toggle("south"); 
		
		// Call the main d3 initialization code.		
		init();
				
		var queryString = window.location.search;
		
		if(queryString.indexOf("?") != -1) { 
			$("#oidsubmit").attr('oid', queryString.substr(1));

			// Display the loading animation while search proceeds.
			$("#D3-CANVAS").html(loadingMarkup());			
			
			pageOidSubmit(queryString.substr(1));
		}
				
		// Populate the summary tab as necessary.
		checkPopulateFitnessWidget("#summaryTab");
		$(document).tooltip();
 	});	
	</script>

	<style type="text/css">
	/**
	 *	Basic Layout Theme
	 * 
	 *	This theme uses the default layout class-names for all classes
	 *	Add any 'custom class-names', from options: paneClass, resizerClass, togglerClass
	 */

	.ui-layout-pane { /* all 'panes' */ 
		background: #FFF; 
		border: 1px solid #BBB; 
		padding: 10px; 
		overflow: auto;
	} 

	.ui-layout-resizer { /* all 'resizer-bars' */ 
		background: #DDD; 
	} 

	.ui-layout-toggler { /* all 'toggler-buttons' */ 
		background: #AAA; 
	} 
	</style>

	<style type="text/css">
	body {
		font-family: Arial, sans-serif;
		font-size: 0.85em;
	}
	p {
		margin: 1em 0;
	}
	ul {
		/* rules common to BOTH inner and outer UL */
		z-index:	100000;
		margin:		1ex 0;
		padding:	0;
		list-style:	none;
		cursor:		pointer;
		border:		1px solid Black;
		/* rules for outer UL only */
		width:		15ex;
		position:	relative;
	}
	ul li {
		background-color: #EEE;
		padding: 0.15em 1em 0.3em 5px;
	}
	ul ul {
		display:	none;
		position:	absolute;
		width:		100%;
		left:		-1px;
		/* Pop-Up */
		bottom:		0;
		margin:		0;
		margin-bottom: 1.55em;
	}
	.ui-layout-north ul ul {
		/* Drop-Down */
		bottom:		auto;
		margin:		0;
		margin-top:	1.45em;
	}
	ul ul li		{ padding: 3px 1em 3px 5px; }
	ul ul li:hover	{ background-color: #FF9; }
	ul li:hover ul	{ display:	block; background-color: #EEE; }
	</style>
</head>
<body>

<!-- manually attach allowOverflow method to pane -->
<div class="ui-layout-north" onmouseover="myLayout.allowOverflow('north')" onmouseout="myLayout.resetOverflow(this)">
	<div class="ui-widget-header">
	<table border="0">
		<tr>
			<td><a href="/plus/"><img border="0" src="media/img/tci_logo.png" alt="Provenance: Linking and Understanding Sources (PLUS)"/></a></td>
			<td><h1><a href="/plus/">Provenance</a></h1></td>
			<td class="center">
			    <form onsubmit='submitSearch("#searchTerm", "#D3-CANVAS"); return false'>
					<input type="text" name="searchTerm" id='searchTerm' size="70" title="Enter search term here" value=""/> 
					<input id='searchSubmit' type='submit' value='Search'/>			
			    </form>
			</td>
		</tr>
	</table>
	
	<div width='100%' style='text-align:right;'><%=user.getDisplayName()%> / <%=user.getEmail()%></div> <!--  / <%=user.getUserIdentifier()%> / <%=user.getName() %> / <%=user.getId() %> -->
	</div>
</div>

<!-- allowOverflow auto-attached by option: west__showOverflowOnHover = true -->
<div class="ui-layout-west">
	<div id="tabs" style="width: 100%; height: 95%">
		<ul style="width: 95%">
			<li><a href="#objSumTab">Current Item</a>			
			<!--  span class="ui-icon ui-icon-close" role="presentation">Remove Tab</span -->
			</li>
			<li><a href='#graphStatsTab'>Graph Stats</a></li>
			<li id="addTabHeader"><a href="#addTab">+</a></li>
		</ul>
		
		<div id="objSumTab">
			<div id="metaDataLabel" class="ui-widget-header" style="vertical-align: middle; text-align: center; width:100%;">Data Provenance</div>
				
			<div class="ui-widget-content" style="height: 97%; width: 100%">
				<div id="metaData"></div>
				</br></br>
				<button id="removeTaint" onclick="showAddTaint()">Add Marking</button>
				
				<div id="taintDiv" style="display: none;">
					<form action="javascript:addTaintForm();">
						Marking text: <textarea name="taintReason" id='taintReason' rows="1" style="width: 98%; margin: 0px; height: 70px;"></textarea>	
						<!--input type="text" name="taintReason" id='taintReason' size="70"-->
						<input id="taintSubmitButton" type="submit" value="Confirm Taint">											
					</form>
				</div>
				
				<button id="removeTaint" onclick="removeTaintButton()">Remove Marking</button>
			</div>
		</div>
		
		<div id='graphStatsTab'>
			<div class='ui-widget-header' style='vertical-align: middle; text-align: center; width=100%;'>Properties of this Provenance Graph</div>
			<div class="ui-widget-content" style="height: 97%; width: 100%">
				<div id='graphStats'>
				</div>
			</div>
		</div>
				
		<div id="addTab">		
			<select id="add_tab_selector"></select>
			<button id="add_tab">Add Tab</button>
			<div id="tabChooser">			
			</div>
		</div>
	</div>	
</div>

<div id='configurationDialog' title='Graph Settings'>
	<table border='0'>	
		<tr><td><label for='maxNodes'>Maximum nodes to show</label></td><td><input id="maxNodes" class="spinner" min="5" max="200" name="maxNodes" /></td></tr>
		<tr><td><label for='maxHops'>Maximum depth</label></td><td><input id='maxHops' class="spinner" min="1" max="200" name='maxHops'/></td></tr>
		<tr><td><label for='includeNPEs'>Include non-provenance edges</label></td><td><input type='checkbox' id='includeNPEs' name='includeNPEs'/></td></tr>
		<tr><td><label for='followNPIDs'>Discover new content by non-provenance IDs</label></td><td><input type='checkbox' id='followNPIDs' name='followNPIDs'/></td></tr>
		<tr><td><label for='backward'>Look backwards</label></td><td><input type='checkbox' id='backward' name='backward'/></td></tr>
		<tr><td><label for='forward'>Look forwards</label></td><td><input type='checkbox' id='forward' name='forward'/></td></tr>
		<tr><td><label for='breadthFirst'>Breadth first</label></td><td><input type='checkbox' id='breadthFirst' name='breadthFirst'/></td></tr>	
	</table>
	
	<p>The graph will be reloaded as soon as you apply these changes.</p>
</div>

<script>
$(document).ready(function () {
	// Set up spinners and force them to stay inside min/max.
	function keepInBounds(event, ui) {
		if (ui.value > $(this).attr("max")) {
			$(this).spinner("value", $(this).attr("max"));
			return false;
		} else if (ui.value < $(this).attr("min")) {
			$(this).spinner("value", $(this).attr("min"));
			return false;
		}		
	}
	
	$(".spinner").spinner({
		spin : keepInBounds,
		change: keepInBounds
	});
	
	// This is the nice jQuery way of doing it, but with our 
	// theme toggle buttons look confusing.
	// $( "input[type=checkbox]").button();	
});
</script>

<div class="ui-layout-center" id="centerPanel">
	<div class="ui-widget-header" style="vertical-align: middle; text-align: center; width:100%;">
		<span style="float:center; vertical-align: middle;">
			<table border='0'>
				<tr><td><form onsubmit="browseWorkflows(); return false">
			    	<input id='wkflws' type="submit" value="Browse Workflows"/>
			    </form></td>
				<td><form onsubmit="pageOidSubmit(false); return false">
					<input id='oidsubmit' type="submit" value="Re-Graph"/>
				</form></td>
				<td><form onsubmit="configure(); return false">
					<input id='configureButton' type='submit' value='Configure'/>
				</form></td></tr>
			</table>
		</span>
		<span style="float:right; vertical-align: middle;"> Cluster By -				 				
			<select name="clusterSelection" id="clusterSelection" onchange="clusterSelectionOnChange(this)" style="display: inline-block;">
				<option value="order">Order (Default)</option>
				<option value="label">Label</option>
				<option value="ownerid">Owner</option>
				<option value="function">Function</option>
			</select>				
		</span>
	</div>
	<div id="D3-CANVAS" tabindex="0" class="ui-widget-content" style="height: 100%; width: 100%">  
    </div>
</div>

<div class="ui-layout-south" id="southPanel">
	<div class="ui-widget-header">Information Fitness</div>
	<div class="ui-widget-content">
		<div id='fitnessWidgetTabs' style="width: 100%;">
			<ul>
				<li><a href="#summaryTab">Current Focus</a></li>
				<li><a href="#assessorTab">Assessments</a></li>
				<li><a href="#timeSpanTab">Time Span</a></li>
				<li><a href="#custodyTab">Chain of Custody</a></li>
				<li><a href="#queryTab">Custom Query</a></li>
			</ul>
			
			<div id='summaryTab'>Summary</div>			
			<div id='assessorTab'>Custom Assessments</div>
			<div id='timeSpanTab'>Time span</div>
			<div id='custodyTab'>Chain of Custody</div>
			<div id='queryTab'>Custom Query</div>
		</div>		
	</div>
</div>

</body>
</html>