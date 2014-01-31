var modal = (function(){
	var 
	method = {},
	$overlay,
	$modal,
	$content,
	$close;

	// Center the modal in the viewport
	method.center = function () {
		var top, left;

		top = Math.max($(window).height() - $modal.outerHeight(), 0) / 2;
		left = Math.max($(window).width() - $modal.outerWidth(), 0) / 2;

		$modal.css({
			top:top + $(window).scrollTop(), 
			left:left + $(window).scrollLeft()
		});
	};

	// Open the modal
	method.open = function (settings) {
		$content.empty().append(settings.content);

		$modal.css({
			width: settings.width || 'auto', 
			height: settings.height || 'auto'
		});

		method.center();
		$(window).bind('resize.modal', method.center);
		$modal.show();
		$overlay.show();
	};

	// Close the modal
	method.close = function () {
		$modal.hide();
		$overlay.hide();
		$content.empty();
		$(window).unbind('resize.modal');
	};

	// Generate the HTML and add it to the document
	$overlay = $('<div id="overlay" style="position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: #000; opacity: 0.5; filter: alpha(opacity=50); z-index: 9999"></div>');
	$modal = $('<div id="modal" style="position:absolute; background:url(tint20.png) 0 0 repeat; background:rgba(0,0,0,0.2); border-radius:14px; padding:8px; z-index: 10000"></div>');
	$content = $('<div id="content" style="border-radius:8px; background:#fff; padding:20px;"></div>');
	$close = $('<a id="close" style="position:absolute; background:url(http://localhost:8182/close.png) 0 0 no-repeat; width:24px; height:27px; display:block; text-indent:-9999px; top:-7px; right:-7px;" href="#">close</a>');

	$modal.hide();
	$overlay.hide();
	$modal.append($content, $close);

	//$(document).ready(function(){
		$('body').append($overlay, $modal);						
	//});

	$close.click(function(e){
		e.preventDefault();
		method.close();
	});

	return method;
}());