package org.openedit.tracker;

import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.httpclient.util.DateUtil;
import org.entermedia.email.PostMail;
import org.entermedia.email.Recipient;
import org.entermedia.email.TemplateWebEmail;
import org.openedit.Data;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.util.DateStorageUtil;

import com.openedit.WebPageRequest;
import com.openedit.users.User;

public class JobTracker
{
	protected BaseJobArchive fieldJobArchive;
	protected JobSearcher fieldJobSearcher;
	protected TodoSearcher fieldTodoSearcher;
	protected File fieldRoot;
	protected JobNotify fieldJobNotify;
	protected SearcherManager fieldSearcherManager;
	protected String fieldCatalogId;
	protected PostMail fieldPostMail;
	
	public BaseJobArchive getJobArchive()
	{
		return fieldJobArchive;
	}
	


	public void sendInvitation(Data inInvite,String body, WebPageRequest inReq)
	{
		TemplateWebEmail email = getPostMail().getTemplateWebEmail();
		email.loadSettings(inReq); //uses systemfromemail
		
		User sender = inReq.getUser();
		email.setProperty("senderuserid", sender.getId());
		email.setProperty("senderfirstname", sender.getFirstName());
		email.setProperty("senderlastname", sender.getLastName());
		email.setMessage(body);
		email.setTo(inInvite.get("email"));
		email.setFrom("support@openedit.org");
		email.send();
	}
	
	public void setJobArchive(BaseJobArchive inJobArchive)
	{
		fieldJobArchive = inJobArchive;
	}
	public void setPostMail(PostMail inPostMail)
	{
		fieldPostMail = inPostMail;
	}
	
	public JobSearcher getJobSearcher()
	{
		if( fieldJobSearcher == null)
		{
			fieldJobSearcher = (JobSearcher)getSearcherManager().getSearcher(getCatalogId(),"job");
		}
		return fieldJobSearcher;
	}
	
	public void setJobSearcher(JobSearcher inJobSearch)
	{
		fieldJobSearcher = inJobSearch;
	}
	
	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
		getJobArchive().setCatalogId(inCatalogId);
		getJobArchive().setPropertyDetailsArchive(getJobSearcher().getPropertyDetailsArchive());
		
		//TODO: Lazy load these?
		getJobSearcher().setIndexPath("/" + inCatalogId + "/data/jobs/search/index" );		
		getTodoSearcher().setIndexPath("/" + inCatalogId + "/data/jobs/todos/search/index");
		
		getJobArchive().setJobSearcher(getJobSearcher()); //This seems bad
		
		//TODO: Get rid of this bi-dependency:
		getJobSearcher().setJobArchive(getJobArchive());
		getTodoSearcher().setJobArchive(getJobArchive());

		getJobNotify().setTemplate("/" + inCatalogId + "/email/onchange.html");
		//getJogetTimeEntryArchive().setCatalogId(inCatalogId);
		
	}
	public void invite(Job inJob, String inAddresses, WebPageRequest inReq)
	{
		List recipients = getPostMail().getTemplateWebEmail().setRecipientsFromUnknown(inAddresses);
		for (Iterator iterator = recipients.iterator(); iterator.hasNext();)
		{
			Recipient r = (Recipient) iterator.next();
			String id = nextInviteId();
			//String body = inBody.replace("${invitationid}", id);
			inReq.putPageValue("invitationid", id);
			String body = inReq.getRequestParameter("content");
			body = body.replace("${invitationid}", id);
			String subject = inReq.findValue("subject");
			Data invite = saveInvite(inJob, id, r.getEmailAddress(), subject, body);
			
			sendInvitation(invite, body, inReq);
		}
	}
	public Data saveInvite(Job inJob, String inInviteId, String inEmail, String inSubject, String inBody)
	{
		//need to build recipient list this shouldn't be too bad
		//Now we need to add data for future lookups
		
		//Now save the Invite ID
		Searcher inviationSearcher  = getInvitationSearcher();
		Data invite = inviationSearcher.createNewData();

		invite.setSourcePath(inJob.getSourcePath());
		invite.setId(inInviteId);
		
		invite.setProperty("email", inEmail);
		invite.setProperty("jobid", inJob.getId());
		invite.setProperty("subject", inSubject);
		invite.setProperty("invitestatus", "1");
		invite.setProperty("body", inBody);
		String now = DateStorageUtil.getStorageUtil().formatForStorage( new Date());
		invite.setProperty("datesent", now);

		inviationSearcher.saveData(invite, null);
		return invite;
	}
	public PostMail getPostMail()
	{
		return fieldPostMail;
	}
	public String nextInviteId()
	{
		return getInvitationSearcher().nextId();
	}
	public Searcher getInvitationSearcher()
	{
		return getSearcherManager().getSearcher(getCatalogId(), "invitation");
	}
	public String getCatalogId()
	{
		return fieldCatalogId;
	}
	
	public File getRoot()
	{
		return fieldRoot;
	}
	
	public void setRoot(File inRoot)
	{
		fieldRoot = inRoot;
	}
	
	public PropertyDetailsArchive getFieldArchive()
	{
		return getJobSearcher().getPropertyDetailsArchive();
	}
		
	public List getJobDataProperties(String inScreenName, User inUser) throws Exception
	{
		return getFieldArchive().getDataProperties("job", inScreenName, inUser);
	}
	
	public List getTodoDataProperties(String inScreenName, User inUser) throws Exception
	{
		return getFieldArchive().getDataProperties("todo", inScreenName, inUser);
	}
	
	public TodoSearcher getTodoSearcher()
	{
		if( fieldTodoSearcher == null)
		{
			fieldTodoSearcher = (TodoSearcher)getSearcherManager().getSearcher(getCatalogId(),"todo");
		}
		return fieldTodoSearcher;
	}
	
	public void setTodoSearcher(TodoSearcher inTodoSearcher) {
		fieldTodoSearcher = inTodoSearcher;
	}
	
	public JobNotify getJobNotify() {
		return fieldJobNotify;
	}
	
	public void setJobNotify(JobNotify inJobNotify) {
		fieldJobNotify = inJobNotify;
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}
	
}
