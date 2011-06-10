package org.openedit.tracker;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.openedit.users.User;

public class JobTimeEntry
{
	protected String fieldId;
	
	protected double fieldHours;
	protected User fieldUser;
	
	protected String fieldDescription;
	protected Date fieldDate;
	public double getHours()
	{
		return fieldHours;
	}
	public void setHours(double inHours)
	{
		fieldHours = inHours;
	}
	public User getUser()
	{
		return fieldUser;
	}
	public void setUser(User inUser)
	{
		fieldUser = inUser;
	}
	public String getDescription()
	{
		return fieldDescription;
	}
	public void setDescription(String inDescription)
	{
		fieldDescription = inDescription;
	}
	public Date getDate()
	{
		return fieldDate;
	}
	public void setDate(Date inDate)
	{
		fieldDate = inDate;
	}
	public String getId()
	{
		return fieldId;
	}
	public void setId(String inId)
	{
		fieldId = inId;
	}
	
	public String getFormatedDate()
	{
		if( getDate() == null)
		{
			return "";
		}
		return SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT,SimpleDateFormat.SHORT).format(getDate());
	}
	
	
}
