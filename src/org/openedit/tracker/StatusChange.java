package org.openedit.tracker;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.openedit.users.User;

public class StatusChange
{
	protected String fieldId;
	protected String fieldType;
	protected User fieldUser;
	protected Date fieldDated;
	protected String fieldNote;
	protected String fieldAttachmentPath;
	
	//protected Map fieldProperties;
	
	public String getNote()
	{
		return fieldNote;
	}
	public String getNoteSnip(int inSize)
	{
		if( fieldNote != null)
		{
			String snip =  fieldNote.substring(0,Math.min(fieldNote.length(), inSize));
			if( snip.length() < fieldNote.length())
			{
				snip = snip + "...";
			}
			return snip;
		}
		return "";
	}
	public void setNote(String inNote)
	{
		fieldNote = inNote;
	}
	//protected 
	public String getId()
	{
		return fieldId;
	}
	public void setId(String inId)
	{
		fieldId = inId;
	}
	public String getType()
	{
		return fieldType;
	}
	public void setType(String inType)
	{
		fieldType = inType;
	}
	public User getUser()
	{
		return fieldUser;
	}
	public void setUser(User inUsername)
	{
		fieldUser= inUsername;
	}
	public Date getDated()
	{
		return fieldDated;
	}
	public void setDated(Date inDated)
	{
		fieldDated = inDated;
	}
	public String toString()
	{
		return getType();
	}
	public String getFormatedDate()
	{
		if( getDated() == null)
		{
			return "";
		}
		return SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT,SimpleDateFormat.SHORT).format(getDated());
	}
	public boolean isClosed()
	{
		return "closed".equals(getType());
	}
	public String getAttachmentPath()
	{
		return fieldAttachmentPath;
	}
	public void setAttachmentPath( String inPath)
	{
		fieldAttachmentPath = inPath;
	}
}
