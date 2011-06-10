package org.openedit.tracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.entermedia.email.PostMail;
import org.entermedia.email.Recipient;
import org.entermedia.email.TemplateWebEmail;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.Group;
import com.openedit.users.User;
import com.openedit.users.UserManager;

public class JobNotify 
{	
	protected PostMail fieldPostMail;
	protected String fieldTemplate;
	protected UserManager fieldUserManager;
	protected PageManager fieldPageManager;

	
	
	/**
	 * @param inJob
	 * @param inUser
	 * @param added : set this to "true" if the user is being added, "false" if user is being removed.
	 * @param inReq
	 * @throws Exception
	 */
	public void followerNotify(Job inJob, User inUser, boolean added, WebPageRequest inReq) throws Exception
	{
		TemplateWebEmail mailer = getPostMail().getTemplateWebEmail();
		Page template = getPageManager().getPage("/" + inReq.findValue("catalogid") + "/email/followerAdded.html");
		mailer.loadSettings(inReq.copy(template));
		mailer.setMailTemplatePage(template);		

		String subject = mailer.getWebPageContext().findValue("emailsubject");
		
		
		String message = null;
		if(added)
		{
			subject = subject + "You have a new subscriber.";
			message = inUser.getScreenName() + " is now following your postings in " + inJob.getName() + ".";
		}
		else
		{
			subject = subject + "You have lost a subscriber.";
			message = inUser.getScreenName() + " is no longer following your postings in " + inJob.getName() + ".";
		}
		mailer.setSubject( subject );
		mailer.getWebPageContext().putPageValue("message", message);
		mailer.getWebPageContext().putPageValue("job", inJob);
		String from = mailer.getWebPageContext().findValue("from");
		mailer.setFrom(from);	
		if( mailer.getFrom() == null)
		{
			mailer.setFrom("support@openedit.org");
		}		
		//Email Job Owner
		User jobOwner = getUserManager().getUser(inJob.getAssignedTo());
		if(jobOwner != null && jobOwner.getEmail()!= null)
		{
			Recipient recipient = new Recipient();
			recipient.setEmailAddress(jobOwner.getEmail());
			recipient.setLastName(jobOwner.getLastName());
			recipient.setFirstName(jobOwner.getFirstName());
			mailer.setRecipient(recipient);
			mailer.getWebPageContext().putPageValue("sendto", jobOwner);
			mailer.send();
		}
	}
	public void notify(Job inJob, Todo inTodo, StatusChange inNewChange, WebPageRequest inReq) throws Exception
	{
		TemplateWebEmail mailer = getPostMail().getTemplateWebEmail();
		Page template = getPageManager().getPage(getTemplate());
		mailer.loadSettings(inReq.copy(template));
		mailer.setMailTemplatePage(template);		

		String subject = mailer.getWebPageContext().findValue("emailsubject");
		String jobname = inJob.getName();
		if(jobname == null || jobname.equals(""))
		{
			jobname = getUserManager().getUser(inJob.getCreatedByUser()).getScreenName() + "'s Issues";
		}
			
		subject = subject + " " + jobname + " - " + inTodo.getName();
		mailer.setSubject( subject );

		mailer.getWebPageContext().putPageValue("message", inNewChange);
		mailer.getWebPageContext().putPageValue("job", inJob);
		mailer.getWebPageContext().putPageValue("todo", inTodo);
		String from = mailer.getWebPageContext().findValue("from");
		mailer.setFrom(from);	
		//mailer.setFrom(inNewChange.getUser().getEmail());
		//Email everyone in job group
		List list = findUsersFor(inJob);
		//email the creator
		if( mailer.getFrom() == null)
		{
			mailer.setFrom("support@openedit.org");
		}
		
//		User created = getUserManager().getUser(inJob.getCreatedByUser() );
//		if( created != null)
//		{
//			if (mailer.getFrom() == null)
//			{
//				mailer.setFrom(created.getEmail());
//			}
//		}
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			User user = (User) iter.next();
			String email = user.getEmail();
			if( email != null)
			{
				Recipient recipient = new Recipient();
				recipient.setEmailAddress(email);
				recipient.setLastName(user.getLastName());
				recipient.setFirstName(user.getFirstName());
				mailer.setRecipient(recipient);
				mailer.getWebPageContext().putPageValue("sendto", user);
				mailer.send();
			}
		}
	}

	public List findUsersFor(Job inJob)
	{
		Set users = new HashSet();	
		
		User created = getUserManager().getUser(inJob.getCreatedByUser() );
		if( created != null && created.getEmail() != null)
		{
			users.add(created);
		}
		
		for (Iterator iter = inJob.getGroups().iterator(); iter.hasNext();) {
			Group group = (Group) iter.next();

			HitTracker list = getUserManager().getUsersInGroup(group);
			for (Iterator userIter = list.iterator(); userIter.hasNext();)
			{
				User user = (User) userIter.next();
				String email = user.getEmail();
		
				if ((email != null) && (email.length() > 0))
				{
					users.add(user);
				}
			}
		}
		List userentries = inJob.getUserEntries();
		for (Iterator iterator = userentries.iterator(); iterator.hasNext();)
		{
			JobUserEntry entry = (JobUserEntry) iterator.next();
			users.add(entry.getUser());
		}
		List sorted = new ArrayList(users);
		Collections.sort(sorted);
		return sorted;
	}
	
	
	public PostMail getPostMail() {
		return fieldPostMail;
	}

	public void setPostMail(PostMail inPostMail) {
		fieldPostMail = inPostMail;
	}

	public String getTemplate() {
		return fieldTemplate;
	}

	public void setTemplate(String inTemplate) {
		fieldTemplate = inTemplate;
	}

	public UserManager getUserManager() {
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager) {
		fieldUserManager = inUserManager;
	}

	public PageManager getPageManager() {
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager) {
		fieldPageManager = inPageManager;
	}

}
