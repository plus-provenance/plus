<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@page import="org.mitre.provenance.user.OpenIDUser"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%
if(request.getParameter("logout") != null) { 
	session.setAttribute("plus_user", null); 
	session.invalidate();
}
OpenIDUser user = (OpenIDUser)session.getAttribute("plus_user");
%>

<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
		<title>Passport, Please</title>		
		
		<script src="//ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
		<script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.11.4/jquery-ui.min.js"></script>
    	<script src="//cdnjs.cloudflare.com/ajax/libs/jquery-layout/1.4.3/jquery.layout.min.js"></script>	
		<link rel="stylesheet" type="text/css" href="//ajax.googleapis.com/ajax/libs/jqueryui/1.11.4/themes/pepper-grinder/jquery-ui.css" />
		
		<script>
		var myLayout = null;
		
		$(document).ready(function () {
			myLayout = $('body').layout({
				// enable showOverflow on west-pane so popups will overlap north pane
				west__showOverflowOnHover: false
			});
		});
		</script>				
	</head>
<body>
<div class="ui-layout-north" onmouseover="myLayout.allowOverflow('north')" onmouseout="myLayout.resetOverflow(this)">
	<div class="ui-widget-header">
	<table border="0">
		<tr>
			<td><img src="media/img/tci_logo.png" alt="PLUS"/></td>
			<td><h1>Provenance: Linking and Understanding Sources</h1></td>
		</tr>
	</table>
	</div>
</div>
	
<div class="ui-layout-west">
<!-- 
	<div class='ui-widget-header'>
		Blah blah basic information TBD
	</div>
-->

	<div class='ui-widget-content' width='100%' height='100%' style='background: #FFFFFF'>
	(C) 2013 MITRE Corporation
	</div>
</div>	

<div class="ui-layout-center">

	<div class="ui-widget-content" width='100%' height='100%' style='background: #FFFFFF'>
		<!-- OpenID Connect via MITRE's provider -->
		<h2>Log in with MITREid OpenID Connect by clicking the button below:</h2>
		<form action="openid_connect_login" method="get">
			<img alt='OpenID' width='100' src='/plus/media/img/openid-logo.png'/>
			<input type="hidden" name="identifier" value="https://id.mitre.org/connect/"/>
			<input type="submit" />
		</form>
	
		<!-- Additional servers can be added here -->
	
		<!-- OpenID Connect via any provider, if they support dynamic client registration -->
		<h2>Or, log in with any OpenID Connect provider below by entering your issuer:</h2>
		<p>Your server must support dynamic registration for this to work.</p>
		<form action="openid_connect_login" method="get">
			<input type="text" name="identifier" />
			<input type="submit" />
		</form>	
		<p>
		
		<!-- OpenID 2.0 via major providers -->
		<h2>Log in with OpenID 2.0 below by selecting your provider:</h2>
		
		<table cellpadding="10" cellspacing="10" border='0' style="background: white">
		   <tr><td>		
				<form name="google" action="j_spring_openid_security_check" method="post">
					<input name="openid_identifier" type="hidden" value="https://www.google.com/accounts/o8/id" />
					<input type='submit' value="Log in With Google" onclick="document.google.submit();" alt="Google"/>
				</form></td>
				
			<td>
				<form name="yahoo" action="j_spring_openid_security_check" method="post">
					<input name="openid_identifier" type="hidden" value="https://me.yahoo.com" />
					<input type='submit' value="Log in with Yahoo" onclick="document.yahoo.submit();" alt="Yahoo"/>
				</form>
			</td>
				
			<td>
				<form name="aol" action="j_spring_openid_security_check" method="post">
					<input name="openid_identifier" type="hidden" value="https://www.aol.com" />
					<input type='submit' value="Log in with AOL" onclick="document.aol.submit();" alt="AOL"/>
				</form>
			</td>
			</tr>
		</table>
		
		<!-- OpenID 2.0 via any provider -->
		<h2>Log in with OpenID 2.0 via Spring Security below by entering your identifier:</h2>
		
		<form action="j_spring_openid_security_check" method="post">
	  		<label for="openid_identifier">Login</label>:
	  		<input id="openid_identifier" name="openid_identifier" size="20" maxlength="100" type="text"/>
	  		<br />
	  		<input type="submit" value="Login"/>
		</form>
	
	</div>
	<!-- Display the page footer -->
    (C) 2013 MITRE Corporation
</div>
</body>
</html>