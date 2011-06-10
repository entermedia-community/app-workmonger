package org.openedit.tracker.modules;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermedia.email.PostMail;
import org.entermedia.email.Recipient;
import org.entermedia.email.TemplateWebEmail;
import org.entermedia.upload.FileUpload;
import org.entermedia.upload.FileUploadItem;
import org.entermedia.upload.UploadRequest;
import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.repository.ContentItem;
import org.openedit.tracker.BaseJobArchive;
import org.openedit.tracker.BaseJobTrackingModule;
import org.openedit.tracker.Job;
import org.openedit.tracker.JobArchive;
import org.openedit.tracker.JobSearcher;
import org.openedit.tracker.JobTimeEntry;
import org.openedit.tracker.JobTracker;
import org.openedit.tracker.JobUserEntry;
import org.openedit.tracker.StatusChange;
import org.openedit.tracker.Todo;
import org.openedit.tracker.TodoSearcher;
import org.openedit.tracker.hours.Day;
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.ListHitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.Group;
import com.openedit.users.User;
import com.openedit.users.authenticate.PasswordGenerator;

public class JobTrackingModule extends BaseJobTrackingModule
{	
	private static final Log log = LogFactory.getLog(JobTrackingModule.class);
	protected DateFormat fieldDateFormat;
	protected PostMail fieldPostMail;
	public HitTracker getJobList( WebPageRequest inReq) throws Exception
	{
		JobTracker jobtracker = getJobTracker(inReq);
		HitTracker jobs = jobtracker.getJobSearcher().findAllJobs();
		inReq.putPageValue("jobs", jobs);
		inReq.putPageValue("searcher", getJobTracker(inReq).getJobSearcher());
		return jobs;
	}

	public void checkInvitation(WebPageRequest inReq) throws Exception
	{
		String inviteid = inReq.getRequestParameter("invitationid");
		if( inviteid != null)
		{
			Job job = loadJob(inReq);
			acceptInvitation(job, inviteid, inReq.getUser(), inReq);
		}
	}
	
	public void listPendingInvites(WebPageRequest inReq) throws Exception
	{
		Searcher invitationSearcher = getJobTracker(inReq).getInvitationSearcher();
		SearchQuery q = invitationSearcher.createSearchQuery();
		Job job = loadJob(inReq);
		q.addMatches("jobid",job.getId());
		q.setHitsName("pending");
		invitationSearcher.cachedSearch(inReq, q);
		inReq.putPageValue("searcherManager", getSearcherManager());
	}
	
	public boolean acceptInvitation(Job inJob, String inInviteId, User inTestUser, WebPageRequest inReq)
	{
		//look up the invite id
		Searcher invitationSearcher = getJobTracker(inReq).getInvitationSearcher();

		SearchQuery q = invitationSearcher.createSearchQuery();
		q.addMatches("jobid",inJob.getId());
		q.addMatches("invitestatus","1");
		q.addMatches("id",inInviteId);
		HitTracker hits = invitationSearcher.cachedSearch(inReq, q);

		if( hits.size() == 0)
		{
			throw new OpenEditException("Invitation has already been used or is invalid");
		}
		
		Data invitation = (Data)hits.first();
		//Load the invitation
		invitation = (Data)invitationSearcher.searchById(invitation.getId()); //so we can edit
		
		if(invitation != null && invitation.get("jobid") != null && invitation.get("jobid").equals(inJob.getId()))
		{
			addUserToJob(inTestUser.get("userName"), inJob, inReq);
			inReq.putPageValue("inviteresult", true);
			//makeFriends(inOwner, inTestUser.getId(), inTestUser);
			//remove the row from the invitations xml file the invitation has been taken care of
			invitation.setProperty("invitestatus", "2");
			invitation.setProperty("addeduser", inTestUser.get("userName"));
			invitationSearcher.saveData(invitation, inTestUser);
			return true;
		}
		return false;
	}
	public HitTracker getAssignedJobList( WebPageRequest inReq) throws Exception
	{
		if(inReq.getUser() == null)
		{
			return null;
		}
		JobTracker jobtracker = getJobTracker(inReq);
		//String sort = inReq.findValue("defaultsort");
		HitTracker jobs = jobtracker.getJobSearcher().findAssignedJobs(inReq.getUser());
		inReq.putPageValue("searcher", getJobTracker(inReq).getJobSearcher());
		inReq.putPageValue("yourjobs", jobs);
		inReq.putPageValue("jobs", jobs);
		return jobs;
	}
	
	public void autoAddMember(WebPageRequest inReq) throws Exception
	{
		String referred = inReq.getRequestParameter("referred");
		User user = inReq.getUser();
		Job job = loadJob(inReq);
		if(referred != null && Boolean.parseBoolean(referred) && !job.hasMember(user))
		{
			if(job.getPendingUsers().contains(user.getEmail()))
			{
				job.addUserEntry(user);
				job.removePendingUser(user.getEmail());
			}
		}
	}
	
	public void inviteMembers(WebPageRequest inReq) throws Exception
	{
		//need to build recipient list this shouldn't be too bad
		String addresses = inReq.getRequestParameter("email");
		mailInvites(inReq, null, addresses);
		
		String[] addresslist = addresses.split(",");
		Job job = loadJob(inReq);
		for(int i=0;i<addresslist.length;i++)
		{
			job.addPendingUser(addresslist[i]);
		}
		String temp = addresslist[0];
	}
	
	public void mailInvites(WebPageRequest inReq, List<Recipient> inRecipients, String inCommaSepRecipients)
	{
		TemplateWebEmail email = getPostMail().getTemplateWebEmail();
		email.loadSettings(inReq);
		
		User sender = inReq.getUser();
		email.setProperty("senderuserid", sender.getId());
		email.setProperty("senderfirstname", sender.getFirstName());
		email.setProperty("senderlastname", sender.getLastName());

		if (sender.getEmail() != null)
		{
			email.setFrom(sender.getEmail());
			email.setFromName(sender.toString());
		}

		if(inRecipients != null)
		{
			email.setRecipients(inRecipients);
		}
		else if(inCommaSepRecipients != null)
		{	
			email.setRecipientsFromUnknown(inCommaSepRecipients);
		}
		
		try
		{
			email.send();
		}
		catch (Exception e)
		{
			inReq.putPageValue("emailerror", e.getMessage());
		}
	}

