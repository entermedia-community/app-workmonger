package org.openedit.tracker;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.modules.BaseModule;

public abstract class BaseJobTrackingModule extends BaseModule {

	protected JobTrackerMap fieldJobTrackers;

	public BaseJobTrackingModule() {
		super();
	}

	public JobTrackerMap getJobTrackers() {
		return fieldJobTrackers;
	}

	public void setJobTrackers(JobTrackerMap inJobTrackers) {
		fieldJobTrackers = inJobTrackers;
	}

	public JobArchive getJobArchive(WebPageRequest inReq) throws OpenEditException {
		JobTracker jobtracker = getJobTracker(inReq);
		JobArchive jobArchive = jobtracker.getJobArchive();
		return jobArchive;
	}

	public JobTracker getJobTracker(WebPageRequest inReq) {
		String catalogId = inReq.findValue("catalogid");
		JobTracker jobs = (JobTracker)getJobTrackers().get(catalogId);
		if( jobs == null)
		{
			jobs = (JobTracker)getBeanFactory().getBean("jobTracker");
			//Set the catalog id on all the children of jobs
			jobs.setCatalogId(catalogId);
			getJobTrackers().put( catalogId, jobs);
		}
		inReq.putPageValue("jobtracker", jobs);
		String rootFolder = inReq.findValue("rootfolder");
		if(rootFolder != null){
			inReq.putPageValue("cataloghome", "/"+ rootFolder );
		} else{
			inReq.putPageValue("cataloghome", "/"+ jobs.getCatalogId() );
		}
		
		inReq.putPageValue("applicationid", inReq.findValue("applicationid") );
		inReq.putPageValue("catalogid", jobs.getCatalogId() );
		inReq.putPageValue("jobtracker", jobs);
		
		return jobs;
	}

	

	public BaseJobArchive loadJobArchive(WebPageRequest inReq) throws Exception {
			// TODO Auto-generated method stub
			JobTracker jobtracker = getJobTracker(inReq);
			BaseJobArchive jobs = jobtracker.getJobArchive();
			inReq.putPageValue("jobarchive", jobs);
			return jobs;
	//		List types = jobs.getDataProperties("jobsearch", inReq.getUser());
	//		inReq.putPageValue("jobfields", types);
		}

	public Job loadJob(WebPageRequest inReq) throws Exception {
		JobTracker jobtracker = getJobTracker(inReq);
		String id = inReq.findValue("jobid");
//		if( id == null)
//		{
//			String path = inReq.getPath();
//			if (path.endsWith(".html"))
//			{
//				id = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf(".html"));
//			}			
//		}
		//HitTracker jobList = getJobList( inRequest);
			Job job = jobtracker.getJobArchive().getJobById(id);
		if( job == null)
		{
			job = (Job)inReq.getSessionValue("tmpjob");
			if( job != null && !job.getId().equals(id))
			{
				job = null;
			}
		}
		inReq.putPageValue("job", job);
		return job;
	}

	public Todo loadTodo(WebPageRequest inReq) throws Exception {
		JobTracker jobtracker = getJobTracker(inReq);
		//strip off the Todo id
		String did = inReq.getRequestParameter("todoid");
		if( did == null)
		{
			String path = inReq.getPath();
			if (path.endsWith(".html"))
			{
				did = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf(".html"));
				
			}			
		}
		String jid = inReq.findValue("jobid");
		if( jid == null)
		{
			String path = inReq.getPath();
			if (path.endsWith(".html"))
			{
				path = path.substring(0,path.lastIndexOf("/"));
				jid = path.substring(path.lastIndexOf("/")+1, path.length());
			}			
		}
		
		//HitTracker jobList = getJobList( inRequest);
		Job job = jobtracker.getJobArchive().getJobById(jid);
		if( job != null)
		{
			Todo todo =job.getTodo(did);
			inReq.putPageValue("job", job);
			inReq.putPageValue("todo", todo);
			return todo;
		}
		return null;
	}

}