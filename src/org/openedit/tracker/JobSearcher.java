package org.openedit.tracker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.lucene.BaseLuceneSearcher;
import org.openedit.data.lucene.LuceneSearchQuery;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.manage.PageManager;
import com.openedit.users.Group;
import com.openedit.users.User;
import com.openedit.util.FileUtils;
import com.openedit.util.PathUtilities;

public class JobSearcher extends BaseLuceneSearcher
{
	private static final Log log = LogFactory.getLog(JobSearcher.class);
	protected BaseJobArchive fieldJobArchive;
	protected PageManager fieldPageManager;

	public void updateIndex(Job inJob) throws OpenEditException
	{
		try
		{
			Document doc = new Document();
			PropertyDetails details = getPropertyDetailsArchive().getPropertyDetails("job");
			updateIndex(inJob, doc, details);
			Term term = new Term("id", inJob.getId().toLowerCase());
			getIndexWriter().updateDocument(term,doc,getAnalyzer());
			flush();
			clearIndex();
		}
		catch ( Exception ex)
		{
			throw new OpenEditException(ex);
		}
	}
	
	
	protected void updateIndex(Job job, Document doc, PropertyDetails inJobDetails) throws CorruptIndexException, IOException
	{
		doc.add(new Field("id", job.getId(), Field.Store.YES, Field.Index.NO_NORMS));
		if( job.getName() != null)
		{
			doc.add(new Field("name", job.getName(), Field.Store.YES, Field.Index.TOKENIZED));
			doc.add(new Field("namesorted", job.getName(), Field.Store.NO, Field.Index.NO_NORMS));
		}

		StringBuffer keywords = new StringBuffer();
		keywords.append(job.getId());
		keywords.append(" ");
		keywords.append(job.getName());
		List details = inJobDetails.findIndexProperties();
		for (Iterator iterator = details.iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			if( detail.getId().equals("id") ||  detail.getId().equals("name") || detail.getId().equals("deliverablestatus") || detail.getId().equals("jobhidden"))
			{
				continue;
			}
			String value = job.get(detail.getId());
			if( value != null)
			{
				if( detail.isStored())
				{
					doc.add( new Field(detail.getId(),value, Field.Store.YES, Field.Index.NO_NORMS));
				}
				else
				{
					doc.add( new Field(detail.getId(),value, Field.Store.NO, Field.Index.NO_NORMS));
				}
				if( detail.isKeyword())
				{
					keywords.append(" ");
					keywords.append(value);
				}
			}
		}
		doc.add( new Field("description",keywords.toString(), Field.Store.NO, Field.Index.TOKENIZED));
		boolean hidden = Boolean.parseBoolean( job.get("hidden") );
		doc.add( new Field("jobhidden",String.valueOf( hidden ), Field.Store.NO, Field.Index.NO_NORMS));
		
		String permissions = job.listPermissions();
		doc.add(new Field("permissions", permissions, Field.Store.NO, Field.Index.TOKENIZED));
		super.updateIndex(job, doc, inJobDetails);
	}
	
	public HitTracker fieldSearch(WebPageRequest inReq) throws OpenEditException
	{
		SearchQuery search = addStandardSearchTerms(inReq);
		if( search == null)
		{
			return null; //Noop
		}
		addActionFilters(inReq, search);
		inReq.putPageValue("searcher", this);
		return search(inReq, search);
	}

	public void changeSort(WebPageRequest inReq, JobTracker tracker) throws OpenEditException
	{
		String sort = inReq.getRequestParameter("sortby");
		String hitsname = inReq.findValue("hitsname");
		inReq.putPageValue("searcher", this);
		changeSort(inReq, sort, hitsname);
	}

	public synchronized void reIndexAll(IndexWriter writer)
	{
		List inPaths = getPageManager().getRepository().getChildrenNames(getJobArchive().getJobsDataPath());
		//http://www.onjava.com/pub/a/onjava/2003/03/05/lucene.html
		//http://www.onjava.com/pub/a/onjava/2003/03/05/lucene.html?page=2
		//writer.mergeFactor = 10;
		writer.setMergeFactor(100);
		try
		{
			 PropertyDetails details = getPropertyDetailsArchive().getPropertyDetails("job");
			// PropertyDetails delivdetails = getFieldArchive().getPropertyDetails("jobdeliverable");
			for (Iterator iter = inPaths.iterator(); iter.hasNext();)
			{
				String path = (String) iter.next();
				String id = PathUtilities.extractPageName(path);
				Job job = getJobArchive().getJobById(id);
					
				if (job != null )
				{
					Document doc = new Document();
					try
					{
						updateIndex(job, doc, details);
						writer.addDocument(doc);
					}
					catch(Exception ex)
					{
						log.error(ex);
					}

				}
				else
				{
					log.info("Error loading product:" + id);
				}
			}
			writer.optimize();
		}
		catch( Exception ex)
		{
			throw new OpenEditException(ex);
		}
		getJobArchive().clear();
	}
	
	

