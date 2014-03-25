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
 * D3-based provenance visualization application
 * Originally written by Matt Howansky
 * Modified by M. David Allen
 */

// Longest possible text label on a node, used to prevent
// overflowing the box's available area.
var MAX_LABEL_LENGTH = 20;

var CANVAS = "#D3-CANVAS";

function browseWorkflows() {
	$(CANVAS).html(loadingMarkup());
	
	Provenance().getWorkflows({
		success: function(g) {
			tableInject(CANVAS, generateObjectTable(g, "Browse Workflows", CANVAS));
		},
		error: genericError("Get Workflows")
	});
	return true;
} // End browseWorkflows

// set up SVG for D3

//colors = d3.scale.category10(),
var width=1000, height=1000;
var	colors = function(taint) {
		if (taint){
			return 'rgb(200,0,0)';
		}
		return 'rgb(176,196,222)';
	
	},
	nodeSize = 50,
	taintHeads = [];

var svg = d3.select(CANVAS);

var selectedOID = "urn:uuid:mitre:plus:4033da91-8f83-46fd-bafb-9f9083a33c7e";

function getSelectedOID() { 
	return selectedOID;
}

 function isSelected(string){
	return string === selectedOID;
 }

// set up initial nodes and links
//  - nodes are known by 'id', not by index in array.
//  - reflexive edges are indicated on the node (as a bold black circle).
//  - links are always source < target; edge directions are set by 'left' and 'right'.
var nodes = {};
var links = {};
var PROVENANCE = ProvenanceGraph({nodes:[], links:[]});  // Overall structure returned from server
var allLabels = [];
var currentForce = 'order';
var lastNodeId = 12;

// init D3 force layout
var force = null;

// line displayed when dragging new nodes
var drag_line = null;

// handles to link and node element groups
var path = null, 
    svgGraphElements = null;

// mouse event vars
var selected_node = null,
    selected_link = null,
    mousedown_link = null,
    mousedown_node = null,
    mouseup_node = null;

function resetMouseVars() {
  mousedown_node = null;
  mouseup_node = null;
  mousedown_link = null;
}

function getShape(shape){
	var input = "data";
	shape = shape.substring(0, input.length) === input;
	return (shape*100);
}

var linkedByIndex = {};
    

function isConnected(a, b) {

        return linkedByIndex[a.index + "," + b.index] || linkedByIndex[b.index + "," + a.index] || a.index == b.index;
    }

function fade(opacity,d) {
	svgGraphElements.style("stroke-opacity", function(o) {
		thisOpacity = isConnected(d, o) ? 1 : opacity;
		this.setAttribute('fill-opacity', thisOpacity);
		return thisOpacity;
	});

	path.style("stroke-opacity", function(o) {
		return o.source === d || o.target === d ? 1 : opacity;
	});
};

// update force layout (called automatically each iteration)
var forces = {};

function tick(e) {
	//Add to links to index
	links.forEach(function(d) {
		linkedByIndex[d.source.index + "," + d.target.index] = 1;
	});

	// Push nodes toward their designated focus.
	var k = .91 * e.alpha;
	
	/*
	f = forces['labels'][o.label];
	f = forces['functions'][o.function];
	f = forces['orders'][o.order];
	 */
	nodes.forEach(function(o, i) {
		if (currentForce != 'order'){
			f = forces[currentForce + 's'][o[currentForce]];			
		} else {
			f = forces['orders'][o.dagre.rank][o.dagre.order];
		}
		o.y = Math.max(Math.min(o.y + (f.y - o.y) * k, height - nodeSize), nodeSize);
		o.x = Math.max(Math.min(o.x + (f.x - o.x) * k, width - nodeSize), nodeSize);
	});

	svgGraphElements.attr('transform', function(d) {
		return 'translate(' + (d.x ) + ',' + (d.y) + ')';
	});
	
	// draw directed edges with proper padding from node centers

	path.attr('d', function(d) {
		var deltaX = d.target.x - d.source.x,
		deltaY = d.target.y - d.source.y,
		dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY),
		normX = deltaX / dist,
		normY = deltaY / dist,
		sourcePadding = d.left ? nodeSize+5 : nodeSize,
				targetPadding = d.right ? nodeSize+5 : nodeSize,
						sourceX = d.source.x + (sourcePadding * normX),
						sourceY = d.source.y + (sourcePadding * normY),
						targetX = d.target.x - (targetPadding * normX),
						targetY = d.target.y - (targetPadding * normY);
		return 'M' + sourceX + ',' + sourceY + 'L' + targetX + ',' + targetY;
	});
}

function makeRow(key, value, linkMetadata) {
	var valueText = "";
	
	if(value instanceof Array) {
		valueText = "";
		
		for(var idx in value) {
			valueText = valueText + value[idx] + "<br/>\n";
		}		
	} else { 
		valueText = value;
	}
	
	var keyCell = key;
	if(linkMetadata) {
		keyCell = "<a href='#' onclick=\"showSharedMetadata('" + key + "', '" + value + "', '" + CANVAS + "'); return false\">" + key + "</a>";				
	}
	
	var tableRow = '<tr><td class="metadataTableCell">' + keyCell + '</td><td class="metadataTableCell">' + valueText + "</td></tr>";
	return tableRow;
}

