package org.openedit.tracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.ListOrderedMap;
import org.openedit.Data;

import com.openedit.users.Group;
import com.openedit.users.User;

public class Job implements Data
{
	protected String fieldId;
	
	protected List fieldTodos;
	protected Map fieldProperties;
	protected boolean fieldExist;
	protected List fieldGroups;
	protected List fieldUserEntries;
	protected List fieldClientList;
	protected int priority;
	protected Budget fieldBudget;
	protected List fieldPendingUsers;
	
	//protected String createdby;
	
	public void addPendingUser(String inEmail)
	{
		getPendingUsers().add(inEmail);
	}
	public void removePendingUser(String inEmail)
	{
		if(getPendingUsers().contains(inEmail))
		{
			getPendingUsers().remove(inEmail);
		}
	}
	public List getPendingUsers()
	{
		if(fieldPendingUsers == null)
		{
			fieldPendingUsers = new ArrayList();
		}
		return fieldPendingUsers;
	}
	public void setTodos(List inTodos) {
		fieldTodos = inTodos;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}
	
	public String getAssignedTo()
	{
		return get("assignedto");
	}
	
	public String getCreatedByUser()
	{
		return get("createdby");
	}

	public void setCreatedByUser(String inCreatedByUser)
	{
		setProperty("createdby", inCreatedByUser);
	}
	
	public String getId()
	{
		return fieldId;
	}

	public void setId(String inId)
	{
		fieldId = inId;
	}

	public void addTodo(Todo inChange)
	{
		inChange.setJobId(getId());
		if( getTodos().size() > 0)
		{
			getTodos().add(0,inChange);
		}
		else
		{
			getTodos().add(inChange);
		}
	}
	public List getRequiredTodos()
	{
		List todos = new ArrayList(getTodos().size());
		for (Iterator iterator = getTodos().iterator(); iterator.hasNext();)
		{
			Todo todo = (Todo) iterator.next();
			if( todo.isRequirement() )
			{
				todos.add(todo);
			}
		}
		Collections.sort(todos);
		return todos;
	}
	
	public List getInProgressTodos()
	{
		List todos = new ArrayList(getTodos().size());
		for (Iterator iterator = getTodos().iterator(); iterator.hasNext();)
		{
			Todo todo = (Todo) iterator.next();
			if( todo.isRequirement() || todo.isInProgress() ){
				todos.add(todo);
			}
		}
		Collections.sort(todos);
		return todos;
	}
	
	public List getClosedTodos()
	{
		List todos = new ArrayList(getTodos().size());
		for (Iterator iterator = getTodos().iterator(); iterator.hasNext();)
		{
			Todo todo = (Todo) iterator.next();
			if( !todo.isRequirement() &&  todo.isInClosed() )
			{
				todos.add(todo);
			}
		}
		Collections.sort(todos);
		return todos;
	}
	public List getTodos()
	{
		if (fieldTodos == null)
		{
			fieldTodos = new ArrayList();
		}
		return fieldTodos;
	}
	public String get(String inKey)
	{
		if( inKey.equals("priority"))
		{
			return String.valueOf(getPriority());
		}
		if( inKey.equals("id"))
		{
			return getId();
		}
		if( inKey.equals("open"))
		{
			return String.valueOf(isOpen());
		}
		if( inKey.equals("assignedto"))
		{
			String user = (String)getProperties().get(inKey);
			if( user == null)
			{
				user = getCreatedByUser();
			}
			return user;
		}
		if( inKey.equals("sourcepath"))
		{
			return getSourcePath();
		}
		
		
		return (String)getProperties().get(inKey);
	}
	public void addProperty(String inKey, String inValue)
	{
		if( inKey.equals("priority"))
		{
			if( inValue == null)
			{
				setPriority(0);
			}
			else
			{
				setPriority(Integer.parseInt(inValue));
			}
		}
		else
		{
			getProperties().put(inKey, inValue);
		}
	}
	public Map getProperties()
	{
		if (fieldProperties == null)
		{
			fieldProperties = ListOrderedMap.decorate(new HashMap());
		}
		return fieldProperties;
	}
	public boolean isOpen()
	{
		String val = get("jobopen");
		return Boolean.parseBoolean(val);
	}
	public void setOpen(boolean inOpen)
	{
		setProperty("jobopen",String.valueOf(inOpen));
	}
	