	public BaseJobArchive getJobArchive()
	{
		return fieldJobArchive;
	}

	public void setJobArchive(BaseJobArchive inJobArchive)
	{
		fieldJobArchive = inJobArchive;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public HitTracker findClosedJobs(User inUser) throws Exception
	{
		//TODO: Make this use user permissions
//		if( inSort == null)
//		{
//			inSort = "name";
//		}
		SearchQuery search = createSearchQuery();

		StringBuffer permissions  = new StringBuffer("true ");
		permissions.append("user" + inUser.getUserName());
		permissions.append(" ");		
		for (Iterator iterator = inUser.getGroups().iterator(); iterator.hasNext();)
		{
			Group group = (Group) iterator.next();
			permissions.append(group.getId());
			permissions.append(" ");
		}
		search.addMatches("jobopen","false");
	//	search.addNot("assignedto", inUser.getUserName());
		search.addOrsGroup("permissions", permissions.toString());
		List sorts = new ArrayList();
		sorts.add("hiddenDown");
		sorts.add("namesorted");
		
		HitTracker hits = search(search.toQuery(),sorts);
		return hits;
	}

	
	public HitTracker findOpenJobs(User inUser) throws Exception
	{
		//TODO: Make this use user permissions
//		if( inSort == null)
//		{
//			inSort = "name";
//		}
		SearchQuery search = createSearchQuery();

		StringBuffer permissions  = new StringBuffer("true ");
		permissions.append("user" + inUser.getUserName());
		permissions.append(" ");		
		for (Iterator iterator = inUser.getGroups().iterator(); iterator.hasNext();)
		{
			Group group = (Group) iterator.next();
			permissions.append(group.getId());
			permissions.append(" ");
		}
		search.addNot("jobopen","false");
	//	search.addNot("assignedto", inUser.getUserName());
		search.addOrsGroup("permissions", permissions.toString());
		List sorts = new ArrayList();
		sorts.add("hiddenDown");
		sorts.add("namesorted");
		
		HitTracker hits = search(search.toQuery(),sorts);
		return hits;
	}

	public HitTracker findAssignedJobs(User inUser) throws Exception
	{
//		if( inSort == null)
//		{
//			inSort = "name";
//		}
		SearchQuery search = createSearchQuery();

		search.addNot("jobopen","false");
		search.addExact("assignedto", inUser.getUserName());
		List sorts = new ArrayList();
		sorts.add("hiddenDown");
		sorts.add("namesorted");
		HitTracker hits = search(search.toQuery(), sorts);
		return hits;
	}

	
	public HitTracker findAllJobs() throws OpenEditException
	{
		return search("jobopen:true OR jobopen:false");
	}

	public HitTracker getAllHits(WebPageRequest inReq)
	{
		return null;
	}


	public HitTracker findOpenGroupJobs(Group inGroup, String inSort) throws Exception
	{
		//TODO: Make this use user permissions
		if( inSort == null)
		{
			inSort = "name";
		}
		SearchQuery search = new LuceneSearchQuery();

		StringBuffer permissions  = new StringBuffer();
		permissions.append(inGroup.getId());
		//search.addNot("jobopen","false","Not Closed");
		search.addOrsGroup("permissions", permissions.toString());
		HitTracker hits = search(search.toQuery(),inSort);
		return hits;
	}


	public HitTracker findVisibleJobs(User inUser, String inSort)
	{
		
		if( inSort == null)
		{
			inSort = "name";
		}
		SearchQuery search = createSearchQuery();
		search.addMatches("jobopen", "true");
		search.addNot("jobhidden","true");
		if(inUser != null)
		{
			search.addNot("assignedto", inUser.getUserName());
		}
		HitTracker hits = search(search.toQuery(),inSort);
		return hits;
	}

	public HitTracker findPrivateJobs(WebPageRequest inReq)
	{
		
		//TODO: Make this use user permissions
		SearchQuery search = createSearchQuery();

		StringBuffer permissions  = new StringBuffer();
		permissions.append("user" + inReq.getUserName());
		permissions.append(" ");		
		for (Iterator iterator = inReq.getUser().getGroups().iterator(); iterator.hasNext();)
		{
			Group group = (Group) iterator.next();
			permissions.append(group.getId());
			permissions.append(" ");
		}
	//	search.addNot("assignedto", inUser.getUserName());
		search.addExact("createdby",inReq.getUserName());
		search.addOrsGroup("permissions", permissions.toString());

//		SearchQuery or = createSearchQuery();
//		or.addExact("createdby",inReq.getUserName());
//		or.addExact("jobopen","true");
//		or.setAndTogether(false);
		
//		search.addChildQuery(or);
		
		HitTracker hits = cachedSearch(inReq,search);
		return hits;

	}
	
}