function updateGraphStats(provGraph) { 
	var fp = provGraph.getFingerprint();

	var tableHtml = '<table id="statsTable"  cellspacing="1" cellpadding="1" style="table-layout: fixed; width: 100%"><tbody></tbody></table>';    
	$("#graphStats").html(tableHtml);
	
	for (prop in fp) { 
		// Don't display IDs.
		if(prop.indexOf("Id") != -1 || prop=='created') { continue; } 
		
		var o = fp[prop];
		
		// Check if it's a number; if so, round it.
		if(!isNaN (o-0) && o !== null && o.replace(/^\s\s*/, '') !== "" && o !== false) {		
			o = Math.round(o*100)/100;	
		}
				
		$("#statsTable").append(makeRow(prop, o));
	}
}

/**
 * Assign an rgb color for the display of a particular link.
 * @param link the link whose color you want
 * @returns a CSS style suitable for "stroke".
 */
function assignLinkColor(link) {		
	if(link.type === "npe") {
		// Non-provenance edges are styled as blue.
		return d3.rgb('rgb(0,0,255)').toString();
	} else { 
		return d3.rgb('rgb(0,0,0)').toString();
	}
	
	// Alternatively, we could return a color depending on whether the incident node was selected or not.
	// return (link.source === selected_node) ? d3.rgb('rgb(0,0,255)').toString() :  d3.rgb('rgb(0,0,0)').toString();
	
	// Or we could do this.
	/*
	return (d.source === selected_node || d.target == selected_node) ? 
			d3.rgb('rgb(0,0,255)').toString() :  
				d3.rgb('rgb(0,0,0)').toString(); })
     */
} // End assignLinkColor

function mouseUpNode(d) {
	if(!mousedown_node) return;

	// needed by FF
	drag_line
	.classed('hidden', true)
	.style('marker-end', '');

	// check for drag-to-self
	mouseup_node = d;
	if(mouseup_node === mousedown_node) { resetMouseVars(); return; }

	// unenlarge target node
	d3.select(this).attr('transform', '');

	// add link to graph (update if exists)
	// NB: links are strictly source < target; arrows separately specified by booleans
	var source, target, direction;
	if(mousedown_node.id < mouseup_node.id) {
		source = mousedown_node;
		target = mouseup_node;
		direction = 'right';
	} else {
		source = mouseup_node;
		target = mousedown_node;
		direction = 'left';
	}

	var link;
	link = links.filter(function(l) {
		return (l.source === source && l.target === target);
	})[0];

	if(link) {
		link[direction] = true;
	} else {
		link = {source: source, target: target, left: false, right: false};
		link = {
				"to": "urn:uuid:mitre:plus:492ced53-5595-4685-bb09-be63931e5db0",
				"source": source,
				"sourceHints": "local",
				"target": target,
				"label": "generated",
				"left": false,
				"from":source.id,
				"workflow": target.id,
				"right": true
		};

		link[direction] = true;
		links.push(link);
		// console.log(mousedown_node);
		// console.log(mousedown_node.subtype);
		if (mousedown_node.subtype == 'taint'){
			console.log('Taint Added');
			spreadTaint(mousedown_node.id);		
		}		
	}

	// select new link
	selected_link = link;
	selected_node = null;	
	restart();
} // End mouseUpNode

