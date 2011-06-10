/*
 * Created on Mar 24, 2004
 */
package com.openedit.tracker;
import org.openedit.jobtracking.JobTest;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author jguerre
 * 
 */
public class AllTests
{
	public static Test suite()
	{
		TestSuite suite = new TestSuite("tracker Test");
		//$JUnit-BEGIN$
		/*
		String base = System.getProperty("oe.root.path");
		if ( base == null)
		{
			System.setProperty("oe.root.path","webapp");
		}
		*/
		suite.addTestSuite(JobTest.class);
		//$JUnit-END$
		return suite;
	}
}