	public double getHours()
	{
		double time = 0;
		for (Iterator iterator = getTodos().iterator(); iterator.hasNext();)
		{
			Todo task = (Todo) iterator.next();
			time = time + task.getHours();
		}
		return time;//NumberFormat.getNumberInstance().format(time);
	}
	public double addNumbers(String inProperty)
	{
		double total = 0;
		for (Iterator iterator = getTodos().iterator(); iterator.hasNext();)
		{
			Todo task = (Todo) iterator.next();
			String val = task.get(inProperty);
			if( val != null && val.length() > 0 && Character.isDigit(val.charAt(0)))
			{
				total = total + Double.parseDouble(val);
			}
		}
		return total;//NumberFormat.getNumberInstance().format(time);
	}

	public boolean isExist()
	{
		return fieldExist;
	}

	public void setExist(boolean inExist)
	{
		fieldExist = inExist;
	}

	public void setProperty(String inId, String inValue)
	{
		if( inId.equals("priority"))
		{
			if( inValue == null )
			{
				setPriority(0);
			}
			else
			{
				setPriority(Integer.parseInt(inValue));
			}
		}
		else
		{
			getProperties().put(inId,inValue);
		}
	}

	public String getName()
	{
		return get("name");
	}

	public void setName(String inName)
	{
		setProperty("name", inName);
	}

	public Todo getTodo(String inDelid)
	{
		for (Iterator iterator = getTodos().iterator(); iterator.hasNext();)
		{
			Todo dev = (Todo) iterator.next();
			if( dev.getId().equals(inDelid))
			{
				return dev;
			}
		}
		return null;
	}
	
	public void addUserEntry(User inUser)
	{
		JobUserEntry jue = new JobUserEntry();
		jue.setUser(inUser);
		addUserEntry(jue);
	}
	public void addUserEntry(JobUserEntry inUser )
	{
		if( inUser != null && !getUserEntries().contains(inUser))
		{
			getUserEntries().add( inUser);
			int subscribercount = getSubscriberCount();
			setSubscriberCount(++subscribercount);
		}
		
	}
	
	private void setSubscriberCount(int inCount)
	{
		setProperty("subscribercount", String.valueOf(inCount));
	}
	public int getSubscriberCount()
	{
		String count = get("subscribercount");
		if(count != null && count.length()>0)
		{
			return Integer.parseInt(count);
		}
		return 0;
	}
	public List getUserEntries() {
		if (fieldUserEntries == null) {
			fieldUserEntries = new ArrayList();
			
		}

		return fieldUserEntries;
	}
		
	public boolean hasUser(String inUserName)
	{
		for (Iterator iterator = getUserEntries().iterator(); iterator.hasNext();)
		{
			JobUserEntry user = (JobUserEntry) iterator.next();
			
			if( user.getUser() != null && user.getUser().getUserName().equals(inUserName))
			{
				return true;
			}
		}
		return false;
	}
	public void setUsers(List inUsers) {
		fieldUserEntries = inUsers;
	}