function onSelectNode(d) {
	//console.log("onSelectNode");
	//console.log(d);
	
	if(d3.event && d3.event.ctrlKey) return;

	// select node
	//This is fading not linked nodes
	//fade(.1,d);
	mousedown_node = d;
	if(mousedown_node === selected_node) selected_node = null;
	else selected_node = mousedown_node;

	// Update the OID box to include this value.
	$('#oidsubmit').attr('oid', d.id);	 	

	// console.log(d);
	var tableHtml = '<table id="metaTable" cellspacing="1" cellpadding="1" style="table-layout: fixed; width: 100%"><tbody></tbody></table>';    
	$("#metaData").html(tableHtml);
	
	// The list of properties which might occur in the object which we don't want to display.
	var ignoreList = ['x','y','px','py','metadata','index','weight','width', 'height',
	                  'fixed','startPoint','id','oid','taint','ownerid',
	                  'workflow','label','activity']; 
	for (i in d){
		var input = d[i];
		if(($.inArray(i, ignoreList) < 0) && d.hasOwnProperty(i)){
			if (i == 'name'){
				$("#metaDataLabel").html(input);
			} else {
				if (i == 'created'){
					// console.log(input);
					var date = new Date(parseInt(input,10));
					input = date;					
				}
				if (i == 'dagre'){
					i = "Rank/Order";
					input = input.rank + "/" + input.order;					
				}
				
				$('#metaTable').append(makeRow(i, input));
			}
		}
	}
		
	// Special case formatting
	if(d.ownerid) { 
		Provenance().getActor({
			aid : d.ownerid,
			success: function(provActor) {
				if(provActor) {
					var link = "<a href='#' onclick=\"showOwnedObjects('" + provActor.getId() + "', '" + provActor.getName() + "'); return false\">" + 
					           provActor.getName() + "</a>";
					
					$("#metaTable").prepend(makeRow("Owner", link));
				} else { 
					$("#metaTable").prepend(makeRow("Owner", "FAILED: " + d.ownerid));
				}
				$("#metaData").styleTable();
			},
			error: genericError("Fetching actor")
		});
	}
	
	// Workflow formatting...
	if(d.workflow) { 
		Provenance().getObject({
			oid: d.workflow,
			success:  function (provGraph) { 
				var node = provGraph.getNode(d.workflow);
				$("#metaTable").prepend(makeRow("Workflow", 
						"<a href='?" + node.oid + "'>" + 
						node.label + "</a>"));	
				$("#metaData").styleTable();
			},
			error: genericError("Fetching workflow")
		});
	}
	
	if(d.activity && !(d.activity === DEFAULT_ACTIVITY_OID)) { 
		Provenance().getObject({
			oid: d.activity,
			success:  function (provGraph) { 
				var node = provGraph.getNode(d.activity);
				$("#metaTable").prepend(makeRow("Activity", 
						"<a href='?" + node.oid + "'>" + 
						node.label + "</a>"));	
				$("#metaData").styleTable();
			},
			error: genericError("Fetching activity")
		});		
	}
	
	// Loop through regular metadata keys, but in sorted order.
	var keys = [];
	for (k in d.metadata) {
	    if (d.metadata.hasOwnProperty(k)) {
	        keys.push(k);
	    }
	}

	keys.sort();

	len = keys.length;
	for (var i = 0; i < len; i++){
	    var key = keys[i];
	    var value = d.metadata[key];
		if($.inArray(key, ignoreList) >= 0) { continue; } 			
		$('#metaTable').append(makeRow(key, value, true));	    
	}

	$("#metaData").styleTable();

	selected_link = null;

	// reposition drag line
	drag_line
	.style('marker-end', 'url(#end-arrow)')
	.classed('hidden', false)
	.attr('d', 'M' + mousedown_node.x + ',' + mousedown_node.y + 'L' + mousedown_node.x + ',' + mousedown_node.y);

	restart();
} // End onSelectNode

