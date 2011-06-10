package org.openedit.tracker;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.openedit.OpenEditException;
import com.openedit.util.IntCounter;

public class BaseJobProjectNumberCreator
{
	protected File fieldCounterFile;
	protected IntCounter fieldIdCounter;
	protected DateFormat fieldSaveDateFormat;
	
	//Year, Month, Day, Progression (starting at 01 daily) 
	public synchronized String createNextProjectNumber(TimeZone zone) throws OpenEditException
	{
		GregorianCalendar cal = new GregorianCalendar(zone);
		getSaveDateFormat().setTimeZone(zone); //This is an annoying requirement in the JDK
		String formated = getSaveDateFormat().format(cal.getTime());
		
		//look up the daily number?
		int count = getIdCounter().incrementCount();
		if( count == 0 || !String.valueOf(count).startsWith(formated))
		{
			count = Integer.parseInt(formated + "1");
			try
			{
				getIdCounter().saveCount(count);
			}
			catch (IOException e)
			{
				throw new OpenEditException(e);
			}
		}
		return String.valueOf(count);
	}
	
	public IntCounter getIdCounter()
	{
		if( fieldIdCounter == null)
		{
			fieldIdCounter = new IntCounter();
//			File upload = new File( getRoot(), "/" + getCatalogId() + "/data/jobs/jobid.properties");
//			upload.getParentFile().mkdirs();
			fieldIdCounter.setCounterFile(getCounterFile());
		}
		return fieldIdCounter;
	}

	public void setIdCounter(IntCounter inIdCounter)
	{
		fieldIdCounter = inIdCounter;
	}

	public DateFormat getSaveDateFormat()
	{
		if( fieldSaveDateFormat == null)
		{
			fieldSaveDateFormat = new SimpleDateFormat("yyyyMMdd");
		}
		return fieldSaveDateFormat;
	}

	public void setSaveDateFormat(DateFormat inSaveDateFormat)
	{
		fieldSaveDateFormat = inSaveDateFormat;
	}

	public File getCounterFile()
	{
		return fieldCounterFile;
	}

	public void setCounterFile(File inCounterFile)
	{
		fieldCounterFile = inCounterFile;
	}

	
}
