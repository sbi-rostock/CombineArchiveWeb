
//the main collection with all archives in this workspace
var pageRouter = null;
var workspaceArchives = null;
var navigationView = null;
var archiveView = null;
var startView = null;
var createView = null;
var statsView = null;
var aboutView = null;
var messageView = null;
var templateCache = {};

function displayError (err) {
	console.log ("this is an error: " + err);
}

// http://stackoverflow.com/a/18650828/3910121
function bytesToSize(bytes) {
	if(bytes == 0) return '0 Byte';
	var k = 1000;
	var sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
	var i = Math.floor(Math.log(bytes) / Math.log(k));
	return (bytes / Math.pow(k, i)).toPrecision(3) + ' ' + sizes[i];
}
// http://www.sitepoint.com/testing-for-empty-values/
function empty(data) {
	if(typeof(data) == 'number' || typeof(data) == 'boolean')
	{ 
		return false; 
	}
	if(typeof(data) == 'undefined' || data === null)
	{
		return true; 
	}
	if(typeof(data.length) != 'undefined')
	{
		return data.length == 0;
	}
	var count = 0;
	for(var i in data)
	{
		if(data.hasOwnProperty(i))
		{
			count ++;
		}
	}
	return count == 0;
}

//Underscore template interpolate character to {{# ... }}
_.templateSettings =  {
		evaluate: /\{\{#(.+?)\}\}/g,
		interpolate: /\{\{([^#].*?)\}\}/g
};
var __escape = {
		lt: new RegExp("<", "g"),
		gt: new RegExp(">", "g"),
		br: new RegExp("\n", "g")
};
var __deescape = {
		amp: new RegExp("&amp;", "g"),
		lt: new RegExp("&lt;", "g"),
		gt: new RegExp("&gt;", "g")
};

function escape(string, brline) {
	if( string == null || string == undefined )
		return string;
	else {
		if( brline == true )
			return string.replace(__escape.lt, "&lt;").replace(__escape.gt, "&gt;").replace(__escape.br, "<br />\n");
		else
			return string.replace(__escape.lt, "&lt;").replace(__escape.gt, "&gt;");
	}
}

$(document).ready(function () {

	$("#noJs").remove ();

	var isIE = /*@cc_on!@*/false || !!document.documentMode;
	if (!isIE)
		$("#noBrowser").remove ();
	else
		return;

	// read in and compile all tempates
	$("#templates").children().each( function(index, element) {
		var $el = $(element);
		var id = $el.attr("id");
		var html = $el.html();

		if( id != undefined && html != undefined && html.length > 1) {
			html = html.replace(__deescape.amp, "&").replace(__deescape.lt, "<").replace(__deescape.gt, ">");
			templateCache[ id ] = _.template(html);
		}
		$el.remove();
	});

	// heatbeat
	$.get( RestRoot + "heartbeat", function(data) {
		if( data.substring(0, 2) == "ok" ) {
			// remove cookie note
			$("#noCookie").remove ();

			// init router and views
			pageRouter = new PageRouter();
			pageRouter.navigate("", {trigger: true});

			$("#about-footer-link").click(function(event) {
				//navigationView.goToPage("about-page");
				pageRouter.navigate("about", {trigger: true});
				return false;
			});

			// show the page
			$("#page").show();
		}
	});

});

