package org.openedit.tracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.ListOrderedMap;
import org.openedit.Data;
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException;
import com.openedit.OpenEditRuntimeException;
import com.openedit.users.Group;

public class Todo implements Comparable, Data {
	protected String fieldId;
	protected String fieldJobId;
	protected List fieldStatusChanges;
	protected Map fieldProperties;
	protected List fieldTimeEntries;
	protected JobTimeArchive fieldJobTimeArchive;
	protected int rank;
	protected String createdby;
	protected Date fieldDueDate;

	public Date getDueDate() {
		return fieldDueDate;
	}

	public void setDueDate(Date inDueDate) {
		fieldDueDate = inDueDate;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public String getCreatedByUser() {
		return createdby;
	}

	public void setCreatedByUser(String createdby) {
		this.createdby = createdby;
	}

	public boolean isRequirement() {
		return Boolean.parseBoolean(get("required"));
	}

	public List getTimeEntries() {
		if (fieldTimeEntries == null && getJobTimeEntryArchive() != null) {
			try {
				fieldTimeEntries = getJobTimeEntryArchive().getTimeEntries(this);
			} catch (OpenEditException e) {
				throw new OpenEditRuntimeException(e);
			}
		}
		return fieldTimeEntries;
	}

	public void setTimeEntries(List inTimeEntries) {
		fieldTimeEntries = inTimeEntries;
	}

	public void addTimeEntry(JobTimeEntry entry) throws OpenEditException {
		getTimeEntries().add(entry);
		Collections.sort(getTimeEntries(), new Comparator() {
			public int compare(Object o1, Object o2) {
				JobTimeEntry e1 = (JobTimeEntry) o1, e2 = (JobTimeEntry) o2;
				return e1.getDate().compareTo(e2.getDate());
			}

		});
		saveTime();
	}

	public void saveTime() throws OpenEditException {
		getJobTimeEntryArchive().save(this);
	}

	public void removeTimeEntry(JobTimeEntry entry) throws OpenEditException {
		getTimeEntries().remove(entry);
		saveTime();
	}

	public String getId() {
		return fieldId;
	}

	public void setId(String inId) {
		fieldId = inId;
	}

	public void addStatusChange(StatusChange inChange) {
		StatusChange change = getLastStatus();
		if (change != null) {
			if (inChange.getType() == null || inChange.getType().equals(change.getType())) {
				if (change.getNote() == null && inChange.getNote() == null) {
					return;
				}
				if (change.getNote() != null && change.getNote().equals(inChange.getNote())) {
					return;
				}
			}
		}
		getStatusChanges().add(inChange);

	}

	public List getStatusChanges() {
		if (fieldStatusChanges == null) {
			fieldStatusChanges = new ArrayList();
		}
		return fieldStatusChanges;
	}

	public StatusChange getFirstStatus() {
		if (getStatusChanges().size() > 0) {
			StatusChange change = (StatusChange) getStatusChanges().get(0);
			return change;
		}
		return null;
	}

	public StatusChange getFirstStatus(String type) {
		if (type == null) {
			return null;
		}
		for (Iterator iterator = getStatusChanges().iterator(); iterator.hasNext();) {
			StatusChange change = (StatusChange) iterator.next();
			if (type.equals(change.getType())) {
				return change;
			}
		}
		return null;
	}

	public StatusChange getFirstStatusInGroup(String inGroup) {
		Group admingroup = getJobTimeEntryArchive().getUserManager().getGroup(inGroup);
		for (Iterator iterator = getStatusChanges().iterator(); iterator.hasNext();) {
			StatusChange change = (StatusChange) iterator.next();
			if (change.getUser().isInGroup(admingroup)) {
				return change;
			}
		}
		return null;
	}

	public StatusChange getLastStatus() {
		if (getStatusChanges().size() > 0) {
			StatusChange change = (StatusChange) getStatusChanges().get(getStatusChanges().size() - 1);
			return change;
		}
		return null;
	}

	public Object getProperty(String inId) {
		// TODO Auto-generated method stub
		return getProperties().get(inId);
	}

	public String getName() {
		return get("name");
	}

	public void setName(String inName) {
		setProperty("name", inName);
	}

	public Map getProperties() {
		if (fieldProperties == null) {
			fieldProperties = ListOrderedMap.decorate(new HashMap());

		}
		return fieldProperties;
	}

	public void setProperties(Map inProperties) {
		fieldProperties = inProperties;
	}

	public void setStatusChanges(List inStatusChanges) {
		fieldStatusChanges = inStatusChanges;
	}

	public String get(String inKey) {
		if (inKey != null) {
			if ("todolaststatus".equals(inKey)) {
				StatusChange change = getLastStatus();
				return change == null ? null : change.getType();
			} else if ("todolaststatusdate".equals(inKey)) {
				StatusChange change = getLastStatus();
				return change == null ? null : change.getFormatedDate();
			} else if ("todolaststatususer".equals(inKey)) {
				StatusChange change = getLastStatus();
				if (change != null && change.getUser() != null) {
					return change.getUser().getUserName();
				}
			} else if ("hours".equals(inKey)) {
				return String.valueOf(getHours());
			} else if ("rank".equals(inKey)) {
				return String.valueOf(getRank());
			} else if ("jobid".equals(inKey)) {
				return getJobId();
			}
		}
		return (String) getProperties().get(inKey);
	}

	public JobTimeArchive getJobTimeEntryArchive() {
		return fieldJobTimeArchive;
	}

	public void setJobTimeEntryArchive(JobTimeArchive inJobTimeEntryArchive) {
		fieldJobTimeArchive = inJobTimeEntryArchive;
	}

	public double getHours() {
		double time = 0;
		for (Iterator iterator2 = getTimeEntries().iterator(); iterator2.hasNext();) {
			JobTimeEntry entry = (JobTimeEntry) iterator2.next();

			time = time + entry.getHours();
		}
		return time;
	}

	public void clearTime() throws OpenEditException {
		setTimeEntries(new ArrayList());
	}

	public String getJobId() {
		return fieldJobId;
	}

	public void setJobId(String inJobId) {
		fieldJobId = inJobId;
	}

	public JobTimeEntry getTimeEntry(String inEntryId) {
		for (Iterator iterator = getTimeEntries().iterator(); iterator.hasNext();) {
			JobTimeEntry entry = (JobTimeEntry) iterator.next();
			if (entry.getId().equals(inEntryId)) {
				return entry;
			}
		}
		return null;
	}

	public StatusChange getStatusChange(String inEntryId) {
		for (Iterator iterator = getStatusChanges().iterator(); iterator.hasNext();) {
			StatusChange entry = (StatusChange) iterator.next();
			if (entry.getId().equals(inEntryId)) {
				return entry;
			}
		}
		return null;
	}

	public void removeStatusChange(StatusChange entry) throws OpenEditException {
		getStatusChanges().remove(entry);
	}

	public int compareTo(Object o2) {
		if (o2 instanceof Todo) {
			Todo t2 = (Todo) o2;

			Date duedate = getDueDate();
			Date duedate2 = t2.getDueDate();
			if (duedate != null && duedate2 != null) {
				return duedate.compareTo(duedate2);
			}

			if (getRank() > t2.getRank()) {
				return 1;
			}
			if (getRank() < t2.getRank()) {
				return -1;
			}
		}
		return 0;
	}

	public boolean isInProgress() {
		StatusChange change = getLastStatus();
		if (change != null && !change.isClosed() && change.getType() != null) {
			return true;
		}
		return false;
	}

	public boolean isInClosed() {
		StatusChange change = getLastStatus();
		if (change != null && change.isClosed()) {
			return true;
		}
		return false;
	}

	public List asList() {
		List all = new ArrayList();
		all.add(this);
		return all;

	}

	public void setSourcePath(String inSourcepath) {
	}

	public String getSourcePath() {
		return getJobId() + "/" + getId();
	}

	public void setProperty(String inId, String inValue) {
		if (inId.equals("duedate")) {
			if (inValue == null) {
				setDueDate(null);
			} else {
				setDueDate(DateStorageUtil.getStorageUtil().parseFromStorage(inValue));
			}
		}

		if (inId.equals("rank")) {
			if (inValue != null) {
				setRank(Integer.parseInt(inValue.toString()));
			} else {
				setRank(0);
			}
		}

		if (inValue == null) {
			getProperties().remove(inId);
		} else {
			getProperties().put(inId, inValue);
		}
	}
}
