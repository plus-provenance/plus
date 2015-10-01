<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@page import="org.mitre.provenance.user.OpenIDUser"%>
<%@page import="org.mitre.provenance.user.PrivilegeClass"%>
<%@page import="org.mitre.provenance.tools.PLUSUtils"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<% if(request.getParameter("logout") != null) { session.invalidate(); } %>
<html><head>
<% OpenIDUser user = (OpenIDUser)session.getAttribute("plus_user"); 
   if(user == null) { 
	   user = new OpenIDUser(PLUSUtils.generateID(), "JohnQ@public.com");
	   user.addPrivilege(PrivilegeClass.ADMIN);
	   user.setEmail("JohnQ@Public.com");
	   session.setAttribute("plus_user", user);
   }
%>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title>Provenance: Linking and Understanding Sources</title>		
		
	<script src="//ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
	<script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.11.4/jquery-ui.min.js"></script>
    <script src="//cdnjs.cloudflare.com/ajax/libs/jquery-layout/1.4.3/jquery.layout.min.js"></script>	
	
	<script src="media/js/jquery.tablesorter.js"></script>
	<script src="media/js/ui.js"></script>	
	<script src="media/js/provenance.js"></script>
	
	<link rel="stylesheet" type="text/css" href="//ajax.googleapis.com/ajax/libs/jqueryui/1.11.4/themes/pepper-grinder/jquery-ui.css" />
	<link rel="stylesheet" type="text/css" href="media/js/tablesorter/style.css"/>

	<style>
		.tabcontent {
			height: 700px;
			overflow-y: auto;
		}
	</style>

	<script>
		var myLayout = null;
		
		function populateTab(sel) {		
			if(sel == "#search" || sel == "#dashboard") { return false; } 
			
			$(sel).html(loadingMarkup());
			
			if(sel == '#latestObjects') {
				new Provenance().getLatestObjects({
					success: function(provGraph) {
						tableInject(sel, generateObjectTable(provGraph, "Latest Reported Provenance Objects", sel));
					},
					error: function() { applicationError("Couldn't fetch latest reported provenance", sel); }
				});
			} else if(sel == '#owners') {
				new Provenance().getOwners({
					success: function(provGraph) {
						tableInject(sel, generateActorTable(provGraph, "Data Owners", sel));
					},
					error: function() { applicationError("Couldn't fetch latest owners", sel); }
				});				
			} else if(sel == '#npids') {
				new Provenance().getLatestNPIDs({
					success: function(provGraph) {
						tableInject(sel, generateObjectTable(provGraph, "Latest Reported External Identifiers", sel));
					},
					error: function() { applicationError("Couldn't fetch latest non-provenance edges", sel); }
				});
				
				$("#npids").html("Not yet implemented");
			} else if(sel == "#workflows") {
				new Provenance().getWorkflows({
					success: function(provGraph) {
						tableInject(sel, generateObjectTable(provGraph, "Latest Workflows", sel));
					},
					error: function() {
						applicationError("Couldn't fetch latest workflows", sel);
					}
				});
			} 
			
			return false;
		} // End populateTab
		
		$(document).ready(function () {
			myLayout = $('body').layout({
				// enable showOverflow on west-pane so popups will overlap north pane
				west__showOverflowOnHover: false
			});
			
			$("#tabs").tabs({
				activate: function( event, ui ) {
					populateTab(ui.newPanel.selector);
				}			
			});		
			
			populateTab('#search');
		});
	</script>				
	</head>
<body>
<div class="ui-layout-north" onmouseover="myLayout.allowOverflow('north')" onmouseout="myLayout.resetOverflow(this)">
	<div class="ui-widget-header">

	<table cellpadding='10' border="0">
		<tr>
			<td><a href="/plus/"><img src="media/img/tci_logo.png" border="0" alt="Provenance: Linking and Understanding Sources (PLUS)"/></a></td>
			<td><h1><a href="/plus/">Provenance</a></h1></td>
			<td width='50%'><div style='vertical-align: center; text-align:right;'>Welcome, <%=user.getEmail()%></div></td>			
		</tr>		
	</table>
	
	</div>
</div>
	
<div class="ui-layout-center">
	<div id='tabs'>
		<ul>
			<li><a href="#search">Search</a>
			<li><a href="#latestObjects">Object Feed</a></li>
			<li><a href="#workflows">Workflow Catalog</a>
			<li><a href="#owners">Owners</a></li>
			<li><a href="#npids">Non-Provenance Data</a></li>
			<li><a href="#dashboard">Dashboard</a></li>
		</ul>
		
		<div id='search' style='overflow-y:auto'>			    
			<input type="text" 
			       name="searchTerm" 
			       id='searchTerm' 
			       size="70"
			       onkeydown="if (event.keyCode == 13) { submitSearch('#searchTerm', '#searchResults'); return false; }" 
			       title="Enter search term here" value=""/> 
			<input id='searchSubmit' type='submit' value='Search' onclick="submitSearch('#searchTerm', '#searchResults'); return false"/>			
			    		
			<div id='searchResults'></div>
		</div>
		
		<div id='workflows' class='tabcontent'>&nbsp;</div>
		
		<div id='latestObjects' class='tabcontent'>&nbsp;</div>
		
		<div id='owners' class='tabcontent'>&nbsp;</div>		
		
		<div id='npids' class='tabcontent'>&nbsp;</div>
		
		<div id='dashboard' class='tabcontent'>
			<h1>Dashboard Queries</h1>
			
			<script>
			function submitDashboardQuery() {				
				var query = $("#querySelector").val();				
				var results = "#dashboardQueryResults";
				
				optionsWithURLs = {
					'connectedData' : "/plus/api/feeds/connectedData?n=10&format=json",
					'hashedContent' : "/plus/api/feeds/hashedContent?n=10&format=json",
				};
				
				var title = $("#querySelector option[value='" + query + "']").text();				
				
				if(optionsWithURLs[query]) {
					var url = optionsWithURLs[query];
					
					$(results).html(loadingMarkup());
					
					$.ajax({
						url: url,
						contentType:"application/x-javascript;",
						dataType:"json",
						type: "GET",
						success:function(datum) {				
							var data = jQuery.extend(true, {}, datum);							
							var pg = ProvenanceGraph(data);
							tableInject(results, generateObjectTable(pg, title, results));
						},
						error: function() {
							applicationError("Could not run query " + query, results);
						},						
					});
				}
				
				return false;
			} // End submitDashboardQuery
			</script>
			
			<p>Select a dashboard query from the list below.</p>
			<form action='#'>
				<select name='querySelector' id='querySelector'>
	  				<option value="connectedData">Latest Connected Data</option>
	  				<option value="hashedContent">Latest Hashed Data</option>
				</select> 
				<input type='submit' name='Run Query' value='Run Query'
					   onclick='submitDashboardQuery()'>
			</form>

			<div id='dashboardQueryResults'>&nbsp;</div>			
		</div>
	</div>
	
	<div>(c) MITRE 2014 / <a href="docs/index.html">API documentation</a> / <a href="https://github.com/plus-provenance/plus">GitHub Repo</a></div>
</div>
</body>
</html>