	public HitTracker getOpenJobList( WebPageRequest inReq) throws Exception
	{
		if(inReq.getUser() == null)
		{
			return null;
		}
		JobTracker jobtracker = getJobTracker(inReq);
		//String sort = inReq.findValue("defaultsort");
		HitTracker jobs = jobtracker.getJobSearcher().findOpenJobs(inReq.getUser());
		inReq.putPageValue("searcher", getJobTracker(inReq).getJobSearcher());
		inReq.putPageValue("jobs", jobs);
		return jobs;
	}
	public HitTracker getClosedJobList( WebPageRequest inReq) throws Exception
	{
		if(inReq.getUser() == null)
		{
			return null;
		}
		JobTracker jobtracker = getJobTracker(inReq);
		//String sort = inReq.findValue("defaultsort");
		HitTracker jobs = jobtracker.getJobSearcher().findClosedJobs(inReq.getUser());
		inReq.putPageValue("searcher", getJobTracker(inReq).getJobSearcher());
		inReq.putPageValue("jobs", jobs);
		return jobs;
	}
	public HitTracker getVisibleJobList( WebPageRequest inReq) throws Exception
	{
		JobTracker jobtracker = getJobTracker(inReq);
		String sort = inReq.findValue("defaultsort");
		HitTracker jobs = jobtracker.getJobSearcher().findVisibleJobs(inReq.getUser(), sort);
		inReq.putPageValue("searcher", getJobTracker(inReq).getJobSearcher());
		inReq.putPageValue("visiblejobs", jobs);
		return jobs;
	}
	
	public HitTracker getGroupJobList( WebPageRequest inReq) throws Exception
	{
		JobTracker jobtracker = getJobTracker(inReq);
		String sort = inReq.findValue("defaultsort");
		String groupid = inReq.findValue("groupid");
		Group group = getUserManager().getGroup(groupid);
		HitTracker jobs = jobtracker.getJobSearcher().findOpenGroupJobs(group, sort);
		inReq.putPageValue("searcher", jobtracker.getJobSearcher());
		inReq.putPageValue("jobs", jobs);
		return jobs;
	}
	
	
	public HitTracker searchJobs(WebPageRequest inReq) throws Exception
	{
		JobTracker jobtracker = getJobTracker(inReq);
		
		HitTracker jobs = jobtracker.getJobSearcher().fieldSearch(inReq);
		inReq.putPageValue("jobhits", jobs);
		
		getJobArchive(inReq);
		inReq.putPageValue("searcher", getJobTracker(inReq).getJobSearcher());
		return jobs;
	}
	
	public HitTracker searchTodos(WebPageRequest inReq) throws Exception
	{
		HitTracker jobs = getJobTracker(inReq).getTodoSearcher().fieldSearch(inReq);
		inReq.putPageValue("todos", jobs);
		inReq.putPageValue("searcher", getJobTracker(inReq).getTodoSearcher());
		getJobArchive(inReq);
		return jobs;
	}
	
	public void loadOpenTodos(WebPageRequest inReq) throws Exception
	{
		TodoSearcher searcher = getJobTracker(inReq).getTodoSearcher();
		SearchQuery query = searcher.createSearchQuery();
		
//		query.addMatches("name", "*");
		query.addNot("jobopen", "false");
		//query.addNot("jobhidden", "true");
		query.addNot("todolaststatus", "closed");
		
		String sort = inReq.findValue("defaulttodosort");

		query.addSortBy(sort);
		HitTracker hits = searcher.searchByUser(inReq,query);
		hits.setHitsPerPage(50);
		inReq.putPageValue("todohits", hits);
	}
	
	
	public HitTracker getFollowingList(WebPageRequest inReq)
	{
		JobSearcher jobsearcher = getJobTracker(inReq).getJobSearcher();
		HitTracker jobs = jobsearcher.findAllJobs();
		User user = inReq.getUser();
		ListHitTracker goodhits = new ListHitTracker();
		JobArchive jobarchive = getJobArchive(inReq);
		for (Iterator iterator = jobs.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			Job job = jobarchive.getJobById(hit.get("id"));
			//job is open, public, and has current user as a member
			String temp = job.get("jobopen");
			String temp2 = job.get("hidden");
			if(job.hasMember(user) && !"false".equals(job.get("jobopen")) && !"true".equals(job.get("hidden")))
			{
				goodhits.add(hit);
			}
		}
		inReq.putPageValue("followinghits", goodhits);
		return goodhits;
	}
	
	public HitTracker findPrivateJobs(WebPageRequest inReq)
	{
		JobSearcher jobsearcher = getJobTracker(inReq).getJobSearcher();
		HitTracker hits = jobsearcher.findPrivateJobs(inReq);
		inReq.putPageValue("jobs", hits);
		return hits;
	}
	
//	public void loadPrivateTodos(WebPageRequest inReq) throws Exception
//	{
//		TodoSearcher searcher = getJobTracker(inReq).getTodoSearcher();
//		SearchQuery query = searcher.createSearchQuery();
//		
//		HitTracker jobhits = getPrivateJobsForUser(inReq);
//		for (Iterator iterator = jobhits.iterator(); iterator.hasNext();)
//		{
//			Data object = (Data) iterator.next();
//			query.addMatches("jobid", object.get("id"));
//		}
//		if(!query.isEmpty())
//		{
//			HitTracker hits = searcher.search(query);
//			inReq.putPageValue("privatetodohits", hits);
//		}
//		
//	}
	public Boolean canViewJob(WebPageRequest inReq) throws Exception
	{
		Job job = loadJob(inReq);
		if(job == null)
		{
			return true;
		}
		User user = inReq.getUser();
		//if(user == null)
		//{
		//	return false;
		//}
		if(user!= null && "true".equals( inReq.getPageValue("cantrackeradmin")) )
		{
			return true;
		}
		String hidden = job.get("hidden");
		if(hidden!=null && hidden.equals("true"))
		{
			if(user == null)
			{
				return false;
			}
			else
			{
				return(job.hasMember(user));
			}
		}
		return true;
	}
	
