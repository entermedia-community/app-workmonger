package org.openedit.tracker;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.lucene.BaseLuceneSearcher;
import org.openedit.data.lucene.LuceneHitTracker;
import org.openedit.data.lucene.LuceneSearchQuery;

import com.openedit.OpenEditException;
import com.openedit.OpenEditRuntimeException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.manage.PageManager;
import com.openedit.users.Group;
import com.openedit.users.User;
import com.openedit.util.FileUtils;
import com.openedit.util.PathUtilities;

public class TodoSearcher extends BaseLuceneSearcher
{
	//this has a Job Lucene Search and Lucene has an Archive
	private static final Log log = LogFactory.getLog(TodoSearcher.class);
	protected BaseJobArchive fieldJobArchive;
	protected PageManager fieldPageManager; 

	public TodoSearcher()
	{
		// TODO Auto-generated constructor stub
	}
	
	public void updateIndex(Job inJob) throws OpenEditException
	{
		try
		{
			//TODO: If job is closed then remove from TODO list
			PropertyDetails details = getPropertyDetailsArchive().getPropertyDetails("job");
			PropertyDetails delivdetails = getPropertyDetailsArchive().getPropertyDetails("todo");

			for (Iterator iter = inJob.getTodos().iterator(); iter.hasNext();) {
				Document doc = new Document();
				Todo deliv = (Todo) iter.next();
				updateIndex(inJob, deliv, doc, details,delivdetails);
				Term term = new Term("id", deliv.getId().toLowerCase());
				getIndexWriter().updateDocument(term,doc,getAnalyzer());
			}
			clearIndex();
		}
		catch ( Exception ex)
		{
			throw new OpenEditException(ex);
		}
	}
	public void updateIndex(Job inJob, Todo inTodo) throws OpenEditException
	{
		try
		{
			PropertyDetails details = getPropertyDetailsArchive().getPropertyDetails("job");
			PropertyDetails delivdetails = getPropertyDetailsArchive().getPropertyDetails("todo");

			Document doc = new Document();
			updateIndex(inJob, inTodo, doc, details,delivdetails);
			Term term = new Term("id", inTodo.getId().toLowerCase());
			getIndexWriter().updateDocument(term,doc,getAnalyzer());
			
			flush();
		}
		catch ( Exception ex)
		{
			throw new OpenEditException(ex);
		}
	}

	
	
