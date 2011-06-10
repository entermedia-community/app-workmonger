package org.openedit.tracker;

import com.openedit.OpenEditException;
import com.openedit.users.User;

public interface JobArchive {

	public Job getJobById(String inJobNumber) throws OpenEditException;

	public Job getJobById(String inJobNumber, boolean inCache)
			throws OpenEditException;

	public Job createJob(String inJobNumber) throws OpenEditException;

	public void clear();
	public void save(Job inJob, User inUser) throws OpenEditException ;
	public Job loadJob(String inJobNumber) throws OpenEditException;

	int createId();

}