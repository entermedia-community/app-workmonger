runajax = function(e)
{
	var nextpage= jQuery(this).attr('href');
	var targetDiv = jQuery(this).attr("targetdiv");
	targetDiv = targetDiv.replace(/\//g, "\\/");
	jQuery("#"+targetDiv).load(nextpage)
	return false;
}


trackerinit = function()
{
	jQuery("input.datepicker").livequery(
		function() 
		{
			jQuery(this).datepicker(
			{
				dateFormat: 'mm/dd/yy', showOn: 'button',
				buttonImage: '$home$page.themeprefix/entermedia/images/cal.gif',
				buttonImageOnly: true
			});
		}
	);
	
	jQuery("a.ajax").live('click', runajax);

	jQuery("form.ajaxform").livequery('submit',	
		function() 
		{
			var targetdiv = jQuery(this).attr("targetdiv");
			targetdiv = targetdiv.replace(/\//g, "\\/");
			//allows for posting to a div in the parent from a fancybox.
			if(targetdiv.indexOf("parent.") == 0)
			{
				targetdiv = targetdiv.substr(7);
				parent.jQuery(this).ajaxSubmit({target: "#" + targetdiv});
				//closes the fancybox after submitting
				parent.jQuery.fn.fancybox.close();
			}
			else
			{
				jQuery(this).ajaxSubmit( {target:"#" + targetdiv} );
			}
			return false;
		}
	);
	
	jQuery("a.thickbox, a.emdialog").livequery(
		function() 
		{
			jQuery(this).fancybox(
			{ 
				'zoomSpeedIn': 300, 'zoomSpeedOut': 300, 'overlayShow': true,
				enableEscapeButton: true, type: 'iframe'
			});
		}
	); 
}

jQuery(document).ready(function() 
{ 
	trackerinit();

}); 