	public HitTracker searchMyTodos(WebPageRequest inReq) throws Exception
	{
		if(inReq.getUser() == null)
		{
			return null;
		}
		HitTracker todos = getJobTracker(inReq).getTodoSearcher().fieldSearchForUser(inReq, inReq.getUser());
		inReq.putPageValue("searcher", getJobTracker(inReq).getTodoSearcher());
		String hitname = inReq.findValue("hitsname");
		inReq.putPageValue(hitname, todos);
		getJobArchive(inReq);
		return todos;
	}

	
	public void reindex(WebPageRequest inReq) throws Exception
	{
		JobTracker jobtracker = getJobTracker(inReq);
		jobtracker.getJobArchive().clear();
		jobtracker.getJobArchive().getJobSearcher().reIndexAll();
		jobtracker.getTodoSearcher().reIndexAll();
	}

	public Job createNewJob(WebPageRequest inReq) throws Exception
	{
		JobTracker jobtracker = getJobTracker(inReq);
		String id = jobtracker.getJobArchive().nextId();
		Job job = jobtracker.getJobArchive().createJob(id);
		String timezonecode = inReq.getPageProperty("jobprojecttimezone");
		TimeZone zone = null;
		if( timezonecode == null)
		{
			zone = TimeZone.getDefault();
		}
		else
		{
			zone = TimeZone.getTimeZone(timezonecode);
		}
		job.setName(jobtracker.getJobArchive().nextProjectNumber(zone));
		job.setOpen(true);
		job.addGroup(getUserManager().getGroup("notify"));
		job.setCreatedByUser(inReq.getUserName());
		job.setProperty("jobcreationdate", new SimpleDateFormat("M/dd/yyyy").format(new Date()));
		job.setPriority(1);
		inReq.putPageValue("job", job);
		inReq.putSessionValue("tmpjob", job);
		//createJob(inReq, jobtracker, job);
		inReq.redirect("/" + jobtracker.getCatalogId() + "/jobmanager/jobtodos.html?jobid=" + job.getId() );
		return job;
	}
	public void saveJob(WebPageRequest inReq) throws Exception
	{
		JobTracker jobtracker = getJobTracker(inReq);
//		List details = jobtracker.getJobDataProperties("jobedit", inReq.getUser());
		String jobid = inReq.getRequestParameter("jobid");
		Job job = (Job)inReq.getSessionValue("tmpjob");
		if( jobid != null)
		{
			if( job != null && !job.getId().equals(jobid))
			{
				job = null; //Not the one in the session
			}
			if( job == null)
			{
				job = jobtracker.getJobArchive().getJobById(jobid);
			}
		}
		if( job == null)
		{
			job = new Job();
			job.setId(jobid);
		}
		String[] fields = inReq.getRequestParameters("field");
		if( fields == null)
		{
			return;
		}
		PropertyDetails details = jobtracker.getFieldArchive().getPropertyDetails("job");
		
		for (int i = 0; i < fields.length; i++)
		{
			String field = fields[i];
			PropertyDetail detail = details.getDetail(field);
			if( detail != null && (detail.isEditable() || inReq.getUser().hasPermission("tracker.editall")))
			{
				String value = inReq.getRequestParameter(detail.getId()+".value");
				job.setProperty(detail.getId(),value);
			}
		}

		//update todo rank
		String priority = inReq.getRequestParameter("priority.value");
		if (priority == null)
		{
			job.setPriority(10);
		} else
		{
			job.setPriority(Integer.parseInt(priority));
		}
//		List todos = job.getTodos();
//		for (int i=0; i<todos.size(); i++)
//		{
//			((Todo) todos.get(i)).setRank(i+1);
//		}
		jobtracker.getJobArchive().save(job, inReq.getUser());
		jobtracker.getJobSearcher().updateIndex(job);
		jobtracker.getTodoSearcher().updateIndex(job);
		
		inReq.putPageValue("job",job);
	}
	public void saveJobTodo(WebPageRequest inReq) throws Exception
	{
		JobTracker jobtracker = getJobTracker(inReq);
		String jobnumber = inReq.getRequestParameter("jobid");
		Job job = jobtracker.getJobArchive().getJobById(jobnumber);
		String delid = inReq.getRequestParameter("todoid");
		Todo todo = null;
		if( delid == null)
		{
			todo = new Todo();
			job.addTodo(todo);
		}
		else
		{
			todo= job.getTodo(delid);
		}
		List details = jobtracker.getTodoDataProperties("todoedit", inReq.getUser());
		for (Iterator iterator = details.iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			if( detail.isEditable())
			{
				String value = inReq.getRequestParameter(detail.getId()+".value");
				todo.setProperty(detail.getId(),value);
			}
		}
		jobtracker.getJobArchive().save(job, inReq.getUser());
		//jobtracker.getJobSearch().updateIndex(job);
		jobtracker.getTodoSearcher().updateIndex(job, todo);
		
		inReq.putPageValue("job",job);
		inReq.putPageValue("todo",todo);
		inReq.setRequestParameter("todoid", todo.getId());
	}
	public void deleteTimeEntry(WebPageRequest inReq) throws Exception
	{
		Todo todo = loadTodo(inReq);
		if(todo == null){
			throw new OpenEditException("cannot add time to a non existant todorable");
		}
		String entryId = inReq.getRequestParameter("timeid");
		JobTimeEntry entry = todo.getTimeEntry(entryId);		
		todo.removeTimeEntry(entry);
	}	
	public void saveTimeEntry(WebPageRequest inReq) throws Exception
	{
		Todo todo = loadTodo(inReq);
		if(todo == null){
			throw new OpenEditException("cannot add time to a non existant todorable");
		}
		JobTimeEntry timeEntry = null;
		String id = inReq.getRequestParameter("timeid");
		if( id == null)
		{
			timeEntry = todo.getTimeEntry(id);
		}
		if(timeEntry == null)
		{
			timeEntry = new JobTimeEntry();
			JobTracker tracker = getJobTracker(inReq);
			timeEntry.setId(String.valueOf(tracker.getJobArchive().getIdCounter().incrementCount()));
		}

		String description = inReq.getRequestParameter("description");
		double hours = Double.parseDouble(inReq.getRequestParameter("hours"));
		String dateString = inReq.getRequestParameter("date");
		String dateFormat = inReq.getRequestParameter("dateformat");
		Date date;
		if(dateString != null)
		{
			DateFormat df;
			if (dateFormat != null)
				df = new SimpleDateFormat(dateFormat);
			else
				df = getDateFormat();
			try
			{
				date = df.parse(dateString);
			}
			catch (Throwable ee)
			{
				throw new OpenEditException(ee);
			}
						
		}
		else 
		{
			date = new Date();
		}
		timeEntry.setUser(inReq.getUser());
		timeEntry.setDescription(description);
		timeEntry.setHours(hours);
		timeEntry.setDate(date);
		todo.addTimeEntry(timeEntry);
	}
	
