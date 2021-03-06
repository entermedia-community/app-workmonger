/* Generated by Together */

package org.openedit.tracker;

import java.io.OutputStream;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;

import com.openedit.OpenEditException;
import com.openedit.page.Page;
import com.openedit.users.Group;
import com.openedit.users.User;

public class XmlJobArchive extends BaseJobArchive {

	private static final Log log = LogFactory.getLog(XmlJobArchive.class);
	
	public void save(Job inJob, User inUser) throws OpenEditException 
	{	
			PropertyDetails delivfields = getPropertyDetailsArchive().getPropertyDetails("todo");
			PropertyDetails  jobfields = getPropertyDetailsArchive().getPropertyDetails("job");
	//		if( !inUser.hasPermission("archive.jobediting") )
	//		{
	//			throw new OpenEditException("No permission to save");
	//		}
			if( inJob.getId() == null)
			{
				inJob.setId(String.valueOf(getIdCounter().incrementCount()));
			}
			Element root = DocumentHelper.createElement("job");
			root.addAttribute("id", inJob.getId());
			root.addAttribute("name", inJob.getName());
			root.addAttribute("createdby", inUser.getUserName());
			//priority as an atribute
			root.addAttribute("priority", String.valueOf(inJob.getPriority()));
			//
			for (Iterator iterator = jobfields.getDetails().iterator(); iterator.hasNext();)
			{
				
				PropertyDetail	detail = (PropertyDetail)iterator.next();
				String dname = detail.getId();
				if( dname.equals("priority"))
				{
					continue;
				}
				String value = inJob.get(detail.getId());
				if( value != null)
				{
					Element child = root.addElement("property");
					child.addAttribute("id", detail.getId());
					if (detail.isDataType("boolean"))
					{
						child.setText(value);
					} else if (detail.isDate())
					{
	//						Date date = detail.getDateFormat().parse(value);
	//						data.setValue(date);
						child.setText(value);
	
					} else if (detail.isDataType("number"))
					{
	
	//						Integer intval = new Integer(value);
	//						data.setValue(intval);
						child.setText(value);
					} else if (detail.isDataType("double"))
					{
	//						Double dval = new Double(value);
	//						data.setValue(dval);
						child.setText(value);
					} else
					{
						child.setText(value);
					}
				}
			}
			for (Iterator iter = inJob.getGroups().iterator(); iter.hasNext();) {
				Group	group = (Group) iter.next();
				Element elem = root.addElement("group");
				elem.addAttribute("id", group.getId());
			}
			for (Iterator iter = inJob.getUserEntries().iterator(); iter.hasNext();) {
				JobUserEntry user = (JobUserEntry) iter.next();
				Element elem = root.addElement("user");
				elem.addAttribute("id", user.getUser().getUserName());
				if( user.isEditor())
				{
					elem.addAttribute("editor", "true");
				}
			}
			for (Iterator iterator = inJob.getTodos().iterator(); iterator.hasNext();)
			{
				Todo jobdeliv = (Todo) iterator.next();
				jobdeliv.setJobTimeEntryArchive(getJobTimeArchive());
				Element deliv = root.addElement("todo");
				//Element deliv = DocumentHelper.createElement("todo");
				if( jobdeliv.getId() == null)
				{
					jobdeliv.setId(String.valueOf(getIdCounter().incrementCount()));
				}
				deliv.addAttribute("id",jobdeliv.getId());
				deliv.addAttribute("rank",String.valueOf(jobdeliv.getRank()));
				deliv.addAttribute("createdby",inUser.getUserName());
				for (Iterator delibfields = delivfields.getDetails().iterator(); delibfields.hasNext();)
				{
					PropertyDetail	detail = (PropertyDetail)delibfields.next();
					String value = (String)jobdeliv.getProperty(detail.getId());
					if( value != null)
					{
						Element child = deliv.addElement("property");
						child.addAttribute("id", detail.getId());
						child.setText(value);
					}
				}
				for (Iterator siterator = jobdeliv.getStatusChanges().iterator(); siterator.hasNext();)
				{
					StatusChange change= (StatusChange)siterator.next();
					if( change.getId() == null)
					{
						change.setId(String.valueOf(getIdCounter().incrementCount()));
					}
					
					Element dstatus = deliv.addElement("todostatus");
					dstatus.addAttribute("id",change.getId());
					dstatus.addAttribute("type",change.getType());
					dstatus.addAttribute("attachment",change.getAttachmentPath());
					if( change.getUser() != null)
					{
						dstatus.addAttribute("user",change.getUser().getUserName());
					}
					if( change.getNote() != null)
					{
						String fixed = change.getNote();
						dstatus.addCDATA(fixed);
					}
					dstatus.addAttribute("date",fieldDateFormat.format(change.getDated()));
				}
				//Page todoFile = getPageManager().getPage("/" + getCatalogId() + "/postings/" + inJob.getId() + "/todos/" + jobdeliv.getId() +"/data.xml");
				//OutputStream out = getPageManager().saveToStream(todoFile);
				//getXmlUtil().saveXml(root,out,todoFile.getCharacterEncoding());
			}
			Page userJob = getPageManager().getPage("/WEB-INF/data/" + getCatalogId() + "/jobs/" + inJob.getId() + "/" + inJob.getId() +"data.xml");
			
			OutputStream out = getPageManager().saveToStream(userJob);
			getXmlUtil().saveXml(root,out,userJob.getCharacterEncoding());
			getCache().put(inJob.getId(), inJob);
			fireJobChangeEvent(inJob);
		}
/*
	private void populateJobs(Category category, List all)
			throws OpenEditException {
				if( category == null)
				{
					return;
				}
				for (Iterator iterator = category.getChildren().iterator(); iterator.hasNext();)
				{
					Category name = (Category) iterator.next();
					Job job = getJobById(name.getId());
					if( job != null)
					{
						all.add(job);
					}
				}
			}
*/
	public Job getJobById(String inJobNumber) throws OpenEditException {
		return getJobById(inJobNumber, true);
	}

