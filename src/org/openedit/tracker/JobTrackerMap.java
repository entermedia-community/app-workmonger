package org.openedit.tracker;

import java.util.HashMap;
import java.util.Map;

public class JobTrackerMap {

	protected Map fieldJobTrackers;

	public Map getJobTrackers() {
	if (fieldJobTrackers == null) {
		fieldJobTrackers = new HashMap();
		
	}

	return fieldJobTrackers;
	}

	public void setJobTrackers(Map inJobTrackers) {
		fieldJobTrackers = inJobTrackers;
	}

	public JobTracker get(String inCatalogId) {
	return (JobTracker)getJobTrackers().get(inCatalogId);
	}

	public void put(String inCatalogId, JobTracker inJobs) {
	getJobTrackers().put(inCatalogId, inJobs);
		
	}

	
	public void clear(){
		getJobTrackers().clear();
	}
	
}