	public void loadHits(WebPageRequest inReq) throws Exception
	{
		String hitsname = inReq.findValue("hitsname");
		JobTracker tracker = getJobTracker(inReq);
		tracker.getJobSearcher().loadHits(inReq, hitsname);
	}
	
	public void loadTodoHits(WebPageRequest inReq) throws Exception
	{
		String hitsname = inReq.findValue("hitsname");
		JobTracker tracker = getJobTracker(inReq);
		tracker.getTodoSearcher().loadHits(inReq, hitsname);
	}

	protected DateFormat getDateFormat()
	{
		if (fieldDateFormat == null)
		{
			fieldDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
			fieldDateFormat.setLenient(true);
		}
		return fieldDateFormat;
	}

	protected void setDateFormat(DateFormat inDateFormat)
	{
		fieldDateFormat = inDateFormat;
	}
	
	public void jobReceived(WebPageRequest inReq) throws Exception
	{
		//See who this bug is for
		String subject = inReq.getRequestParameter("summary");
		if( subject == null)
		{
			return; //Validation is broken
		}
		String email = inReq.getRequestParameter("email");
		User 	user = null;
		if( email != null)
		{
			user = getUserManager().getUserByEmail(email);
			if( user == null)
			{
				user = getUserManager().createUser(null, new PasswordGenerator().generate());
				user.setEmail(email);
			}
		}
		JobTracker tracker = getJobTracker(inReq);

		String ln = inReq.getRequestParameter("lastname");
		if( ln != null)
		{
			user.setLastName(ln);
		}
		String fn = inReq.getRequestParameter("firstname");
		if(fn != null)
		{
			user.setFirstName(fn);
		}	
		String company = inReq.getRequestParameter("Company");
		if( company != null)
		{
			user.put("Company",company);
		}
		String phone = inReq.getRequestParameter("Phone1");
		if( phone != null)
		{
			user.put("Phone1",phone);
		}

		String jobid = inReq.getRequestParameter("newjobid");
		if( jobid == null)
		{
			jobid = inReq.getRequestParameter("jobid");
		}
		if( user == null)
		{
			user = inReq.getUser();
		}
		if( jobid == null)
		{
			jobid = user.getUserName() + "_home";
		}
		Job existing = (Job)inReq.getPageValue("tmpjob");
		if( existing == null || !existing.getId().equals(jobid))
		{
			existing = tracker.getJobArchive().getJobById(jobid);
		}
		inReq.removeSessionValue("tmpjob");
		if( existing == null)
		{
			existing = tracker.getJobArchive().createJob(jobid);
			//existing.setName(user.getEmail());
			existing.setCreatedByUser(user.getUserName());
			existing.setProperty("assignedto", user.getUserName());
		}
		String jobname = inReq.getRequestParameter("jobname.value");
		if( jobname != null)
		{
			existing.setName(jobname);
		}
			List details = tracker.getJobDataProperties("jobdetailsclient", inReq.getUser());
			for (Iterator iterator = details.iterator(); iterator.hasNext();)
			{
				PropertyDetail detail = (PropertyDetail) iterator.next();
				if( detail.isEditable())
				{
					String value = inReq.getRequestParameter(detail.getId()+".value");
					if( value != null)
					{
						existing.setProperty(detail.getId(),value);
					}
				}
			}
			existing.setOpen(true);
		
		//append todo
		Todo todo = new Todo();
		todo.setName(subject);
		
		GregorianCalendar today = new GregorianCalendar();
		today.add(Calendar.DAY_OF_YEAR, 1);
		
		todo.setProperty("duedate", DateStorageUtil.getStorageUtil().formatForStorage(today.getTime()));
		
		List tododetails = tracker.getTodoDataProperties("todoedit", inReq.getUser());
		for (Iterator iterator = tododetails.iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			if( detail.isEditable())
			{
				String value = inReq.getRequestParameter(detail.getId()+".value");
				if( value != null)
				{
					todo.setProperty(detail.getId(),value);
				}
			}
		}
		
		StatusChange change = new StatusChange();
		String status = inReq.getRequestParameter("todostatus");
		if( status == null)
		{
			status = "receivedonline";
		}
		change.setType(status);
		change.setDated(new Date());
		change.setNote(inReq.getRequestParameter("note"));
		change.setUser(user);
		todo.addStatusChange(change);
		todo.setCreatedByUser(user.getUserName());
		existing.addTodo(todo);
		String area = inReq.getRequestParameter("todoarea");
		todo.setProperty("todoarea", area);
		getUserManager().saveUser(user);

		Group notifygroup = getUserManager().getGroup("notify");		
		if( notifygroup == null)
		{
			notifygroup = getUserManager().createGroup("notify");
		}
		existing.addGroup(notifygroup);
		tracker.getJobArchive().save(existing, user);
		tracker.getJobSearcher().updateIndex(existing);
		tracker.getTodoSearcher().updateIndex(existing, todo);

		//For the thanks email
//		inReq.setRequestParameter("to", email);
//		inReq.setRequestParameter("subject", inReq.findValue("subjectprefix") + todo.getName() );
//		inReq.putPageValue("todo", todo);
//		inReq.putPageValue("job", existing);
//		inReq.putPageValue("message", change);
//		inReq.putPageValue("password",getUserManager().decryptPassword(user));
//		inReq.putPageValue("client",user);

		String sentnotification = inReq.getRequestParameter("notify");
		if( Boolean.parseBoolean(sentnotification))
		{
			try
			{
				tracker.getJobNotify().notify(existing, todo, change, inReq);
			}
			catch (OpenEditException e)
			{
				log.info("Unable to send e-mail notification for tracker posting by user: " + user.getId());
			}
			
		}

		String edittodo = inReq.findValue("viewpostings");
		
		if( Boolean.parseBoolean(edittodo))
		{///jobmanager/todos/todomessages.html?jobid=1814&todoid=541
			///jobmanager/jobtodos.html?jobid=1814
			inReq.redirect("/" + tracker.getCatalogId() + "/jobmanager/jobtodos.html?jobid=" + existing.getId() );
		}		
	}
/*
	protected void createJob(WebPageRequest inReq, JobTracker tracker,
			Job existing) {
		String path = "/" + tracker.getCatalogId() + "/postings/" + existing.getId();
		PageSettings home = getPageManager().getPageSettingsManager().getPageSettings(path + "/_site.xconf");
			PageProperty fb = home.getProperty("fallbackdirectory");
			fb = new PageProperty("fallbackdirectory");
			home.putProperty(fb);
			fb.setValue("/${catalogid}/jobmanager");
			fb = new PageProperty("jobid");
			home.putProperty(fb);
			fb.setValue(existing.getId());
			getPageManager().getPageSettingsManager().saveSetting(home);
			getPageManager().getPageSettingsManager().clearCache(home.getPath());
			getPageManager().clearCache(path);
			String edittodo = inReq.findValue("viewpostings");
			
			if( Boolean.parseBoolean(edittodo))
			{///jobmanager/todos/todomessages.html?jobid=1814&todoid=541
				///jobmanager/jobtodos.html?jobid=1814
				inReq.redirect("/" + tracker.getCatalogId() + "/jobmanager/jobtodos.html?jobid=" + existing.getId() );
			}
	}	
*/	
	 public HitTracker loadJobTodos(WebPageRequest inReq) throws Exception
     {
             JobTracker jobtracker = getJobTracker(inReq);                
             Job job = loadJob(inReq);                                                
             inReq.setRequestParameter("jobid", job.getId());
             HitTracker todos = jobtracker.getTodoSearcher().fieldSearch(inReq);
             inReq.putPageValue("todos", todos);
             
             return todos;
     }
	
	
	public void loadIssuesForUser(WebPageRequest inReq) throws Exception
	{
		User user = inReq.getUser();
		if( user == null)
		{
			return;
		}
		JobTracker tracker  = getJobTracker(inReq);
		Job job = tracker.getJobArchive().getJobById(user.getUserName() + "_issues");
		inReq.putPageValue("job", job);
	}
	public void uploadFinish( WebPageRequest inContext) throws Exception
	{
		FileUpload command = new FileUpload();
		command.setPageManager(getPageManager());
		UploadRequest properties = command.parseArguments(inContext);
		if ( properties == null)
		{
			return;
		}
		
		Job job = loadJob(inContext);
		String catalog = loadJobArchive(inContext).getCatalogId();
		
		for (Iterator iterator = properties.getUploadItems().iterator(); iterator.hasNext();) {
			FileUploadItem item	 = (FileUploadItem) iterator.next();
			String ext = item.getName();
			String path = "/WEB-INF/data/"+ catalog + "/" + job.getSourcePath() + "/attachments/" + ext;
			properties.saveFileAs(item,path, inContext.getUser());
				
		}
			
	}
	public void uploadTodoFinish( WebPageRequest inContext) throws Exception
	{
		FileUpload command = new FileUpload();
		command.setPageManager(getPageManager());
		UploadRequest properties = command.parseArguments(inContext);
		if ( properties == null)
		{
			return;
		}
		String ext = properties.getFirstItem().getName();
		Job job = loadJob(inContext);
		Todo todo = loadTodo(inContext);
		
		String messageid = inContext.getRequestParameter("messageid");
		
		BaseJobArchive archive = loadJobArchive(inContext);
		String catalog = archive.getCatalogId();
		String path = "/"+ catalog + "/postings/" + job.getId() + "/todos/" + todo.getId() + "/attachments/" + messageid + "/" +  ext;
		properties.saveFirstFileAs(path, inContext.getUser());		
		inContext.putPageValue("path", path);
	}