	public Job getJobById(String inJobNumber, boolean inCache)
			throws OpenEditException {
				Job njob = (Job)getCache().get(inJobNumber);
				if( njob == null)
				{
					njob = loadJob(inJobNumber);
					if( njob != null)
					{
						if( njob.isExist() && inCache)
						{
							getCache().put( inJobNumber, njob);
						}
					}
				}
				return njob;
			}

	public Job loadJob(String inJobNumber) throws OpenEditException {
		
		Page page = getPageManager().getPage("/WEB-INF/data/" + getCatalogId() + "/jobs/" + inJobNumber + "/" + inJobNumber +"data.xml");

		if( !page.exists())
		{
			page = getPageManager().getPage( getJobsDataPath() + inJobNumber + ".xml");
		}
		
		getPageManager().clearCache(page);
		if( !page.exists() )
		{
			return null;
		}
		Job njob = new Job();
		njob.setId(inJobNumber);
		njob.setExist(page.exists());
	
		Element root = null;
		try
		{
			root = getXmlUtil().getXml(page.getReader(), "UTF-8");
		}
		catch ( Exception ex)
		{
			throw new OpenEditException("Could not load job " + inJobNumber,ex);
		}
		njob.setName(root.attributeValue("name"));
		njob.setCreatedByUser(root.attributeValue("createdby"));
		String pri = root.attributeValue("priority");
		if( pri != null)
		{
			njob.setPriority(Integer.parseInt(pri));
		}
		
		//DateFormat formater = new SimpleDateFormat("MM/dd/yyyy");//DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
		
		for (Iterator iterator = root.elementIterator("property"); iterator.hasNext();)
		{
			Element prop = (Element)iterator.next();
			String id = prop.attributeValue("id");
			if( id == null)
			{
				log.error("Data error: " + inJobNumber);
			}
			else
			{
				njob.addProperty(id, prop.getText());
			}
		}
		for (Iterator iter = root.elementIterator("group"); iter.hasNext();) {
			Element element = (Element) iter.next();
			Group group = getUserManager().getGroup(element.attributeValue("id"));
			njob.addGroup(group);
		}
		for (Iterator iter = root.elementIterator("user"); iter.hasNext();) {
			Element element = (Element) iter.next();
			User user = getUserManager().getUser(element.attributeValue("id"));
			JobUserEntry entry = new JobUserEntry();
			entry.setUser(user);
			String type = element.attributeValue("editor");
			entry.setEditor(Boolean.parseBoolean( type) );				
			njob.addUserEntry(entry);
		}
		

		//This is deprecated. We should only load this as needed
		try
		{
			//deliverable
			for (Iterator iterator = root.elementIterator("todo"); iterator.hasNext();)
			{
				Element echange = (Element)iterator.next();
				Todo task =  new Todo();
				task.setJobTimeEntryArchive(getJobTimeArchive());
				task.setId(echange.attributeValue("id"));
				task.setName(echange.attributeValue("name") );
				task.setCreatedByUser(echange.attributeValue("createdby"));
				String rank = echange.attributeValue("rank");
				if( rank != null)
				{
					try {
					task.setRank(Integer.parseInt(rank));
					} catch (NumberFormatException e) {
						task.setRank(0);
					}
				}				
				try {
					task.setCreatedByUser(echange.attributeValue("createdby"));
					} catch (NumberFormatException e) {
						task.setCreatedByUser("0");
					}
				
				for (Iterator delpropiterator= echange.elementIterator("property"); delpropiterator.hasNext();)
				{
					Element prop = (Element)delpropiterator.next();
					task.setProperty(prop.attributeValue("id"), prop.getText());
				}
				//deliverablestatus
				//TODO: Move to another file location
				for (Iterator siterator = echange.elementIterator("todostatus"); siterator.hasNext();)
				{
					Element dstatus = (Element)siterator.next();
					StatusChange change =  new StatusChange();
					change.setId(dstatus.attributeValue("id"));
					change.setType(dstatus.attributeValue("type"));
					change.setAttachmentPath(dstatus.attributeValue("attachment"));
					change.setNote(dstatus.getText());
					change.setUser(getUserManager().getUser(  dstatus.attributeValue("user") ) );
					change.setDated(fieldDateFormat.parse(dstatus.attributeValue("date")));
					task.addStatusChange(change);
				}
				njob.addTodo(task);
			}
			return njob;
		}
		catch ( Exception ex)
		{
			throw new OpenEditException(ex.getMessage());
		}
	}
	public int createId()
	{
		int count = getIdCounter().incrementCount();
		return count;
	}
}
