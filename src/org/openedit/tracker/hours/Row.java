package org.openedit.tracker.hours;

import org.openedit.tracker.Job;
import org.openedit.tracker.JobTimeEntry;
import org.openedit.tracker.Todo;

public class Row
{
	protected JobTimeEntry fieldTimeEntry;
	protected Job fieldJob;
	protected Todo fieldTodo;
	public JobTimeEntry getTimeEntry()
	{
		return fieldTimeEntry;
	}
	public void setTimeEntry(JobTimeEntry inTimeEntry)
	{
		fieldTimeEntry = inTimeEntry;
	}
	public Job getJob()
	{
		return fieldJob;
	}
	public void setJob(Job inJob)
	{
		fieldJob = inJob;
	}
	public Todo getTodo()
	{
		return fieldTodo;
	}
	public void setTodo(Todo inTodo)
	{
		fieldTodo = inTodo;
	}
}