	public void searchFiles(WebPageRequest inReq) throws Exception
	{
		Job job = loadJob(inReq);
		
		String catalog = getJobTracker(inReq).getCatalogId();
		String path = "/WEB-INF/data/"+ catalog + "/" + job.getSourcePath() + "/attachments/";

		List children = getPageManager().getChildrenPaths(path);
		List hits = new ArrayList();
		for (Iterator iterator = children.iterator(); iterator.hasNext();)
		{
			String onepath = (String) iterator.next();
			if( !onepath.contains(".versions"))
			{
				ContentItem page = getPageManager().getLatestVersion(onepath);
				hits.add(page);
			}
		}
		
		Collections.sort(hits,new java.util.Comparator()
		{
			public int compare(Object arg0, Object arg1)
			{
				ContentItem item1 = (ContentItem)arg0;
				ContentItem item2 = (ContentItem)arg1;
				return item1.lastModified().compareTo(item2.lastModified());
			}
		}
		);
		
		HitTracker results = new ListHitTracker(hits);
		inReq.putPageValue("hits", results);
			
	}
	
	
	
	public void saveJobPermissions(WebPageRequest inReq) throws Exception
	{
		if( inReq.getRequestParameter("save") != null )
		{
			Job job = loadJob(inReq);
			JobTracker jobtracker = getJobTracker(inReq);
			
			String[] groups = inReq.getRequestParameters("group");
			job.getGroups().clear();
			if( groups != null)
			{
				for (int i = 0; i < groups.length; i++)
				{
					Group group = getUserManager().getGroup(groups[i]);
					job.addGroup(group);
				}
			}
			jobtracker.getJobArchive().save(job, inReq.getUser());
			//jobtracker.getJobSearch().updateIndex(job);
			jobtracker.getTodoSearcher().updateIndex(job);
			inReq.putPageValue("savedok", "true");
		}
	}
	
