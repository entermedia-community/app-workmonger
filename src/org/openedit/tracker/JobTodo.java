package org.openedit.tracker;

import java.text.NumberFormat;
import java.util.Date;

import com.openedit.users.User;

public class JobTodo
{
	protected String fieldId;
	protected String fieldNotes;
	protected User fieldUser;
	protected Date fieldDated;
	protected double fieldHours;
	
	public String getId()
	{
		return fieldId;
	}
	public void setId(String inId)
	{
		fieldId = inId;
	}
	public String getNotes()
	{
		return fieldNotes;
	}
	public void setNotes(String inNotes)
	{
		fieldNotes = inNotes;
	}
	public User getUser()
	{
		return fieldUser;
	}
	public void setUser(User inUser)
	{
		fieldUser = inUser;
	}
	public Date getDated()
	{
		return fieldDated;
	}
	public void setDated(Date inDated)
	{
		fieldDated = inDated;
	}
	public double getHours()
	{
		return fieldHours;
	}
	public void setHours(double inHours)
	{
		fieldHours = inHours;
	}
	public String formatedTime()
	{
		return NumberFormat.getNumberInstance().format(getHours());
	}

	
}
