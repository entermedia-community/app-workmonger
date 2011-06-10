package org.openedit.jobtracking;

import java.util.Date;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.tracker.BaseJobArchive;
import org.openedit.tracker.BaseJobTrackingModule;
import org.openedit.tracker.Job;
import org.openedit.tracker.JobTimeEntry;
import org.openedit.tracker.JobTracker;
import org.openedit.tracker.StatusChange;
import org.openedit.tracker.Todo;
import org.openedit.tracker.modules.JobTrackingModule;

import com.openedit.BaseTestCase;
import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.users.User;

public class JobTest extends BaseTestCase
{
	private static final Log log = LogFactory.getLog(JobTest.class);
	
	public JobTest(String inName)
	{
		super(inName);
		// TODO Auto-generated constructor stub
	}

	public void testSave() throws Exception
	{
		BaseJobTrackingModule mod = (BaseJobTrackingModule) getBean("JobTracking");
		WebPageRequest req = getFixture().createPageRequest("/testcatalog/admin/jobs/index.html");
		JobTracker tracker = mod.getJobTracker(req);
		Job job = getTestJob(req, tracker);
	
		assertNotNull(job.getId());
		HitTracker hits = tracker.getJobSearcher().search("id:testjob");
		assertTrue(hits.size() > 0);
		tracker.getJobArchive().clear();
		Job job2 = tracker.getJobArchive().getJobById(job.getId());
		assertNotNull(job2);
		assertNotNull(job2.get("shortdescription"));
		assertEquals(job.get("shortdescription"), job2.get("shortdescription"));
		
		
	}

	public void testTime() throws Exception
	{
		BaseJobTrackingModule mod = (BaseJobTrackingModule) getBean("JobTracking");
		WebPageRequest req = getFixture().createPageRequest("/testcatalog/admin/jobs/index.html");
		JobTracker tracker = mod.getJobTracker(req);
		Job job = getTestJob(req, tracker);
		Todo del = job.getTodo("testjobpocket");
		StatusChange change = new StatusChange();
		change.setDated(new Date());
		change.setUser(req.getUser());
		change.setNote("Waiting for Jim");
		change.setType("1");
		del.addStatusChange(change);

		User user = req.getUser();
		del.clearTime();

		JobTimeEntry entry = new JobTimeEntry();
		entry.setDate(new Date());
		entry.setDescription("Started project");
		entry.setHours(1);
		entry.setUser(user);
		del.addTimeEntry(entry);
		tracker.getJobArchive().save(job, req.getUser());

		entry = new JobTimeEntry();
		entry.setDate(new Date());
		entry.setDescription("Finished project");
		entry.setHours(1.5);
		entry.setUser(user);

		del.addTimeEntry(entry);
		// save?

		tracker.getJobArchive().clear();

		job = tracker.getJobArchive().getJobById("testjob");
		del = job.getTodo("testjobpocket");
		assertEquals(del.getTimeEntries().size(), 2);
		//double hours = del.getHours();
		assertEquals(String.valueOf(del.getHours()), "2.5");

		assertTrue(del.getStatusChanges().size() > 0);
	}

	private Job getTestJob(WebPageRequest req, JobTracker tracker) throws OpenEditException
	{
		Job job = tracker.getJobArchive().getJobById("testjob");
		if( job == null)
		{
			job = tracker.getJobArchive().createJob("testjob");
		}
		job.addProperty("status", "open");
		job.addProperty("shortdescription", "Short Descri[p");
		job.setName("RMCC Aurora");
		Todo deliv = job.getTodo("testjobpocket");
		if (deliv == null)
		{
			deliv = new Todo();
			deliv.setId("testjobpocket");
			deliv.setName("Pocket Calendar");
			deliv.setProperty("quantity", "100/20");
			deliv.setProperty("costofprinting", "32343");
			job.addTodo(deliv);			
		}
		//clear out old status changes for the test
		deliv.setStatusChanges(null);
		tracker.getJobArchive().save(job, req.getUser());
		tracker.getJobSearcher().updateIndex(job);
		return job;
	}