	public void changeJobSort(WebPageRequest inReq) throws Exception
	{
		JobTracker jobtracker = getJobTracker(inReq);
		jobtracker.getJobSearcher().changeSort(inReq,jobtracker);
	}
	public void changeTodoSort(WebPageRequest inReq) throws Exception
	{
		JobTracker jobtracker = getJobTracker(inReq);
		jobtracker.getTodoSearcher().changeSort(inReq,jobtracker);
	}

	
	public void updateTodoRanks(WebPageRequest inReq) throws Exception
	{
		String orders = inReq.getRequestParameter("order");
		if (orders==null)
			return;
		
		Job job = loadJob(inReq);
		JobTracker jobtracker = getJobTracker(inReq);
		//A lower number is better. So 0's go first. 
		//int offset = (job.getPriority() + 1) * 1000;
		String[] order=orders.split("&");
		for (int i=0; i<order.length;i++)
		{
			String[] values = order[i].split("=");
			if( values.length > 1)
			{
				String todoid=values[1];
				int rank = job.getPriority() * (i+1);
				Todo todo = job.getTodo(todoid);
				todo.setRank(rank);
			}
			//System.out.println("todo "+todoid+ " ranked "+(i+1));
		}
		jobtracker.getJobArchive().save(job, inReq.getUser());
		jobtracker.getJobSearcher().updateIndex(job);
		jobtracker.getTodoSearcher().updateIndex(job);
	}
	
	class Comparator implements java.util.Comparator {

		private JobTracker jobTracker;
		
		public Comparator(JobTracker jt) {
			this.jobTracker = jt;
		}
		
		public int compare(Object o1, Object o2) {
			if (o1 instanceof Todo && o2 instanceof Todo)
			{
				Todo t1=(Todo)o1, t2=(Todo) o2;
				try {
					Job j1= jobTracker.getJobArchive().getJobById(t1.getJobId());
					Job j2= jobTracker.getJobArchive().getJobById(t2.getJobId());
					if (j1.getPriority()==0) j1.setPriority(10);
					if (j2.getPriority()==0) j2.setPriority(10);
					return (t1.getRank()+(j1.getPriority()*1000))-(t2.getRank()+(j2.getPriority()*1000));
				} catch (OpenEditException e) {
					e.printStackTrace();
				}
			}
			return 0;
		}
		
	}
	
	/**
	 * Generates a report of the hours spent by some user between to specified dates.
	 * Requires request parameters {dateTo, dateFrom} and a valid user in session.
	 * Puts a page value "report", which is a list of maps of pairs {JobTimeEntry, StringTokenizer}. Every map inside "report"
	 * represents a different date. The StringTokenizer element contains allways two tokens: jobId and todoId (e.g. 203 and 301). 
	 * @param inContext
	 * @throws Exception 
	 */
	public void generateHoursReport ( WebPageRequest inContext ) throws Exception
	{
		if (inContext.getRequestParameter("datefrom") == null || inContext.getRequestParameter("dateto") == null)
			return;
		
		DateFormat df, reportdf;
		if (inContext.getRequestParameter("dateformat") != null)
			df = new SimpleDateFormat(inContext.getRequestParameter("dateformat"));
		else
			df = getDateFormat();
		
		if (inContext.getRequestParameter("reportdateformat") != null)
			reportdf = new SimpleDateFormat(inContext.getRequestParameter("reportdateformat"));
		else
			reportdf = getDateFormat();
		
		
		GregorianCalendar dateFrom = new GregorianCalendar(), dateTo = new GregorianCalendar();
		try 
		{
			dateFrom.setTime(df.parse(inContext.getRequestParameter("datefrom")));
			dateTo.setTime(df.parse(inContext.getRequestParameter("dateto")));
		} 
		catch (Exception e)
		{
			throw new OpenEditException("Cannot read search dates", e);
		}
		
		if ( dateFrom == null || dateTo == null || inContext.getUser() == null)
		{
			return;
		}
		
		Map days = ListOrderedMap.decorate(new HashMap());
		
		JobArchive archive = getJobArchive(inContext);
		Iterator jobs = null;
		String jobid = inContext.getRequestParameter("jobid");
		if( jobid == null)
		{
			jobs = getJobList(inContext).getAllHits();
		}
		String username = inContext.getRequestParameter("username");
		while (jobs == null || jobs.hasNext())
		{
			if( jobs != null)
			{
				jobid = String.valueOf( ((Data) jobs.next()).get("id") );
			}
			Job job = archive.getJobById(jobid,false);
			if (job == null)
			{
				throw new OpenEditException("Could'n get job: " + jobid );
			}
			Iterator todos = job.getTodos().iterator();
			while (todos.hasNext())
			{
				Todo todo = (Todo) todos.next();
				Iterator timeEntries = todo.getJobTimeEntryArchive().getTimeEntries(todo).iterator();
				while (timeEntries.hasNext())
				{
					JobTimeEntry timeEntry = (JobTimeEntry) timeEntries.next();
					GregorianCalendar date = new GregorianCalendar();
					date.setTime(timeEntry.getDate());
					if (date.equals(dateFrom) || date.equals(dateTo) || (date.after(dateFrom) && date.before(dateTo))  )
					{
						if( username != null && !username.equals(timeEntry.getUser().getUserName()))
						{
							continue;
						}
						Day day = (Day)days.get(date);
						if( day == null)
						{
							day = new Day();
							days.put(date, day);
						}
						//This is a group of hours for one date
						day.addRow(timeEntry, job, todo );
					}
				}
			}
			if( jobs == null)
			{
				break;
			}
		}
		List dates = new ArrayList();
		dates.addAll(days.keySet());
		List report = new ArrayList();
		Collections.sort(dates);
		for (Iterator iterator = dates.iterator(); iterator.hasNext();) {
			report.add(days.get(iterator.next()));
		}
		inContext.putPageValue("report", report); //Send a list of days
		inContext.putPageValue("dateformater", reportdf);
	}
	
	public void getDate ( WebPageRequest inContext ) throws OpenEditException
	{
		String dateFormat = inContext.findValue("dateformat");
		if (dateFormat == null)
			dateFormat = "MM-dd-yyyy";
		
		SimpleDateFormat df = new SimpleDateFormat(dateFormat);
		inContext.putPageValue("today", df.format(new Date()));
	}
	