// update graph (called when needed)
function restart() {
  var llength = links.length;

  // svgGraphElements (node) group
  // NB: the function arg is crucial here! nodes are known by id, not by index!
  
  svgGraphElements = svgGraphElements.data(nodes, function(d) { return d.id; });

  // update existing nodes (reflexive & selected visual states)
  var attrValue = "translate(" + -nodeSize + ", "+ -nodeSize+")";
  svgGraphElements.selectAll('rect')
    .style('fill', function(d) { return (d === selected_node) ? d3.rgb(colors(d.taint)).brighter().toString() : colors(d.taint); })    
	.attr('transform', attrValue)
    .classed('reflexive', function(d) { return isSelected(d.id); });

  // add new nodes
  var g = svgGraphElements.enter().append('svg:g');

  var basicShape = 'svg:rect';
  
  var rect =  g.append(basicShape)
    .attr('class', 'node')
    .attr('width', nodeSize*2)
	.attr('height', nodeSize*2)
	
	// Even ovals are rectangles!  getShape controls a value that rounds
	// the corners of the rectange, and at extremes, rounded corner rectangles
	// look like ovals.  How about that.
	.attr("rx", function(d) { return  getShape(d.type);})
	.attr("ry", function(d) { return  getShape(d.type);})
    .style('fill', function(d) { return d3.rgb(colors(d.taint)).toString();})
    .style('stroke', function(d) { return d3.rgb(colors(d.taint)).darker().toString(); })    
    .attr('transform', attrValue)
    
    // This right here sets the tooltip text.   Anything with CSS class 'node'
    // will get tooltip'd.
    .attr('title', function(d) {
    	var tooltip = d.label;
    	    	    	
    	tooltip = tooltip + "  " + d.type + " / " + d.subtype;
		
    	if(PROVENANCE.additionalInfoAvailable(d.oid)) { 
		   tooltip = "This item (" + tooltip + ") has other connections not shown in the current graph";
		}
    	
    	return tooltip; 
    })    
    .on('mouseover', function(d) {
      if(!mousedown_node || d === mousedown_node) return;
      // enlarge target node
      //d3.select(this).attr('transform', 'scale(1.1)');
    })
    .on('mouseout', function(d) {    
      if(!mousedown_node || d === mousedown_node) return;
      // unenlarge target node
      //d3.select(this).attr('transform', '');
    })
    .on('mousedown', onSelectNode)
    .on('mouseup', mouseUpNode);
    
  // Label nodes.
  var text = g.append('foreignObject')
      .attr('class', 'nodeLabel')
      .attr('x', (nodeSize/-1.2))
      .attr('y', (nodeSize/-1.2))
	  .attr('pointer-events', 'none')
      .attr('width', nodeSize * 2)
	  .attr('height', nodeSize * 2)	  
	  .append("xhtml:body")
	  .append("p")
	  .html(function(d) { 
		  var l = d.label;
		
		  if(l.length > MAX_LABEL_LENGTH) {
			  l = l.substring(0, MAX_LABEL_LENGTH) + "(...) ";
		  }
		  
		  if(PROVENANCE.additionalInfoAvailable(d.oid)) { 
			  l = l + " (+)";
		  }
		  
		  return l;
	  })	  	  
	  .style('font-size', function(d) { return "1.0em"; })
	  .style('text-align', function(d) { return "center"; })
	  .style('vertical-align', function(d) { return "top"; })
	  .style("word-wrap", function(d) { return "break-word"; });
	
  // remove old nodes
  svgGraphElements.exit().remove();

  //console.log(links[0].source)
    // path (link) group
  path = path.data(links);  
  //console.log(links[0].source)

  // update existing links
  path.classed('selected', function(d) { return d === selected_link; })
    .style('marker-start', function(d) { return d.left ? 'url(#start-arrow)' : ''; })
    .style('marker-end', function(d) { return d.right ? 'url(#end-arrow)' : ''; })
	.style('stroke', assignLinkColor);

  // add new links

  path.enter().append('svg:path')
    .attr('class', 'link')
    .attr('title', function(d) { return d.label; })
    .classed('selected', function(d) { return d === selected_link; })
    .style('marker-start', function(d) { return d.left ? 'url(#start-arrow)' : ''; })    
    .style('marker-end', function(d) { return d.right ? 'url(#end-arrow)' : ''; }) 
    .style('stroke-dasharray', function(d) { return d.type === "npe" ? "5,5" : "10,0" })
	.style('stroke', assignLinkColor)
    .on('mousedown', function(d) {
      if(d3.event.ctrlKey) return;

      // select link
      mousedown_link = d;
      if(mousedown_link === selected_link) selected_link = null;
      else selected_link = mousedown_link;
      selected_node = null;
      restart();
    });    

  // remove old links
  path.exit().remove();
  
  $(".node").tooltip({	  
	 // tweak the position
	 position: { my: "left+15 center", at: "right center" },
	 track: false,
	 html: true,
	 // use the "slide" effect
	 show: 'slideDown'
  });
  
  $(".link").tooltip({	  
	  // tweak the position
	  position: { my: "left+15 center", at: "right center" },
  	  track: false,
  	  html: true,
	  // use the "slide" effect
	  show: 'fold'
  });
    
  // set the graph in motion
  force.start();
}

function mousedown() {
  // prevent I-bar on drag
  //d3.event.preventDefault();
  
  // because :active only works in WebKit?
  svg.classed('active', true);

  if(d3.event.ctrlKey || mousedown_node || mousedown_link) return;

  // insert new node at point
  // Removed but working
  /*
  var point = d3.mouse(this),
	  node = {"id":"urn:uuid:mitre:plus:f767e247-e392-444b-addb-c85b732b5b1f" + Math.random().toString(),
			"sgfs":[],
			"created":"Thu Jul 25 09:03:14 EDT 2013",
			"sourceHints":"local",
			"certainty":"100%",
			"subtype":"taint",
			"privileges":[],
			"label":"Taint",
			"type":"heritable",
			"metadata":{},
			"function":"unknown",
			"startPoint":true,
			"taint":true,
			"order": 0,
		}
  node.x = point[0];
  node.y = point[1];
  nodes.push(node);
  */
  restart();
}

function mousemove() {
  if(!mousedown_node) return;
  // update drag line
  drag_line.attr('d', 'M' + mousedown_node.x + ',' + mousedown_node.y + 'L' + d3.mouse(this)[0] + ',' + d3.mouse(this)[1]);

  restart();
}

function mouseup() {
  if(mousedown_node) {
    // hide drag line
    drag_line
      .classed('hidden', true)
      .style('marker-end', '');
  }

  // because :active only works in WebKit?
  svg.classed('active', false);

  // clear mouse event vars
  resetMouseVars();
}

function spliceLinksForNode(node) {
  var toSplice = links.filter(function(l) {
    return (l.source === node || l.target === node);
  });
  toSplice.map(function(l) {
    links.splice(links.indexOf(l), 1);
  });
}

// only respond once per keydown
var lastKeyDown = -1;

