(function(){

	// the minimum version of jQuery we want
	var v = "1.7.0";

	console.log("checking JQuery");
	// check prior inclusion and version
	if (window.jQuery === undefined || window.jQuery.fn.jquery < v) {
		console.log("injecting JQuery");
		var done = false;
		var script = document.createElement("script");
		script.src = "http://ajax.googleapis.com/ajax/libs/jquery/" + v + "/jquery.min.js";
		script.onload = script.onreadystatechange = function(){
			if (!done && (!this.readyState || this.readyState == "loaded" || this.readyState == "complete")) {
				done = true;
				initMyBookmarklet();
			}
		};
		document.getElementsByTagName("head")[0].appendChild(script);
	} else {
		initMyBookmarklet();
	}
	
	function initMyBookmarklet() {
		(window.worldsearchBookmarklet = function() {
			if (window.modal === undefined) {
				$.getScript("http://localhost:8182/modal.js", function() {
					initMyBookmarklet();
				});
			} else {
				$.getJSON( "http://localhost:8182/search?url="+document.URL+"&callback=?", function( data ) {
					var map1 = "http://maps.googleapis.com/maps/api/staticmap?center="+data.lat+","+data.lng+"&zoom=2&size=300x300&sensor=false&markers="+data.lat+","+data.lng;
					var map2 = "http://maps.googleapis.com/maps/api/staticmap?center="+data.lat+","+data.lng+"&zoom=12&size=300x300&sensor=false&markers="+data.lat+","+data.lng;
					
					modal.open({
						content: "<h1 style='text-align: center'>Estimated location: " + data.lat + ", " + data.lng+"</h1>" +
								"<br/>" +
								"<a href='http://localhost:8182/imageSearch?url=" + document.URL + "' target='_'>view image search results (opens in a new window)</a>" +
								"<br/>" +
								"<br/>" +
								"<img style='padding-right:10px' src='"+map1+"'/>" +
								"<img style='padding-left:10px' src='"+map2+"'/>",
						width: "660px", height: "426px"});
				});
			}
			
		})();
	}
})();