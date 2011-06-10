package org.openedit.tracker;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import com.openedit.events.Event;
import com.openedit.events.UserCalendar;
import com.openedit.events.UserCalendarArchive;
import com.openedit.page.Page;
import com.openedit.users.User;

public class Budget
{
	protected UserCalendarArchive fieldArchive;
	protected UserCalendar fieldUserCalendar;
	protected Page fieldBudgetDataFile;
	protected String fieldCatalogId;
	
	public UserCalendarArchive getArchive()
	{
		return fieldArchive;
	}

	public void setArchive(UserCalendarArchive inArchive)
	{
		fieldArchive = inArchive;
	}
	
	public UserCalendar getCalendar()
	{
		if( fieldUserCalendar == null)
		{
			fieldUserCalendar = getArchive().loadCalendar(getCatalogId(), getBudgetDataFile().getPath(), false);
			if( fieldUserCalendar.getSelectedCalendar() == null)
			{
				com.openedit.events.Calendar cal = new com.openedit.events.Calendar();
				cal.setPage(getBudgetDataFile());
				cal.setId(getBudgetDataFile().getId());
				fieldUserCalendar.addMixedInCalendar(cal);
			}
		}
		return fieldUserCalendar;
	}

	public void addHours(Todo inTodo, JobTimeEntry inEntry, boolean newrecord)
	{
		//Make sure it is not already in here
		if( inEntry.getId() == null)
		{
			return;
		}
		Event event = null;
		if( !newrecord )
		{
			event = getCalendar().getEvent(inEntry.getId());
		}
		if( event == null)
		{
			String text = inTodo.getName();
			if( inEntry.getDescription() != null && inEntry.getDescription().length() > 0)
			{
				text =  text + " (" + inEntry.getDescription() + ")";
			}
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(inEntry.getDate());
			long minutes = Math.round(inEntry.getHours() * 60D);
			cal.add(Calendar.MINUTE, (int)minutes);
			event = getCalendar().getSelectedCalendar().createEvent(inEntry.getDate(), cal.getTime(), text);
			if( inEntry.getUser() != null)
			{
				event.addProperty("username", inEntry.getUser().getUserName());
			}
			event.addProperty("todoid", inTodo.getId());
			event.addProperty("type", "hours");
			event.addProperty("amount", String.valueOf(inEntry.getHours()));
			event.addProperty("todoname", inTodo.getName());
			getCalendar().getSelectedCalendar().addEvent(event);
		}
	}

	public List findPastYearsRecords(int inYears)
	{
		GregorianCalendar past = new GregorianCalendar();
		past.add(Calendar.YEAR, 0 - inYears);
		Date start = past.getTime();
		
		past.add(Calendar.YEAR, inYears + 1);
		Date end = past.getTime();
		
		List events = getCalendar().findEvents(start, end);
		return events;
	}

	public List findRecordsFrom(Date inDate, Date inEndDate)
	{
		List events = getCalendar().findEvents(inDate, inEndDate);
		return events;
	}

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public Page getBudgetDataFile()
	{
		return fieldBudgetDataFile;
	}

	public void setBudgetDataFile(Page inBudgetDataFile)
	{
		fieldBudgetDataFile = inBudgetDataFile;
	}

	public float add(float inOne, String inTwo )
	{
		try
		{
			float value = inOne + Float.parseFloat(inTwo);
			return round( value, 2);
		}
		catch ( Exception ex)
		{
			return 0;
		}
		
	}
	public float subtract(float inOne, String inTwo )
	{
		try
		{
		float value = inOne - Float.parseFloat(inTwo);
		return round( value, 2);
		}
		catch ( Exception ex)
	{
		return 0;
	}
	}

	 /**
     * Round a double value to a specified number of decimal 
     * places.
     *
     * @param val the value to be rounded.
     * @param places the number of decimal places to round to.
     * @return val rounded to places decimal places.
     */
    public static float round(float val, int places) {
	long factor = (long)Math.pow(10,places);

	// Shift the decimal the correct number of places
	// to the right.
	val = val * factor;

	// Round to the nearest integer.
	long tmp = Math.round(val);

	// Shift the decimal the correct number of places
	// back to the left.
	return (float)tmp / factor;
    }

	public void clearHours()
	{
		List all = getCalendar().getSelectedCalendar().getAllEvents();
		for (Iterator iterator = all.iterator(); iterator.hasNext();)
		{
			Event event = (Event) iterator.next();
			if( "hours".equals(event.get("type")))
			{
				getCalendar().getSelectedCalendar().removeEvent(event);
			}
		}
	}
	public void addCredit(String inAmount, Date inDate, String inNote, User inUser)
	{
		Event event = getCalendar().getSelectedCalendar().createEvent(inDate,inDate, inNote);
		event.addProperty("type", "credit");
		event.addProperty("amount", inAmount);
		event.addProperty("username", inUser.getUserName());
		getCalendar().getSelectedCalendar().addEvent(event);
		getArchive().saveCalendar(getCalendar().getSelectedCalendar(), inUser);
	}
	public void addDebit(String inAmount, Date inDate, String inNote, User inUser)
	{
		Event event = getCalendar().getSelectedCalendar().createEvent(inDate,inDate, inNote);
		event.addProperty("type", "debit");
		event.addProperty("amount", inAmount);
		event.addProperty("username", inUser.getUserName());
		getCalendar().getSelectedCalendar().addEvent(event);
		getArchive().saveCalendar(getCalendar().getSelectedCalendar(), inUser);
	}

	public void removeEntry(String inEid, User inUser)
	{
		Event event = getCalendar().getSelectedCalendar().getEvent(inEid);
		getCalendar().getSelectedCalendar().removeEvent(event);
		getArchive().saveCalendar(getCalendar().getSelectedCalendar(), inUser);
		
	}
}