function keydown() {
  d3.event.preventDefault();

  if(lastKeyDown !== -1) return;
  lastKeyDown = d3.event.keyCode;

  // ctrl
  if(d3.event.keyCode === 17) {
    svgGraphElements.call(force.drag);
    svg.classed('ctrl', true);
  }

  if(!selected_node && !selected_link) return;
  switch(d3.event.keyCode) {
    case 8: // backspace
    case 46: // delete
      if(selected_node) {
        nodes.splice(nodes.indexOf(selected_node), 1);
        spliceLinksForNode(selected_node);
      } else if(selected_link) {
        links.splice(links.indexOf(selected_link), 1);
      }
      selected_link = null;
      selected_node = null;
      restart();
      break;
    case 66: // B
      if(selected_link) {
        // set link direction to both left and right
        selected_link.left = true;
        selected_link.right = true;
      }
      restart();
      break;
    case 76: // L
      if(selected_link) {
        // set link direction to left only
        selected_link.left = true;
        selected_link.right = false;
      }
      restart();
      break;
    case 82: // R
      if(selected_node) {
        // toggle node reflexivity
        selected_node.reflexive = !selected_node.reflexive;
      } else if(selected_link) {
        // set link direction to right only
        selected_link.left = false;
        selected_link.right = true;
      }
      restart();
      break;
  }
}

function keyup() {
  lastKeyDown = -1;

  // ctrl
  if(d3.event.keyCode === 17) {
    svgGraphElements
      .on('mousedown.drag', null)
      .on('touchstart.drag', null);
    svg.classed('ctrl', false);
  }
}



function showAddTaint(){
	$('#taintDiv').toggle();
}

function addTaintForm() {
	var OID = $('#oidsubmit').attr('oid');
	var taintReason = $('#taintReason').val();
	console.log("Adding Taint to:" + OID + "\nReason: " + taintReason);
	if (taintReason.length){
		addTaint(OID, "reason=" + taintReason);	
	}
	$('#taintDiv').toggle();
}

function removeTaintButton(){
	var OID = $('#oidsubmit').attr('oid');
	removeTaint(OID);
}

function refreshFitnessWidgets(oid) {
	
} // End refreshFitnessWidgets

function oidSubmit(inputOID) {
	var OID = inputOID;
	
	if(!OID) { OID = $('#oidsubmit').attr('oid'); } 
	
	if(OID == "" || !OID) { 
		console.log("ERROR:  Missing OID");
		return false;
	}
	
	$("#oidsubmit").attr("oid", OID);
		
	// console.log("Submitting OID " + OID);
    
    selectedOID = OID;

    var p = Provenance();
    p.getProvenanceGraph({
    	oid: OID,
    	success:function(provGraph) { 
    		PROVENANCE = provGraph;
    		applyNewData(PROVENANCE);
    		updateGraphStats(PROVENANCE);
    		onSelectNode(provGraph.getNode(OID));				
    	},
    	error:function(jqXHR, textStatus, errorThrown) {
    		console.log("failure");
    		console.log(errorThrown);
    		console.log(jqXHR);
    		console.log(textStatus);
    		applicationError("There was an error fetching provenance for ID " + OID, ": " + textStatus);
    	}
    });

    return false;
}

function applyNewData(provGraph){
	// console.log("Applying New Data");
	force.stop();
	
	var json = provGraph.getD3Json();
	nodes = json.nodes;
	links = json.links;
	
	//console.log("Before Length: " + links.length);
		
	links = links.filter(function(l) {
		return (l.source >= 0) && (l.target >= 0);
	});
		
	nodes.forEach(function(node, index, array){
		node.width = nodeSize*2;
		node.height = nodeSize*2;
		//getHeight(node.oid, index)
	});
	
	/*/Don't force links to the right
	nodes.forEach(function(node, index, array){
		if (node.dagre.order == nodes.length){
			var rv = node.dagre.order
			links.forEach(function(l) {
		
				if (l.to == node.oid) {
					rv = Math.min(rv, (nodes[l.source].dagre.order + 1));
				}
				
				
			});
		
			nodes[index].order = rv 
			console.log("Ended With: " + nodes[index].order);
		
		}
	});
	*/
	//console.log("After Length: " + links.length);
		
	var minWidth = (10 * 150);
	var minHeight = (10 * 40); 
	
	width = (150*nodes.length);
	height = (40*nodes.length);
		
	if(width < minWidth) { width = minWidth; } 
	if(height < minHeight) { height = minHeight; } 		
	
	resetSVG();
	
	//Needed to force d3 to forget old IDs
	svgGraphElements = svgGraphElements.data([], function(d) { return d.id; });
	svgGraphElements.exit().remove();
	
	resetMouseVars();
	linkedByIndex = {};
	
	addNodeFunctions();
	checkAndSpreadTaint();
	
	//console.log(links[0].source);
	links.forEach(function(l) {
		l.source = nodes[l.source];	
		l.target = nodes[l.target];	
	});
	//console.log(links[0].source);
		
	dagre.layout()
     .nodes(nodes)
     .edges(links)
	 .nodeSep(30)
	 .rankSep(30)
     .rankDir("LR")
     .run();
	 
	createForces();
	restart();
	 
	changeForce(currentForce);
	restart();
}

