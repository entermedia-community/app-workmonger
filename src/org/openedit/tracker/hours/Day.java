package org.openedit.tracker.hours;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openedit.tracker.Job;
import org.openedit.tracker.JobTimeEntry;
import org.openedit.tracker.Todo;

public class Day
{
	protected List fieldRows;

	public List getRows()
	{
		if (fieldRows == null)
		{
			fieldRows = new ArrayList();
		}
		return fieldRows;
	}

	public void setRows(List inHours)
	{
		fieldRows = inHours;
	}
	public void add(double inHours)
	{
		getRows().add(new Double(inHours));
	}

	public void addRow(JobTimeEntry inTimeEntry, Job inJob, Todo inTodo)
	{
		Row row = new Row();
		row.setJob(inJob);
		row.setTimeEntry(inTimeEntry);
		row.setTodo(inTodo);
		getRows().add(row);
	}
	public double getHours()
	{
		double hours = 0;
		for (Iterator iterator = getRows().iterator(); iterator.hasNext();)
		{
			Row row = (Row) iterator.next();
			hours = hours + row.getTimeEntry().getHours();
		}
		return hours;
	}
	
}
