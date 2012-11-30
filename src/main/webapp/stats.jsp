<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@page import="java.util.*" %>
<%@page import="com.cloudspokes.squirrelforce.*" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<STYLE TYPE="text/css">
<!-- 
body {text-decoration:none;
		font-size:12pt;
		font-family: "verdana";}
 -->
</STYLE>
<%  Map<String, Integer> stats = MessageStats.getStats();%>
<%  String[] langs = MessageStats.getLangs();%>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Message Stats</title>
</head>
<body>
<h1>Rabbit MQ Processing Statistics</h1>

<h3>Statistics</h3>
MainQ Messages Received: <%= stats.get(Constants.MAIN_RECEIVER_KEY) %>
<br/>MainQ Messages Sent: <%= stats.get(Constants.MAIN_SENDER_KEY) %><br/><br/>
<%  for (String lang : langs) {%>
<br/><%=lang%>Q Messages Received: <%= stats.get(Constants.LANG_RECEIVER_KEY_PREFIX + lang) %>
<%  } %></body>
</html>