function checkAndSpreadTaint(){
	taintHeads = [];
		
	// console.log("In CAST: " + links.length);
	links.forEach(function(l) {	
		var source = nodes[l.source];
			if (source.startPoint == undefined) {
				source.startPoint = true;
			
			}
		var target = nodes[l.target];
			target.startPoint = false;
	});
	
	nodes.forEach(function(n) {
		if (n.startPoint){
			if (n.subtype == 'taint'){
				spreadTaint(n.id);
			}
			else {
				checkForTaint(n.id);
			}
		}
	});
}


function checkForTaint(oid){
	Provenance().getTaint({
		oid: oid,
		success: function(provGraph) { 
			createTaintCallback(oid);
		},
		error: genericError("Checking for taint"),
	});
	
	/*
	$.ajax({
			url: "/plus/api/object/taint/" + oid,
			contentType:"application/x-javascript;",
			type:"GET",
			success: createTaintCallback(oid),
			error:function(jqXHR, textStatus, errorThrown ){
				console.log("failure");
				console.log(errorThrown);
				console.log(jqXHR);
				console.log(textStatus);			
			},
			
			dataType:"json"
		});
     */
}

function createTaintCallback(oid){
	return function(data) {
		if (data.nodes.length > 0){
			spreadTaint(oid);
		}
	};
}

function spreadTaint(oid){
	var taintList  = spreadTaintHelper(oid);
	
	//console.log("Final List");	
	//console.log(taintList);
	
	nodes.forEach(function(n) {
		if (n.taint || taintList.indexOf(n.id) >= 0){
			n.taint = true;
		}
		else {
			n.taint = false;
		}
	});
	;
}

function spreadTaintHelper(oid){
	var taintList = [];
	taintList.push(oid);
	links.forEach(function(l) {
		if ((l.from) == oid)
			taintList = taintList.concat(spreadTaintHelper(l.to));
		

			
	}); 
	
	//console.log(taintList);
	return taintList;
}

function clusterSelectionOnChange(dropDown){
	var cluster = dropDown.options[dropDown.selectedIndex].value;
	changeForce(cluster);
}

function changeForce(f){	
	force.stop();
	currentForce = f;
	
	var k = Math.sqrt(nodes.length / (width * height));
	
	var k = 0.003;
	console.log("Nodes " + nodes.length + " width " + width + " height " + height + " k=" + k);
	
	if (f == 'order'){		
		force = d3.layout.force()
		.nodes(nodes)
		.links(links)
		.size([width, height])
		.linkDistance(200)
		.linkStrength(.00000000001) //Todo think if a function here would work better
		//.charge(-80*nodeSize)
		.charge(-10 / k)
		.gravity(100 * k)
		.on('tick', tick);
	} else {		
		force = d3.layout.force()
		.nodes(nodes)
		.links(links)
		.size([width, height])
		.linkDistance(160)
		.linkStrength(.01)
		.charge(-100*nodeSize)
		//.charge(-10 / k)
		.gravity(100 * k)
		.on('tick', tick);
	}
	
	/* Don't need to start here, because it's started
	 * later in the restart function.
	 */
	// console.log("Starting force graph");
	try { force.start(); }
	catch(err) { console.log("Ignored error on starting force: " + err) ;}
	//console.log("Finished change force");	
}

function removeTaint(OID){
	var newOID = OID;
	//Find Previously tainted Node to use for callback
	for (var i=0;i<links.length;i++){ 
		var l = links[i];
		if ((l.from) == OID){
			newOID = l.to;
			console.log("NewOID:" + newOID);
			break;
		}		
	}
	selectedOID = newOID;
	
	Provenance().deleteTaint({
		oid: OID,
		success: function(provGraph){
			// Prov graph  usually just has empty
			// result.
			// Redraw graph with newOID.			
			Provenance().getProvenanceGraph({
				oid: newOID,
				success:function (newFetchedGraph) {
					PROVENANCE = newFetchedGraph;
					applyNewData(PROVENANCE);
				},				
				error: genericError("Refresh prov graph after delete taint"),
	     	});
		},
		error: genericError("Deleting taint"),
	});
}

function addTaint(tNode, reason){
	OID = tNode;
	selectedOID = OID;
	
	Provenance().assertMarking({
		oid: OID,
		type: "taint",
		reason: reason,
		success: function(provGraph) { 
			Provenance().getProvenanceGraph({
				oid: OID,
				success: function(provGraph) { 
					PROVENANCE = provGraph;
					applyNewData(provGraph);
				},
				error: genericError("Getting graph after adding taint"),
			});
		},
		error: genericError("Adding taint"),
	});
}

/*
 * This function creates the forces that will control graph layout.
 * Right now the forces object has a key corresponding to each type of "clustering" that
 * can occur.   Basically we go through all of the nodes, find all the unique values for each
 * cluster feature.   We then assign a force with an x/y corresponding to each unique value.
 */
