package org.openedit.tracker.modules;

import org.openedit.tracker.BaseJobTrackingModule;
import org.openedit.tracker.JobArchive;

import com.openedit.WebPageRequest;

public class ChartModule extends BaseJobTrackingModule
{
	public void loadMonthlyIssues(WebPageRequest inReq) throws Exception
	{
		JobArchive archive = getJobArchive(inReq);
		
	}
}