	public void reIndexAll(IndexWriter writer)
	{
		List list = getPageManager().getRepository().getChildrenNames(getJobArchive().getJobsDataPath());

		//http://www.onjava.com/pub/a/onjava/2003/03/05/lucene.html
		//http://www.onjava.com/pub/a/onjava/2003/03/05/lucene.html?page=2
		//writer.mergeFactor = 10;
		writer.setMergeFactor(100);
		try
		{
			 PropertyDetails details = getPropertyDetailsArchive().getPropertyDetails("job");
			 PropertyDetails delivdetails = getPropertyDetailsArchive().getPropertyDetails("todo");
			for (Iterator iter = list.iterator(); iter.hasNext();)
			{
				String path = (String) iter.next();
				String id = PathUtilities.extractPageName(path);
				Job job = getJobArchive().getJobById(id);
				
				if (job != null )
				{
					for (Iterator jobiter = job.getTodos().iterator(); jobiter.hasNext();) {
						Document doc = new Document();
						Todo deliv = (Todo) jobiter.next();
						updateIndex(job, deliv, doc, details,delivdetails);
						Term term = new Term("id", deliv.getId().toLowerCase());
						writer.updateDocument(term,doc,getAnalyzer());
					}
				}
				else
				{
					log.info("Error loading job:" + id);
				}
			}
			writer.optimize();

		}
		catch( Exception ex)
		{
			throw new OpenEditException(ex);
		}
	}
	protected void updateIndex(Job job, Todo inTodo, Document doc, PropertyDetails inJobDetails, PropertyDetails inTodoDetail) throws CorruptIndexException, IOException
	{
		doc.add(new Field("id", inTodo.getId(), Field.Store.YES, Field.Index.NO_NORMS));

		
		String permissions = job.listPermissions();
		doc.add(new Field("todopermissions", permissions, Field.Store.NO, Field.Index.TOKENIZED));

		if( inTodo.getName() != null)
		{
			doc.add(new Field("name", inTodo.getName(), Field.Store.YES, Field.Index.NO_NORMS));
		}
		StringBuffer keywords = new StringBuffer();
		keywords.append(inTodo.getName() );
		keywords.append(" ");
		keywords.append(job.getName() );
		keywords.append(" ");
		keywords.append(inTodo.getCreatedByUser() );
		keywords.append(" ");
		keywords.append(inTodo.getId() );
		
		doc.add(new Field("description",keywords.toString(), Field.Store.NO, Field.Index.TOKENIZED));

		List detailfields = inTodoDetail.findIndexProperties();
		StatusChange laststatus = inTodo.getLastStatus();
		StatusChange firststatus = inTodo.getFirstStatus();
		for (Iterator iterator = detailfields.iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			if( detail.getId().equals("hours") )
			{
				double hours = inTodo.getHours();
				doc.add(new Field("hours", String.valueOf(hours), Field.Store.YES, Field.Index.NO_NORMS));
			}
			else if( laststatus != null && detail.getId().equals("todolaststatus") && laststatus.getType() != null )
			{
					doc.add(new Field("todolaststatus", laststatus.getType(), Field.Store.YES, Field.Index.NO_NORMS));
			}
			else if( laststatus != null && detail.getId().equals("todolaststatussummary"))
			{
				String summary = laststatus.getNoteSnip(500);
				if(summary != null)
				{
					doc.add(new Field("todolaststatussummary", summary, Field.Store.YES, Field.Index.TOKENIZED));
				}
			}
			else if(firststatus != null && detail.getId().equals("todofirststatussummary"))
			{
				String firstsummary = firststatus.getNoteSnip(70);
				if(firstsummary != null)
				{
					doc.add(new Field("todofirststatussummary", firstsummary, Field.Store.YES, Field.Index.TOKENIZED));
				}
			}
			
			else if( laststatus != null && detail.isDate() && detail.getId().equals("todolaststatusdate") )
			{
				try
				{
					String val = DateTools.dateToString(laststatus.getDated(), Resolution.MINUTE);
					doc.add(new Field(detail.getId(), val, Field.Store.YES,	Field.Index.NO_NORMS));
				}
				catch (Exception ex)
				{
					throw new OpenEditRuntimeException(ex);
				}
			} 
			else if( laststatus != null && laststatus.getUser() != null &&  detail.getId().equals("todolaststatususer") )
			{
				doc.add(new Field("todolaststatususer", laststatus.getUser().getUserName(), Field.Store.YES, Field.Index.NO_NORMS));
			}
			else if( detail.getId().equals("jobhidden"))
			{
				String isHidden = job.get("hidden");
				if(isHidden == null)
				{
					isHidden = "false";
				}
				doc.add(new Field("jobhidden", isHidden,  Field.Store.YES, Field.Index.NO_NORMS));
			}
			else if( detail.getId().equals("statuscount"))
			{
				int count = inTodo.getStatusChanges().size();
				doc.add(new Field("statuscount", String.valueOf(count), Field.Store.YES, Field.Index.NO_NORMS));
			}
			else if( detail.getId().equals("rank"))
			{
				int rank = inTodo.getRank() + (job.getPriority() * 1000 );
				String val  = String.valueOf(rank);
				String all = "0000000000" + val;
				val = all.substring(all.length() - 10); // 10 is the max width
				doc.add(new Field(detail.getId(), val, Field.Store.YES,	Field.Index.UN_TOKENIZED));
			}
			else if( detail.isIndex())
			{
				String val = null;
				if( detail.getId().startsWith("job"))
				{
					String id = detail.getId().substring(3);
					val = job.get(id);
				}
				else
				{
					val  = inTodo.get(detail.getId());
				}
				if( val != null)
				{
					getLuceneIndexer().updateIndex(inTodo,val, doc, keywords, detail);
				}
			}
		}
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

	public HitTracker findOpenDeliverables(User inUser, String inSort) throws Exception
	{
		if( inSort == null)
		{
			inSort = "id";
		}
		HitTracker hits = search("deliverablestatus:open",inSort);
		return hits;
	}
	
	public HitTracker fieldSearch(WebPageRequest inReq) throws OpenEditException
	{
		SearchQuery search = addStandardSearchTerms(inReq);
		String jobid = inReq.getRequestParameter("jobid");
		
		if( search == null && jobid == null )
		{
			return null; //Noop
		}
		if( jobid != null)
		{
			if(search == null){
				search = createSearchQuery();
			}
			search.addMatches("jobid",jobid);
		}
		inReq.putPageValue("searcher", this);
		return search(inReq, search);

	}
	public HitTracker loadTodosForJob(WebPageRequest inReq) throws OpenEditException
	{
		SearchQuery search = createSearchQuery();
		String jobid = inReq.getRequestParameter("jobid");
		if(jobid == null){
			return null;
		}
		search.addMatches("jobid",jobid);
		inReq.putPageValue("searcher", this);
		return search(inReq, search);

	}
	public HitTracker fieldSearchForUser(WebPageRequest inReq, User inUser) throws OpenEditException
	{
		SearchQuery search = addStandardSearchTerms(inReq);
		if( search == null)
		{
			search = new LuceneSearchQuery();
		}
		return searchByUser(inReq, search);

	}

	public HitTracker searchByUser(WebPageRequest inReq, SearchQuery search)
	{
		String jobid = inReq.getRequestParameter("jobid");
		if( jobid != null)
		{
			search.addMatches("jobid",jobid);
			search.putInput("jobid", jobid);
		}
		addActionFilters(inReq, search);
		//search.addMatches("customer", inUser.getUserName(), "For user " + inUser.getShortDescription());
		StringBuffer permissions  = new StringBuffer("true ");
		if( inReq.getUser() != null)
		{
			permissions.append("user" + inReq.getUserName());
			permissions.append(" ");		
			for (Iterator iterator = inReq.getUser().getGroups().iterator(); iterator.hasNext();)
			{
				Group group = (Group) iterator.next();
				permissions.append(group.getId());
				permissions.append(" ");
			}
		}
		search.addOrsGroup("todopermissions", permissions.toString());
		//search.setSortBy("jobassignedto");
		//search.addSortBy("rankUp");
		//search.addSortBy("todolaststatusdateDown");
		return search(inReq, search);
	}
	
	public void changeSort(WebPageRequest inReq, JobTracker tracker) throws OpenEditException
	{
		String sort = inReq.getRequestParameter("sortby");
		String hitsname = inReq.findValue("hitsname");
		changeSort(inReq, sort, hitsname);
	}
	
	public HitTracker getAllHits(WebPageRequest inReq) 
	{
		//TODO: return all todos?
		return new LuceneHitTracker();
	}

	
}