function createForces(){
	forces['labels'] = {};
	allLabels = [];
	forces['functions'] = {};
	allFuncts = [];
	forces['orders'] = {};
	forces['ownerids'] = {};
	allOwners = [];
	allOrders = [];
	allRanks = [];
	linkScoreDic = {};

	// First build the data structures that support
	// force layout.  Find a list of valid/unique values for
	// each "feature set".  
	nodes.forEach(function(d) {			
		var value = d.label;
		if ($.inArray(value, allLabels) < 0){
			allLabels.push(value);
		}
			
		value = d['function'];
		if ($.inArray(value, allFuncts) < 0){
			allFuncts.push(value);	
		}
		
		value = d['ownerid'];
		if($.inArray(value, allOwners) < 0){
			allOwners.push(value);
		}
		
		value = d.dagre.order;
		if ($.inArray(value, allOrders) < 0){
			allOrders.push(value);	
		}
		allOrders.sort(function(a,b){return a - b;});
		
		value = d.dagre.rank;
		if ($.inArray(value, allRanks) < 0){
			allRanks.push(value);	
		}
		allRanks.sort(function(a,b){return a - b;});		
	});
	
	// TODO
	// The positioning for each cluster is done like this:
	// Width is broken up into as many different sections as there are cluster values.
	// Ditto for height.  This creates a cascading step layout where each successive
	// cluster appears down and to the right of the next one.   So if there are 4 clusters,
	// The first will be at width/1, height/1 - the second will be at width/2, height/2, 
	// and so on.	
	var labelsCount = allLabels.length + 1;
	var counter = 0;
	allLabels.forEach(function(d) {
		forces['labels'][d] = {x: (width/labelsCount)*++counter, y: (height/labelsCount)*counter};
	});
	
	var functsCount = allFuncts.length + 1;
	counter = 0;
	allFuncts.forEach(function(d) {
		forces['functions'][d] = {x: (width/functsCount)*++counter, y: (height/functsCount)*counter};
	});
	
	var ownersCount = allOwners.length + 1;
	counter = 0;	
	allOwners.forEach(function (d) {
		forces['ownerids'][d] = {x: (width/ownersCount)*++counter, y: (height/ownersCount)*counter };
	});
			
	var ordersCount = allOrders.length + 1;
	var allRanksCount = allRanks.length + 1;
	counter = 0;
	allRanks.forEach(function(r, ri) {
		allOrders.forEach(function(o, oi){
			if (!forces['orders'][r]){
				forces['orders'][r] = [];
			}
			forces['orders'][r][o] = {x: (width/allRanksCount)*ri, y: (height/ordersCount)*oi};
		});
	});
} // End createForces

function addNodeFunctions() {
	var linkDic = {};
	links.forEach(function(l) {
		linkDic[l.to] = l.from;		
	});

	var nodeDic = {};
	nodes.forEach(function(n) {
		nodeDic[n.id] = n.label;
	});

	nodes.forEach(function(n) {
		if (n.type =='invocation'){
			n['function'] = n.label;
		}		
		else if (n.type =='data' && linkDic[n.id]){
			n['function'] = nodeDic[linkDic[n.id]];
		}
		else {
			n['function'] = 'unknown';
		}
	});
}

function resetSVG(){
	$('#D3-CANVAS').html('');

	function redraw() {
		// console.log("here", d3.event.translate, d3.event.scale);
		svg.attr("transform",
				"translate(" + d3.event.translate + ")"
				+ " scale(" + d3.event.scale + ")");
	}
	
	/*
	width  = $('#D3-CANVAS').width();
	height = $('#D3-CANVAS').height();
	*/
	svg = d3.select('#D3-CANVAS')
	  .append('svg')
	  .attr("pointer-events", "all")
	  .attr("width", "100%")   // take up all available space no matter how many nodes.
	  .attr("height", "100%")	  
	  .append('svg:g')   //Nested gs used to collect mouse interaction
	  .call(d3.behavior.zoom().scaleExtent([.05, 5]).on("zoom", redraw))	  
	  .append('svg:g');	  
	
	//Rectangle in the backround to handle mouse interaction away from nodes
	svg.append('svg:rect')
	.attr('width', '100%')
	.attr('height', '100%')
    .attr('fill', 'white');
	
	/*	
	//These are used in the build your own graph mode, currently not compatable with drag and zoom
	svg.on('mousedown', mousedown)
	  .on('mousemove', mousemove)
	  .on('mouseup', mouseup);	  
	
	d3.select('#D3-CANVAS')
	  .on('keydown', keydown)
	  .on('keyup', keyup);	  
	 */ 
	// define arrow markers for graph links
	svg.append('svg:defs').append('svg:marker')
    .attr('id', 'end-arrow')
    .attr('viewBox', '0 -5 10 10')
    .attr('refX', 6)
    .attr('markerWidth', 3)
    .attr('markerHeight', 3)
    .attr('orient', 'auto')
    .append('svg:path')
    .attr('d', 'M0,-5L10,0L0,5')
    .attr('fill', '#000');

	svg.append('svg:defs').append('svg:marker')
    .attr('id', 'start-arrow')
    .attr('viewBox', '0 -5 10 10')
    .attr('refX', 4)
    .attr('markerWidth', 3)
    .attr('markerHeight', 3)
    .attr('orient', 'auto')
    .append('svg:path')
    .attr('d', 'M10,-5L0,0L10,5')
    .attr('fill', '#000');
		
	// line displayed when dragging new nodes
	drag_line = svg.append('svg:path')
	.attr('class', 'link dragline hidden')
	.attr('d', 'M0,0L0,0');

	// handles to link and node element groups
	path = svg.append('svg:g').selectAll('path'),
    svgGraphElements = svg.append('svg:g').selectAll('g');

	force = d3.layout.force()
    .nodes(nodes)
    .links(links)
    .size([width, height])
    .linkDistance(150)
	.linkStrength(.01)
    .charge(-50*nodeSize)
    .on('tick', tick);
}

