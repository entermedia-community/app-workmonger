package org.openedit.tracker.modules;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import org.openedit.tracker.Budget;
import org.openedit.tracker.Job;
import org.openedit.tracker.JobTimeEntry;
import org.openedit.tracker.Todo;

import com.openedit.WebPageRequest;
import com.openedit.events.UserCalendarArchive;
import com.openedit.modules.BaseModule;

public class JobBudgetModule extends BaseModule
{
	protected UserCalendarArchive fieldCalendarArchive;
	
	public void loadBudgetForJob(WebPageRequest inReq) throws Exception
	{
		Job job = (Job)inReq.getPageValue("job");
		String reload = inReq.findValue("forcereload");
		if( job.getBudget() == null || Boolean.parseBoolean(reload))
		{
			Budget budget = new Budget();
			budget.setArchive(getCalendarArchive());
			budget.setCatalogId(inReq.getContentProperty("catalogid"));
			budget.setBudgetDataFile(getPageManager().getPage("/WEB-INF/data/" + budget.getCatalogId() + "/" + job.getSourcePath() + "/budget/" + job.getId() +".ics"));
			budget.clearHours(); //This can be slow?
			
			//add them back
			for (Iterator iterator = job.getTodos().iterator(); iterator.hasNext();)
			{
				Todo task = (Todo) iterator.next();
				for (Iterator iterator2 = task.getTimeEntries().iterator(); iterator2.hasNext();)
				{
					JobTimeEntry entry = (JobTimeEntry) iterator2.next();
					budget.addHours(task,entry, false); //Hope this is not slow
				}
			}
			job.setBudget(budget);
		}
		inReq.putPageValue("budget", job.getBudget());
	}

	public UserCalendarArchive getCalendarArchive()
	{
		return fieldCalendarArchive;
	}

	public void setCalendarArchive(UserCalendarArchive inCalendarArchive)
	{
		fieldCalendarArchive = inCalendarArchive;
	}
	
	public void addCredit(WebPageRequest inReq) throws Exception
	{
		Job job = (Job)inReq.getPageValue("job");
		
		String amount = inReq.findValue("amount");
		String dated = inReq.findValue("date");
		Date date = new SimpleDateFormat("MM/dd/yyyy").parse(dated);
		String note = inReq.findValue("note");

		job.getBudget().addCredit(amount, date, note, inReq.getUser() );
		//added 
	}
	public void addDebit(WebPageRequest inReq) throws Exception
	{
		Job job = (Job)inReq.getPageValue("job");
		
		String amount = inReq.findValue("amount");
		String dated = inReq.findValue("date");
		Date date = new SimpleDateFormat("MM/dd/yyyy").parse(dated);
		String note = inReq.findValue("note");

		job.getBudget().addDebit(amount, date, note, inReq.getUser() );
	}
	public void removeEntry(WebPageRequest inReq) throws Exception
	{
		Job job = (Job)inReq.getPageValue("job");
		
		String eid = inReq.findValue("eventid");

		job.getBudget().removeEntry(eid, inReq.getUser());
	}
}