	public void addGroup(Group inGroup )
	{
		if( inGroup != null && !getGroups().contains(inGroup))
		{
			getGroups().add( inGroup);
		}
	}
	public List getGroups() {
		if( fieldGroups == null)
		{
			fieldGroups = new ArrayList();
		}
		return fieldGroups;
	}
	public boolean hasGroup(String inGroupId)
	{
		for (Iterator iterator = getGroups().iterator(); iterator.hasNext();)
		{
			Group name = (Group) iterator.next();
			if( name.getId().equals(inGroupId))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean hasMember(User inUser)
	{
		if(inUser == null)
		{
			return false;
		}
		for (Iterator iterator = getUserEntries().iterator(); iterator.hasNext();)
		{
			JobUserEntry jue = (JobUserEntry) iterator.next();
			if( jue.getUser().getUserName().equals(inUser.getUserName()))
			{
				return true;
			}
		}
		for (Iterator iterator = getGroups().iterator(); iterator.hasNext();)
		{
			Group name = (Group) iterator.next();
			if( inUser.isInGroup(name))
			{
				return true;
			}
		}
		if(inUser.getUserName().equals(getAssignedTo()))
		{
			return true;
		}
		return false;
		
		
	}
	public void setGroups(List inGroups) {
		fieldGroups = inGroups;
	}
	


	public void clearUsers() {
		fieldUserEntries = new ArrayList();
		
	}
	
	public List getTodosByStatus(String inStatus){
		ArrayList list = new ArrayList();
		for (Iterator iterator = getTodos().iterator(); iterator.hasNext();) {
			Todo todo = (Todo) iterator.next();
			if(todo.getLastStatus().getType().equals(inStatus)){
				list.add(todo);
			}
		}
		return list;
	}
	
	public int getToDoStatusCount(String inStatus){
		ArrayList list = new ArrayList();
		for (Iterator iterator = getTodos().iterator(); iterator.hasNext();) {
			Todo todo = (Todo) iterator.next();
			if(todo.getLastStatus().getType().equals(inStatus)){
				list.add(todo);
			}
		}
		return list.size();
	}
	
	
	public boolean canEdit(User inUser) 
	{
		return isEditor(inUser);
	}	
	public boolean isEditor(User inUser) 
	{
		if(inUser == null)
		{
			return false;
		}
		if( inUser.getUserName().equals( getAssignedTo()))
		{
			return true;
		}
		if(inUser.getId().equals(get("createdby")))
		{
			return true;
		}
		for (Iterator iterator = getUserEntries().iterator(); iterator.hasNext();) {
			JobUserEntry entry = (JobUserEntry) iterator.next();
			if(entry.getUser().getUserName().equals(inUser.getUserName())){
				return entry.isEditor();
			}
		}
		for (Iterator iterator = getGroups().iterator(); iterator.hasNext();)
		{
			Group group = (Group) iterator.next();
			if( inUser.isInGroup(group) )
			{
				return true;
			}
		}
		
		return false;
	}

	public Budget getBudget()
	{
		return fieldBudget;
	}

	public void setBudget(Budget inBudget)
	{
		fieldBudget = inBudget;
	}
	
	
	public void removeUserEntry(User inUser)
	{
		for (Iterator iterator = getUserEntries().iterator(); iterator.hasNext();) {
			JobUserEntry entry = (JobUserEntry) iterator.next();
			if(entry.getUser().getUserName().equals(inUser.getUserName()))
			{
				getUserEntries().remove(entry);
				int count = getSubscriberCount();
				setSubscriberCount(--count);
				return;
			}
		}
	}
	
	public JobUserEntry setEditor(User inUser, boolean inEditor)
	{
		for (Iterator iterator = getUserEntries().iterator(); iterator.hasNext();) {
			JobUserEntry entry = (JobUserEntry) iterator.next();
			if(entry.getUser().getUserName().equals(inUser.getUserName()))
			{
				entry.setEditor(inEditor);
				return entry;
			}
		}
		return null;
	}

	public String getSourcePath()
	{
		// TODO Auto-generated method stub
		return "jobs/" + getId();
	}

	public void setSourcePath(String inSourcepath)
	{
		// TODO Auto-generated method stub
		
	}
	
	public String listPermissions()
	{
		String hidden = get("hidden");
		if( hidden == null || "false".equals(hidden))
		{
			return "true"; //dont need security since it is open
		}
		
		StringBuffer permissions= new StringBuffer();
		permissions.append(getCreatedByUser());
		permissions.append(' ');
		for (Iterator iterator = getUserEntries().iterator(); iterator.hasNext();)
		{
			JobUserEntry entry = (JobUserEntry) iterator.next();
			permissions.append("user" + entry.getUser().getUserName());
			permissions.append(' ');
		}
		String owner = getCreatedByUser();
		if( owner != null)
		{
			permissions.append("user" + owner);
			permissions.append(' ');
		}
		if( fieldGroups != null)
		{
			for (Iterator iterator = getGroups().iterator(); iterator.hasNext();)
			{
				Group group = (Group) iterator.next();
				permissions.append(group.getId());
				permissions.append(' ');
			}
		}
		return permissions.toString();
	}
	public java.util.Date getFirstTodoDate(List inTodos)
	{
		java.util.Date first = null;
		for (Iterator iterator = inTodos.iterator(); iterator.hasNext();)
		{
			Todo todo = (Todo) iterator.next();
			java.util.Date dated = todo.getFirstStatus().getDated();
			if( first == null || first.after( dated))
			{
				first = dated;
			}
		}
		return first;
	}
	public java.util.Date getLastTodoDate(List inTodos)
	{
		java.util.Date last = null;
		for (Iterator iterator = inTodos.iterator(); iterator.hasNext();)
		{
			Todo todo = (Todo) iterator.next();
			java.util.Date dated = todo.getLastStatus().getDated();
			if( last == null || last.before( dated))
			{
				last = dated;
			}
		}
		return last;
	}
	@Override
	public void setProperties(Map<String, String> inProperties)
	{
		fieldProperties = inProperties;
		
	}
}