function getHeight(oid, index){
	var potentialChains = [];
	var rv = nodes.length;
	console.log("Index = " + index);
	if (nodes[index]['order']){
		rv =  nodes[index]['order'];
		console.log('already determined');	
	} else {
		links.forEach(function(l) {		
			if (l.from == oid) {
				console.log("Found a link");
				potentialChains.push(getHeight(l.to, l.target));
			}
		});
		
		if (potentialChains.length > 0) {
			rv = (Math.min.apply(null, potentialChains) - 1);
		}
		//If this is an end node don't force it to the right
	}
	
	nodes[index]['order'] = rv;
	console.log(oid + ': ' + rv);
	return rv;	
}

// app starts here
function init() {
	$('#taintReason').bind('keyup', function () {
		if ($(this).val().length){
			$("#taintSubmitButton").removeAttr("disabled");   
		}
		else {
			$("#taintSubmitButton").attr('disabled', 'disabled');
		}
	});
		
	$("#taintSubmitButton").attr('disabled', 'disabled');

	//Handle Tab Pane
	var tabs = $( "#tabs" ).tabs();
	var tabTemplate = "<li><a href='#{href}'>#{label}</a> <span class='ui-icon ui-icon-close' role='presentation'>Remove Tab</span></li>",
		tabCounter = 2;
		
	//TODO Make rest call to get list of available widgets [[name,URI ]...]
	var widgets = [["Decision Spaces","https://tinker.mitre.org:8443/ds-user-fe"],["Recursion","https://tinker.mitre.org:8443/DG/"]];
	var html = "";
	widgets.forEach(function(widget){
		html += '<option value="' + widget[1] + '">' + widget[0] + '</option>';	
	});
	
	$( "#add_tab_selector" ).html(html);
	// addTab button: Populates possible widgets
    $( "#add_tab" )
      .button()
      .click(function() {
        var label = "T" + tabCounter,
        id = "tabs-" + tabCounter,
        li = $( tabTemplate.replace( /#\{href\}/g, "#" + id ).replace( /#\{label\}/g, label ) );
        
		var tabContentHtml = '<iframe style="width: 95%; margin: 0px; height: 90%;" src="' + $( "#add_tab_selector" ).val() + '"></iframe>';
 
		  tabs.find( ".ui-tabs-nav li:last" ).before( li );
		  tabs.append( "<div style='width: 95%;  height: 95%;' id='" + id + "'>" + tabContentHtml + "</div>" );
		  tabs.tabs( "refresh" );
		  tabCounter++;
      });
	  
	// close icon: removing the tab on click
    tabs.delegate( "span.ui-icon-close", "click", function() {
      var panelId = $( this ).closest( "li" ).remove().attr( "aria-controls" );
      $( "#" + panelId ).remove();
      tabs.tabs( "refresh" );
    });
 
    tabs.bind( "keyup", function( event ) {
      if ( event.altKey && event.keyCode === $.ui.keyCode.BACKSPACE ) {
        var panelId = tabs.find( ".ui-tabs-active" ).remove().attr( "aria-controls" );
        $( "#" + panelId ).remove();
        tabs.tabs( "refresh" );
      }
    });
	
	// todo autogenerate the width
	width  = $('#D3-CANVAS').width();
	height = $('#D3-CANVAS').height();
	
	// console.log("Width=" + width + "; height=" + height);
	var chart = $('#D3-CANVAS');
	$('#centerPanel').on("resize", function() {
	    var targetWidth = chart.parent().width();
	    var targetHidth = chart.parent().height();
	    console.log("Target width: " + targetWidth);
	    console.log("Target height: " + targetHidth);
	});
		
	resetSVG();		
	changeForce(currentForce);		
	applyNewData(PROVENANCE);
	restart();
	//Needs second restart to draw once already on the page
};
