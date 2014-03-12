<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@page import="org.mitre.provenance.user.OpenIDUser"%>
<%@page import="org.mitre.provenance.tools.PLUSUtils"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<% if(request.getParameter("logout") != null) { session.invalidate(); } %>
<html><head>
<% OpenIDUser user = (OpenIDUser)session.getAttribute("plus_user"); 
   if(user == null) { 
	   user = new OpenIDUser(PLUSUtils.generateID(), "JohnQ@public.com");
	   user.setEmail("JohnQ@Public.com");
	   session.setAttribute("plus_user", user);
   }
%>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title>Provenance: Linking and Understanding Sources</title>		
		
	<script src="//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
	<script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.10.3/jquery-ui.min.js"></script>
    <script src="//cdnjs.cloudflare.com/ajax/libs/jquery-layout/1.3.0-rc-30.79/jquery.layout.min.js"></script>	
	
	<script src="media/js/jquery.tablesorter.js"></script>
	<script src="media/js/ui.js"></script>	
	<script src="media/js/provenance.js"></script>
	<script src="media/js/fitness.js"></script>		
	
	<link rel="stylesheet" type="text/css" href="//ajax.googleapis.com/ajax/libs/jqueryui/1.10.3/themes/pepper-grinder/jquery-ui.css" />
	<link rel="stylesheet" type="text/css" href="media/js/tablesorter/style.css"/>
		
	<script>
		var myLayout = null;
		
		function populateTab(sel) {		
			if(sel == "#search") { return false; } 
			
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
			} else if(sel == '#dashboard') {
				new Provenance().getLatestConnectedData({
					success: function(provGraph) {
						tableInject(sel, generateObjectTable(provGraph, "Latest Connected Data", sel));
					},
					error: function() {
						applicationError("Couldn't fetch latest dashboard", sel);
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
		
		<div id='search'>			    
			<input type="text" name="searchTerm" id='searchTerm' size="70" title="Enter search term here" value=""/> 
			<input id='searchSubmit' type='submit' value='Search' onclick="submitSearch('#searchTerm', '#searchResults'); return false"/>			
			    		
			<div id='searchResults'></div>
		</div>
		
		<div id='workflows'>&nbsp;</div>
		
		<div id='latestObjects'>&nbsp;</div>
		
		<div id='owners'>&nbsp;</div>		
		
		<div id='npids'>&nsbp;</div>
		
		<div id='dashboard'>&nbsp;</div>
	</div>
</div>
</body>
</html>