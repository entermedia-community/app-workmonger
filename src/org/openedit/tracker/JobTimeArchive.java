package org.openedit.tracker;

import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openedit.xml.XmlArchive;

import com.openedit.OpenEditException;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.users.UserManager;
import com.openedit.util.XmlUtil;

public class JobTimeArchive
{
	protected XmlArchive fieldXmlArchive;
	protected PageManager fieldPageManager;
	protected XmlUtil fieldXmlUtil;
	protected String catalogId;
	protected DateFormat fieldDateFormat = new SimpleDateFormat("MM/dd/yyyy");
	protected UserManager fieldUserManager;
	public DateFormat getDateFormat()
	{
		return fieldDateFormat;
	}

	public void setDateFormat(DateFormat inDateFormat)
	{
		fieldDateFormat = inDateFormat;
	}



	public XmlArchive getXmlArchive()
	{
		return fieldXmlArchive;
	}

	public void setXmlArchive(XmlArchive inXmlArchive)
	{
		fieldXmlArchive = inXmlArchive;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}
	

	public List getTimeEntries(Todo inDeliverable) throws OpenEditException
	{
		Page userJob = getPageManager().getPage("/WEB-INF/data/" + getCatalogId() + "/jobs/" + inDeliverable.getJobId() + "/todos/" + inDeliverable.getId() + "/time.xml");
		ArrayList timeList = new ArrayList();
		try
		{
			if( userJob.exists() )
			{
				Element root = getXmlUtil().getXml(userJob.getReader(), "UTF-8");
				
				for (Iterator iterator = root.elementIterator("timeentry"); iterator.hasNext();)
				{
					Element timeElement = (Element)iterator.next();
					JobTimeEntry e = new JobTimeEntry();
					e.setId(timeElement.attributeValue("id"));
					e.setDate(getDateFormat().parse(timeElement.attributeValue("date")));
					e.setHours(Double.parseDouble(timeElement.attributeValue("hours")));
					e.setDescription(timeElement.getText());
					String userName = timeElement.attributeValue("user");
					User user = getUserManager().getUser(userName);
					e.setUser(user);
					timeList.add(e);
				
				}
			}
		} catch (NumberFormatException e)
		{
			throw new OpenEditException(e);
		} catch (ParseException e)
		{
			throw new OpenEditException(e);
		}
		return timeList;
	}

	
	
	
	public void save(Todo inDeliverable) throws OpenEditException
	{	
		
		Element root = DocumentHelper.createElement("time");
		
		root.addAttribute("id", inDeliverable.getId());
	
		for (Iterator iterator = inDeliverable.getTimeEntries().iterator(); iterator.hasNext();)
		{
			JobTimeEntry entry = (JobTimeEntry) iterator.next();
			Element elem = root.addElement("timeentry");
			elem.addAttribute("date", getDateFormat().format(entry.getDate()));
			User user = entry.getUser();
			if(user != null){
			  elem.addAttribute("user", entry.getUser().getUserName());
			}
			elem.addAttribute("id", entry.getId());
			elem.addAttribute("hours", String.valueOf(entry.getHours()));
			if( entry.getDescription() != null)
			{
				elem.setText(entry.getDescription());
			}
		}
    	
		Page userJob = getPageManager().getPage("/WEB-INF/data/" + getCatalogId() + "/jobs/" + inDeliverable.getJobId() + "/todos/" + inDeliverable.getId()+ "/time.xml");
		OutputStream out = getPageManager().saveToStream(userJob);
		getXmlUtil().saveXml(root,out,userJob.getCharacterEncoding());
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
	
	
	public String getCatalogId()
	{
		return catalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		catalogId = inCatalogId;
	}

	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}
	
	public Todo createJobDeliverable(){
		Todo d  = new Todo();
		d.setJobTimeEntryArchive(this);
		return d;
	}
}
