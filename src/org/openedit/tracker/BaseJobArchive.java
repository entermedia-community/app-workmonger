/*
 * Created on Jul 2, 2006
 */
package org.openedit.tracker;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.openedit.data.PropertyDetailsArchive;

import com.openedit.ModuleManager;
import com.openedit.OpenEditException;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.users.UserManager;
import com.openedit.util.IntCounter;
import com.openedit.util.XmlUtil;

public abstract class BaseJobArchive implements JobArchive 
{
	protected PageManager fieldPageManager;
	protected ModuleManager fieldModuleManager;
	protected UserManager fieldUserManager;
	protected PropertyDetailsArchive fieldPropertyDetailsArchive;
	protected JobSearcher fieldJobSearcher;
	protected Map fieldCache;
	protected XmlUtil fieldXmlUtil;
	protected String fieldCatalogId;
	protected BaseJobProjectNumberCreator fieldJobIdCounter;
	protected IntCounter fieldIdCounter;
	protected File fieldRoot;
	protected JobTimeArchive fieldJobTimeArchive;
    private List fieldJobChangeListeners;
	
	
	protected DateFormat fieldDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    /**
     *@link aggregation
     *      @associates org.openedit.tracker.jobtracking.JobChangeListener
     */

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}


	/* (non-Javadoc)
	 * @see org.openedit.tracker.jobtracking.JobArchive#createJob(java.lang.String)
	 */
	public Job createJob(String inJobNumber) throws OpenEditException
	{
		Job njob = new Job();
		njob.setId(inJobNumber);
		return njob;
		
	}
	public String getJobsDataPath()
	{
		// TODO Auto-generated method stub
		return "/WEB-INF/data/" + getCatalogId() + "/jobs/";
	}

	protected Map getCache()
	{
		if (fieldCache == null)
		{
			fieldCache = new HashMap();
		}
		return fieldCache;
	}
	public JobSearcher getJobSearcher()
	{
		return fieldJobSearcher;
	}

	public void setJobSearcher(JobSearcher inJobSearch)
	{
		fieldJobSearcher = inJobSearch;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public XmlUtil getXmlUtil()
	{
		if (fieldXmlUtil == null)
		{
			fieldXmlUtil = new XmlUtil();
		}
		return fieldXmlUtil;
	}

	public void setXmlUtil(XmlUtil inXmlUtil)
	{
		fieldXmlUtil = inXmlUtil;
	}

	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}
	public List getDataProperties(String inScreenName, User inUser) throws Exception
	{
		return getPropertyDetailsArchive().getDataProperties("job", inScreenName, inUser);
	}

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
		getJobTimeArchive().setCatalogId(inCatalogId);
	}

	public PropertyDetailsArchive getPropertyDetailsArchive()
	{
		return getJobSearcher().getPropertyDetailsArchive();
	}

	public void setPropertyDetailsArchive(PropertyDetailsArchive inPropertyDetailsArchive)
	{
		//fieldPropertyDetailsArchive = inPropertyDetailsArchive;
		getJobSearcher().setPropertyDetailsArchive(inPropertyDetailsArchive);
	}
	public String nextProjectNumber(TimeZone zone) throws OpenEditException
	{
		return getJobIdCreator().createNextProjectNumber(zone);
	}
	public String nextId() throws OpenEditException
	{
		return String.valueOf(getIdCounter().incrementCount());
	}
	
	protected BaseJobProjectNumberCreator getJobIdCreator()
	{
		if( fieldJobIdCounter == null)
		{
			fieldJobIdCounter = new BaseJobProjectNumberCreator();
			File upload = new File( getRoot(), "/WEB-INF/data/" + getCatalogId() + "/jobs/jobid.properties");
			upload.getParentFile().mkdirs();
			fieldJobIdCounter.setCounterFile(upload);
		}
		return fieldJobIdCounter;
	}
	public IntCounter getIdCounter()
	{
		if( fieldIdCounter == null)
		{
			fieldIdCounter = new IntCounter();
			File upload = new File( getRoot(), "/WEB-INF/data/" + getCatalogId() + "/jobs/id.properties");
			upload.getParentFile().mkdirs();
			fieldIdCounter.setCounterFile(upload);
		}
		return fieldIdCounter;
	}

	public File getRoot()
	{
		return fieldRoot;
	}

	public void setRoot(File inRoot)
	{
		fieldRoot = inRoot;
	}

	/* (non-Javadoc)
	 * @see org.openedit.tracker.jobtracking.JobArchive#clear()
	 */
	public void clear()
	{
		getCache().clear();
		
	}

	public JobTimeArchive getJobTimeArchive()
	{
		return fieldJobTimeArchive;
	}

	public void setJobTimeArchive(JobTimeArchive inJobTimeArchive)
	{
		fieldJobTimeArchive = inJobTimeArchive;
	}

	public List getJobChangeListeners() {
		if (fieldJobChangeListeners == null) {
			fieldJobChangeListeners = new ArrayList();
			
		}
		return fieldJobChangeListeners;
	}

	public void setJobChangeListeners(List jobChangeListeners) {
		this.fieldJobChangeListeners = jobChangeListeners;
	}
	
	public void addJobChangeListener(JobChangeListener listener){
		getJobChangeListeners().add(listener);
	}
	
	public void removeJobChangeListener(JobChangeListener listener){
		getJobChangeListeners().remove(listener);
	}
	
	protected void fireJobChangeEvent(Job job){
	for (Iterator iterator = getJobChangeListeners().iterator(); iterator.hasNext();) {
		JobChangeListener listener = (JobChangeListener) iterator.next();
		listener.jobChanged(job);
		
	}	
	}
	
	
}