	public void deleteTodoStatusChange(WebPageRequest inReq) throws Exception
	{
		Todo todo = loadTodo(inReq);
		if(todo == null){
			throw new OpenEditException("cannot add time to a non existant todorable");
		}
		String messageId = inReq.getRequestParameter("messageid");
		StatusChange message = todo.getStatusChange(messageId);		
		todo.removeStatusChange(message);
		
		JobTracker jobtracker = getJobTracker(inReq);
		String jobnumber = inReq.getRequestParameter("jobid");
		Job job = jobtracker.getJobArchive().getJobById(jobnumber);
		jobtracker.getJobArchive().save(job, inReq.getUser());
	}	

	//This is used to edit an existing status change
	public void saveTodoStatusChange(WebPageRequest inReq) throws Exception
	{
		JobTracker jobtracker = getJobTracker(inReq);
		String jobnumber = inReq.getRequestParameter("jobid");
		Job job = jobtracker.getJobArchive().getJobById(jobnumber);
		String delid = inReq.getRequestParameter("todoid");
		
		Todo todo = null;
		if( delid == null)
		{
			todo = new Todo();
			job.addTodo(todo);
		}
		else
		{
			todo= job.getTodo(delid);
		}
		String messageid = inReq.getRequestParameter("messageid");
		StatusChange statuschange = todo.getStatusChange(messageid);
		
		statuschange.setType(inReq.getRequestParameter("todostatus"));
		statuschange.setNote(inReq.getRequestParameter("note"));

		String path = attachFile(jobtracker.getCatalogId(), job, todo, statuschange.getId());
		statuschange.setAttachmentPath(path);
		jobtracker.getJobArchive().save(job, inReq.getUser());
		
	}

	private String attachFile(String inCatId, Job job, Todo todo, String messageid)
	{
		String path = "/"+ inCatId + "/postings/" + job.getId() + "/todos/" + todo.getId() + "/attachments/" + messageid + "/";
		
		List paths = getPageManager().getChildrenPaths(path);
		if( paths.size() > 0)
		{
			for (Iterator iterator = paths.iterator(); iterator.hasNext();)
			{
				String found = (String) iterator.next();
				if( !found.endsWith(".versions"))
				{
					//grab the last one
					return found;
				}
			}
		}
		return null;
	}
	
	public void createId(WebPageRequest inReq) throws Exception
	{
		String id = (String)inReq.getSessionValue("newid");
		if( id == null)
		{
			JobArchive archive = getJobArchive(inReq);
			int newChange = archive.createId();
			inReq.putSessionValue("newid", String.valueOf(newChange));
		}
	}
	
	public void changeTodoStatus( WebPageRequest inReq ) throws Exception
	{
		String type = inReq.getRequestParameter("todostatus");
		if( type == null)
		{
			inReq.removePageValue("todo");
			return;
		}
		Job job = loadJob(inReq);//getDeinReq.getRequestParameter("jobnumber");
		Todo todo = loadTodo(inReq);

		StatusChange newChange = new StatusChange();
		newChange.setNote(inReq.getRequestParameter("note"));
		newChange.setUser(inReq.getUser());
		newChange.setType(type);
		newChange.setDated(new Date());
		String messageid = inReq.getRequestParameter("messageid");
		newChange.setId(messageid);
		inReq.removeSessionValue("newid");
		JobTracker tracker = getJobTracker(inReq);
		String path = attachFile(tracker.getCatalogId(), job, todo, messageid);
		newChange.setAttachmentPath(path);
		
		todo.addStatusChange(newChange);
		
		tracker.getJobArchive().save(job, inReq.getUser());
		tracker.getTodoSearcher().updateIndex(job);
		
		String notify = inReq.getRequestParameter("notify");
		if (Boolean.parseBoolean(notify))
		{
			tracker.getJobNotify().notify(job, todo, newChange, inReq);
		}
		String subscribe = inReq.getRequestParameter("subscribe");
		if(Boolean.parseBoolean(subscribe))
		{
			addUserToPublicJob(inReq);
		}
		//inReq.removePageValue("todo");
		inReq.putPageValue("message","Message saved");
		inReq.removePageValue("todo");

	}
	public void moveTodo(WebPageRequest inReq) throws Exception
	{
		Job job = loadJob(inReq);
		
		String id = inReq.getRequestParameter("jobiddest");

		JobTracker jobtracker = getJobTracker(inReq);
		Job jobdest = jobtracker.getJobArchive().getJobById(id);
	
		String[] todos = inReq.getRequestParameters("todoselected");
		for (int i = 0; i < todos.length; i++)
		{
			Todo todo = job.getTodo(todos[i]);
			Todo copy = new Todo();
			copy.setId(jobdest.getId() + "_" + todo.getId());
			copy.setJobId(jobdest.getId());
			copy.setProperties(todo.getProperties());
			//status
			copy.setStatusChanges(todo.getStatusChanges());
			todo.setStatusChanges(Collections.EMPTY_LIST);
			
			//time
			copy.setTimeEntries(todo.getTimeEntries());
			copy.setJobTimeEntryArchive(todo.getJobTimeEntryArchive());
			todo.setTimeEntries(Collections.EMPTY_LIST);
			//save
			jobdest.addTodo(copy);
			jobtracker.getJobArchive().save(jobdest, inReq.getUser());
			todo.saveTime();
			copy.saveTime(); 
			job.getTodos().remove(todo);
		}
		jobtracker.getJobArchive().save(job, inReq.getUser());
		jobtracker.getJobArchive().save(jobdest, inReq.getUser());
		job.setBudget(null);
		jobdest.setBudget(null);
		//move the old time data
		
		
		inReq.putPageValue("jobdest", jobdest);
	}
	public boolean checkViewPermission(WebPageRequest inReq) throws Exception
	{
		Job job = loadJob(inReq);
		if(job != null)
		{
			Object hidden = job.getProperties().get("hidden");
			if(hidden != null)
			{
				boolean isHidden = Boolean.parseBoolean(hidden.toString());
				if(isHidden)
				{
					User user = inReq.getUser();
					if(!job.hasMember(user))
					{
						return false;
					}
				}
			}
		}
		return true;
	}
	
