package org.openedit.tracker;

import com.openedit.users.User;

public class JobUserEntry  {

	
	protected User user;
	protected boolean fieldEditor;
	
	
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	public boolean isEditor()
	{
		return fieldEditor;
	}
	public void setEditor(boolean inEditor)
	{
		fieldEditor = inEditor;
	}
	
}
