var ganttData = [
#foreach($todo in $todos )#if( $velocityCount > 1 ) , #end	
	{
		id:$velocityCount, itemId: '$todo.id', itemName: '$todo.name.replaceAll("'","")<br> $url_util.textEscape($todo.getLastStatus().getNoteSnip(100))', series: [
			#set( $started = $todo.getFirstStatus().getDated() )
			##load up admin user
			#set( $ended = $todo.getLastStatus().getDated() )
			 
			{ seriesName: '$todo.getFirstStatus().getUser() #if( !$todo.isInClosed() ) (open) #end', start: new Date($started.getTime()), end: new Date($ended.getTime()) }
			#set( $firstadmin = false )
			#set( $firstadmin = $todo.getFirstStatusInGroup("notify") )
			#if( $firstadmin )
				#set( $adminstarted =  $firstadmin.getDated() )				
			,{ seriesName: "$firstadmin.getUser() #if( !$todo.isInClosed() ) (open) #end", start: new Date($adminstarted.getTime()), end: new Date($ended.getTime()) }
			#end
		]
	}#end 
];

