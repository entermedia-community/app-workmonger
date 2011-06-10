import java.text.SimpleDateFormat

import org.openedit.Data
import org.openedit.data.SearcherManager
import org.openedit.entermedia.util.CSVReader
import org.openedit.entermedia.util.CSVWriter
import org.openedit.entermedia.util.ImportFile
import org.openedit.tracker.Job
import org.openedit.tracker.JobSearcher
import org.openedit.tracker.JobTracker
import org.openedit.tracker.StatusChange
import org.openedit.tracker.Todo
import org.openedit.tracker.TodoSearcher


public void init(){
	JobTracker tracker = context.getPageValue("jobtracker");
	JobSearcher searcher = tracker.getJobSearcher();
	TodoSearcher todosearcher = tracker.getTodoSearcher();
	SearcherManager manager = tracker.getSearcherManager();
	Job job = context.getPageValue("job");
	List tasks = job.getTodos();
	String startdate = context.getRequestParameter("startdate");
	String enddate = context.getRequestParameter("enddate");
	int highcount = 0;
	int regularcount = 0;
	int lowcount = 0;
	int emailcount = 0;
	int greencount = 0;
	int redcount = 0;



	StringWriter output  = new StringWriter();
	CSVWriter writer  = new CSVWriter(output);
	String[] headers = [
		"Issue Number",
		"Title",
		"Web App",
		"Business Function",
		"Initial Priority",
		"Current Status",
		"Issues Opened Date/time",
		"Issue in Progress Date/Time",
		"Issue Ready For Testing Date/Time",
		"Issue Approved Date/Time",
		"Issue Closed Date/Time",
		"Elapsed time from Opened to In Progress",
		"Elapsed Time from in Progress to Ready for Testing",
		"Elapsed Time from Ready For Testing to Approved",
		"Elapsed Time from Opened to Closed",
		"Issue Closing Remarks/Resolution",
		"Resources"
	];
	
	writer.writeNext(headers);
	SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy h:mm a");


	tasks.each({
		def nextrow = new String[headers.length+1];
		Todo target = (Todo) it;
		nextrow[0] = target.getId();
		nextrow[1] = target.get("name");
		nextrow[2] = job.getName();

		String areas = target.get("todoarea");
		Data area = manager.getData(tracker.getCatalogId(), "todoarea", areas);
		if(area != null){
			nextrow[3] = area.toString();
		}

		String status = target.get("todolaststatus");

		String firststatus = target.getFirstStatus().getType();
		if("receivedonlinehigh".equals(firststatus)){
			highcount++;
		}

		if("receivedonline".equals(firststatus)){
			regularcount++;
		}
		if("receivedonlinelow".equals(firststatus)){
			lowcount++;
		}


		Data priority = manager.getData(tracker.getCatalogId(), "todolaststatus", firststatus);
		if(priority != null){
			nextrow[4] = priority.toString();
		}
		Data state = manager.getData(tracker.getCatalogId(), "todolaststatus", status);

		if(state != null){
			nextrow[5] = state.toString();
		}

		Date open = target.getFirstStatus().getDated();
		nextrow[6] = format.format(open);
		boolean closed = "closed".equals(status);
		if(target.getStatusChanges().size() > 1){

			Date initial = target.getStatusChanges().get(1).getDated();
			long hours = hoursBetween(open, initial);
			nextrow[7] = format.format(initial);
			nextrow[11] = hoursToFriendly(hours);

			
			
		
			
			StatusChange readyfortesting = target.getFirstStatus("readyfortesting");
			if(readyfortesting != null){
				long timetoready = hoursBetween(initial, readyfortesting.getDated());
				nextrow[8] = format.format(readyfortesting.getDated());
				nextrow[12] = hoursToFriendly(timetoready);
			}
		
			StatusChange approved = target.getFirstStatus("approved");
			if(approved != null && readyfortesting != null){
				nextrow[9] = format.format(readyfortesting.getDated());
				long readytoapproved = hoursBetween(readyfortesting.getDated(), approved.getDated());
				nextrow[13] = hoursToFriendly(readytoapproved);
				
			
			}
			
			

			if("receivedonlinehigh".equals(firststatus) && hours > 2){
				redcount ++;
			}
			else if("receivedonline".equals(firststatus) && hours > 8) {
				redcount ++;
			}
			else if("receivedonlinelow".equals(firststatus) && hours > 16) {
				redcount ++;
			}

			else{
				greencount++;
			}
			
			
			
		}
		
		
	
		
		
		if(closed){
			Date finaldate = target.getLastStatus().getDated();
			nextrow[10] = format.format(finaldate);
			int timetoclosed = hoursBetween(open, finaldate);
			nextrow[14] = hoursToFriendly(timetoclosed);
		}
		
		
	
	
		
		

		nextrow[15] = target.getLastStatus().getNote();
		String users = collectPostUsers(target);
		nextrow[16] = users;
		writer.writeNext(nextrow);
		
		
	
	});
	writer.writeNext([""] as String[]);
	writer.writeNext([""]as String[]);

	String[] totalsrow = ["Totals"];
	String [] high = ["High Issues Opened", "${highcount}"];
	String [] regular = ["Regular Issues Opened", "${regularcount}"];
	String [] low = ["Low Issues Opened", "${lowcount}"];

	writer.writeNext(totalsrow);
	writer.writeNext(high);
	writer.writeNext(regular);
	writer.writeNext(low);
	writer.writeNext([""] as String[]);
	writer.writeNext([""]as String[]);
	String [] resultsrow = ["Response Time: "];
	writer.writeNext(resultsrow);
	String [] green = ["Green: ", "${greencount}"];
	String [] red = ["Red: ", "${redcount}"];
	writer.writeNext(green);
	writer.writeNext(red);

	String export = output.toString();
	context.putPageValue("export",export);

	StringReader reader = new StringReader(export)
	CSVReader parser = new CSVReader(reader);
	ImportFile csvfile = new ImportFile();
	csvfile.setParser(parser);
	csvfile.read(reader);
	context.putPageValue("csvfile", csvfile);
}



init();



public long hoursBetween(Date start, Date end){
	Calendar startcal = Calendar.getInstance();
	startcal.setTime(start);
	Calendar endcal = Calendar.getInstance();
	endcal.setTime(end);
	return hoursBetween(startcal, endcal);
}


public  long hoursBetween(Calendar startDate, Calendar endDate) {
	Calendar date = (Calendar) startDate.clone();
	long daysBetween = 0;
	while (date.before(endDate)) {
		date.add(Calendar.HOUR, 1);
		daysBetween++;
	}
	return daysBetween;
}


public String hoursToFriendly(long hours){
	int days = hours / 24;
	int leftover = hours % 24;

	String friendly = "${days} days ${leftover} hours";
	return friendly;
}

public String collectPostUsers(Todo inTodo){
	ArrayList users = new ArrayList();
	StringBuffer buffer = new StringBuffer();

	inTodo.getStatusChanges().each{
		StatusChange change = it;
		if(!users.contains(change.getUser())){
			users.add(change.getUser());
		}
	}

	users.each{
		buffer.append(it.toString());
		buffer.append("\r\n");
	}
	return buffer.toString();
}