	public void testReindex() throws Exception
	{
		JobTrackingModule mod = (JobTrackingModule) getBean("JobTracking");
		WebPageRequest req = getFixture().createPageRequest("/testcatalog/admin/jobs/index.html");
		mod.reindex(req);
	}

//	public void testList() throws Exception
//	{
//		JobTrackingModule mod = (JobTrackingModule) getBean("JobTracking");
//
//		WebPageRequest req = getFixture().createPageRequest("/testcatalog/admin/jobs/index.html");
//		HitTracker tracker = mod.getJobList(req);
//		assertNotNull(tracker);
//
//		req = getFixture().createPageRequest("/testcatalog/admin/jobs/data/1234.html");
//		Job job = mod.loadJob(req);
//		assertNotNull(job);
//	}

	public void testLoadSearchForm() throws Exception
	{
		BaseJobTrackingModule mod = (BaseJobTrackingModule) getBean("JobTracking");
		WebPageRequest req = getFixture().createPageRequest("/testcatalog/admin/jobs/index.html");

		BaseJobArchive archive = mod.loadJobArchive(req);
		List fields = archive.getDataProperties("jobsearch", req.getUser());
		assertNotNull(fields);
	}
	
	public void testNewJob() throws Exception
	{
		JobTrackingModule mod = (JobTrackingModule) getBean("JobTracking");
		WebPageRequest req = getFixture().createPageRequest("/testcatalog/admin/jobs/index.html");
		mod.createNewJob(req);
		Job job = (Job)req.getPageValue("job");
		assertNotNull(job);
		assertNotNull(job.getName());
		log.info(job.getName());
		assertTrue(job.getName().length() > 5);
		assertTrue(job.getName().startsWith("201"));		
	}
	public void testTodo() throws Exception
	{
		BaseJobTrackingModule mod = (BaseJobTrackingModule) getBean("JobTracking");
		WebPageRequest req = getFixture().createPageRequest("/testcatalog/admin/jobs/index.html");
		JobTracker tracker = mod.getJobTracker(req);
		Job job = getTestJob(req, tracker);
		Todo todo = job.getTodo("testjobpocket");
		StatusChange change = new StatusChange();
		change.setDated(new Date());
		change.setNote("New\nLine .");
		change.setId("someid");
		change.setUser(req.getUser());
		todo.addStatusChange(change);
		tracker.getJobArchive().save(job,req.getUser());
		
		tracker.getJobArchive().clear();
		Job newjob = tracker.getJobArchive().getJobById("testjob");
		Todo newtodo = newjob.getTodo("testjobpocket");
		assertEquals(newtodo.getLastStatus().getNote(),"New\nLine .");
		
	}
	
	public void xxxtestImportJobs() throws Exception
	{
		System.setProperty("oe.root.path", "./webapp");
		BaseJobTrackingModule mod = (BaseJobTrackingModule) getBean("JobTracking");
		WebPageRequest req = getFixture().createPageRequest("/testcatalog/admin/csvimport.html");
		JobTracker tracker = mod.getJobTracker(req);
		req.putPageValue("tracker", tracker);
		getFixture().getEngine().executePageActions(req);

		
		Job job = tracker.getJobArchive().loadJob("newJobId");
		assertNotNull(job);
		assertEquals("Test Job", job.getName());
		assertEquals(1, job.getPriority());
		assertTrue(job.isExist());
		assertTrue(job.isOpen());
		assertEquals("Test client", job.get("client"));
		assertEquals("project", job.get("jobtype"));
		assertEquals("Short Desc", job.get("shortdescription"));
		assertEquals("Contact name", job.get("contact"));
		assertEquals("admin", job.get("assignedto"));
	}

}