	public void addUserToJob(WebPageRequest inReq) throws Exception
	{
		Job job = loadJob(inReq);
		String[] users = inReq.getRequestParameters("usertoadd");
		for(int i=0; i<users.length;i++)
		{
			addUserToJob(users[i], job, inReq);
		}
		//send notification
		//tracker.getJobNotify().followerNotify(job, inReq.getUser(), true, inReq);
		
	}
	
	public void addUserToJob(String inUsername, Job inJob, WebPageRequest inReq)
	{
		JobUserEntry entry = new JobUserEntry();
		User user = getUserManager().getUser(inUsername);
		if(user != null)
		{
			entry.setUser(user);
			inJob.addUserEntry(entry);
		}
		JobTracker tracker = getJobTracker(inReq);
		getJobArchive(inReq).save(inJob, inReq.getUser());
		tracker.getJobSearcher().updateIndex(inJob);
		tracker.getTodoSearcher().updateIndex(inJob);
	}
	public void sentInvites(WebPageRequest inReq) throws Exception
	{
		String addresses = inReq.getRequestParameter("email");
		Job job = loadJob(inReq);
		if(addresses == null)
		{
			String targetid = inReq.getRequestParameter("targetid");
			User targetUser = getUserManager().getUser(targetid);
			addresses = targetUser.getEmail();
			
		}
		try
		{
			getJobTracker(inReq).invite(job, addresses, inReq);
		}
		catch ( Exception ex)
		{
			inReq.putPageValue("emailerror", ex);
			log.error( ex);
		}
	}
	public void addUserToPublicJob(WebPageRequest inReq) throws Exception
	{
		Job job = loadJob(inReq);
		JobUserEntry entry = new JobUserEntry();
		entry.setUser(inReq.getUser());
		job.addUserEntry(entry);
		JobTracker tracker = getJobTracker(inReq);
		getJobArchive(inReq).save(job, inReq.getUser());
		tracker.getJobSearcher().updateIndex(job);
		tracker.getTodoSearcher().updateIndex(job);
		
		//send notification
		tracker.getJobNotify().followerNotify(job, inReq.getUser(), true, inReq);
	}
	
	public void removeUserFromPublicJob(WebPageRequest inReq) throws Exception
	{
		Job job = loadJob(inReq);
		job.removeUserEntry(inReq.getUser());
		getJobArchive(inReq).save(job, inReq.getUser());
		JobTracker tracker = getJobTracker(inReq);
		tracker.getJobSearcher().updateIndex(job);
		tracker.getTodoSearcher().updateIndex(job);
		
		//send Notification
		tracker.getJobNotify().followerNotify(job, inReq.getUser(), false, inReq);
		
	}
	public void removeUserFromJob(WebPageRequest inReq) throws Exception
	{
		
		String userToRemove = inReq.getRequestParameter("toremove");
		if(userToRemove != null)
		{
			Job job = loadJob(inReq);
			User user = getUserManager().getUser(userToRemove);
			job.removeUserEntry(user);
			getJobArchive(inReq).save(job, inReq.getUser());
			getJobTracker(inReq).getJobSearcher().updateIndex(job);
			getJobTracker(inReq).getTodoSearcher().updateIndex(job);
		}
		
	}
	
	public void jobHasImage(WebPageRequest inReq) throws Exception
	{
		Job job = loadJob(inReq);
		job.setProperty("hasimage", "true");
		getJobArchive(inReq).save(job, inReq.getUser());
		
	}
	public void toggleEditPermission(WebPageRequest inReq) throws Exception
	{
		String username = inReq.getRequestParameter("username");
		Job job = loadJob(inReq);
		User user = getUserManager().getUser(username);
		JobUserEntry jue = null;
		if(job.isEditor(user))
		{
			jue = job.setEditor(user, false);
		}
		else
		{
			jue = job.setEditor(user, true);
		}
		getJobArchive(inReq).save(job, inReq.getUser());
		inReq.putPageValue("follower", jue);
	}
	public PostMail getPostMail()
	{
		return fieldPostMail;
	}

	public void setPostMail(PostMail inPostMail)
	{
		fieldPostMail = inPostMail;
	}

	
	public List getTodosForMonth(WebPageRequest inReq) throws Exception
	{
		String inMonth = inReq.getRequestParameter("month");
		Job job = loadJob(inReq);
		List todos = new ArrayList();
		
		for (Iterator iterator = job.getTodos().iterator(); iterator.hasNext();)
		{
			Todo todo = (Todo) iterator.next();
			if( inMonth == null || inMonth.length() == 0)
			{
				todos.add(todo);
				continue;
			}
			StatusChange first = todo.getFirstStatus();
			if( first == null )
			{
				continue;
			}
			int m = first.getDated().getMonth() + 1;
			if( inMonth.equals(String.valueOf(m)))
			{
				todos.add(todo);
			}
		}
		
		Collections.sort(todos,new java.util.Comparator()
		{
			public int compare(Object inO1, Object inO2)
			{
				Todo t1 = (Todo)inO1;
				Todo t2 = (Todo)inO2;
				StatusChange first1 = t1.getFirstStatus();
				StatusChange first2 = t2.getFirstStatus();
				if( first1 == null)
				{
					return 1;
				}
				return first1.getDated().compareTo(first2.getDated());
			}
		});

		if( inMonth == null && todos.size() > 50)
		{
			todos = todos.subList(todos.size() - 50,todos.size());
		}
		
		inReq.putPageValue("todos", todos);
		Date starting = job.getFirstTodoDate(todos);
		Date ending = job.getLastTodoDate(todos);
		if( starting == null )
		{
			GregorianCalendar cal = new GregorianCalendar();
			if( ending != null)
			{
				cal.setTime(ending);
				cal.add(Calendar.MONTH , -1);
				starting = cal.getTime();
			}
			if( ending == null && starting != null)
			{
				cal.setTime(starting);
				cal.add(Calendar.MONTH , 1);
			}
		}
		
		//TODO: padd the dates a little
		
		inReq.putPageValue("starting", starting);
		inReq.putPageValue("ending", ending);
		
		return todos;
	}

	
}


